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
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Logger;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpMethodBase;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;

/**
 *
 * @author Praqma
 */
public class RQMHttpClient extends HttpClient {
    private String usr,contextRoot,password,project,host,port;
    private URL url;
    private static final Logger log = Logger.getLogger(RQMHttpClient.class.getName());
        
    public RQMHttpClient(String host, String port, String contextRoot, String project, String usr, String password) throws MalformedURLException {
        this.usr = usr;
        this.password = password;
        this.project = project;
        this.contextRoot = contextRoot;
        this.host = host;
        this.port = port;
        
        this.url = new URL(host.contains("https") ? "https" : "http", host, Integer.parseInt(port), "/");
        
    }
    
    public int login() throws HttpException, IOException {
        GetMethod methodPart1 = new GetMethod(host+"/"+contextRoot+"/auth/authrequired");
        int response = executeMethod(methodPart1);
        
        followRedirects(methodPart1, response);
        
        GetMethod methodPart2 = new GetMethod(this.url.toString()+ contextRoot + "/authenticated/identity");
		response = executeMethod(methodPart2);
		followRedirects(methodPart2, response);
        
        HttpMethodBase authenticationMethod = null;
		Header authenticateHeader = methodPart2.getResponseHeader("WWW-Authenticate"); //$NON-NLS-1$
        
        if ((response == HttpURLConnection.HTTP_UNAUTHORIZED) && (authenticateHeader != null) && (authenticateHeader.getValue().toLowerCase().indexOf("basic realm") == 0)) { //$NON-NLS-1$
				
			super.getState().setCredentials(new AuthScope(host, Integer.parseInt(port)), new UsernamePasswordCredentials(usr, password));

			authenticationMethod = new GetMethod(this.url.toString() + "/authenticated/identity");
			response = super.executeMethod(methodPart2); 	
		}
		
		//Configure the HTTP client for form authentication:
		else{
			
			authenticationMethod = new PostMethod(this.url.toString() + contextRoot + "/j_security_check");
	
			NameValuePair[] nvps = new NameValuePair[2];
			nvps[0] = new NameValuePair("j_username", usr);
			nvps[1] = new NameValuePair("j_password", password);
			((PostMethod)(authenticationMethod)).addParameters(nvps);
			((PostMethod)(authenticationMethod)).addRequestHeader("Content-Type", "application/x-www-form-urlencoded; charset=utf-8"); //$NON-NLS-1$ //$NON-NLS-2$
            log.fine("Authentication method query string: "+authenticationMethod.getQueryString());
			response = executeMethod(authenticationMethod);
			Header location = authenticationMethod.getResponseHeader("X-com-ibm-team-repository-web-auth-msg");
			if (location != null && location.getValue().indexOf("authfailed") >= 0) {
				response = HttpURLConnection.HTTP_UNAUTHORIZED;
			}
		}
        
        		// Only proceed if there were no problems posting the authentication.
		if ((response != HttpURLConnection.HTTP_OK) && (response != HttpURLConnection.HTTP_MOVED_TEMP)) {
			if (response != HttpURLConnection.HTTP_UNAUTHORIZED) {
				String body = "";
				try {
					body = authenticationMethod.getResponseBodyAsString();
				} catch (Exception e) {

				}
                log.severe(String.format("Failed to login, with response code %s \n Response body: \n %s",response,body));
			}
		} else {
			followRedirects(authenticationMethod, response);
            String methodText = String.format("%s%s/service/com.ibm.team.repository.service.internal.webuiInitializer.IWebUIInitializerRestService/initializationData", url.toString(),contextRoot);
			GetMethod get3 = new GetMethod(methodText);
			response = executeMethod(get3);
			followRedirects(get3, response);
		}
        
        
        return response;
    }
    
    
    public int logout() {
        return 0;
    }
    
    public void followRedirects(HttpMethodBase method, int responseCode) throws HttpException, IOException {
        Header location = method.getResponseHeader("Location");
		while (location != null && responseCode == HttpURLConnection.HTTP_MOVED_TEMP) {
			GetMethod get3 = new GetMethod(location.getValue());
			responseCode = executeMethod(get3);
			if (responseCode != HttpURLConnection.HTTP_OK) {
                log.fine(String.format("Follow redirects returned code %s",responseCode));
			}
			location = get3.getResponseHeader("Location");

		}
    }
}
