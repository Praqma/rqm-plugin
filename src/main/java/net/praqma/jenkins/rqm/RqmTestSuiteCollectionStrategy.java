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

import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import net.praqma.jenkins.rqm.model.TestPlan;
import net.praqma.jenkins.rqm.request.RqmParameterList;
import org.apache.commons.httpclient.NameValuePair;

/**
 *
 * @author mads
 */
public class RqmTestSuiteCollectionStrategy extends RqmCollector<TestPlan> {
    protected final String suiteNames;
    
    
    public RqmTestSuiteCollectionStrategy(int port, String hostname, String contextRoot, String usrName, String passwd, final String testPlan, final String project, final String suiteNames ) {
        super(port, hostname, contextRoot, usrName, passwd, testPlan, project);
        this.suiteNames = suiteNames;
    }

    @Override
    public TestPlan collect(BuildListener listener, AbstractBuild<?, ?> build) throws Exception {
        TestPlan plan = new TestPlan(getTestPlan());
        String planRequestFeed = plan.getFeedUrlForTestPlans(getHostName(), getPort(), getContextRoot(), getProject());

        NameValuePair[] requestParameters = plan.getParametersForFeedUrlForTestPlans();
        RqmParameterList list = new RqmParameterList(getHostName(), getPort(), getContextRoot(), getProject(), getUsrName(), getPasswd(), planRequestFeed, requestParameters, "GET", null);
  
        listener.getLogger().println("Getting TestPlans using feed url:");
        listener.getLogger().println(planRequestFeed+"?fields="+requestParameters[0].getValue());
        
        RqmObjectCreator<TestPlan> object = new RqmObjectCreator<TestPlan>(plan, list);
        plan = build.getWorkspace().act(object);
        return plan;
    }
    
}
