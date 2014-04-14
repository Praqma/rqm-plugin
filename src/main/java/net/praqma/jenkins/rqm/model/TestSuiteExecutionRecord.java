/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package net.praqma.jenkins.rqm.model;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.praqma.jenkins.rqm.model.exception.ClientCreationException;
import net.praqma.jenkins.rqm.model.exception.LoginException;
import net.praqma.jenkins.rqm.model.exception.RQMObjectParseException;
import net.praqma.jenkins.rqm.model.exception.RequestException;
import net.praqma.jenkins.rqm.request.RQMGetRequest;
import net.praqma.jenkins.rqm.request.RQMHttpClient;
import net.praqma.jenkins.rqm.request.RQMUtilities;
import net.praqma.jenkins.rqm.request.RqmParameterList;
import net.praqma.util.structure.Tuple;
import org.apache.commons.httpclient.NameValuePair;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 *
 * @author Mads
 */
public class TestSuiteExecutionRecord extends RqmObject<TestSuiteExecutionRecord> implements TopLevelObject {    
    private final static String RESOURCE_RQM_NAME = "suiteexecutionrecord";
    private static final Logger log = Logger.getLogger(TestSuiteExecutionRecord.class.getName());
    
    //The test suite execution record belongs to exactly one testsuite
    private TestSuite testSuite;
    private TestPlan testPlan;
    private String testSuiteExecutionRecordTitle;
    
    @Override
    public TestSuiteExecutionRecord initializeSingleResource(String xml) throws RQMObjectParseException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
    public static String getResourceFeedUrl(String host, int port, String context, String project) throws UnsupportedEncodingException {
        String request = String.format("%s:%s/%s/service/com.ibm.rqm.integration.service.IIntegrationService/resources/%s/%s", host, port, context, URLEncoder.encode(project,"UTF-8"), RESOURCE_RQM_NAME);                               
        return request;
    }
    
    public static NameValuePair[] getFilteringProperties(String host, int port, String context, String choseSuites) {
        NameValuePair nvp = new NameValuePair("fields", String.format("feed/entry/content/suiteexecutionrecord[title='%s']/*", choseSuites));              
        return new NameValuePair[] { nvp };
    }

    @Override
    public List<TestSuiteExecutionRecord> readMultiple(RqmParameterList parameters) throws IOException {
        List<TestSuiteExecutionRecord> tsers = new ArrayList<TestSuiteExecutionRecord>();
        RQMHttpClient client = null;
        try {            
            client = RQMUtilities.createClient(parameters.hostName, parameters.port, parameters.contextRoot, parameters.projectName, parameters.userName, parameters.passwd);
        } catch (MalformedURLException ex) {
            log.logp(Level.SEVERE, this.getClass().getName(), "read", "Caught MalformedURLException in read throwing IO Exception",ex);
            throw new IOException("RqmMethodInvoker exception", ex);
        } catch (ClientCreationException cre) {
            log.logp(Level.SEVERE, this.getClass().getName(), "read", "Caught ClientCreationException in read throwing IO Exception", cre);
            throw new IOException("RqmMethodInvoker exception(ClientCreationException)", cre);
        }
        try {
            Tuple<Integer,String> res = new RQMGetRequest(client, parameters.requestString, parameters.parameterList).executeRequest();
            //Look for ns4:suiteexecutionrecord.
            //Add testplan and testsuite to the record
            Document doc = RqmObject.getDocumentReader(res.t2);
            
            NodeList n = doc.getElementsByTagName("ns4:suiteexecutionrecord");
            log.fine( String.format( "Found %s test suite execution records", n.getLength() ));
            
            for(int i = 0; i<n.getLength(); i++) {
                TestSuiteExecutionRecord record = new TestSuiteExecutionRecord();                        
                Node nl = n.item(i);                    
                if(nl.getNodeType() == Node.ELEMENT_NODE) {
                    Element suiteElement = (Element)nl;
                    String testSuiteExecutionRecordHref = suiteElement.getElementsByTagName("ns6:identifier").item(0).getTextContent();
                    String tserTitle = suiteElement.getElementsByTagName("ns6:title").item(0).getTextContent();
                    String testPlanHref = ((Element)suiteElement.getElementsByTagName("ns4:testplan").item(0)).getAttribute("href");
                    String testSuiteHref = ((Element)suiteElement.getElementsByTagName("ns4:testsuite").item(0)).getAttribute("href");                    
                    /**
                     * Load the objects
                     */
                    record.setRqmObjectResourceUrl(testSuiteExecutionRecordHref);
                    record.setTestSuite(new TestSuite(testSuiteHref).read(parameters).get(0));
                    record.setTestPlan(new TestPlan(testPlanHref).read(parameters).get(0));
                    record.setTestSuiteExecutionRecordTitle(tserTitle);
                }
                tsers.add(record);
            }
            
             
        } catch (LoginException ex) {
            Logger.getLogger(TestSuiteExecutionRecord.class.getName()).log(Level.SEVERE, null, ex);
        } catch (RequestException ex) {
            Logger.getLogger(TestSuiteExecutionRecord.class.getName()).log(Level.SEVERE, null, ex);
        } catch (RQMObjectParseException ex) {
            Logger.getLogger(TestSuiteExecutionRecord.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        return tsers;
    }


    @Override
    public String getResourceName() {
        return RESOURCE_RQM_NAME;
    }

    /**
     * @return the testSuite
     */
    public TestSuite getTestSuite() {
        return testSuite;
    }

    /**
     * @param testSuite the testSuite to set
     */
    public void setTestSuite(TestSuite testSuite) {
        this.testSuite = testSuite;
    }

    /**
     * @return the testPlan
     */
    public TestPlan getTestPlan() {
        return testPlan;
    }

    /**
     * @param testPlan the testPlan to set
     */
    public void setTestPlan(TestPlan testPlan) {
        this.testPlan = testPlan;
    }

    /**
     * @return the testSuiteExecutionRecordTitle
     */
    public String getTestSuiteExecutionRecordTitle() {
        return testSuiteExecutionRecordTitle;
    }

    /**
     * @param testSuiteExecutionRecordTitle the testSuiteExecutionRecordTitle to set
     */
    public void setTestSuiteExecutionRecordTitle(String testSuiteExecutionRecordTitle) {
        this.testSuiteExecutionRecordTitle = testSuiteExecutionRecordTitle;
    }

    @Override
    public Set<TestCase> getAllTestCases() {
        HashSet<TestCase> testCases = new HashSet<TestCase>();
        testCases.addAll(getTestSuite().getTestcases());
        return testCases;
    }
}
