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
import hudson.tasks.BatchFile;
import hudson.tasks.BuildStep;
import hudson.tasks.Shell;
import java.util.Arrays;
import net.praqma.jenkins.rqm.RqmBuildAction;
import net.praqma.jenkins.rqm.RqmBuilder;
import net.praqma.jenkins.rqm.RqmCollector;
import net.praqma.jenkins.rqm.collector.DummyCollectionStrategy;
import org.apache.commons.lang.SystemUtils;
import org.junit.Test;
import static org.junit.Assert.*;
import org.mockito.Mockito;

/**
 *
 * @author mads
 */
public class TestBuilder extends RqmTestCase {
    
    private RqmBuilder createBuilder(boolean shouldFail) {
        RqmBuilder builder = null;
        RqmCollector collector = new DummyCollectionStrategy(shouldFail);
        if(!shouldFail) {
            if(SystemUtils.IS_OS_WINDOWS) {        
                final BuildStep f = new BatchFile("dir");                      
                final BuildStep f2 = new BatchFile("dir");            
                builder = new RqmBuilder(collector, Arrays.asList(f,f2), Arrays.asList(f,f2), Arrays.asList(f,f2)  );

            } else {
                final BuildStep s = new Shell("ls");
                final BuildStep s2 = new Shell("ls");
                builder = new RqmBuilder(collector, Arrays.asList(s,s2), Arrays.asList(s,s2), Arrays.asList(s,s2)  );
            }
        } else {
            if(SystemUtils.IS_OS_WINDOWS) {        
                final BuildStep f = new BatchFile("dir");                      
                final BuildStep f2 = new BatchFile("exit 1");            
                builder = new RqmBuilder(collector, Arrays.asList(f,f2), Arrays.asList(f,f2), Arrays.asList(f,f2)  );

            } else {
                final BuildStep s = new Shell("ls");
                final BuildStep s2 = new Shell("exit 1");
                builder = new RqmBuilder(collector, Arrays.asList(s,s2), Arrays.asList(s,s2), Arrays.asList(s,s2)  );
            }
        }
        return builder;
    }
            
        
    @Test
    public void buildSuccess() throws Exception {
        RqmBuilder builder = createBuilder(false);        
        RqmBuilder spy = Mockito.spy(builder);
        
        //Mock the configuration 
        Mockito.doReturn(true).when(spy).checkGlobaConfiguration();        
        FreeStyleProject proj = createSuccesConfiguration(spy);
        
        AbstractBuild<?,?> build = proj.scheduleBuild2(0).get();
        
        RqmBuildAction action = build.getAction(RqmBuildAction.class);
        assertNotNull(action);
        
        assertTrue(build.getResult().isBetterOrEqualTo(Result.SUCCESS));
        
    }
    
    /**
     * Add a failing builder to the mix
     * @throws Exception 
     */
    @Test
    public void buildFailure() throws Exception {
        RqmBuilder builder = createBuilder(true);
        
        RqmBuilder spy = Mockito.spy(builder);
        //Mock the configuration 
        Mockito.doReturn(true).when(spy).checkGlobaConfiguration();
        
        FreeStyleProject proj = createFailingConfiguartion(spy);

        final FreeStyleBuild builres = proj.scheduleBuild2(0).get();        
        
        assertTrue(builres.getResult().isWorseThan(Result.SUCCESS));
    }
}
