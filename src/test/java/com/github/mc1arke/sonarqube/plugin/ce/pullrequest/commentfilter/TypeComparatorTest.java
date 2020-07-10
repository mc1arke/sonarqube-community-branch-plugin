package com.github.mc1arke.sonarqube.plugin.ce.pullrequest.commentfilter;

import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.PostAnalysisIssueVisitor;
import org.junit.Test;
import org.sonar.api.rules.RuleType;
import org.sonar.core.issue.DefaultIssue;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TypeComparatorTest {

    @Test
    public void whenFirstValueLowerThenDifferencePositive(){
        DefaultIssue issue1 = mock(DefaultIssue.class);
        when(issue1.type()).thenReturn(RuleType.CODE_SMELL);
        PostAnalysisIssueVisitor.ComponentIssue componentIssue1 = mock(PostAnalysisIssueVisitor.ComponentIssue.class);
        when(componentIssue1.getIssue()).thenReturn(issue1);

        DefaultIssue issue2 = mock(DefaultIssue.class);
        when(issue2.type()).thenReturn(RuleType.BUG);
        PostAnalysisIssueVisitor.ComponentIssue componentIssue2 = mock(PostAnalysisIssueVisitor.ComponentIssue.class);
        when(componentIssue2.getIssue()).thenReturn(issue2);

        TypeComparator comparator = new TypeComparator();
        assertEquals(1,comparator.compare(componentIssue1,componentIssue2));
    }

    @Test
    public void whenFirstValueHigherThenDifferenceNegative(){
        DefaultIssue issue1 = mock(DefaultIssue.class);
        when(issue1.type()).thenReturn(RuleType.BUG);
        PostAnalysisIssueVisitor.ComponentIssue componentIssue1 = mock(PostAnalysisIssueVisitor.ComponentIssue.class);
        when(componentIssue1.getIssue()).thenReturn(issue1);

        DefaultIssue issue2 = mock(DefaultIssue.class);
        when(issue2.type()).thenReturn(RuleType.CODE_SMELL);
        PostAnalysisIssueVisitor.ComponentIssue componentIssue2 = mock(PostAnalysisIssueVisitor.ComponentIssue.class);
        when(componentIssue2.getIssue()).thenReturn(issue2);

        TypeComparator comparator = new TypeComparator();
        assertEquals(-1,comparator.compare(componentIssue1,componentIssue2));
    }
}
