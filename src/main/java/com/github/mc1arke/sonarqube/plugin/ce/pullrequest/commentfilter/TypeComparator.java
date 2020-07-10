package com.github.mc1arke.sonarqube.plugin.ce.pullrequest.commentfilter;

import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.PostAnalysisIssueVisitor;

import java.util.Comparator;

public class TypeComparator implements Comparator<PostAnalysisIssueVisitor.ComponentIssue> {
    @Override
    public int compare(PostAnalysisIssueVisitor.ComponentIssue o1, PostAnalysisIssueVisitor.ComponentIssue o2) {
        return o2.getIssue().type().getDbConstant() - o1.getIssue().type().getDbConstant();
    }
}
