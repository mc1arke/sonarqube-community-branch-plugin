package com.github.mc1arke.sonarqube.plugin.ce.pullrequest.commentfilter;

import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.PostAnalysisIssueVisitor;

import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class IssueFilterRunner {
    private final List<Predicate<PostAnalysisIssueVisitor.ComponentIssue>> filters;
    private Integer maxAmountOfIssues;
    private final SeverityComparator severityComparator;
    private final TypeComparator typeComparator;

    public IssueFilterRunner(List<Predicate<PostAnalysisIssueVisitor.ComponentIssue>> filters, Integer maxAmountOfIssues) {
        this.filters = filters;
        this.maxAmountOfIssues = maxAmountOfIssues;
        this.severityComparator = new SeverityComparator();
        this.typeComparator = new TypeComparator();
    }

    public IssueFilterRunner(
            List<Predicate<PostAnalysisIssueVisitor.ComponentIssue>> filters,
            SeverityComparator severityComparator,
            TypeComparator typeComparator) {
        this.filters = filters;
        this.severityComparator = severityComparator;
        this.typeComparator = typeComparator;
    }

    public IssueFilterRunner(
            List<Predicate<PostAnalysisIssueVisitor.ComponentIssue>> filters, Integer maxAmountOfIssues,
            SeverityComparator severityComparator,
            TypeComparator typeComparator) {
        this(filters, severityComparator, typeComparator);
        this.maxAmountOfIssues = maxAmountOfIssues;
    }

    public List<PostAnalysisIssueVisitor.ComponentIssue> filterIssues(List<PostAnalysisIssueVisitor.ComponentIssue> issues) {
        Stream<PostAnalysisIssueVisitor.ComponentIssue> stream = issues.stream()
                .filter(filters.stream()
                        .reduce(issue -> true,
                                Predicate::and))
                .sorted(severityComparator.thenComparing(typeComparator));

        if (maxAmountOfIssues != null && maxAmountOfIssues > 0) stream = stream.limit(maxAmountOfIssues);

        return Collections.unmodifiableList(stream.collect(Collectors.toList()));
    }

    public List<Predicate<PostAnalysisIssueVisitor.ComponentIssue>> getFilters() {
        return filters;
    }

    public Integer getMaxAmountOfIssues() {
        return maxAmountOfIssues;
    }
}
