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

import com.cloudbees.plugins.credentials.Credentials;
import com.cloudbees.plugins.credentials.CredentialsMatcher;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardListBoxModel;
import com.cloudbees.plugins.credentials.common.StandardUsernameCredentials;
import com.cloudbees.plugins.credentials.common.UsernamePasswordCredentials;
import hudson.AbortException;
import hudson.EnvVars;
import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.ItemGroup;
import hudson.security.ACL;
import hudson.tasks.BuildStep;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.ListBoxModel;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;
import jenkins.model.Jenkins;
import net.praqma.jenkins.rqm.model.RqmObject;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

/**
 *
 * @author Praqma
 */
public class RqmBuilder extends Builder {
    private static final Logger log = Logger.getLogger(RqmBuilder.class.getName());
    public final List<BuildStep> preBuildSteps;
    public final List<BuildStep> postBuildSteps;
    public final List<BuildStep> iterativeTestCaseBuilders;
    public final RqmCollector collectionStrategy;
    private String credentialId;
     
    @DataBoundConstructor
    public RqmBuilder(RqmCollector collectionStrategy, final List<BuildStep> iterativeTestCaseBuilders, final List<BuildStep> preBuildSteps, final List<BuildStep> postBuildSteps, String credentialId) {        
        this.collectionStrategy = collectionStrategy;
        this.preBuildSteps = preBuildSteps;
        this.postBuildSteps = postBuildSteps;
        this.iterativeTestCaseBuilders = iterativeTestCaseBuilders;
        this.credentialId = credentialId;
        collectionStrategy.setup(getGlobalHostName(), getGlobalContextRoot(), getGlobalUsrName(), getGlobalPasswd(), getGlobalPort());        
    }
    
    @Deprecated
    public RqmBuilder(RqmCollector collectionStrategy, final List<BuildStep> iterativeTestCaseBuilders, final List<BuildStep> preBuildSteps, final List<BuildStep> postBuildSteps) {        
        this.collectionStrategy = collectionStrategy;
        this.preBuildSteps = preBuildSteps;
        this.postBuildSteps = postBuildSteps;
        this.iterativeTestCaseBuilders = iterativeTestCaseBuilders;        
        collectionStrategy.setup(getGlobalHostName(), getGlobalContextRoot(), getGlobalUsrName(), getGlobalPasswd(), getGlobalPort());        
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
        for (Entry<String, String> entry : values.entrySet()) {
            env.put(entry.getKey().toUpperCase(), entry.getValue().replace(" ", "_"));
        }
    }

    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
        PrintStream console = listener.getLogger();               
        console.println(Jenkins.getInstance().getPlugin("rqm-plugin").getWrapper().getVersion());
        
        boolean success = true;
        List<? extends RqmObject> results = null;
        try {
            console.println( String.format( "Connecting to RQM server @ %s", getGlobalHostName()) );
            console.println( String.format( "Using port %s", getGlobalPort()) );
            console.println( String.format( "Context root is '%s'", getGlobalContextRoot()));
            collectionStrategy.setup(getGlobalHostName(), getGlobalContextRoot(), getGlobalUsrName(), getGlobalPasswd(), getGlobalPort());
            results = collectionStrategy.withCredentials(credentialId).collect(listener, build);            
            success = collectionStrategy.withCredentials(credentialId).execute(build, listener, launcher, preBuildSteps, postBuildSteps, iterativeTestCaseBuilders, results);            
        } catch (TimeoutException ex) {
            console.println(String.format("Unable to complete retrieval of RQM data, timeout exceeded"));
            log.logp(Level.SEVERE, this.getClass().getName(), "perform", "Timeout", ex);
            success = false;
            throw new AbortException("Unable to complete retrieval of RQM data, timeout exceeded. Trace written to log.");
        } catch (Exception ex) {
            success = false;
            console.println(String.format("Failed to retrieve relevant test data.%n%s", ex.getMessage()));
            log.logp(Level.SEVERE, this.getClass().getName(), "perform", "Failed to retrieve relavant test data", ex);
            throw new AbortException("Error in retrieving data from RQM, trace written to log");
        } finally {            
            RqmBuildAction action = new RqmBuildAction(results);
            action.setIndex(build.getActions(RqmBuildAction.class).size()+1);
            build.addAction(action);
        }
 
        return success;
    }

    /**
     * @return the credentialId
     */
    public String getCredentialId() {
        return credentialId;
    }

    /**
     * @param credentialId the credentialId to set
     */
    public void setCredentialId(String credentialId) {
        this.credentialId = credentialId;
    }
    
    @Extension
    public static class RqmDescriptor extends BuildStepDescriptor<Builder> {
        
        private String contextRoot, hostName, usrName, passwd;
        private int port;
 
        public RqmDescriptor() {
            super(RqmBuilder.class);
            load();
        }
        
        @Override
        public boolean isApplicable(Class<? extends AbstractProject> arg0) {
            return true;
        }
        
        public List<hudson.model.Descriptor<? extends BuildStep>> getApplicableBuildSteps(AbstractProject<?, ?> p) {
            List<hudson.model.Descriptor<? extends BuildStep>> list = new ArrayList<hudson.model.Descriptor<? extends BuildStep>>();
            list.addAll(Builder.all());
            return list;
        }
        
        public List<RqmCollectorDescriptor> getCollectionStrategies() {
            List<RqmCollectorDescriptor> descriptors = new ArrayList<RqmCollectorDescriptor>();
            descriptors.addAll(RqmCollector.getDescriptors());
            return descriptors;
        }
        
        public ListBoxModel doFillCredentialIdItems(final @AncestorInPath ItemGroup<?> context) {
            final List<StandardUsernameCredentials> credentials = CredentialsProvider.lookupCredentials(StandardUsernameCredentials.class, context, ACL.SYSTEM);            
            StandardListBoxModel model2 = (StandardListBoxModel) new StandardListBoxModel().withEmptySelection().withMatching(new CredentialsMatcher() {
                @Override
                public boolean matches(Credentials item) {
                    return item instanceof UsernamePasswordCredentials;
                }
            }, credentials);            
            return model2;
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
            return super.configure(req, json);
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