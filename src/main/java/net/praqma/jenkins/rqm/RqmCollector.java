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
package net.praqma.jenkins.rqm;

import hudson.DescriptorExtensionList;
import hudson.ExtensionPoint;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractDescribableImpl;
import hudson.model.BuildListener;
import hudson.tasks.BuildStep;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import jenkins.model.Jenkins;
import net.praqma.jenkins.rqm.model.RqmObject;
import org.apache.commons.lang.StringUtils;

/**
 * @author mads
 */
public abstract class RqmCollector extends AbstractDescribableImpl<RqmCollector> implements ExtensionPoint {    
    
    private String hostName;
    private String contextRoot;
    private String usrName;
    private String passwd;
    private int port;
    protected String credentialId;
    
    public RqmCollector() { }
    
    @Override
    public RqmCollectorDescriptor getDescriptor() {
        return (RqmCollectorDescriptor)super.getDescriptor();
    }
    
    public static DescriptorExtensionList<RqmCollector, RqmCollectorDescriptor> all() {
        return Jenkins.getInstance().<RqmCollector, RqmCollectorDescriptor> getDescriptorList(RqmCollector.class);
    }
    
    public static List<RqmCollectorDescriptor> getDescriptors() {
        List<RqmCollectorDescriptor> list = new ArrayList<RqmCollectorDescriptor>();
        for (RqmCollectorDescriptor d : all()) {
            list.add(d);
        }
        return list;
    }
    
    public abstract <T extends RqmObject> List<T> collect(BuildListener listener, AbstractBuild<?,?> build) throws Exception;
    public boolean execute(AbstractBuild<?,?> build, BuildListener listener, Launcher launcher, final List<BuildStep> preBuildSteps, final List<BuildStep> postBuildSteps, List<BuildStep> iterativeTestCaseBuilders, List<? extends RqmObject> results) throws Exception {
        return true;
    }
    
    public RqmCollector withCredentials(String credentialsId) {
        this.credentialId = credentialsId;
        return this;
    }
    
    public void setup(String hostname, String contextRoot, String usrName, String passwd, int port) {       
        setContextRoot(contextRoot);
        setPasswd(passwd);
        setPort(port);
        setUsrName(usrName);
        setHostName(hostname);
    }    
    
    public boolean checkSetup() throws IOException {
        
        List<String> errors = new ArrayList<String>();
        
        if(getPort() == 0) {
            errors.add("Port not defined in global configuration");
        }
        
        if(StringUtils.isBlank(getHostName())) {
            errors.add("Hostname not defined in global configuration");
        }
        
        if(StringUtils.isBlank(getContextRoot())) {
            errors.add("Context root not defined in global configuration");
        }
        
        if(!errors.isEmpty()) {
            throw new IOException(String.format("Error in configuration%n%s", errors));
        }
        
        return true;
    }
    
    /**
     * @return the hostName
     */
    public String getHostName() {
        return hostName;
    }

    /**
     * @param hostName the hostName to set
     */
    public void setHostName(String hostName) {
        this.hostName = hostName;
    }

    /**
     * @return the contextRoot
     */
    public String getContextRoot() {
        return contextRoot;
    }

    /**
     * @param contextRoot the contextRoot to set
     */
    public void setContextRoot(String contextRoot) {
        this.contextRoot = contextRoot;
    }

    /**
     * @return the usrName
     */
    public String getUsrName() {
        return usrName;
    }

    /**
     * @param usrName the usrName to set
     */
    public void setUsrName(String usrName) {
        this.usrName = usrName;
    }

    /**
     * @return the passwd
     */
    public String getPasswd() {
        return passwd;
    }

    /**
     * @param passwd the passwd to set
     */
    public void setPasswd(String passwd) {
        this.passwd = passwd;
    }

    /**
     * @return the port
     */
    public int getPort() {
        return port;
    }

    /**
     * @param port the port to set
     */
    public void setPort(int port) {
        this.port = port;
    }

}
