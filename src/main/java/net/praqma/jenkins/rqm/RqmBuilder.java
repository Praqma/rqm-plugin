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

import net.praqma.jenkins.rqm.collector.DummyCollectionStrategy;
import hudson.AbortException;
import hudson.EnvVars;
import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.EnvironmentContributingAction;
import hudson.tasks.BuildStep;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import jenkins.model.Jenkins;
import net.praqma.jenkins.rqm.model.TestCase;
import net.praqma.jenkins.rqm.model.TestPlan;
import net.praqma.jenkins.rqm.model.TestScript;
import net.praqma.jenkins.rqm.model.TestSuite;
import net.praqma.jenkins.rqm.model.exception.RequestException;
import net.praqma.jenkins.rqm.request.RqmParameterList;
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
    private static final Logger log = Logger.getLogger(RqmBuilder.class.getName());
    public final List<BuildStep> preTestBuildSteps;
    public final List<BuildStep> postTestBuildSteps;
    public final List<BuildStep> iterativeTestCaseBuilders;
    public final RqmCollector collector;
     
    @DataBoundConstructor
    public RqmBuilder(RqmCollector collectionStrategy, final List<BuildStep> iterativeTestCaseBuilders, final List<BuildStep> preTestBuildSteps, final List<BuildStep> postTestBuildSteps) {        
        this.collector = collectionStrategy;
        this.preTestBuildSteps = preTestBuildSteps;
        this.postTestBuildSteps = postTestBuildSteps;
        this.iterativeTestCaseBuilders = iterativeTestCaseBuilders;
        collector.setup(getGlobalHostName(), getGlobalContextRoot(), getGlobalUsrName(), getGlobalPasswd(), getGlobalPort());
        //this.collector = new RqmTestSuiteCollectionStrategy(getGlobalPort(), getGlobalHostName(), getGlobalContextRoot(), getGlobalUsrName(), getGlobalPasswd(), planName, projectName, suiteNames);
    }

    public final int getGlobalPort() {
        return ((RqmBuilder.RqmDescriptor)getDescriptor()).getPort();
    }
    
    public final String getGlobalHostName() {
        return ((RqmBuilder.RqmDescriptor)getDescriptor()).getHostName();
    }
    
    public final String getGlobalContextRoot() {
        return ((RqmBuilder.RqmDescriptor)getDescriptor()).getContextRoot();
    }
    
    public final String getGlobalUsrName() {
        return ((RqmBuilder.RqmDescriptor)getDescriptor()).getUsrName();
    }
    
    public final String getGlobalPasswd() {
        return ((RqmBuilder.RqmDescriptor)getDescriptor()).getPasswd();
    }
        
    /**
     * Scrubs the attributes from RQM uppercasing them, and removing spaces in values
     * @param env
     * @param values 
     */
    public static void addToEnvironment(EnvVars env, HashMap<String,String> values) {
        for (String key : values.keySet()) {
            env.put(key.toUpperCase(), values.get(key).replace(" ", "_"));
        }
    }
    
    public boolean executeIterativeTest(AbstractBuild<?,?> build, BuildListener listener, Launcher launcher, final TestPlan plan, final List<BuildStep> preBuildSteps, final List<BuildStep> postBuildSteps) throws InterruptedException, IOException {
        
        boolean success = true;
        
        if(preBuildSteps != null) {
            listener.getLogger().println(String.format("Performing pre build step"));
            for (BuildStep bs : preBuildSteps) {                
                success &= bs.perform(build, launcher, listener);
            }
        }
        
        for(final TestSuite tsuite : plan.getTestSuites()) {            
            for(final TestCase tc : tsuite.getTestcases()) {
                if(tc.getScripts().isEmpty()) {
                    listener.getLogger().println(String.format("Skipping test case %s, no scripts attached", tc.getTestCaseTitle()));                
                    continue;
                }

                for(final TestScript ts : tc.getScripts()) {
                    for(BuildStep step : iterativeTestCaseBuilders) {
                       build.addAction(new EnvironmentContributingAction() {
                           @Override                    
                           public void buildEnvVars(AbstractBuild<?, ?> build, EnvVars env) {
                               addToEnvironment(env, tsuite.attributes());
                               addToEnvironment(env, plan.attributes());
                               addToEnvironment(env, tc.attributes());                         
                               addToEnvironment(env, ts.attributes());
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
                       
                       listener.getLogger().println(String.format( "Executing test case %s", tc.getTestCaseTitle() ) );                       
                       success &= step.perform(build, launcher, listener);

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

    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
        PrintStream console = listener.getLogger();        
        
        console.println(Jenkins.getInstance().getPlugin("rqm-plugin").getWrapper().getVersion());
        
        TestPlan plan = null;
        boolean success = true;
        try {

            plan = (TestPlan)collector.collect(listener, build);            
            console.println(String.format( "Executing tests for %s testcases", plan.getAllTestCases().size() ));            
            success = executeIterativeTest(build, listener, launcher, plan, preTestBuildSteps, postTestBuildSteps);

        } catch (Exception ex) {

            console.println("Failed to retrieve relevant test data. Fulle trace writen to log");
            ex.printStackTrace(console);
            
            log.logp(Level.SEVERE, this.getClass().getName(), "perform", "Failed to retrieve relavant test data", ex);
            throw new AbortException("Error in retrieving data from RQM, trace written to log");
        } finally {
            if(!success) {
                console.println("Error caught in test execution, review log for details");
            }
            RQMBuildAction action = new RQMBuildAction(plan, build, null);
            build.getActions().add(action);
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
        
        @Override
        public String getDisplayName() {
            return "RQM TestScript Iterator";
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
