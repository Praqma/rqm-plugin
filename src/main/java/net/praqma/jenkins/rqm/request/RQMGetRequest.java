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
import java.util.logging.Logger;
import javax.xml.ws.http.HTTPException;
import net.praqma.util.structure.Tuple;
import org.apache.commons.httpclient.methods.GetMethod;

/**
 *
 * @author Praqma
 */
public class RQMGetRequest implements RQMRequest {
    GetMethod method = null;
    RQMHttpClient client = null;
    private static final Logger log = Logger.getLogger(RQMHttpClient.class.getName());
    
    public RQMGetRequest(RQMHttpClient client, String url) {  
        method = new GetMethod(url);
    }

    @Override
    public Tuple<Integer,String> executeRequest() {
        Tuple<Integer,String> result = new Tuple<Integer, String>();
        
        try {
            client.login();
            int response = client.executeMethod(method);
            result.t1 = response;
        } catch (HTTPException ex) {
            log.severe(ex.getMessage());
            result.t1 = ex.getStatusCode();
            
        } catch (IOException ioex) {
            
        } finally {
            client.logout();
        }
        
        return result;
    }
}
