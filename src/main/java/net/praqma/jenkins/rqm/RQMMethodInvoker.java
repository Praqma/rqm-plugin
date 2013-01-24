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
package net.praqma.jenkins.rqm;

import hudson.FilePath;
import hudson.remoting.VirtualChannel;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URLEncoder;
import net.praqma.jenkins.rqm.model.exception.ClientCreationException;
import net.praqma.jenkins.rqm.model.exception.LoginException;
import net.praqma.jenkins.rqm.model.exception.RequestException;
import net.praqma.jenkins.rqm.request.RQMGetRequest;
import net.praqma.jenkins.rqm.request.RQMHttpClient;
import net.praqma.jenkins.rqm.request.RQMUtilities;
import net.praqma.util.structure.Tuple;
import org.apache.commons.httpclient.NameValuePair;

/**
 *
 * @author Praqma
 */
public class RQMMethodInvoker implements FilePath.FileCallable<Tuple<Integer,String>> {
    
    //resourceUrl (for single-object)
    
    
    private transient RQMHttpClient client = null;
   
    public final NameValuePair[] parameterList;
    public final String hostName;
    public final String contextRoot;
    public final String requestString;
    public final String projectName;
    public final String userName;
    public final String passwd;
    public final int port;
    
    public RQMMethodInvoker(final String hostName, final int port, final String contextRoot, final String projectName, final String userName, final String passwd, final String requestString, final NameValuePair[] parameterList) throws ClientCreationException {
        this.hostName = hostName;
        this.port = port;
        this.projectName = projectName;
        this.userName = userName;
        this.passwd = passwd;
        this.contextRoot = contextRoot;
        this.requestString = requestString;
        this.parameterList = parameterList;
        
    }
    
    @Override
    public Tuple<Integer,String> invoke(File file, VirtualChannel vc) throws IOException, InterruptedException {
        Tuple<Integer,String> result = null;
        try {            
            client = RQMUtilities.createClient(hostName, port, contextRoot, projectName, userName, passwd);
        } catch (MalformedURLException ex) {
            throw new IOException("RqmMethodInvoker exception", ex);
        } catch (ClientCreationException cre) {
            throw new IOException("RqmMethodInvoker exception(ClientCreationException)", cre);
        }
        
        try {
            result = new RQMGetRequest(client, requestString, parameterList).executeRequest();
        } catch (LoginException loginex) {
            throw new IOException("RqmMethodInvoker exception(LoginException)",loginex);
        } catch (RequestException reqExeception) {
            throw new IOException("RqmMethodInvoker exception(RequestException)",reqExeception);
        } catch (Exception ex) {
            throw new IOException("RqmMethodInvoker exception(Exception)", ex);
        }

        
        return result;
    }
}
