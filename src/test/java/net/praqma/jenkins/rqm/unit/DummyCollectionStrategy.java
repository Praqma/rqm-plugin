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

import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.tasks.BuildStep;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import net.praqma.jenkins.rqm.RqmCollector;
import net.praqma.jenkins.rqm.RqmCollectorDescriptor;
import net.praqma.jenkins.rqm.model.RqmObject;
import net.praqma.jenkins.rqm.model.TestCase;
import net.praqma.jenkins.rqm.model.TestPlan;
import net.praqma.jenkins.rqm.model.TestScript;
import net.praqma.jenkins.rqm.model.TestSuite;
import net.praqma.jenkins.rqm.model.exception.RQMObjectParseException;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 *
 * @author mads
 */
public class DummyCollectionStrategy extends RqmCollector {

    public boolean alwaysFail = false;
    
    @DataBoundConstructor
    public DummyCollectionStrategy() { }
    
    public DummyCollectionStrategy(boolean alwaysFail) {
        this.alwaysFail = alwaysFail;
    }
    
    @Extension
    public static class DummyCollectionStrategyImpl extends RqmCollectorDescriptor {

        @Override
        public String getDisplayName() {
            return "Dummy collection strategy";
        }        
    }
    

    public TestPlan defaultTestPlan() throws RQMObjectParseException {
        TestPlan plan = new TestPlan("TestPlan1");
        plan.setRqmObjectResourceUrl("testplan:tp1");

        TestSuite suite = new TestSuite(null, "TestSuite1");
        suite.setRqmObjectResourceUrl("testsuite:ts1");

        //First testcase
        TestCase tc = new TestCase("testcase:tc1", "TestCase1");

        TestScript ts = new TestScript("testcript:ts1");
        tc.setScripts(Arrays.asList(ts));
        
        //Second testcase
        TestCase tc2 = new TestCase("testcase:tc2", "TestCase2");

        TestScript ts2 = new TestScript("testcript:ts2");
        tc2.setScripts(Arrays.asList(ts2));
        
        HashSet<TestCase> cases = new HashSet<TestCase>();
        cases.add(tc);
        cases.add(tc2);

        suite.setTestcases(cases);

        HashSet<TestSuite> suites = new HashSet<TestSuite>();
        suites.add(suite);

        plan.setTestSuites(suites);

        return plan;
    }

    @Override
    public <T extends RqmObject> List<T> collect(BuildListener listener, AbstractBuild<?, ?> build) throws Exception {
        return Arrays.asList((T)defaultTestPlan());
    }

    @Override
    public boolean execute(AbstractBuild<?, ?> build, BuildListener listener, Launcher launcher, List<BuildStep> preBuildSteps, List<BuildStep> postBuildSteps, List<BuildStep> iterativeTestCaseBuilders, List<? extends RqmObject> results) throws Exception {
        return !alwaysFail; 
    }
    
    
}
