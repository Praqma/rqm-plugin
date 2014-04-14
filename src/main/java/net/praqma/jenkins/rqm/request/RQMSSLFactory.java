package net.praqma.jenkins.rqm.request;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.security.GeneralSecurityException;
import javax.net.SocketFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import org.apache.commons.httpclient.ConnectTimeoutException;
import org.apache.commons.httpclient.params.HttpConnectionParams;
import org.apache.commons.httpclient.protocol.ProtocolSocketFactory;
import org.apache.commons.httpclient.protocol.SecureProtocolSocketFactory;

/**
 *
 * @author Praqma
 */
public class RQMSSLFactory implements ProtocolSocketFactory, SecureProtocolSocketFactory {

    private SSLContext sslcontext = null;
    private static RQMSSLFactory instance;

    public static RQMSSLFactory getInstance() throws GeneralSecurityException {
        if (instance == null) {
            instance = new RQMSSLFactory();
        }
        return instance;
    }

    private RQMSSLFactory() throws GeneralSecurityException {
        super();

        // Create a trust manager that does not validate certificate chains
        TrustManager trustAllCerts = new X509TrustManager() {
            @Override
            public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                return null;
            }

            @Override
            public void checkClientTrusted(
                    java.security.cert.X509Certificate[] certs, String authType) {
            }

            @Override
            public void checkServerTrusted(
                    java.security.cert.X509Certificate[] certs, String authType) {
            }
        };
        sslcontext = RQMSSLContext.createSSLContext(trustAllCerts);
    }
    
    @Override
    public Socket createSocket(String host, int port) throws IOException,
            UnknownHostException {
        return getSSLContext().getSocketFactory().createSocket(host, port);
    }
    
    @Override
    public Socket createSocket(String host, int port, InetAddress clientHost,
            int clientPort) throws IOException, UnknownHostException {
        return getSSLContext().getSocketFactory().createSocket(host, port,
                clientHost, clientPort);
    }
    @Override
    public Socket createSocket(final String host, final int port, final InetAddress localAddress, final int localPort, final HttpConnectionParams params) throws IOException, UnknownHostException, ConnectTimeoutException {

        int timeout = params.getConnectionTimeout();
        SocketFactory socketfactory = getSSLContext().getSocketFactory();
        if (timeout == 0) {
            return socketfactory.createSocket(host, port, localAddress,
                    localPort);
        } else {
            Socket socket = socketfactory.createSocket();
            SocketAddress localaddr = new InetSocketAddress(localAddress,
                    localPort);
            SocketAddress remoteaddr = new InetSocketAddress(host, port);
            socket.bind(localaddr);
            socket.connect(remoteaddr, timeout);
            return socket;
        }
    }
    
    @Override
    public Socket createSocket(Socket socket, String host, int port, boolean autoClose) throws IOException, UnknownHostException {
        return getSSLContext().getSocketFactory().createSocket(socket, host, port, autoClose);
    }

    public SSLContext getSSLContext() {
        return sslcontext;
    }
}
