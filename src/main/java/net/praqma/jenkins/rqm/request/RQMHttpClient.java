/*
 * The MIT License
 *
 * Copyright 2013 Praqma.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package net.praqma.jenkins.rqm.request;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;
import java.security.GeneralSecurityException;
import java.util.logging.Logger;
import net.praqma.jenkins.rqm.model.exception.ClientCreationException;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpMethodBase;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.cookie.CookiePolicy;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.params.HttpClientParams;
import org.apache.commons.httpclient.protocol.Protocol;

/**
 *
 * @author Praqma
 */
public class RQMHttpClient extends HttpClient {

    private final String usr, contextRoot, password, project, host;
    private final int port;
    private URL url;
    private static final Logger log = Logger.getLogger(RQMHttpClient.class.getName());
    private final static String JAZZ_LOGOUT_URL = "/service/com.ibm.team.repository.service.internal.ILogoutRestService";

    public RQMHttpClient(String host, int port, String contextRoot, String project, String usr, String password) throws ClientCreationException {
        this.usr = usr;
        this.password = password;
        try {
            this.project = URLEncoder.encode(project, "UTF-8");
        } catch (UnsupportedEncodingException ex) {
            throw new ClientCreationException("Failed to create RQMHttpClient (constructor) ", ex);
        }
        this.contextRoot = contextRoot;
        this.host = host;
        this.port = port;
        try {
            if (host.contains("https://")) {
                this.url = new URL("https", host.replace("https://", ""), port, "/");
            } else if (host.contains("http://")) {
                this.url = new URL("http", host.replace("http://", ""), port, "/");
            }
        } catch (MalformedURLException malformedUrl) {
            throw new ClientCreationException("Failed to create RQMHttpClient(constructor)", malformedUrl);
        }
        getParams().setParameter(HttpClientParams.SINGLE_COOKIE_HEADER, true);
        String cookiePolicy = System.getenv("RQM_CLIENT_COOKIE_POLICY");
        if (cookiePolicy == null) {
            cookiePolicy = CookiePolicy.BROWSER_COMPATIBILITY;
        }
        getParams().setCookiePolicy(cookiePolicy);
    }

    public void registerTrustingSSL() throws GeneralSecurityException {
        log.finest("registering trusting ssl factory");
        Protocol.registerProtocol("https", new Protocol("https",
                RQMSSLFactory.getInstance(), 443));
    }

    public int login() throws HttpException, IOException, GeneralSecurityException {
        log.finest("Register trusting ssl");
        registerTrustingSSL();

        GetMethod methodPart1 = new GetMethod(url.toString() + contextRoot + "/auth/authrequired");

        int response = executeMethod(methodPart1);

        followRedirects(methodPart1, response);

        GetMethod methodPart2 = new GetMethod(this.url.toString() + contextRoot + "/authenticated/identity");

        response = executeMethod(methodPart2);

        followRedirects(methodPart2, response);

        HttpMethodBase authenticationMethod = null;
        Header authenticateHeader = methodPart2.getResponseHeader("WWW-Authenticate"); //$NON-NLS-1$

        if ((response == HttpURLConnection.HTTP_UNAUTHORIZED) && (authenticateHeader != null) && (authenticateHeader.getValue().toLowerCase().indexOf("basic realm") == 0)) {

            super.getState().setCredentials(new AuthScope(host, port), new UsernamePasswordCredentials(getUsr(), getPassword()));

            String authMethod = this.url.toString() + "/authenticated/identity";
            authenticationMethod = new GetMethod(authMethod);
            response = super.executeMethod(methodPart2);
        } else {
            authenticationMethod = new PostMethod(this.url.toString() + contextRoot + "/j_security_check");
            NameValuePair[] nvps = new NameValuePair[2];
            nvps[0] = new NameValuePair("j_username", getUsr());
            nvps[1] = new NameValuePair("j_password", getPassword());
            ((PostMethod) (authenticationMethod)).addParameters(nvps);
            ((PostMethod) (authenticationMethod)).addRequestHeader("Content-Type", "application/x-www-form-urlencoded; charset=utf-8");
            response = executeMethod(authenticationMethod);
            Header location = authenticationMethod.getResponseHeader("X-com-ibm-team-repository-web-auth-msg");
            if (location != null && location.getValue().indexOf("authfailed") >= 0) {
                response = HttpURLConnection.HTTP_UNAUTHORIZED;
            }
        }

        if ((response != HttpURLConnection.HTTP_OK) && (response != HttpURLConnection.HTTP_MOVED_TEMP)) {
            if (response != HttpURLConnection.HTTP_UNAUTHORIZED) {
                String body = "";
                try {
                    body = authenticationMethod.getResponseBodyAsString();
                } catch (Exception e) {
                    log.severe(String.format("Failed to login, with response code %s \n Response body: \n %s", response, body));
                }
                log.severe(String.format("Failed to login, with response code %s \n Response body: \n %s", response, body));
            }
        } else {
            followRedirects(authenticationMethod, response);
            String methodText = String.format("%s%s/service/com.ibm.team.repository.service.internal.webuiInitializer.IWebUIInitializerRestService/initializationData", url.toString(), contextRoot);
            GetMethod get3 = new GetMethod(methodText);
            response = executeMethod(get3);
            followRedirects(get3, response);
        }

        log.finest("Response was: " + response);
        return response;
    }

    public int logout() {
        log.finest("Logout");
        try {
            log.finest("URI:" + url.toURI().toString());
        } catch (URISyntaxException ex) {
            log.finest("Logout URISyntaxException " + ex.getMessage());
        }

        String logoutUrl = this.url.toString() + contextRoot + JAZZ_LOGOUT_URL;
        log.finest("Attempting to log out with this url: " + logoutUrl);
        HttpMethodBase authenticationMethod = new PostMethod(this.url.toString() + contextRoot + JAZZ_LOGOUT_URL);
        authenticationMethod.setFollowRedirects(false);
        UsernamePasswordCredentials credentials = (UsernamePasswordCredentials) super.getState().getCredentials(new AuthScope(host, port));
        if (null != credentials && !(credentials.getUserName().isEmpty() && credentials.getPassword().isEmpty())) {

            log.finest(String.format("Adding authorizationheadder for logout for user: %s", credentials.getUserName()));
            authenticationMethod.addRequestHeader("Authorization:", "Base " + credentials.getUserName() + ":" + credentials.getPassword());
        }
        String body = "";
        String status = "";
        int responseCode = 0;;
        try {
            responseCode = executeMethod(authenticationMethod);
            body = authenticationMethod.getResponseBodyAsString();
            status = authenticationMethod.getStatusText();
            log.finest(String.format("Response code %s, Status text %s", responseCode, status));
        } catch (Exception e) {
            log.finest("Failed to log out!: " + e.getMessage());
        }
        return responseCode;
    }

    public void followRedirects(HttpMethodBase method, int responseCode) throws HttpException, IOException {
        Header location = method.getResponseHeader("Location");
        while (location != null && responseCode == HttpURLConnection.HTTP_MOVED_TEMP) {
            GetMethod get3 = new GetMethod(location.getValue());
            responseCode = executeMethod(get3);
            if (responseCode != HttpURLConnection.HTTP_OK) {
                log.fine(String.format("Follow redirects returned code %s", responseCode));
            }
            location = get3.getResponseHeader("Location");

        }
    }

    /**
     * @return the usr
     */
    public String getUsr() {
        return usr;
    }

    /**
     * @return the password
     */
    public String getPassword() {
        return password;
    }
}
