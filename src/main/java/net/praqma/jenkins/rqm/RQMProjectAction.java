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
import hudson.model.AbstractProject;
import hudson.model.Actionable;
import hudson.model.ProminentProjectAction;
import java.util.ArrayList;
import java.util.List;
import net.praqma.jenkins.rqm.model.TestCase;

/**
 *
 * @author Praqma
 */
public class RQMProjectAction extends Actionable implements ProminentProjectAction {
    
    public final AbstractProject<?,?> project;
    private static final String PROJECT_NAME = "RQM Plugin";
    private static final String PROJECT_URL = "prqm";
    
    public RQMProjectAction(AbstractProject<?,?> project) {
        this.project = project;
    }

    @Override
    public String getDisplayName() {
        return PROJECT_NAME;
    }

    @Override
    public String getSearchUrl() {
        return null;
    }

    @Override
    public String getIconFileName() {
        return null;
    }

    @Override
    public String getUrlName() {
        return PROJECT_URL;
    }
    
    private RQMBuildAction _getPrevious(AbstractBuild<?,?> build) {
        RQMBuildAction action = build.getAction(RQMBuildAction.class);
        if(action == null) {
            while( build != null ) {
                build = build.getPreviousCompletedBuild();
                if(build.getAction(RQMBuildAction.class) != null) {
                    return build.getAction(RQMBuildAction.class);
                }
            }
        }        
        return action;
    }
    
    public RQMBuildAction getMostRecentNotNullRQMBuildAction() {
        RQMBuildAction action = null;
        AbstractBuild<?,?> current = project.getLastCompletedBuild();
        if(current == null) {
            return action;
        } else {
            action = _getPrevious(current);
        }
        return action;
    }
    
    public RqmPublisher getRqmPublisher() {
        return project.getPublishersList().get(RqmPublisher.class);
    }
    
    public List<TestCase> getListOfSelectedTestCases(String customPropertyName) {
        List<TestCase> list = new ArrayList<TestCase>();
        list.addAll(getMostRecentNotNullRQMBuildAction().testplan.getTestCaseHavingCustomFieldWithName(customPropertyName));        
        return list;
    }
}
