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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.security.GeneralSecurityException;
import java.util.logging.Logger;
import javax.xml.ws.http.HTTPException;
import net.praqma.jenkins.rqm.model.exception.LoginException;
import net.praqma.jenkins.rqm.model.exception.RequestException;
import net.praqma.util.structure.Tuple;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.methods.GetMethod;

/**
 *
 * @author Praqma
 */
public class RQMGetRequest implements RQMRequest {
    GetMethod method = null;
    RQMHttpClient client = null;
    private static final Logger log = Logger.getLogger(RQMHttpClient.class.getName());
    
    public RQMGetRequest(RQMHttpClient client, String url, NameValuePair[] parameters) {  
        this.method = new GetMethod(url);
        method.addRequestHeader("Accept", "");
        method.addRequestHeader("Content-Type","application/atom+xml");
        
        if(parameters != null) {
            method.setQueryString(parameters);
        }
        Header[] headers = method.getRequestHeaders() ;
 
        for(Header h : headers) {
            log.finest(String.format("[%s,%s]",h.getName(),h.getValue()));
        }

        this.client = client;
    }

    @Override
    public Tuple<Integer,String> executeRequest() throws LoginException, RequestException {
        Tuple<Integer,String> result = new Tuple<Integer, String>();
        try {
            try {
                log.finest("ExecuteRequest");
                client.login();
            } catch (HttpException ex) {
                log.finest("HttpException: "+ex.getMessage());       
                throw new LoginException(client.getUsr(), client.getPassword(), ex);
            } catch (GeneralSecurityException ex) {
                log.finest("GeneraSecurityException: "+ex.getMessage());
                throw new LoginException(client.getUsr(), client.getPassword(), ex);
            }
            int response = client.executeMethod(method);
            BufferedReader reader = new BufferedReader(new InputStreamReader(method.getResponseBodyAsStream(), Charset.forName("utf8")));
            StringBuilder builder = new StringBuilder();
            String line;
            while((line = reader.readLine()) != null)  {
                builder.append(line);
            }
            
            reader.close();

            
            result.t1 = response;
            result.t2 = builder.toString();
            
        } catch (HTTPException ex) {
            
            log.severe(ex.getMessage());
            result.t1 = ex.getStatusCode();
            result.t2 = ex.getMessage() != null ? ex.getMessage() : "No message";
            throw new RequestException(result, ex);
            
        } catch (IOException ioex) {
            result.t1 = -1;
            result.t2 = ioex.getMessage() != null ? ioex.getMessage() : "No message";
            throw new RequestException(result, ioex);
        } finally {
            client.logout();
        }
        return result;
    }
}
