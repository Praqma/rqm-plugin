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

import hudson.model.AbstractBuild;
import hudson.model.Action;
import net.praqma.jenkins.rqm.model.TestPlan;

/**
 *
 * @author Praqma
 */
public class RQMBuildAction implements Action {
    
    private final String NAME = "RQM Test Report";
    public final String customKey;
    public final TestPlan testplan;
    public final AbstractBuild<?, ?> build;
    
    public RQMBuildAction(final TestPlan testplan, final String customKey, final AbstractBuild<?, ?> build) {
        this.build = build;
        this.testplan = testplan;
        this.customKey = customKey;
    }

    @Override
    public String getIconFileName() {
        return "/plugin/rqm-plugin/images/64x64/rqm-icon.png";        
    }

    @Override
    public String getDisplayName() {
        return NAME;
    }

    @Override
    public String getUrlName() {
        return NAME;
    }
    
}
