package com.github.mc1arke.sonarqube.plugin.ce.pullrequest.commentfilter;

import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.PostAnalysisIssueVisitor;
import com.google.common.base.Preconditions;
import org.sonar.api.rule.Severity;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class SeverityExclusionFilter implements Predicate<PostAnalysisIssueVisitor.ComponentIssue> {

    private final List<String> exclusions;

    public SeverityExclusionFilter(String severityString) {
        this.exclusions = parseString(severityString);
    }

    private List<String> parseString(String severityString) {
        List<String> severityStringList = Arrays.asList(severityString.split(","));

        return Collections.unmodifiableList(severityStringList.stream()
                .map(String::trim)
                .map(String::toUpperCase)
                .filter(Severity.ALL::contains)
                .collect(Collectors.toList()));
    }

    @Override
    public boolean test(PostAnalysisIssueVisitor.ComponentIssue componentIssue) {
        return !exclusions.contains(componentIssue.getIssue().severity());
    }
}
