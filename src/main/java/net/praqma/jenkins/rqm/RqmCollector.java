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

import hudson.ExtensionPoint;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractDescribableImpl;
import hudson.model.BuildListener;
import hudson.tasks.BuildStep;
import java.io.IOException;
import java.util.List;
import net.praqma.jenkins.rqm.model.RQMObject;
import net.praqma.jenkins.rqm.model.TestPlan;

/**
 * @author mads
 */
public abstract class RqmCollector extends AbstractDescribableImpl<RqmCollector> implements ExtensionPoint {    
    private String testPlan;
    private String project;
    private String hostName;
    private String contextRoot;
    private String usrName;
    private String passwd;
    private int port;
    
    
    public RqmCollector() { }

    public RqmCollector(int port, String hostname, String contextRoot, String usrName, String passwd, final String testPlan, final String project) {
        this.testPlan = testPlan;
        this.project = project;
        this.port = port;
        this.contextRoot = contextRoot;
        this.usrName = usrName;
        this.passwd = passwd;
        this.hostName = hostname;
    }
    
    @Override
    public RqmCollectorDescriptor getDescriptor() {
        return (RqmCollectorDescriptor)super.getDescriptor();
    }
    
    public abstract <T extends RQMObject> T collect(BuildListener listener, AbstractBuild<?,?> build) throws Exception;
    public boolean execute(AbstractBuild<?,?> build, BuildListener listener, Launcher launcher, final List<BuildStep> preBuildSteps, final List<BuildStep> postBuildSteps, List<BuildStep> iterativeTestCaseBuilders) throws Exception {
        return true;
    }
    
    
    public void setup(String hostname, String contextRoot, String usrName, String passwd, int port) {
        setHostName(hostname);
        setContextRoot(contextRoot);
        setUsrName(usrName);
        setPasswd(passwd);
        setPort(port);       
    }    

    /**
     * @return the testPlan
     */
    public String getTestPlan() {
        return testPlan;
    }

    /**
     * @param testPlan the testPlan to set
     */
    public void setTestPlan(String testPlan) {
        this.testPlan = testPlan;
    }

    /**
     * @return the project
     */
    public String getProject() {
        return project;
    }

    /**
     * @param project the project to set
     */
    public void setProject(String project) {
        this.project = project;
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