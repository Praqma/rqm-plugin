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

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URLEncoder;
import net.praqma.jenkins.rqm.model.exception.ClientCreationException;

/**
 *
 * @author Praqma
 */
public class RQMUtilities {
    private static final String SERVICE = "/service/com.ibm.rqm.integration.service.IIntegrationService";
    private static final String RESOURCES = "/resources/";
    private static RQMHttpClient client;
    
    /**
     * Append an id on this one to get the correct path
     * @param qmServerURL
     * @param projectAlias
     * @param resourceType
     * @throws UnsupportedEncodingException
     * @return 
     */
    public static String getSingleResourceBaseUrl(String qmServerURL, String projectAlias, String resourceType) throws UnsupportedEncodingException {
        return String.format("%s%s%s%s%s", qmServerURL, SERVICE, RESOURCES, URLEncoder.encode(projectAlias, "UTF-8"),"/"+resourceType+"/");
    }
    
    public static String getSingleResourceBaseUrlWithId(String contextRoot, String hostName, int port, String projectAlias, String resourceType, String externalId) throws UnsupportedEncodingException {
        return String.format("%s%s%s%s%s", RQMUtilities.getServerUrl(contextRoot, hostName, port), SERVICE, RESOURCES, URLEncoder.encode(projectAlias, "UTF-8"),"/"+resourceType+"/"+externalId);
    }
       
    public static String getServerUrl(String context, String host, int port) {
        return String.format("%s:%s/%s", host, port, context);
    }
    
    public static String getSingleFeedUrl(String qmServerURL, String projectAlias, String resourceType) {
        return qmServerURL+SERVICE+RESOURCES+projectAlias+"/"+resourceType;
    }
    
   
    public static RQMHttpClient createClient(String host, int port, String contextRoot, String project, String user, String password) throws MalformedURLException, ClientCreationException {
        if (client == null) {
            client = new RQMHttpClient(host, port, contextRoot, project, user, password);
        }
        return client;
    }
    
}
