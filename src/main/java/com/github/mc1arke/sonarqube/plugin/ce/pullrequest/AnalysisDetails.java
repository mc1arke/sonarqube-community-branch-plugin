/*
 * Copyright (C) 2020-2022 Michael Clarke
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 *
 */
package com.github.mc1arke.sonarqube.plugin.ce.pullrequest;


import org.sonar.api.ce.posttask.Analysis;
import org.sonar.api.ce.posttask.PostProjectAnalysisTask;
import org.sonar.api.ce.posttask.Project;
import org.sonar.api.ce.posttask.QualityGate;
import org.sonar.api.issue.Issue;
import org.sonar.ce.task.projectanalysis.component.Component;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class AnalysisDetails {

    private static final List<String> OPEN_ISSUE_STATUSES =
            Issue.STATUSES.stream().filter(s -> !Issue.STATUS_CLOSED.equals(s) && !Issue.STATUS_RESOLVED.equals(s))
                    .collect(Collectors.toList());

    private final String pullRequestId;
    private final String commitId;
    private final List<PostAnalysisIssueVisitor.ComponentIssue> issues;
    private final QualityGate qualityGate;
    private final PostProjectAnalysisTask.ProjectAnalysis projectAnalysis;

    AnalysisDetails(String pullRequestId, String commitId, List<PostAnalysisIssueVisitor.ComponentIssue> issues,
                    QualityGate qualityGate, PostProjectAnalysisTask.ProjectAnalysis projectAnalysis) {
        super();
        this.pullRequestId = pullRequestId;
        this.commitId = commitId;
        this.issues = issues;
        this.qualityGate = qualityGate;
        this.projectAnalysis = projectAnalysis;
    }

    public String getPullRequestId() {
        return pullRequestId;
    }

    public String getCommitSha() {
        return commitId;
    }

    public QualityGate.Status getQualityGateStatus() {
        return qualityGate.getStatus();
    }

    public List<QualityGate.Condition> findFailedQualityGateConditions() {
        return qualityGate.getConditions().stream()
                .filter(c -> c.getStatus() == QualityGate.EvaluationStatus.ERROR)
                .collect(Collectors.toList());
    }

    public Optional<String> getScannerProperty(String propertyName) {
        return Optional.ofNullable(projectAnalysis.getScannerContext().getProperties().get(propertyName));
    }

    public Date getAnalysisDate() {
        return getAnalysis().getDate();
    }

    public String getAnalysisId() {
        return getAnalysis().getAnalysisUuid();
    }

    public String getAnalysisProjectKey() {
        return getProject().getKey();
    }

    public String getAnalysisProjectName() {
        return getProject().getName();
    }

    public List<PostAnalysisIssueVisitor.ComponentIssue> getIssues() {
        return issues;
    }

    public List<PostAnalysisIssueVisitor.ComponentIssue> getScmReportableIssues() {
        return getIssues().stream()
                .filter(i -> i.getComponent().getReportAttributes().getScmPath().isPresent())
                .filter(i -> i.getComponent().getType() == Component.Type.FILE)
                .filter(i -> i.getIssue().resolution() == null)
                .filter(i -> OPEN_ISSUE_STATUSES.contains(i.getIssue().status()))
                .collect(Collectors.toList());
    }

    public Optional<QualityGate.Condition> findQualityGateCondition(String metricKey) {
        return qualityGate.getConditions().stream().filter(c -> metricKey.equals(c.getMetricKey())).findFirst();
    }

    private Analysis getAnalysis() {
        return projectAnalysis.getAnalysis().orElseThrow();
    }

    private Project getProject() {
        return projectAnalysis.getProject();
    }

}
