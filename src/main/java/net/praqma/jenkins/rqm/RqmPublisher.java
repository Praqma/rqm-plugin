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

import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Action;
import hudson.model.BuildListener;
import hudson.model.FreeStyleProject;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Publisher;
import hudson.tasks.Recorder;
import hudson.util.FormValidation;
import java.io.IOException;
import java.io.PrintStream;
import java.util.HashSet;
import jenkins.model.Jenkins;
import net.praqma.jenkins.rqm.model.TestCase;
import net.praqma.jenkins.rqm.model.TestCaseExecutionRecord;
import net.praqma.jenkins.rqm.model.TestExecutionResult;
import net.praqma.jenkins.rqm.model.TestPlan;
import net.praqma.jenkins.rqm.model.TestScript;
import net.praqma.jenkins.rqm.model.TestSuite;
import net.praqma.jenkins.rqm.model.exception.RequestException;
import net.praqma.jenkins.rqm.request.RQMUtilities;
import net.praqma.jenkins.rqm.request.RqmParameterList;
import net.praqma.util.structure.Tuple;
import net.sf.json.JSONObject;
import org.apache.commons.httpclient.NameValuePair;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

/**
 *
 * @author Praqma
 */
public class RqmPublisher extends Recorder {

    public final String projectName, contextRoot, usrName, passwd, hostName, customProperty, planName;
    public final int port;
    
    @Override
    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.BUILD;
    }
    
    @DataBoundConstructor
    public RqmPublisher(final String projectName, final String contextRoot, final String usrName, final String passwd, final int port, final String hostName, final String customProperty, final String planName) {
        this.projectName = projectName;
        this.contextRoot = contextRoot;
        this.hostName = hostName;
        this.usrName = usrName;
        this.passwd = passwd;
        this.port = port;
        this.customProperty = customProperty;
        this.planName = planName;
    }

    @Override
    public Action getProjectAction(AbstractProject<?, ?> project) {
        return new RQMProjectAction(project);
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
   
            for(TestCase testcase : plan.getTestCases() ) {
                list.requestString = testcase.getRqmObjectResourceUrl();
                build.getWorkspace().act(new RqmObjectCreator<TestCase>(testcase, list));
                for(TestScript script : testcase.getScripts()) {
                    list.requestString = script.getRqmObjectResourceUrl();
                    build.getWorkspace().act(new RqmObjectCreator<TestScript>(script, list));
                }
            }
                    
            for(TestSuite suite : plan.getTestSuites()) {
                list.requestString = suite.getRqmObjectResourceUrl();
                build.getWorkspace().act(new RqmObjectCreator<TestSuite>(suite, list));
                for(TestCase testcase : suite.getTestcases()) {
                    list.requestString = testcase.getRqmObjectResourceUrl();
                    build.getWorkspace().act(new RqmObjectCreator<TestCase>(testcase, list));
                    for(TestScript script : testcase.getScripts()) {
                        list.requestString = script.getRqmObjectResourceUrl();
                        build.getWorkspace().act(new RqmObjectCreator<TestScript>(script, list));                 
                    }
                } 
            }
            
            action = new RQMBuildAction(plan, build);
            
            console.println(String.format( "Test cases in total: %s", plan.getAllTestCases().size() ));
            console.println(String.format( "Test cases with scripts in total: %s", plan.getAllTestCasesWithScripts().size()));
            console.println(String.format( "Test cases with scripts having the specified custom property value: %s", plan.getTestCaseHavingCustomFieldWithName(customProperty).size()));
            
            HashSet<TestCase> interestingCases = plan.getTestCaseHavingCustomFieldWithName(customProperty);
            
            for(TestCase tc : interestingCases) {
                build.getWorkspace().act(new RQMTestCaseScriptExecutor(tc, customProperty));
                if(!tc.isPass()) {
                    success = false;
                }
            }
            
            for(TestCase tc : interestingCases) {
                console.println(tc);
            }
            
            console.println("Publishing results to RQM");            
            
            for(TestCase tc : interestingCases) {                
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
            
            console.println("Done publishing results");
            
            build.getActions().add(action);

        } catch (Exception ex) {
            if(ex instanceof RequestException) {
                console.println( String.format( "Return code: %s", ((RequestException)ex).result.t1 ) );
            }
            
            console.println("Failed to retrieve relevant test data, error when discarding wrapper exception was:");
            ex.printStackTrace(console);
            success = false;
        }
 
        return success;
    }
    
    @Extension
    public static final class DescriptorImpl extends BuildStepDescriptor<Publisher> {

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> type) {
            return type.equals(FreeStyleProject.class);
        }

        @Override
        public String getDisplayName() {
            return "RQM Test Aggregator";
        }
        
        public DescriptorImpl() {
            super(RqmPublisher.class);
            load();
        }
        
        @Override
        public Publisher newInstance(StaplerRequest req, JSONObject formData) throws FormException {
            RqmPublisher publisher = req.bindJSON(RqmPublisher.class, formData);
            save();
            return publisher;
            
        }
        
        public FormValidation doCheckPort(@QueryParameter String value) {
            try {
                Integer val = Integer.parseInt(value);
                if(val <= 0) { 
                    return FormValidation.error("Illegal port number, must be a number and be above 0");
                }
                return FormValidation.ok();
            } catch (NumberFormatException nfe) {
                return FormValidation.error("Must be a number, your selection conatains non-numerical characters");
            }
        }
        
    } 
}
