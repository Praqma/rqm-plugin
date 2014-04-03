/*
 * The MIT License
 *
 * Copyright 2014 mads.
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
import hudson.tasks.BuildStep;
import java.io.IOException;
import java.util.List;
import net.praqma.jenkins.rqm.RqmBuilder;
import net.praqma.jenkins.rqm.RqmCollector;
import net.praqma.jenkins.rqm.RqmCollectorDescriptor;
import net.praqma.jenkins.rqm.RqmObjectCreator;
import net.praqma.jenkins.rqm.model.RQMObject;
import net.praqma.jenkins.rqm.model.TestCase;
import net.praqma.jenkins.rqm.model.TestPlan;
import net.praqma.jenkins.rqm.model.TestScript;
import net.praqma.jenkins.rqm.model.TestSuite;
import net.praqma.jenkins.rqm.request.RqmParameterList;
import org.apache.commons.httpclient.NameValuePair;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 *
 * @author mads
 */
public class RqmTestSuiteCollectionStrategy extends RqmCollector {
    public final String suiteNames;
    public final String planName;
    public final String projectName;
    
    @DataBoundConstructor
    public RqmTestSuiteCollectionStrategy(final String planName, final String projectName, final String suiteNames ) {
        this.suiteNames = suiteNames;
        this.planName = planName;
        this.projectName = projectName;
    }
    
    @Extension
    public static class RqmTestSuiteCollectionStrategyImpl extends RqmCollectorDescriptor {
        
        @Override
        public String getDisplayName() {
            return "Test suite collection stategy";
        }        
    }

    @Override
    public  <T extends RQMObject> T collect(BuildListener listener, AbstractBuild<?, ?> build) throws Exception {
        TestPlan plan = new TestPlan(planName);
        String planRequestFeed = plan.getFeedUrlForTestPlans(getHostName(), getPort(), getContextRoot(), projectName);

        NameValuePair[] requestParameters = plan.getParametersForFeedUrlForTestPlans();
        RqmParameterList list = new RqmParameterList(getHostName(), getPort(), getContextRoot(), projectName, getUsrName(), getPasswd(), planRequestFeed, requestParameters, "GET", null);
  
        listener.getLogger().println("Getting TestPlans using feed url:");
        listener.getLogger().println(planRequestFeed+"?fields="+requestParameters[0].getValue());
        
        RqmObjectCreator<TestPlan> object = new RqmObjectCreator<TestPlan>(plan, list);
        plan = build.getWorkspace().act(object);
        return (T)plan;
    }

    @Override
    public boolean execute(AbstractBuild<?, ?> build, BuildListener listener, Launcher launcher, List<BuildStep> preBuildSteps, List<BuildStep> postBuildSteps, List<BuildStep> iterativeTestCaseBuilders) throws Exception {
        boolean success = true;
        
        final TestPlan plan = collect(listener, build);
        
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
                               RqmBuilder.addToEnvironment(env, tsuite.attributes());
                               RqmBuilder.addToEnvironment(env, plan.attributes());
                               RqmBuilder.addToEnvironment(env, tc.attributes());                         
                               RqmBuilder.addToEnvironment(env, ts.attributes());
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
}
