/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.praqma.jenkins.rqm.model;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Logger;
import net.praqma.jenkins.rqm.model.exception.RQMObjectParseException;
import net.praqma.jenkins.rqm.request.RqmParameterList;

/**
 * @author Praqma
 */
public class TestCaseExecutionRecord extends RqmObject<TestCaseExecutionRecord> {
    
    private static final Logger log = Logger.getLogger(TestCaseExecutionRecord.class.getName());
    private final static String RESOURCE_RQM_NAME = "executionworkitem";
    
    private TestCase testCase;
    private String testCaseExecutionRecordTitle;
        
    public TestCaseExecutionRecord(TestCase testCase) {
        this.testCase = testCase;
    }
    
    public TestCaseExecutionRecord(TestCase testCase, String title) {
        this.testCase = testCase;
        this.testCaseExecutionRecordTitle = title;
    }

    /**
     * This method should construct 
     * @param xml
     * @return
     * @throws RQMObjectParseException 
     */
    @Override
    public TestCaseExecutionRecord initializeSingleResource(String xml) throws RQMObjectParseException {
        return null;
    }

    @Override
    public List<TestCaseExecutionRecord> read(RqmParameterList parameters) throws IOException {
        throw new UnsupportedOperationException("Not implemented yet");
    }
    
    @Override
    public List<TestCaseExecutionRecord> createOrUpdate(RqmParameterList parameters) throws IOException {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    /**
     * @return the testCase
     */
    public TestCase getTestCase() {
        return testCase;
    }

    /**
     * @param testCase the testCase to set
     */
    public void setTestCase(TestCase testCase) {
        this.testCase = testCase;
    }

    /**
     * @return the testCaseExecutionRecordTitle
     */
    public String getTestCaseExecutionRecordTitle() {
        return testCaseExecutionRecordTitle;
    }

    /**
     * @param testCaseExecutionRecordTitle the testCaseExecutionRecordTitle to set
     */
    public void setTestCaseExecutionRecordTitle(String testCaseExecutionRecordTitle) {
        this.testCaseExecutionRecordTitle = testCaseExecutionRecordTitle;
    }

    @Override
    public HashMap<String, String> attributes() {
        HashMap<String, String> attr = new HashMap<String, String>();
        attr.put("testcaseexecutionrecord_title", getTestCaseExecutionRecordTitle());
        return attr;
        
    }

    @Override
    public String getResourceName() {
        return RESOURCE_RQM_NAME;
    }
}
