/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.praqma.jenkins.rqm.request;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.security.GeneralSecurityException;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import net.praqma.jenkins.rqm.model.exception.LoginException;
import net.praqma.jenkins.rqm.model.exception.RequestException;
import net.praqma.util.structure.Tuple;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.methods.InputStreamRequestEntity;
import org.apache.commons.httpclient.methods.PutMethod;

/**
 *
 * @author Praqma
 */
public class RQMPutRequest implements RQMRequest {
    
    private static final Pattern RESOURCE_PATTERN = Pattern.compile("http([s]?)://(.+)(:([0-9]+))?/(.+)/service/com.ibm.rqm.integration.service.IIntegrationService/resources/([^/]+)/([^/]+)(/(.*))?");
    private PutMethod method = null;
    private final RQMHttpClient client;
    private final String requestContent;
    private static final Logger log = Logger.getLogger(RQMPutRequest.class.getName());
    
    
    public RQMPutRequest(RQMHttpClient client, String url, NameValuePair[] parameters, String requestContent) {
        this.method = new PutMethod(url);
        this.client = client;
        this.requestContent = requestContent;
    }
    
    @Override
    public Tuple<Integer, String> executeRequest() throws LoginException, RequestException {
        Tuple<Integer,String> result = new Tuple<Integer, String>();
        try {
            client.login();
            BufferedInputStream bis = new BufferedInputStream(new ByteArrayInputStream(requestContent.getBytes(Charset.forName("utf8"))));
            method.setRequestEntity( new InputStreamRequestEntity(bis, "application/xml"));
            

            int returnCode = client.executeMethod(method);
            result.t1 = returnCode;
            result.t2 = method.getResponseBodyAsString();
            bis.close();
            return result;
        } catch (HttpException ex) {
            log.finest(String.format("HttpException: %s", ex.getMessage()));       
            throw new LoginException(client.getUsr(), client.getPassword(), ex);
        } catch (IOException ex) {
            log.finest(String.format("IOException: %s", ex.getMessage()));
            throw new LoginException(client.getUsr(), client.getPassword(), ex);
        } catch (GeneralSecurityException ex) {
            log.finest(String.format("GeneralSecurityException: %s", ex.getMessage()));
            throw new LoginException(client.getUsr(), client.getPassword(), ex);
        } finally {
            client.logout();
        }
        
    }
}
