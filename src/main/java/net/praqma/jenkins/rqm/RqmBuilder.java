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

import hudson.AbortException;
import hudson.EnvVars;
import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Action;
import hudson.model.BuildListener;
import hudson.model.EnvironmentContributingAction;
import hudson.model.TaskListener;
import hudson.tasks.BuildStep;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import jenkins.model.Jenkins;
import net.praqma.jenkins.rqm.model.TestCase;
import net.praqma.jenkins.rqm.model.TestPlan;
import net.praqma.jenkins.rqm.model.TestScript;
import net.praqma.jenkins.rqm.model.exception.RequestException;
import net.praqma.jenkins.rqm.request.RqmParameterList;
import net.praqma.util.structure.Tuple;
import net.sf.json.JSONObject;
import org.apache.commons.httpclient.NameValuePair;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

/**
 *
 * @author Praqma
 */
public class RqmBuilder extends Builder {
    
    public final String projectName, contextRoot, usrName, passwd, hostName, customProperty, planName;
    public final int port;
    private static final Logger log = Logger.getLogger(RqmBuilder.class.getName());
    public final List<BuildStep> iterativeTestCaseBuilders;
    public final List<BuildStep> singleTestCaseBuilder;
     
    @DataBoundConstructor
    public RqmBuilder(final String projectName, final String contextRoot, final String usrName, final String passwd, final int port, final String hostName, final String customProperty, final String planName, final List<BuildStep> iterativeTestCaseBuilders, final List<BuildStep> singleTestCaseBuilder) {
        this.projectName = projectName;
        this.contextRoot = contextRoot;
        this.hostName = hostName;
        this.usrName = usrName;
        this.passwd = passwd;
        this.port = port;
        this.customProperty = customProperty;
        this.planName = planName;
        this.iterativeTestCaseBuilders = iterativeTestCaseBuilders;
        this.singleTestCaseBuilder = singleTestCaseBuilder;
        System.out.println("Size of:" +" "+iterativeTestCaseBuilders.size());
    }
    
    public void executeIterativeTest(AbstractBuild<?,?> build, BuildListener listener, Launcher launcher, Set<TestCase> testCases) throws InterruptedException, IOException {
        for(final TestCase tc : testCases) {           
            for(BuildStep step : iterativeTestCaseBuilders) {
                build.addAction(new EnvironmentContributingAction() {
                    @Override                    
                    public void buildEnvVars(AbstractBuild<?, ?> build, EnvVars env) {                                                
                        for (TestScript ts : tc.getScripts()) {
                            env.put(customProperty.toUpperCase().replace(" ", "_"), new Random().nextInt(10)+"");
                        }
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
                });
                step.perform(build, launcher, listener);
            }
        }
    }
    
    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
        PrintStream console = listener.getLogger();
        
        console.println(Jenkins.getInstance().getPlugin("rqm-plugin").getWrapper().getVersion());
        console.println(String.format("Project name: %s", projectName));
        console.println(String.format("Test plan name: %s", planName));    
        console.println(String.format("QM Server Port: %s", port));
        console.println(String.format("QM Server Host Name: %s", hostName));
        console.println(String.format("Context root: %s", contextRoot));
        console.println(String.format("Username: %s", usrName));
        console.println(String.format("Password: %s", passwd));
        console.println(String.format("Custom field: %s", customProperty));                       
        
        Tuple<Integer,String> res = null;
        TestPlan plan;
        RQMBuildAction action;
        boolean success = true;
        
        try {
            /*
             * Step 1 - Grab a feed with all the needed info for the selected test plan
             */
            
            plan = new TestPlan(planName);
            String planRequestFeed = plan.getFeedUrlForTestPlans(hostName, port, contextRoot, projectName);
            NameValuePair[] requestParameters = plan.getParametersForFeedUrlForTestPlans();
            
            console.println("Getting TestPlans using feed url:");
            console.println(planRequestFeed+"?fields="+requestParameters[0].getValue());
            
            RqmParameterList list = new RqmParameterList(hostName, port, contextRoot, projectName, usrName, passwd, planRequestFeed, requestParameters, "GET", null);
            
            plan = build.getWorkspace().act(new RqmObjectCreator<TestPlan>(new TestPlan(planName), list));
            
            console.println(String.format( "Test cases in total: %s", plan.getAllTestCases().size() ));
            console.println(String.format( "Test cases with scripts in total: %s", plan.getAllTestCasesWithScripts().size()));
            console.println(String.format( "Test cases with scripts having the specified custom property value(s): %s", plan.getTestCaseHavingCustomFieldWithName(customProperty).size()));
            
            
            
            //TestPlan planAfterRun = (TestPlan) build.getWorkspace().act(new RQMTestCaseScriptExecutor(plan, customProperty));
            
            executeIterativeTest(build, listener, launcher, plan.getTestCaseHavingCustomFieldWithName(customProperty.split(",")));
            
            /*
            for(BuildStep bs : iterativeTestCaseBuilders) {

                build.addAction(new EnvironmentContributingAction() {

                    @Override
                    public void buildEnvVars(AbstractBuild<?, ?> build, EnvVars env) {
                        env.put(customProperty.toUpperCase().replace(" ", "_"), new Random().nextInt(10)+"");
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
                });
                
                bs.perform(build, launcher, listener);
            }
            */ 
       
            
            /* UNCOMMENTED FOR TESTING */
            /*
            for(TestCase tc : planAfterRun.getTestCaseHavingCustomFieldWithName(customProperty)) {
                JUnitParser parser = new JUnitParser(false);
                TestResult tr = null;
                try {                    
                    tr = parser.parse(String.format("tc_%s/*.xml",tc.getInternalId()), build, launcher, listener);                    
                } catch (Exception ex) {                    
                    tc.setOverallResult(TestCase.TestCaseTestResultStatus.NOT_TESTED);
                }
                
                if(tr != null ) {
                    if(tr.getFailCount() > 0) {
                        tc.setOverallResult(TestCase.TestCaseTestResultStatus.FAIL);
                    } else {
                        tc.setOverallResult(TestCase.TestCaseTestResultStatus.PASS);
                    }
                }                
                
                if(!tc.isPass()) {
                    build.setResult(Result.UNSTABLE);
                }
            }
            */
            /*
            for(TestCase tc : planAfterRun.getTestCaseHavingCustomFieldWithName(customProperty)) {
                console.println(tc);
            }
            */ 
            console.println("Publishing results to RQM");            
            
            /*
            for(TestCase tc : planAfterRun.getTestCaseHavingCustomFieldWithName(customProperty)) {                
                //Step 1: Create the test result based on the executed script.                                
                TestCaseExecutionRecord tcer = new TestCaseExecutionRecord(tc);
                String putRequestUrl = RQMUtilities.getSingleResourceBaseUrlWithId(contextRoot, hostName, port, projectName, "executionworkitem", tc.getExternalAssignedTestCaseExecutonRecordId());
                list.requestString = putRequestUrl;
                list.methodType = "PUT";                
                build.getWorkspace().act(new RqmObjectCreator<TestCaseExecutionRecord>(tcer, list));                
                
                //Step 2: Create the test execution record with the testcase and the results.
                TestExecutionResult ter = new TestExecutionResult(tcer);
                list.requestString = RQMUtilities.getSingleResourceBaseUrlWithId(contextRoot, hostName, port, projectName, "executionresult", tc.getExternalAssignedTestCaseResultId());
                build.getWorkspace().act(new RqmObjectCreator<TestExecutionResult>(ter, list));
            }
            */
            console.println("Done publishing results");
            //action = new RQMBuildAction(planAfterRun, customProperty, build);
            
            action = new RQMBuildAction(plan, usrName, build);
            build.getActions().add(action);

        } catch (Exception ex) {
            if(ex instanceof RequestException) {
                console.println( String.format( "Return code: %s", ((RequestException)ex).result.t1 ) );                
            }
            
            console.println("Failed to retrieve relevant test data, error when discarding wrapper exception was:");
            ex.printStackTrace(console);
            log.logp(Level.SEVERE, this.getClass().getName(), "perform", "Failed to retrieve relavant test data", ex);
            throw new AbortException("Error in retrieving data from RQM, trace written to log");
        }
 
        return success;
    }

    @Override
    public Action getProjectAction(AbstractProject<?, ?> project) {
        return new RQMProjectAction(project);
    }
    
    @Extension
    public static class Descriptor extends BuildStepDescriptor<Builder> {

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> arg0) {
            return true;
        }
        
        public List<hudson.model.Descriptor<? extends BuildStep>> getApplicableBuildSteps(AbstractProject<?, ?> p) {
            List<hudson.model.Descriptor<? extends BuildStep>> list = new ArrayList<hudson.model.Descriptor<? extends BuildStep>>();
            list.addAll(Builder.all());
            System.out.println(list.size());
            
            return list;
        }
        
        public final Class<RqmBuilder> rqmBuilderType = RqmBuilder.class;

        @Override
        public String getDisplayName() {
            return "RQM Build Publisher";
        }

        @Override
        public Builder newInstance(StaplerRequest req, JSONObject formData) throws FormException {
            RqmBuilder builder = req.bindJSON(RqmBuilder.class, formData);
            save();
            return builder;
        }        
    }
}
