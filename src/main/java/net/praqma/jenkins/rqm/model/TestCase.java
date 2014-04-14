/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.praqma.jenkins.rqm.model;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
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
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 *
 * @author Praqma
 */
public class TestCase extends RqmObject<TestCase> {
    private static final Logger log = Logger.getLogger(TestCase.class.getName());
    private final static String RESOURCE_RQM_NAME = "testcase";
    private String testCaseTitle;
    private List<TestScript> scripts = new ArrayList<TestScript>();
    
    /**
     * @return the scripts
     */
    public List<TestScript> getScripts() {
        return scripts;
    }
    
    /**
     * @param scripts the scripts to set
     */
    public void setScripts(List<TestScript> scripts) {
        this.scripts = scripts;
    }

    /**
     * @return the testCaseTitle
     */
    public String getTestCaseTitle() {
        return testCaseTitle;
    }

    /**
     * @param testCaseTitle the testCaseTitle to set
     */
    public void setTestCaseTitle(String testCaseTitle) {
        this.testCaseTitle = testCaseTitle;
    }

    @Override
    public List<TestCase> read(RqmParameterList parameters) throws IOException {
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
            Tuple<Integer,String> res = new RQMGetRequest(client, getRqmObjectResourceUrl(), parameters.parameterList).executeRequest();
            return Arrays.asList(initializeSingleResource(res.t2));                            
 
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
    public HashMap<String, String> attributes() {
        HashMap<String,String> attr = new HashMap<String, String>();
        attr.put("testcase_title", testCaseTitle);
        return attr;
    }

    @Override
    public List<TestCase> createOrUpdate(RqmParameterList parameters) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    
    public TestCase() { }
    
    public TestCase(String rqmObjectResourceUrl) {
        this(rqmObjectResourceUrl, null);
    }
    
    public TestCase(String rqmObjectResourceUrl, String testCaseTitle) {
        this.testCaseTitle = testCaseTitle;
        this.rqmObjectResourceUrl = rqmObjectResourceUrl;        
        this.scripts = new ArrayList<TestScript>();
    }

    @Override
    public TestCase initializeSingleResource(String xml) throws RQMObjectParseException {
        Document doc = RqmObject.getDocumentReader(xml);

        NodeList list = doc.getElementsByTagName("ns6:title");

        String title = null;

        for(int i=0; i<list.getLength(); i++) {
            Node elem = list.item(i);
            if(elem.getNodeType() == Node.ELEMENT_NODE) {
                log.fine("Title for test case: "+title);
                title = ((Element)elem).getTextContent();                    
            }
        }

        this.setTestCaseTitle(title);
        NodeList nlistForTestSuites = doc.getElementsByTagName("ns4:remotescript");

        for(int i=0; i<nlistForTestSuites.getLength(); i++) {
            Node elem = nlistForTestSuites.item(i);
            if(elem.getNodeType() == Node.ELEMENT_NODE) {
                Element elm = (Element)elem;
                TestScript ts = new TestScript(elm.getAttribute("href")); 
                getScripts().add(ts);
            }
        }
        return this;
                        
    }
    
    /**
     * A test to see if the scripts associated with this test case has a custom attribute with a specific name. The search is case sensitive
     * @param name
     * @return 
     */
    public boolean hasScriptWithCustomField(String name) {
        for (TestScript ts : getScripts()) {
            if(ts.customAttributes.containsKey(name) && (ts.customAttributes.get(name) != null && !ts.customAttributes.get(name).equals("") )) {
                return true;
            }
        }
        return false;
    }
 
    @Override
    public String toString() {
        
        StringBuilder builder = new StringBuilder();
        
        if(getScripts().size() > 0) {
            builder.append(RqmObject.getDescriptor(this, getTestCaseTitle()));
            
            for(TestScript s : getScripts()) {
                if(!s.customAttributes.isEmpty()) {
                    builder.append(String.format("Test Scripts with custom fields present:\n")); 
                    builder.append(s);
                }
            }
        }        
        return builder.toString();
    }
    
    public String getDescriptor() {
        return RqmObject.getDescriptor(this, getTestCaseTitle());
    }
    
    /**
     * @return the number of scripts assigned to this test case 
     */
    public int getTotalCount() {
        return getScripts().size();                
    }
    
    /*
     * Each test case has one unique test case execution record (re-using it)
     */
    public String getExternalAssignedTestCaseExecutonRecordId() throws RQMObjectParseException {
        return String.format("jenkins.tcer.%s", getInternalId());
    }
    
    /*
     * Each test case has one unique test case result (re-using it). Since we want to run this fairly often better re-use it
     * and have jenkins maintain the grand-overview-
     */
    public String getExternalAssignedTestCaseResultId() throws RQMObjectParseException {
        return String.format("jenkins.ter.%s", getInternalId());
    }

    @Override
    public boolean equals(Object obj) {
        if(obj == null || !(obj instanceof TestCase)){
            return false;
        } else {
            TestCase other = (TestCase)obj;
            return other.getRqmObjectResourceUrl().equals(getRqmObjectResourceUrl());
        }
    }

    @Override
    public String getResourceName() {
        return RESOURCE_RQM_NAME;
    }
}
