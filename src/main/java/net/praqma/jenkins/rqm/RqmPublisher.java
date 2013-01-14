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
import hudson.model.UpdateSite;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Publisher;
import hudson.tasks.Recorder;
import hudson.util.FormValidation;
import java.io.IOException;
import java.io.PrintStream;
import jenkins.model.Jenkins;
import net.praqma.jenkins.rqm.request.RQMHttpClient;
import net.praqma.jenkins.rqm.request.RQMUtilities;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

/**
 *
 * @author Praqma
 */
public class RqmPublisher extends Recorder {

    public final String projectName,contextRoot,usrName,passwd,port,hostName;
    
    @Override
    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.BUILD;
    }
    
    @DataBoundConstructor
    public RqmPublisher(final String projectName, final String contextRoot, final String usrName, final String passwd, final String port, final String hostName) {
        this.projectName = projectName;
        this.contextRoot = contextRoot;
        this.hostName = hostName;
        this.usrName = usrName;
        this.passwd = passwd;
        this.port = port;
    }

    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
        PrintStream console = listener.getLogger();
        
        console.println(Jenkins.getInstance().getPlugin("rqm-plugin").getWrapper().getVersion());
        console.println(String.format("Project name: %s", projectName));
        console.println(String.format("QM Server Port: %s", port));
        console.println(String.format("QM Server Host Name: %s", port));
        console.println(String.format("Context root: %s", contextRoot));
        
        console.println(String.format("Username: %s", usrName));
        console.println(String.format("Password: %s", passwd));
        
        RQMHttpClient client = RQMUtilities.createClient(hostName, port, contextRoot, projectName, port, passwd);
        
        return true;
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
