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
import hudson.model.BuildListener;
import hudson.tasks.BuildStep;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;
import jenkins.model.Jenkins;
import net.praqma.jenkins.rqm.model.RqmObject;
import net.sf.json.JSONObject;
import org.apache.commons.lang.StringUtils;
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
     
    @DataBoundConstructor
    public RqmBuilder(RqmCollector collectionStrategy, final List<BuildStep> iterativeTestCaseBuilders, final List<BuildStep> preBuildSteps, final List<BuildStep> postBuildSteps) {        
        this.collectionStrategy = collectionStrategy;
        this.preBuildSteps = preBuildSteps;
        this.postBuildSteps = postBuildSteps;
        this.iterativeTestCaseBuilders = iterativeTestCaseBuilders;
        collectionStrategy.setup(getGlobalHostName(), getGlobalContextRoot(), getGlobalUsrName(), getGlobalPasswd(), getGlobalPort());        
    }
    
    public boolean checkGlobaConfiguration() throws IOException {
        List<String> errors = new ArrayList<String>();
        
        if(getGlobalPort() == 0) {
            errors.add("Port not defined in global configuration");
        }
        
        if(StringUtils.isBlank(getGlobalHostName())) {
            errors.add("Hostname not defined in global configuration");
        }
        
        if(StringUtils.isBlank(getGlobalContextRoot())) {
            errors.add("Context root not defined in global configuration");
        }
        
        if(StringUtils.isBlank(getGlobalUsrName())) {
            errors.add("Username not defined in global configuration");
        }
        
        if(StringUtils.isBlank(getGlobalPasswd())) {
            errors.add("Password not defined in global configuration");
        }
        
        if(!errors.isEmpty()) {
            throw new IOException(String.format("Error in configuration%n%s", errors));
        }
        
        return true;
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
            checkGlobaConfiguration();
            results = collectionStrategy.collect(listener, build);            
            success = collectionStrategy.execute(build, listener, launcher, preBuildSteps, postBuildSteps, iterativeTestCaseBuilders, results);            
        } catch (Exception ex) {
            success = false;
            console.println(String.format("Failed to retrieve relevant test data. Message was:%n%s", ex.getMessage()));
            log.logp(Level.SEVERE, this.getClass().getName(), "perform", "Failed to retrieve relavant test data", ex);
            throw new AbortException("Error in retrieving data from RQM, trace written to log");
        } finally {
            build.addAction(new RqmBuildAction(results));
        }
 
        return success;
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
