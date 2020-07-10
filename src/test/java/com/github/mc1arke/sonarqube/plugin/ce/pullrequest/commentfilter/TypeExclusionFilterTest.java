package com.github.mc1arke.sonarqube.plugin.ce.pullrequest.commentfilter;

import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.PostAnalysisIssueVisitor;
import org.junit.Test;
import org.sonar.api.rules.RuleType;
import org.sonar.core.issue.DefaultIssue;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TypeExclusionFilterTest {

    @Test
    public void whenInExcludedReturnFalse(){
        DefaultIssue issue = mock(DefaultIssue.class);
        when(issue.type()).thenReturn(RuleType.CODE_SMELL);
        PostAnalysisIssueVisitor.ComponentIssue componentIssue = mock(PostAnalysisIssueVisitor.ComponentIssue.class);
        when(componentIssue.getIssue()).thenReturn(issue);

        TypeExclusionFilter filter = new TypeExclusionFilter("CODE_SMELL");
        assertFalse(filter.test(componentIssue));
    }

    @Test
    public void whenNotInExcludedReturnTrue(){
        DefaultIssue issue = mock(DefaultIssue.class);
        when(issue.type()).thenReturn(RuleType.BUG);
        PostAnalysisIssueVisitor.ComponentIssue componentIssue = mock(PostAnalysisIssueVisitor.ComponentIssue.class);
        when(componentIssue.getIssue()).thenReturn(issue);

        TypeExclusionFilter filter = new TypeExclusionFilter("CODE_SMELL");
        boolean result = filter.test(componentIssue);
        assertTrue(result);
    }
}
