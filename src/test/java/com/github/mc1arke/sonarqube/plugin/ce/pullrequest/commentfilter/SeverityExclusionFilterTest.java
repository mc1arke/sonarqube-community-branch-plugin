package com.github.mc1arke.sonarqube.plugin.ce.pullrequest.commentfilter;

import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.PostAnalysisIssueVisitor;
import org.junit.Test;
import org.sonar.core.issue.DefaultIssue;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SeverityExclusionFilterTest {

    @Test
    public void whenInExcludedReturnFalse(){
        DefaultIssue issue = mock(DefaultIssue.class);
        when(issue.severity()).thenReturn("MAJOR");
        PostAnalysisIssueVisitor.ComponentIssue componentIssue = mock(PostAnalysisIssueVisitor.ComponentIssue.class);
        when(componentIssue.getIssue()).thenReturn(issue);

        SeverityExclusionFilter filter = new SeverityExclusionFilter("MAJOR");
        assertFalse(filter.test(componentIssue));
    }

    @Test
    public void whenNotInExcludedReturnTrue(){
        DefaultIssue issue = mock(DefaultIssue.class);
        when(issue.severity()).thenReturn("");
        PostAnalysisIssueVisitor.ComponentIssue componentIssue = mock(PostAnalysisIssueVisitor.ComponentIssue.class);
        when(componentIssue.getIssue()).thenReturn(issue);

        SeverityExclusionFilter filter = new SeverityExclusionFilter("MAJOR");
        assertTrue(filter.test(componentIssue));
    }
}
