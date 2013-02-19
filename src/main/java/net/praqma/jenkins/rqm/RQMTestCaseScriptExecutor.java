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

import hudson.FilePath.FileCallable;
import hudson.remoting.VirtualChannel;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.praqma.jenkins.rqm.model.TestCase;
import net.praqma.jenkins.rqm.model.TestScript;

/**
 *
 * @author Praqma
 */
public class RQMTestCaseScriptExecutor implements FileCallable<TestCase> {

    private static final Logger log = Logger.getLogger(RQMTestCaseScriptExecutor.class.getName());
    public final TestCase tc;
    public final String customField;
    
    public RQMTestCaseScriptExecutor(TestCase tc, String customField) {
        this.tc = tc;
        this.customField = customField;
    }
    
    @Override
    public TestCase invoke(File file, VirtualChannel vc) throws IOException, InterruptedException {
        for(TestScript script : tc.getScripts()) {            
            
            try {
                File testCaseWorkspaceFolder = new File(file, String.format("tc_%s", tc.getInternalId()));
                testCaseWorkspaceFolder.mkdir();
                script.runScriptContainedInCustomField(customField, testCaseWorkspaceFolder);
                //File f = new File(testCaseWorkspaceFolder, String.format("tc_%s_random_result.xml", script.getInternalId()));
                //generateDummyResults(f);                
                //script.setStatus(res == 0 ? TestScript.TestScriptRunStatus.SUCCESS : TestScript.TestScriptRunStatus.FAILURE);                                                                    
            } catch (Exception ex) {
                log.logp(Level.SEVERE, this.getClass().getName(), "invoke", "Error in script exectuion", ex);
                throw new IOException("Caught exception in class RQMTestCaseScriptExectuor");
            }
        }         
        
        TestCase.TestCaseTestResultStatus result = tc.getAggregatedTestCaseResult();
                        
        log.fine(String.format("Setting test case result to %s", result));
        tc.setOverallResult(result);
        return tc;        
    }
    /**
     * Test method. Creates a bare minimum testCaseResult
     * @param f
     * @throws IOException 
     */
    private int generateDummyResults(File f) throws IOException {
        int randNumber = new Random().nextInt(2);
        StringBuilder builder = new StringBuilder();
        builder.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        builder.append("<testsuite>");
        builder.append("<testcase classname=\"foo\" name=\"ASuccessfulTest\"/>");
        builder.append("<testcase classname=\"foo\" name=\"AnotherSuccessfulTest\"/>");
        if(randNumber == 0) {
            builder.append("<testcase classname=\"foo\" name=\"AnotFailingTest\"/>");
        } else {
            builder.append("<testcase classname=\"foo\" name=\"AFailingTest\">");
            builder.append("<failure type=\"NotEnoughFoo\"> details about failure </failure>");
            builder.append("</testcase>");
        }
        builder.append("</testsuite>");
        
        FileWriter fw = new FileWriter(f,false);
        fw.write(builder.toString());
        fw.close();
        return randNumber;
        
    }
}
