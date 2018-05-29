/*
 * This software is distributed under the Creative Commons Attribution 4.0
 * International license. See LICENSE.TXT in the main directory of this
 * repository for more information.
 */

package wptools.cmds;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Formatter;
import javax.net.ssl.*;

import org.apache.commons.cli.CommandLine;

import wptools.lib.*;

/**
 * Dump the X509 certificates presented to us.
 *
 * @author David Barts <david.w.barts@gmail.com>
 *
 */
public class DumpCerts {
    private static CommandLine cmdLine;

    private static final String[] FTYPES = { "MD5", "SHA-1", "SHA-256" };
    private static final int BUFSIZE = 65536;

    public static void main(String[] args) {
        // Define our name
        Misc.setMyName("DumpCerts");

        // Parse command-line options
        if (args.length != 1)
            Misc.die("expecting URL", 2);
        if ("-?".equals(args[0]) || "--help".equals(args[0])) {
            System.out.format("usage: %s -?|--help|url%n", Misc.getMyName());
            System.out.println(" -?,--help               Print this help message.");
            System.exit(0);
        }
        URL url = null;
        try {
            url = new URL(args[0]);
        } catch (MalformedURLException e) {
            Misc.die(e.getMessage(), 2);
        }

        // Install the dummy cert manager that dumps what it receives.
        installDummyCertManager();
        System.out.println();

        // Connect, make request, discard data (we just want the cert chain,
        // not the data at the URL).
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) url.openConnection();
            if (!(conn instanceof HttpsURLConnection))
                Misc.die(args[0] + " is not an HTTPS url");
            conn.setRequestMethod("GET");
            conn.setRequestProperty("User-Agent", Misc.getMyName());
            conn.setUseCaches(false);
            conn.connect();
            try (InputStream in = conn.getInputStream()) {
                byte[] buf = new byte[BUFSIZE];
                while (in.read(buf) >= 0)
                    ;
            }
        } catch (IOException e) {
            Misc.die(e.getMessage());
        }
    }

    private static void installDummyCertManager() {
        // Create a trust manager
        TrustManager[] trustAllCerts = new TrustManager[] {
            new X509TrustManager() {
                public X509Certificate[] getAcceptedIssuers() {
                    return null;
                }

                public void checkClientTrusted(X509Certificate[] certs, String authType) throws CertificateException {
                    dumpCerts(certs);
                }

                public void checkServerTrusted(X509Certificate[] certs, String authType) throws CertificateException {
                    dumpCerts(certs);
                }

                private void dumpCerts(X509Certificate[] certs) {
                    for (X509Certificate cert : certs)
                        dumpCert(cert);
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

    private static String formatFing(final byte[] fing) {
        if (fing == null)
            return "(null)";
        if (fing.length == 0)
            return "(empty)";
        Formatter fmt = new Formatter();
        boolean needColon = false;
        for (byte b : fing) {
            if (needColon)
                fmt.format("%c", ':');
            else
                needColon = true;
            fmt.format("%02X", b);
        }
        return fmt.toString();
    }

    private static void dumpCert(X509Certificate cert) {
        System.out.println("Serial No.: " +
            formatFing(cert.getSerialNumber().toByteArray()));
        try {
            for (String ftype : FTYPES) {
                MessageDigest md = MessageDigest.getInstance(ftype);
                md.reset();
                System.out.format("%s: %s%n",
                    ftype, formatFing(md.digest(cert.getEncoded())));
            }
        } catch (NoSuchAlgorithmException|CertificateException e) {
            Misc.die(e.getMessage());
        }
        System.out.println("Issued To: " + cert.getSubjectX500Principal());
        System.out.println("Issued By: " + cert.getIssuerX500Principal());
        System.out.format("Valid: from %tFT%<tT%<tz to %tFT%<tT%<tz%n%n",
            cert.getNotBefore(), cert.getNotAfter());
    }
}
