import javax.net.ssl.*;
import java.security.GeneralSecurityException;
import java.security.cert.X509Certificate;

public class SSLUtils {

    public static void main(final String[] args) throws GeneralSecurityException {
        trustAllSslCertificates();
    }

    /**
     * Make the application ignore all SSL Certification problems
     * @throws java.security.GeneralSecurityException (should not occur)
     */
    public static void trustAllSslCertificates() throws GeneralSecurityException {
        // Create a trust manager that does not validate certificate chains like the default TrustManager
        final TrustManager[] trustAllCerts = new TrustManager[]{
                new X509TrustManager() {
                    public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                        return null;
                    }

                    public void checkClientTrusted(final X509Certificate[] certs, final String authType) {
                        //No need to implement.
                    }

                    public void checkServerTrusted(final X509Certificate[] certs, final String authType) {
                        //No need to implement.
                    }
                }
        };

        final SSLContext sc = SSLContext.getInstance("SSL");
        sc.init(null, trustAllCerts, new java.security.SecureRandom());
        HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());

        // Do not validate the certificates hostname, avoiding exceptions for "HTTPS hostname wrong"
        HttpsURLConnection.setDefaultHostnameVerifier(
                new HostnameVerifier() {
                    public boolean verify(final String s, final SSLSession sslSession) {
                        return true;
                    }
                }
        );
    }
}
