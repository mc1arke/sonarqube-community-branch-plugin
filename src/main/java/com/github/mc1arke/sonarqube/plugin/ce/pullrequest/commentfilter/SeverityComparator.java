package com.github.mc1arke.sonarqube.plugin.ce.pullrequest.commentfilter;

import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.PostAnalysisIssueVisitor;
import org.sonar.api.rule.Severity;

import java.util.Comparator;

public class SeverityComparator implements Comparator<PostAnalysisIssueVisitor.ComponentIssue> {
    @Override
    public int compare(PostAnalysisIssueVisitor.ComponentIssue o1, PostAnalysisIssueVisitor.ComponentIssue o2) {
        return Severity.ALL.indexOf(o2.getIssue().severity()) - Severity.ALL.indexOf(o1.getIssue().severity());
    }
}
