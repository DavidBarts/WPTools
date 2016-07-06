package wptools.lib;

import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.text.ParseException;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import javax.net.ssl.*;

/**
 * 
 * @author David Barts <david.w.barts@gmail.com>
 *
 * Miscellaneous stuff, not object-oriented.
 */
public class Misc {
	private static String myName;
	
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
	 * XXX - Disable all SSL authentication. Should be replaced by actual key management.
	 * Cribbed from https://ws.apache.org/xmlrpc/ssl.html
	 * @throws NoSuchAlgorithmException 
	 */
	public static void disableSslAuth() {
	    // Create a trust manager that does not validate certificate chains
	    TrustManager[] trustAllCerts = new TrustManager[] {
	        new X509TrustManager() {
	            public X509Certificate[] getAcceptedIssuers() {
	                return null;
	            }
	 
	            public void checkClientTrusted(X509Certificate[] certs, String authType) {
	                // Trust always
	            }
	 
	            public void checkServerTrusted(X509Certificate[] certs, String authType) {
	                // Trust always
	            }
	        }
	    };
	 
	    // Install the all-trusting trust manager
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
	public static XmlRpcClient xmlRpcService(String url) throws MalformedURLException {
		XmlRpcClientConfigImpl config = new XmlRpcClientConfigImpl();
		config.setServerURL(new URL(url));
		XmlRpcClient client = new XmlRpcClient();
		client.setConfig(config);
		return client;
	}
	
	/**
	 * Get the password to use; look at environment first.
	 * @return A string containing the password.
	 */
	public static String getPassword() {
		String ret = System.getenv("WPTOOLS_PASS");
		if (ret != null)
			return ret;
		return new String(System.console().readPassword("Password: ", null));
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
	 * Read a post body from standard input.
	 * @return         String containing read text
	 */
	public static String readBody() {
		boolean isTty = false;
		if (System.console() != null) {
			isTty = true;
			System.out.println("Enter message, end with EOF or '.' on a line by itself:");
		}
		String nl = System.lineSeparator();
		StringBuilder ret = new StringBuilder();
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		String line;
		try {
			while ((line = br.readLine()) != null) {
				if (isTty && line.equals("."))
					break;
				ret.append(line);
				ret.append(nl);
			}
		} catch (IOException e) {
			die(e.getMessage());
		}
		return ret.toString();
	}
}
