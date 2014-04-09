package net.praqma.jenkins.rqm.request;

import java.io.IOException;
import java.net.Socket;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Logger;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;

/**
 *
 * @author Praqma
 */
public class RQMSSLContext {
    /**
     * Encryption algorithm TLS then SSL - IBM JREs
     */
    private static final Logger log = Logger.getLogger(RQMSSLContext.class.getSimpleName());
    
    public static final String SSL_TLS = "SSL_TLS"; //$NON-NLS-1$
    /**
     * Encryption algorithm TLS then SSL - Sun JREs
     */
    public static final String SSLV3 = "SSLv3"; //$NON-NLS-N$
    /**
     * Encryption algorithm TLS
     */
    public static final String TLS = "TLS"; //$NON-NLS-1$
    /**
     * Encryption algorithm SSL 
     */
    public static final String SSL = "SSL"; //$NON-NLS-1$

    public static SSLContext createSSLContext(TrustManager trustManager) {
        SSLContext context = createSSLContext(SSL_TLS, trustManager);        
        if (context == null) {
            context = createSSLContext(SSLV3, trustManager);
        }
        
        if (context == null) {
            context = createSSLContext(TLS, trustManager);
        }

        if (context == null) {
            context = createSSLContext(SSL, trustManager);
        }

        if (context == null) {
            throw new RuntimeException("No acceptable encryption algorithm found");
        }

        return context;
    }

    private static SSLContext createSSLContext(String algorithm, TrustManager trustManager) {
        SSLContext context;
        try {
            context = SSLContext.getInstance(algorithm);
        } catch (NoSuchAlgorithmException e) {
            return null;
        }
        try {
            context.init(null, new TrustManager[] { trustManager }, null);
        } catch (KeyManagementException e) {
            return null;
        }

        /* Create a socket to ensure this algorithm is acceptable.  This will
         * correctly disallow certain configurations (such as SSL_TLS under FIPS) */
        try {
            Socket s = context.getSocketFactory().createSocket();
            s.close();
        } catch (IOException e) {
            log.finest("Socket failure "+e.getMessage());
            return null;
        } catch (IllegalArgumentException e) {
            log.finest("Socket failure "+e.getMessage());
            return null;
        } 
        return context;
    }  
}
