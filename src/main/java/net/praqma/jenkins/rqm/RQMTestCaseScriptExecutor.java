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
import java.io.IOException;
import java.util.HashSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.praqma.jenkins.rqm.model.RQMObject;
import net.praqma.jenkins.rqm.model.TestCase;
import net.praqma.jenkins.rqm.model.TestPlan;
import net.praqma.jenkins.rqm.model.TestScript;

/**
 *
 * @author Praqma
 */
public class RQMTestCaseScriptExecutor implements FileCallable<RQMObject> {

    private static final Logger log = Logger.getLogger(RQMTestCaseScriptExecutor.class.getName());
    public final RQMObject item;
    public final String customField;
    
    public RQMTestCaseScriptExecutor(RQMObject item, String customField) {
        this.item = item;
        this.customField = customField;
    }
    
    @Override
    public RQMObject invoke(File file, VirtualChannel vc) throws IOException, InterruptedException {
        if(item instanceof TestCase) {
            TestCase tc = (TestCase)item;
            for(TestScript script : tc.getScripts()) {            

                try {
                    File testCaseWorkspaceFolder = new File(file, String.format("tc_%s", tc.getInternalId()));
                    testCaseWorkspaceFolder.mkdir();
                    script.runScriptContainedInCustomField(customField, testCaseWorkspaceFolder);                                                               
                } catch (Exception ex) {
                    log.logp(Level.SEVERE, this.getClass().getName(), "invoke", "Error in script exectuion", ex);
                    throw new IOException("Caught exception in class RQMTestCaseScriptExectuor");
                }
            }         

            TestCase.TestCaseTestResultStatus result = tc.getAggregatedTestCaseResult();

            log.fine(String.format("Setting test case result to %s", result));
            tc.setOverallResult(result);
            return tc;
        } else if(item instanceof TestPlan) {
            try {
                TestPlan tp = (TestPlan)item;
                HashSet<TestCase> testCases = tp.getTestCaseHavingCustomFieldWithName(customField);
                
                for(TestCase tc : testCases) {
                    File testCaseWorkspaceFolder = new File(file, String.format("tc_%s", tc.getInternalId()));
                    for(TestScript tscr : tc.getScripts()) {
                        tscr.runScriptContainedInCustomField(customField, testCaseWorkspaceFolder);
                    }
                    TestCase.TestCaseTestResultStatus result = tc.getAggregatedTestCaseResult();
                    tc.setOverallResult(result);
                }
                
                return tp;
            } catch (Exception ex) {
                log.logp(Level.SEVERE, this.getClass().getName(), "invoke", "Error in script exectuion", ex);
                throw new IOException("Caught exception in class RQMTestCaseScriptExectuor");
            }
        }
        
        return null;
    }
}
