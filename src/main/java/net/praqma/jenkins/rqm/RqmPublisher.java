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
import hudson.model.BuildListener;
import hudson.model.FreeStyleProject;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Publisher;
import hudson.tasks.Recorder;
import hudson.util.FormValidation;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import jenkins.model.Jenkins;
import net.praqma.jenkins.rqm.model.TestCase;
import net.praqma.jenkins.rqm.model.TestPlan;
import net.praqma.jenkins.rqm.model.TestScript;
import net.praqma.jenkins.rqm.model.TestSuite;
import net.praqma.jenkins.rqm.model.exception.RequestException;
import net.praqma.util.structure.Tuple;
import net.sf.json.JSONObject;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.NameValuePair;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

/**
 *
 * @author Praqma
 */
public class RqmPublisher extends Recorder {

    public final String projectName,contextRoot,usrName,passwd,hostName,customProperty,planName;
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
               
        Tuple<Integer,String> res = null;
        TestPlan plan;
        try {

            /*
             * Step 1 - Grab a feed with all the needed info for the selected test plan
             */
            plan = new TestPlan(planName);
            String planRequestFeed = plan.getFeedUrlForTestPlans(hostName, port, contextRoot, projectName);
            NameValuePair[] requestParameters = plan.getParametersForFeedUrlForTestPlans();
            
            console.println("Getting TestPlans using feed url:");
            console.println(planRequestFeed+"?fields="+requestParameters[0].getValue());
            
            res = build.getWorkspace().act(new RQMMethodInvoker(hostName, port, contextRoot, projectName, usrName, passwd, planRequestFeed, requestParameters));
            plan.initializeSingleResource(res.t2);
            List<TestScript> scripts3 = new ArrayList<TestScript>();
            
            for(TestCase testcase : plan.getTestCases() ) {                  
                res = build.getWorkspace().act(new RQMMethodInvoker(hostName, port, contextRoot, projectName, usrName, passwd, testcase.getRqmObjectResourceUrl(), null));
                testcase.initializeSingleResource(res.t2);
                for(TestScript script : testcase.getScripts()) {
                    res = build.getWorkspace().act(new RQMMethodInvoker(hostName, port, contextRoot, projectName, usrName, passwd, script.getRqmObjectResourceUrl(), null));
                    script.initializeSingleResource(res.t2);
                }
                
                scripts3.addAll(build.getWorkspace().act(new RQMScriptExecutor(testcase.getScripts(), customProperty)));
            }
            
            console.println("====TEST CASE SCRIPTS NOT INI SUITE====");
            
            console.println(scripts3);
            
            console.println("====TEST CASE SCRIPTS NOT INI SUITE====");
            
            
            List<TestScript> scripts2 = new ArrayList<TestScript>();
                    
            for(TestSuite suite : plan.getTestSuites()) {
                res = build.getWorkspace().act(new RQMMethodInvoker(hostName, port, contextRoot, projectName, usrName, passwd, suite.getRqmObjectResourceUrl(), null));
                suite.initializeSingleResource(res.t2);
                for(TestCase testcase : suite.getTestcases()) {
                    res = build.getWorkspace().act(new RQMMethodInvoker(hostName, port, contextRoot, projectName, usrName, passwd, testcase.getRqmObjectResourceUrl(), null));
                    testcase.initializeSingleResource(res.t2);
                    for(TestScript script : testcase.getScripts()) {
                        res = build.getWorkspace().act(new RQMMethodInvoker(hostName, port, contextRoot, projectName, usrName, passwd, script.getRqmObjectResourceUrl(), null));
                        script.initializeSingleResource(res.t2);
                        
                    }
                    //Execute scripts!
                    scripts2.addAll(build.getWorkspace().act(new RQMScriptExecutor(testcase.getScripts(), customProperty)));
                }
 
            }
            
            console.println(plan);
            console.println("=======AFTER=======");
            console.print(scripts2);
            RQMBuildAction action = new RQMBuildAction(plan, build);
            build.addAction(action);
            
            /*
            String testCaseResourceUrl = "http://dkplan01.emea.group.grundfos.com:9080/qm/service/com.ibm.rqm.integration.service.IIntegrationService/resources/Team+Test+%28Testing%29/testscript/urn:com.ibm.rqm:testscript:588";
   
            res = build.getWorkspace().act(new RQMMethodInvoker(hostName, port, contextRoot, projectName, usrName, passwd, testCaseResourceUrl, null));


            console.println( String.format( "Return code: %s", res.t1 ) );
            console.println( res.t2 );

            List<TestScript> scripts = new ArrayList<TestScript>();
            TestScript script = new TestScript().initializeSingleResource(res.t2);  
            scripts.add(script);

            console.println(script);

            scripts = build.getWorkspace().act(new RQMScriptExecutor(scripts, customProperty));

            console.println("Script was executed. Returned object was:");
            for(TestScript s : scripts) {
                console.println(s);
            }
            */

        } catch (Exception ex) {
            if(ex instanceof RequestException) {
                console.println( String.format( "Return code: %s", ((RequestException)ex).result.t1 ) );
            }
            
            console.println("Failed to retrieve relevant test data, error when discarding wrapper exception was:");
            ex.printStackTrace(console);
            return false;
        }
        
        build.getActions().add(new RQMBuildAction(plan,build));
        
        return res != null && res.t1.equals(HttpStatus.SC_OK);
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
        
        public FormValidation doCheckPort(@QueryParameter String param) {
            try {
                Integer value = Integer.parseInt(param);
                if(value <= 0) { 
                    return FormValidation.error("Illegal port number, must be a number and be above 0");
                }
                return FormValidation.ok();
            } catch (NumberFormatException nfe) {
                return FormValidation.error("Illegal port number, must be a number and be above 0");
            }
        }
        
    } 
}
