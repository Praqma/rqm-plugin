/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.praqma.jenkins.rqm.cli;

import java.net.MalformedURLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.praqma.jenkins.rqm.model.exception.ClientCreationException;
import net.praqma.jenkins.rqm.model.exception.LoginException;
import net.praqma.jenkins.rqm.model.exception.RequestException;
import net.praqma.jenkins.rqm.request.RQMGetRequest;
import net.praqma.jenkins.rqm.request.RQMHttpClient;
import net.praqma.jenkins.rqm.request.RQMUtilities;
import net.praqma.util.structure.Tuple;

/**
 *
 * @author Praqma
 */
public class PrqmCli {
    
    public static void main(String[] args) throws MalformedURLException {
        System.out.println("Testing Praqma-RQM API for Jenkins");
        RQMHttpClient client = null;
        try {
            client = RQMUtilities.createClient("http://dkplan01", 9080, "qm", "myproject", "901599", "901599");
        } catch (ClientCreationException ex) {
            ex.printStackTrace(System.err);
        }
        String response = "";
        int returnCode = -1;
        Tuple<Integer,String> result = null;
        
        try {
            result = new RQMGetRequest(client, "http://dkplan01:9080/qm/service/com.ibm.rqm.integration.service.IIntegrationService/resources/Team+Test+%28Testing%29/testscript/urn:com.ibm.rqm:testscript:588",null).executeRequest();
        } catch (LoginException ex) {
            ex.printStackTrace(System.err);
        } catch (RequestException ex) {
            ex.printStackTrace(System.err);
        }
        
        if(result != null) {
            System.out.println(result.t1);
            System.out.println(result.t2);
            
        }
    }
    
}
