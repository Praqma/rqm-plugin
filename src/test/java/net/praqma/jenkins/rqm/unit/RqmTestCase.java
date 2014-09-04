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

import hudson.model.FreeStyleProject;
import hudson.tasks.Builder;
import java.util.HashSet;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import net.praqma.jenkins.rqm.RqmBuilder;
import net.praqma.jenkins.rqm.model.TestCase;
import net.praqma.jenkins.rqm.model.TestPlan;
import net.praqma.jenkins.rqm.model.TestSuite;
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
 
        SortedSet<TestCase> cases = new TreeSet<TestCase>();
        cases.add(tc);
        
        suite.setTestcases(cases);
        
        HashSet<TestSuite> suites = new HashSet<TestSuite>();
        suites.add(suite);
        
        plan.setTestSuites(suites);
        
        return plan;
    }
    
    public FreeStyleProject createSuccesConfiguration(RqmBuilder builder) throws Exception {
        FreeStyleProject proj = jenkins.createFreeStyleProject("RQMProject");
        
        List<Builder> blold = proj.getBuildersList().toList();
        
        proj.getBuildersList().clear();
        proj.getBuildersList().add(builder);
        proj.getBuildersList().addAll(blold);
        
        return proj;
        
    }
    
    public FreeStyleProject createFailingConfiguartion(RqmBuilder builder) throws Exception {
        FreeStyleProject proj = jenkins.createFreeStyleProject("RQMProject");
        
        List<Builder> blold = proj.getBuildersList().toList();        
        proj.getBuildersList().clear();
        proj.getBuildersList().add(builder);
        proj.getBuildersList().addAll(blold);
        
        return proj;
        
    }
}
