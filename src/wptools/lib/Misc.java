/*
 * This software is distributed under the Creative Commons Attribution 4.0
 * International license. See LICENSE.TXT in the main directory of this
 * repository for more information.
 */

package wptools.lib;

import org.apache.commons.cli.CommandLine;
import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.text.ParseException;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import javax.net.ssl.*;

/**
 * Miscellaneous stuff, not object-oriented.
 * 
 * @author David Barts <david.w.barts@gmail.com>
 *
 */
public class Misc {
	private static String myName;
	
	/* lengths of the various fingerprint types we support, in bytes */
	private static final int MD5_LEN = 16;
	private static final int SHA1_LEN = 20;
	private static final int SHA256_LEN = 32;

	public static String getMyName() {
		return myName;
	}

	public static void setMyName(String myName) {
		Misc.myName = myName;
	}

	/**
	 * Print an error message.
	 * @param message The error message to print
	 */
	public static void error(String message) {
		System.err.println(myName + ": " + message);
	}
	
	/**
	 * Print an error message and die with status 1.
	 * @param message The error message to print
	 */
	public static void die(String message) {
		die(message, 1);
	}
	
	/**
	 * Print an error message and die with the specified status.
	 * @param message Error message to print.
	 * @param estat Exit status.
	 */
	public static void die(String message, int estat) {
		error(message);
		System.exit(estat);
	}
	
	/**
	 * Bypass the normal SSL certificate authentication. If the passed
	 * fingerprint is null, bypasses all authentication (dangerous).
	 * Else trust anything whose chain contains a cert with the specified
	 * fingerprint.
	 * @param fing		Fingerprint
	 */
	public static void bypassSslAuth(final byte[] fing) {
		// Determine fingerprint type from its length
		final String type;
		if (fing == null) {
			type = null;
		} else {
			switch (fing.length) {
				case MD5_LEN:
					type = "MD5";
					break;
				case SHA1_LEN:
					type = "SHA-1";
					break;
				case SHA256_LEN:
					type = "SHA-256";
					break;
				default:
					throw new IllegalArgumentException("Invalid hash.");
			}
		}

	    // Create a trust manager
		TrustManager[] trustAllCerts = new TrustManager[] {
			new X509TrustManager() {
				public X509Certificate[] getAcceptedIssuers() {
					return null;
				}

				public void checkClientTrusted(X509Certificate[] certs, String authType) throws CertificateException {
					matchFing(certs);
				}

				public void checkServerTrusted(X509Certificate[] certs, String authType) throws CertificateException {
					matchFing(certs);
				}

				private void matchFing(X509Certificate[] certs) throws CertificateException {
					if (fing == null)
						return;
					MessageDigest md = null;
					try {
						md = MessageDigest.getInstance(type);
					} catch (NoSuchAlgorithmException e) {
						throw new CertificateException(e);
					}
					for (X509Certificate cert: certs) {
						md.reset();
						if (Arrays.equals(md.digest(cert.getEncoded()), fing))
							return;
					}
					throw new CertificateException("No matching fingerprint found.");
				}
			}
		};
	 
	    // Install the trust manager
	    SSLContext sc = null;
		try {
			sc = SSLContext.getInstance("SSL");
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		}
		
	    // Create empty HostnameVerifier
		HostnameVerifier hv = new HostnameVerifier() {
            public boolean verify(String arg0, SSLSession arg1) {
                    return true;
            }
        };

	    try {
			sc.init(null, trustAllCerts, new java.security.SecureRandom());
		} catch (KeyManagementException e) {
			throw new RuntimeException(e);
		}
	    HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
	    HttpsURLConnection.setDefaultHostnameVerifier(hv);
	}
	
	/**
	 * Given a URL, return an XmlRpcClient object.
	 * @param The URL, as a string.
	 * @throws MalformedURLException 
	 */
	public static XmlRpcClient xmlRpcService(String url, Properties props, CommandLine cmdLine) throws MalformedURLException {
		XmlRpcClientConfigImpl config = new XmlRpcClientConfigImpl();
		config.setServerURL(new URL(url));
		XmlRpcClient client = new XmlRpcClient();
		client.setConfig(config);
		if (cmdLine.hasOption("insecure"))
			bypassSslAuth(null);
		else if (props.hasKey("accept"))
			bypassSslAuth(parseFing(props.get("accept")));
		return client;
	}
	
	private static byte[] parseFing(String s) {
		String s2 = s.replaceAll(":", "");
		int len = s2.length();
		if (len != MD5_LEN*2 && len != SHA1_LEN*2 && len != SHA256_LEN*2)
			Misc.die("bad fingerprint: " + s);
		byte[] data = new byte[len / 2];
		for (int i = 0; i < len; i += 2) {
			int v0 = Character.digit(s2.charAt(i), 16);
			int v1 = Character.digit(s2.charAt(i+1), 16);
			if (v0 < 0 || v1 < 0)
				Misc.die("bad fingerprint: " + s);
			data[i/2] = (byte) ((v0 << 4) | v1);
		}
		return data;
	}

	/**
	 * Get the password to use; look at environment first.
	 * @return A string containing the password.
	 */
	public static String getPassword() {
		String ret = System.getenv("WPTOOLS_PASS");
		if (ret != null)
			return ret;
		return new String(System.console().readPassword("Password: ", (Object) null));
	}
	
	/**
	 * Format a date to an ISO8601 string
	 * @param d      Date to format.
	 * @param zone   Time zone to suffix it with
	 * @return       Formatted date
	 */
	public static String formatDate(Date d, String zone) {
		return String.format("%tY-%<tm-%<tdT%<tH:%<tM:%<tS%s", d,
			zone == null ? "" : zone);
	}
	
	// Formats we try, in order. These should be non-lenient, longest first.
	private static final SimpleDateFormat[] DATE_FORMATS = new SimpleDateFormat[] {
		new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss"),
		new SimpleDateFormat("yyyy-MM-dd'T'HH:mm"),
		new SimpleDateFormat("yyyy-MM-dd")
	};
	static {
		for (SimpleDateFormat f : DATE_FORMATS)
			f.setLenient(false);
	}
	
	/**
	 * Parse an ISO8601 date/time string
	 * @param unparsed  String containing date/time expression.
	 * @param zone      Time zone suffix to strip, if present.
	 * @return Date parsed into a java.util.Date object. Note
	 *         that the Date is only suitable for setting
	 *         post_date_gmt, not post_date.
	 */
	public static Date parseDate(String unparsed, String zone)
			throws ParseException {
		String munged = unparsed.toUpperCase();
		if (munged.endsWith(zone))
			munged = munged.substring(0, munged.length() - zone.length());
		for (SimpleDateFormat fmt : DATE_FORMATS) {
			ParsePosition pos = new ParsePosition(0);
			Date ret = fmt.parse(munged, pos);
			if (ret != null) {
				return ret;
			}
		}
		throw new ParseException("Unparseable date: " + unparsed, 0);
	}
	
	/**
	 * Truncate a string for printing.
	 * @param s      String
	 * @param length Length to truncate to
	 */
	public static String truncateString(String s, int length) {
		if (s.length() <= length)
			return s;
		return s.substring(0, length-1) + "\u2026";
	}
	
	/**
	 * Read a post body from someplace appropriate.
	 * @param cmdLine  Parsed command line
	 * @return         String containing read text
	 */
	public static String readBody(CommandLine cmdLine) {
		String fileName = cmdLine.getOptionValue("content");
		if (fileName == null) {
			if (System.console() == null)
				return readBodyFromFile(new InputStreamReader(System.in));
			else
				return readBodyFromTty();
		} else {
			try (FileReader fr = new FileReader(fileName)) {
				return readBodyFromFile(fr);
			} catch (IOException e) {
				Misc.die(e.getMessage());
			}
		}
		return null;  // stupid Java
	}
	
	/**
	 * Read a post body from a Reader object.
	 * @return         String containing read text
	 */
	private static int BUFSIZE = 1024;
	private static String readBodyFromFile(Reader rdr) {
		char[] buf = new char[BUFSIZE];
		StringBuilder ret = new StringBuilder();
		try {
			int nread;
			while ((nread = rdr.read(buf)) != -1)
				ret.append(buf, 0, nread);
		} catch (IOException e) {
			Misc.die(e.getMessage());
		}
		return ret.toString();
	}
	
	/**
	 * Read a post body from standard input when a TTY.
	 * @return         String containing read text
	 */
	private static String readBodyFromTty() {
		System.out.println("Enter message, end with EOF or '.' on a line by itself:");
		String nl = System.lineSeparator();
		StringBuilder ret = new StringBuilder();
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		String line;
		try {
			while ((line = br.readLine()) != null) {
				if (line.equals("."))
					break;
				else if (line.startsWith("."))
					line = line.substring(1);
				ret.append(line);
				ret.append(nl);
			}
		} catch (IOException e) {
			die(e.getMessage());
		}
		return ret.toString();
	}
}
