/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.praqma.jenkins.rqm.model;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.HashMap;
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
import org.apache.commons.lang.StringUtils;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 *
 * @author Praqma
 * 
 * A test plan can contain both testCases and test suites.
 * 
 * 
 */
public class TestPlan extends RqmObject<TestPlan> {
    private final static String RESOURCE_RQM_NAME = "testplan";
    private static final Logger log = Logger.getLogger(TestPlan.class.getName());
    private String testPlanTitle;
    private Set<TestCase> testCases = new HashSet<TestCase>();
    private Set<TestSuite> testSuites = new HashSet<TestSuite>();
    
    public TestPlan() {}
    
    public TestPlan(String testPlanName) {
        this.testPlanTitle = testPlanName;
        testCases = new HashSet<TestCase>();
        testSuites = new HashSet<TestSuite>();
    }
    
    @Override
    public HashMap<String,String> attributes() {
        HashMap<String,String> attr = new HashMap<String, String>();
        attr.put("testplan_title",testPlanTitle);
        return attr;
    }
    
    
    /**
     * We initalize the list of test suites by their assocated TestPlanName
     * 
     * Two cases:
     * 
     * Case 1: 
     * TestCase directly in plan
     * 
     * Case 2:
     * TestCase
     * 
     * 
     * @return
     * @throws RQMObjectParseException 
     */
    @Override
    public TestPlan initializeSingleResource(String xml) throws RQMObjectParseException {
        try {
            Document doc = RqmObject.getDocumentReader(xml);
            
            NodeList list = doc.getElementsByTagName("ns3:title");     
            String title = null;
            for(int i=0; i<list.getLength(); i++) {
                Node elem = list.item(i);
                if(elem.getNodeType() == Node.ELEMENT_NODE) {                    
                     title = ((Element)elem).getTextContent();                    
                     setTestPlanTitle(title);
                }
            }
                        
            NodeList nlistForTestCases = doc.getElementsByTagName("testcase");
            for(int i=0; i<nlistForTestCases.getLength(); i++) {
                TestCase tc = new TestCase();
                
                Node elem = nlistForTestCases.item(i);
                if(elem.getNodeType() == Node.ELEMENT_NODE) {
                    Element elm = (Element)elem;
                    tc.setRqmObjectResourceUrl(elm.getAttribute("href"));
                }
                getTestCases().add(tc);
            }
            
            NodeList nlistForTestSuites = doc.getElementsByTagName("testsuite");
            for(int i=0; i<nlistForTestSuites.getLength(); i++) {
                TestSuite suite = new TestSuite(null,null);
                Node elem = nlistForTestSuites.item(i);
                if(elem.getNodeType() == Node.ELEMENT_NODE) {
                    Element elm = (Element)elem;
                    suite.setRqmObjectResourceUrl(elm.getAttribute("href"));
                }
                getTestSuites().add(suite);
            }
                        
            return this;
        } catch (RQMObjectParseException ex) {
            throw new RQMObjectParseException("Failed to parse TestPlan object", ex);
        } catch (DOMException ex) {
            throw new RQMObjectParseException("DOM Exception, malformed xml?", ex);
        }        
    }
    
    /**
     * Method that returns a basic list of feed data. We include everything for a given test plan.
     * @param host
     * @param port
     * @param context
     * @return 
     */
    public String getFeedUrlForTestPlans(String host, int port, String context, String project) throws UnsupportedEncodingException {
        String request = String.format("%s:%s/%s/service/com.ibm.rqm.integration.service.IIntegrationService/resources/%s/testplan", host, port, context, URLEncoder.encode(project,"UTF-8"));        
        return request;
    }
    
    public NameValuePair[] getParametersForFeedUrlForTestPlans() {
        NameValuePair[] pairs = new NameValuePair[1];
        pairs[0] = new NameValuePair("fields", String.format("feed/entry/content/testplan[title='%s']/*", getTestPlanTitle()));
        return pairs;
    }
    

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append(RqmObject.getDescriptor(this, getTestPlanTitle()));        
        builder.append(String.format(" %s testsuite(s)\n %s testcase(s)",getTestSuites().size(),getTestCases().size()));        
        builder.append("===Contents===\n");
        for(TestSuite suite : getTestSuites()) {
            builder.append(suite);
        }
        
        
        return builder.toString();
    }

    /**
     * @return the testCases
     */
    public Set<TestCase> getTestCases() {
        return testCases;
    }

    /**
     * @param testCases the testCases to set
     */
    public void setTestCases(HashSet<TestCase> testCases) {
        this.testCases = testCases;
    }

    /**
     * @return the testSuites
     */
    public Set<TestSuite> getTestSuites() {
        return testSuites;
    }

    /**
     * @param testSuites the testSuites to set
     */
    public void setTestSuites(Set<TestSuite> testSuites) {
        this.testSuites = testSuites;
    }    

    /**
     * @return the testPlanTitle
     */
    public String getTestPlanTitle() {
        return testPlanTitle;
    }

    /**
     * @param testPlanTitle the testPlanTitle to set
     */
    public void setTestPlanTitle(String testPlanTitle) {
        this.testPlanTitle = testPlanTitle;
    }
    
    public HashSet<TestSuite> getSuitesWithNames(String names) {
        HashSet<TestSuite> suites = new HashSet<TestSuite>();
        if(names.contains(",")) {
            for(String suiteName : names.split(",")) {
                for(TestSuite suite : getTestSuites()) {
                    if(suite.getTestSuiteTitle().equals(StringUtils.trim(suiteName))) {
                        suites.add(suite);
                    }
                }
            }
        } else {
            for(TestSuite suite : getTestSuites()) {
                if(suite.getTestSuiteTitle().equals(StringUtils.trim(names))) {
                    suites.add(suite);
                }
            }
        } 
            
        return suites;
    }
    
    public HashSet<TestCase> getAllTestCasesWithinSuites(String suites) {
        if(StringUtils.isBlank(suites)) {
            return getAllTestCases();
        }
        
        HashSet<TestCase> tcs = new HashSet<TestCase>();        
        if(suites.contains(",")) { 
            for(String suiteName : suites.split(",")) {
                for(TestSuite suite : getTestSuites()) {
                    if(suite.getTestSuiteTitle().equals(StringUtils.trim(suiteName))) {
                        tcs.addAll(suite.getTestcases());
                    }
                }
            }
        } else {
            for(TestSuite suite : getTestSuites()) {
                if(suite.getTestSuiteTitle().equals(suites)) {
                    tcs.addAll(suite.getTestcases());
                }
            }
        }
        
        return tcs;
    }
    
    public HashSet<TestCase> getAllTestCases() {
        HashSet<TestCase> allCases = new HashSet<TestCase>();
        //FIXME: What about test cases not assigned to a Suite?
        //allCases.addAll(getTestCases());
        for(TestSuite suite : getTestSuites()) {
            allCases.addAll(suite.getTestcases());
        }        
        return allCases;
    }
    
    /**
     * Small method that returns the union of test cases having scripts
     * @return 
     */
    public HashSet<TestCase> getAllTestCasesWithScripts() {
        HashSet<TestCase> allCases = new HashSet<TestCase>();
        
        for(TestCase tc : getTestCases()) {            
            if(tc.getScripts() == null) {
                log.fine( String.format( "Test case %s was null for testplan %s", tc.getTestCaseTitle(),  getTestPlanTitle()) );
            } else if(tc.getScripts() != null && tc.getScripts().isEmpty()) {
                log.fine( String.format( "Test case %s has no testscripts %s", tc.getTestCaseTitle(), getTestPlanTitle()) );
            }          
            if(tc.getScripts() != null && !tc.getScripts().isEmpty()) {
                allCases.add(tc);
            }
        }
        
        for(TestSuite suite : getTestSuites()) {
            for(TestCase tc : suite.getTestcases()) {
                if(tc.getScripts() != null && !tc.getScripts().isEmpty()) {
                    allCases.add(tc);
                }
            }
        }
        
        return allCases;
    }
    
    /**
     * Basic 'Read' operation (GET) for a test plan
     * @param parameters
     * @return
     * @throws IOException 
     */
    @Override
    public List<TestPlan> read(RqmParameterList parameters) throws IOException {
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
            this.initializeSingleResource(res.t2);

            for(TestCase tc : getTestCases()) {
                parameters.requestString = tc.getRqmObjectResourceUrl();
                tc.read(parameters);
                for(TestScript ts : tc.getScripts()) {
                    parameters.requestString = ts.getRqmObjectResourceUrl();
                    ts.read(parameters);
                }                
            }
            
            for(TestSuite suites : getTestSuites()) {
                parameters.requestString = suites.getRqmObjectResourceUrl();
                suites.read(parameters);                
                for(TestCase tc : suites.getTestcases()) {
                    parameters.requestString = tc.getRqmObjectResourceUrl();
                    tc.read(parameters);                    
                    for(TestScript ts : tc.getScripts()) {
                        parameters.requestString = ts.getRqmObjectResourceUrl();
                        ts.read(parameters);
                    }                    
                }                
            }
             
            return Arrays.asList(this);
 
        } catch (LoginException loginex) {
            log.logp(Level.SEVERE, this.getClass().getName(), "invoke", "Caught login exception in invoke");
            throw new IOException("RqmMethodInvoker exception(LoginException)",loginex);
        } catch (RequestException reqExeception) {
            log.logp(Level.SEVERE, this.getClass().getName(), "invoke", "Caught RequestException in invoke");
            throw new IOException("RqmMethodInvoker exception(RequestException)",reqExeception);
        } catch (Exception ex) {
            log.logp(Level.SEVERE, this.getClass().getName(), "invoke", "Caught Exception in invoke");
            throw new IOException("RqmMethodInvoker exception(Exception)", ex);
        }
    }

    @Override
    public String getResourceName() {
        return RESOURCE_RQM_NAME;
    }

}
