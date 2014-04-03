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
package net.praqma.jenkins.rqm.unit;

import net.praqma.jenkins.rqm.collector.DummyCollectionStrategy;
import hudson.model.FreeStyleProject;
import hudson.tasks.BatchFile;
import hudson.tasks.BuildStep;
import hudson.tasks.Builder;
import hudson.tasks.Shell;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import net.praqma.jenkins.rqm.RqmBuilder;
import net.praqma.jenkins.rqm.RqmCollector;
import net.praqma.jenkins.rqm.model.TestCase;
import net.praqma.jenkins.rqm.model.TestPlan;
import net.praqma.jenkins.rqm.model.TestSuite;
import org.apache.commons.lang.SystemUtils;
import org.junit.Rule;
import org.jvnet.hudson.test.JenkinsRule;
/**
 *
 * @author mads
 */
public class RqmTestCase {
    @Rule public JenkinsRule jenkins = new JenkinsRule();
    
    
    public TestPlan defaultTestPlan() {
        TestPlan plan = new TestPlan("TestPlan1");
        plan.setRqmObjectResourceUrl("testplan:tp1");
        
        TestSuite suite = new TestSuite(null, "TestSuite1");
        suite.setRqmObjectResourceUrl("testsuite:ts1");
        
        TestCase tc = new TestCase("TestCase1");
        tc.setRqmObjectResourceUrl("testcase:tc1");
 
        HashSet<TestCase> cases = new HashSet<TestCase>();
        cases.add(tc);
        
        suite.setTestcases(cases);
        
        HashSet<TestSuite> suites = new HashSet<TestSuite>();
        suites.add(suite);
        
        plan.setTestSuites(suites);
        
        return plan;
    }
    
    public FreeStyleProject createSuccesConfiguration() throws Exception {
        FreeStyleProject proj = jenkins.createFreeStyleProject("RQMProject");

        RqmBuilder builder = null;
        RqmCollector collector = new DummyCollectionStrategy();
      
        if(SystemUtils.IS_OS_WINDOWS) {        
            final BuildStep f = new BatchFile("dir");                      
            final BuildStep f2 = new BatchFile("dir");            
            builder = new RqmBuilder(collector, Arrays.asList(f,f2), Arrays.asList(f,f2), Arrays.asList(f,f2)  );
            
        } else {
            final BuildStep s = new Shell("ls");
            final BuildStep s2 = new Shell("ls");
            builder = new RqmBuilder(collector, Arrays.asList(s,s2), Arrays.asList(s,s2), Arrays.asList(s,s2)  );
        }
         
        
        List<Builder> blold = proj.getBuildersList().toList();
        
        proj.getBuildersList().clear();
        proj.getBuildersList().add(builder);
        proj.getBuildersList().addAll(blold);
        
        return proj;
        
    }
    
    public FreeStyleProject creteFailingConfiguartion() throws Exception {
        FreeStyleProject proj = jenkins.createFreeStyleProject("RQMProject");
        RqmCollector collector = new DummyCollectionStrategy();
        RqmBuilder builder = null;
      
        if(SystemUtils.IS_OS_WINDOWS) {        
            final BuildStep f = new BatchFile("dir");                      
            final BuildStep f2 = new BatchFile("exit 1");            
            builder = new RqmBuilder(collector, Arrays.asList(f,f2), Arrays.asList(f,f2), Arrays.asList(f,f2)  );            
        } else {
            final BuildStep s = new Shell("ls");
            final BuildStep s2 = new Shell("exit 1");
            builder = new RqmBuilder(collector, Arrays.asList(s,s2), Arrays.asList(s,s2), Arrays.asList(s,s2)  );
        }
        
        
        List<Builder> blold = proj.getBuildersList().toList();        
        proj.getBuildersList().clear();
        proj.getBuildersList().add(builder);
        proj.getBuildersList().addAll(blold);
        
        return proj;
        
    }
}
