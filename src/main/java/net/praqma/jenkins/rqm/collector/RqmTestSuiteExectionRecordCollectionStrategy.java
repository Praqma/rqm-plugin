/*
 * The MIT License
 *
 * Copyright 2014 Mads.
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

package net.praqma.jenkins.rqm.collector;

import hudson.EnvVars;
import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.EnvironmentContributingAction;
import hudson.model.Result;
import hudson.tasks.BuildStep;
import java.util.List;
import java.util.logging.Logger;
import net.praqma.jenkins.rqm.RqmBuilder;
import net.praqma.jenkins.rqm.RqmCollector;
import net.praqma.jenkins.rqm.RqmCollectorDescriptor;
import net.praqma.jenkins.rqm.RqmObjectCreator;
import net.praqma.jenkins.rqm.model.RqmObject;
import net.praqma.jenkins.rqm.model.TestCase;
import net.praqma.jenkins.rqm.model.TestScript;
import net.praqma.jenkins.rqm.model.TestSuiteExecutionRecord;
import net.praqma.jenkins.rqm.request.RqmParameterList;
import org.apache.commons.httpclient.NameValuePair;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 * Step 1 : Get a feed of all testsuiterecords within the plan
 * Step 2 : Select the test cases associated with testsuiteexecutionrecord. This has to be done though a testsuite 
 * 
 * 
 * @author Mads
 */
public class RqmTestSuiteExectionRecordCollectionStrategy extends RqmCollector {
    
    private static final Logger log = Logger.getLogger(RqmTestSuiteExectionRecordCollectionStrategy.class.getName());
    public final String executionRecordName;
    public final String planName;
    public final String projectName;
    
    @DataBoundConstructor
    public RqmTestSuiteExectionRecordCollectionStrategy(final String executionRecordName, final String planName, final String projectName) {
        this.executionRecordName = executionRecordName;
        this.planName = planName;
        this.projectName = projectName;
    }
    
    @Extension
    public static class RqmTestSuiteCollectionStrategyImpl extends RqmCollectorDescriptor {
        
        @Override
        public String getDisplayName() {
            return "Test suite exection record selection stategy";
        }        
    }

    @Override
    public <T extends RqmObject> List<T> collect(BuildListener listener, AbstractBuild<?, ?> build) throws Exception {
        NameValuePair[] filterProperties = TestSuiteExecutionRecord.getFilteringProperties(planName, getPort(), planName, executionRecordName);
        String request = TestSuiteExecutionRecord.getResourceFeedUrl(getHostName(), getPort(), getContextRoot(), projectName);        
        RqmParameterList list = new RqmParameterList(getHostName(), getPort(), getContextRoot(), projectName, getUsrName(), getPasswd(), request, filterProperties, "GET", null);
        log.fine(list.toString());
        RqmObjectCreator<TestSuiteExecutionRecord> object = new RqmObjectCreator<TestSuiteExecutionRecord>(TestSuiteExecutionRecord.class, list);        
        return (List<T>)build.getWorkspace().act(object);
    }

    @Override
    public boolean execute(AbstractBuild<?, ?> build, BuildListener listener, Launcher launcher, List<BuildStep> preBuildSteps, List<BuildStep> postBuildSteps, List<BuildStep> iterativeTestCaseBuilders, List<? extends RqmObject> results) throws Exception {
        
        boolean success = true;
        List<TestSuiteExecutionRecord> records = (List<TestSuiteExecutionRecord>)results;
        
        if(preBuildSteps != null) {
            listener.getLogger().println(String.format("Performing pre build step"));
            for (BuildStep bs : preBuildSteps) {                
                success &= bs.perform(build, launcher, listener);
            }
        }
        
        for(final TestSuiteExecutionRecord rec : records) {            
            listener.getLogger().println(String.format( "Test Suite %s [%s] ", rec.getTestSuite().getTestSuiteTitle(), rec.getTestSuite().getRqmObjectResourceUrl()  ));
            listener.getLogger().println(String.format( "Test Suite Execution Record %s [%s]",rec.getTestSuiteExecutionRecordTitle(), rec.getRqmObjectResourceUrl()) );
            for(final TestCase tc : rec.getTestSuite().getTestcases()) {
                listener.getLogger().println(String.format( " Test Case %s [%s]",tc.getTestCaseTitle(), tc.getRqmObjectResourceUrl()) );
                
                if(tc.getScripts().isEmpty()) {
                    listener.getLogger().println("Test case %s does not contain any scripts, setting result to unstable");
                    build.setResult(Result.UNSTABLE);
                }
                
                for(final TestScript ts : tc.getScripts()) {                    
                    listener.getLogger().println(String.format( " * Test Script %s [%s]",ts.getScriptTitle(), ts.getRqmObjectResourceUrl()) );
                    for(BuildStep bstep : iterativeTestCaseBuilders) {
                        
                        final EnvironmentContributingAction envAction = new EnvironmentContributingAction() {
                            @Override
                            public void buildEnvVars(AbstractBuild<?, ?> ab, EnvVars ev) {
                                RqmBuilder.addToEnvironment(ev, rec.getTestSuite().attributes());
                                RqmBuilder.addToEnvironment(ev, rec.getTestPlan().attributes());
                                RqmBuilder.addToEnvironment(ev, tc.attributes());                         
                                RqmBuilder.addToEnvironment(ev, ts.attributes());
                            }

                            @Override
                            public String getIconFileName() {
                                return null;
                            }

                            @Override
                            public String getDisplayName() {
                                return null;
                            }

                            @Override
                            public String getUrlName() {
                                return null;
                            }
                        };
                        
                        build.addAction(envAction);
                        
                        success &= bstep.perform(build, launcher, listener);
                        build.getActions().remove(envAction);
                    }                    
                }
            }
        }
        
        if(postBuildSteps != null) {
            listener.getLogger().println(String.format("Performing post build step"));
            for(BuildStep bs : postBuildSteps) {
                success &= bs.perform(build, launcher, listener);
            }
        }
        
        return success;
    }
    
    
    
}
