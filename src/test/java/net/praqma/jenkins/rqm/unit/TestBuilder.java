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

import hudson.model.AbstractBuild;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Result;
import net.praqma.jenkins.rqm.RqmBuildAction;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author mads
 */
public class TestBuilder extends RqmTestCase {
        
    @Test
    public void buildSuccess() throws Exception {
        FreeStyleProject proj = createSuccesConfiguration();
        
        AbstractBuild<?,?> build = proj.scheduleBuild2(0).get();
        
        RqmBuildAction action = build.getAction(RqmBuildAction.class);
        assertNotNull(action);

        assertEquals(2, action.getSelectedTestCases().length);
        
        assertTrue(build.getResult().isBetterOrEqualTo(Result.SUCCESS));
        
    }
    
    /**
     * Add a failing builder to the mix
     * @throws Exception 
     */
    @Test
    public void buildFailure() throws Exception {
        FreeStyleProject proj = creteFailingConfiguartion();
        final FreeStyleBuild builres = proj.scheduleBuild2(0).get();
        
        String consoleMessage = jenkins.createWebClient().getPage(builres, "console").asText();
        
        assertTrue(consoleMessage, consoleMessage.contains("Error caught in test execution, review log for details"));
        assertTrue(consoleMessage, consoleMessage.contains("Executing test case TestCase2"));
        
        assertTrue(builres.getResult().isWorseThan(Result.SUCCESS));
    }
}
