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
import java.util.HashMap;
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
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

/**
 *
 * @author Praqma
 */
public class RqmBuilder extends Builder {
    
    public final String projectName, planName;
    private static final Logger log = Logger.getLogger(RqmBuilder.class.getName());
    public final List<BuildStep> preTestBuildSteps;
    public final List<BuildStep> postTestBuildSteps;
    public final List<BuildStep> iterativeTestCaseBuilders;
    public final List<BuildStep> singleTestCaseBuilder;
    public final String suiteNames;
     
    @DataBoundConstructor
    public RqmBuilder(final String projectName, final String planName, final String suiteNames, final List<BuildStep> iterativeTestCaseBuilders, final List<BuildStep> singleTestCaseBuilder) {
        this.projectName = projectName;
        this.planName = planName;
        this.iterativeTestCaseBuilders = iterativeTestCaseBuilders;
        this.singleTestCaseBuilder = singleTestCaseBuilder;
        this.preTestBuildSteps = null;
        this.postTestBuildSteps = null;
        this.suiteNames = StringUtils.isBlank(suiteNames) ? null : suiteNames;
    }
    /**
     * Scrubs the attributes from RQM uppercasing them, and removing spaces in values
     * @param env
     * @param values 
     */
    private void _scrubEnv(EnvVars env, HashMap<String,String> values) {
        for (String key : values.keySet()) {
            env.put(key.toUpperCase(), values.get(key).replace(" ", "_"));
        }
    }
    
    public void executeIterativeTest(AbstractBuild<?,?> build, BuildListener listener, Launcher launcher, final TestPlan plan, final List<BuildStep> preBuildSteps, final List<BuildStep> postBuildSteps) throws InterruptedException, IOException {
        
        if(preBuildSteps != null) {
            for (BuildStep bs : preBuildSteps) {
                listener.getLogger().println(String.format("Performing pre build step"));
                bs.perform(build, launcher, listener);
            }
        }
        
        for(final TestCase tc : plan.getAllTestCasesWithinSuites(suiteNames)) {
             for(BuildStep step : iterativeTestCaseBuilders) {
                build.addAction(new EnvironmentContributingAction() {
                    @Override                    
                    public void buildEnvVars(AbstractBuild<?, ?> build, EnvVars env) {
                        //Add relevant attributes from the plan to environment
                        _scrubEnv(env, plan.attributes());
                        //Add the test script attributes to enviroment for this test case
                        for (TestScript ts : tc.getScripts()) {                            
                            _scrubEnv(env, ts.attributes());
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
                
                if(!tc.getScripts().isEmpty()) {
                    listener.getLogger().println(String.format( "Executing test case %s", tc.getTestCaseTitle() ) );
                    step.perform(build, launcher, listener);
                } else {
                    listener.getLogger().println(String.format( "Skipping test case %s, no scripts attached", tc.getTestCaseTitle() ));
                }
            }
        }
        
        if(postBuildSteps != null) {
            for(BuildStep bs : postBuildSteps) {
                listener.getLogger().println(String.format("Performing post build step"));
                bs.perform(build, launcher, listener);
            }
        }
    }
    
    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
        PrintStream console = listener.getLogger();
        
        /**
         * Extract values from global configuration.
         */
        int port = ((RqmBuilder.RqmDescriptor)getDescriptor()).getPort();
        String hostName = ((RqmBuilder.RqmDescriptor)getDescriptor()).getHostName();
        String contextRoot = ((RqmBuilder.RqmDescriptor)getDescriptor()).getContextRoot();
        String usrName = ((RqmBuilder.RqmDescriptor)getDescriptor()).getUsrName();
        String passwd = ((RqmBuilder.RqmDescriptor)getDescriptor()).getPasswd();
        
        console.println(Jenkins.getInstance().getPlugin("rqm-plugin").getWrapper().getVersion());
        console.println(String.format("Project name: %s", projectName));
        console.println(String.format("Test plan name: %s", planName));    
        console.println(String.format("QM Server Port: %s", port));
        console.println(String.format("QM Server Host Name: %s", hostName));
        console.println(String.format("Context root: %s", contextRoot));
        console.println(String.format("Username: %s", usrName));
        console.println(String.format("Password: %s", passwd));
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
            console.println(String.format( "Test cases within selected suites %s",plan.getAllTestCasesWithinSuites(suiteNames)));
            executeIterativeTest(build, listener, launcher, plan, preTestBuildSteps, postTestBuildSteps);

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
            console.println("Publishing resualts to RQM");            
            
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
            RqmBuilder.RqmDescriptor desc = (RqmBuilder.RqmDescriptor)getDescriptor(); 
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
    
    @Extension
    public static class RqmDescriptor extends BuildStepDescriptor<Builder> {
        
        private String contextRoot,hostName,usrName,passwd;
        private int port;
        
        @Override
        public boolean isApplicable(Class<? extends AbstractProject> arg0) {
            return true;
        }
        
        public List<hudson.model.Descriptor<? extends BuildStep>> getApplicableBuildSteps(AbstractProject<?, ?> p) {
            List<hudson.model.Descriptor<? extends BuildStep>> list = new ArrayList<hudson.model.Descriptor<? extends BuildStep>>();
            list.addAll(Builder.all());
            return list;
        }
        
        public final Class<RqmBuilder> rqmBuilderType = RqmBuilder.class;

        @Override
        public String getDisplayName() {
            return "RQM TestScript Iteratior";
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject json) throws FormException {
            contextRoot = json.getString("contextRoot");
            usrName = json.getString("usrName");
            hostName = json.getString("hostName");
            passwd = json.getString("passwd");
            port = json.getInt("port");
            save();
            return true;
        }

        /**
         * @return the contextRoot
         */
        public String getContextRoot() {
            return contextRoot;
        }

        /**
         * @return the hostName
         */
        public String getHostName() {
            return hostName;
        }

        /**
         * @return the usrName
         */
        public String getUsrName() {
            return usrName;
        }

        /**
         * @return the passwd
         */
        public String getPasswd() {
            return passwd;
        }

        /**
         * @return the port
         */
        public int getPort() {
            return port;
        }
       
    }
}
