/*
 * Copyright (C) 2022-2024 Michael Clarke
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
package com.github.mc1arke.sonarqube.plugin.ce.pullrequest.report;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.sonar.api.ce.posttask.Project;
import org.sonar.api.ce.posttask.QualityGate;
import org.sonar.api.config.Configuration;
import org.sonar.api.issue.IssueStatus;
import org.sonar.api.issue.impact.Severity;
import org.sonar.api.issue.impact.SoftwareQuality;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.platform.Server;
import org.sonar.ce.task.projectanalysis.component.Component;
import org.sonar.ce.task.projectanalysis.component.TreeRootHolder;
import org.sonar.ce.task.projectanalysis.measure.Measure;
import org.sonar.ce.task.projectanalysis.measure.MeasureRepository;
import org.sonar.ce.task.projectanalysis.metric.Metric;
import org.sonar.ce.task.projectanalysis.metric.MetricRepository;

import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.AnalysisDetails;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.PostAnalysisIssueVisitor;

class ReportGeneratorTest {

    @CsvSource({"98, passed-16px.png, 1, passed-16px.png",
            ",no-data-16px.png,,no-data-16px.png"})
    @ParameterizedTest
    void shouldProduceCorrectAnalysisSummary(String coverage, String coverageImage, String duplications, String duplicationsImage) {
        AnalysisDetails analysisDetails = mock();
        when(analysisDetails.getPullRequestId()).thenReturn("5");
        when(analysisDetails.getAnalysisProjectKey()).thenReturn("projectKey");

        TreeRootHolder treeRootHolder = mock();

        PostAnalysisIssueVisitor.LightIssue issue1 = mock();
        when(issue1.issueStatus()).thenReturn(IssueStatus.FIXED);

        PostAnalysisIssueVisitor.LightIssue issue2 = mock();
        when(issue2.issueStatus()).thenReturn(IssueStatus.OPEN);
        when(issue2.impacts()).thenReturn(Map.of(SoftwareQuality.RELIABILITY, Severity.HIGH));

        PostAnalysisIssueVisitor.LightIssue issue3 = mock();
        when(issue3.issueStatus()).thenReturn(IssueStatus.OPEN);
        when(issue3.impacts()).thenReturn(Map.of(SoftwareQuality.SECURITY, Severity.HIGH));

        PostAnalysisIssueVisitor.LightIssue issue4 = mock();
        when(issue4.issueStatus()).thenReturn(IssueStatus.OPEN);
        when(issue4.impacts()).thenReturn(Map.of(SoftwareQuality.MAINTAINABILITY, Severity.HIGH));

        PostAnalysisIssueVisitor.LightIssue issue5 = mock();
        when(issue5.issueStatus()).thenReturn(IssueStatus.OPEN);
        when(issue5.impacts()).thenReturn(Map.of(SoftwareQuality.SECURITY, Severity.HIGH));

        PostAnalysisIssueVisitor.LightIssue issue6 = mock();
        when(issue6.issueStatus()).thenReturn(IssueStatus.OPEN);
        when(issue6.impacts()).thenReturn(Map.of(SoftwareQuality.RELIABILITY, Severity.HIGH));

        List<PostAnalysisIssueVisitor.ComponentIssue> issues = Stream.of(issue1, issue2, issue3, issue4, issue5, issue6).map(i -> {
            PostAnalysisIssueVisitor.ComponentIssue componentIssue =
                    mock();
            when(componentIssue.getIssue()).thenReturn(i);
            return componentIssue;
        }).collect(Collectors.toList());
        when(analysisDetails.getIssues()).thenReturn(issues);

        QualityGate.Condition condition1 = mock();
        when(condition1.getStatus()).thenReturn(QualityGate.EvaluationStatus.ERROR);
        when(condition1.getMetricKey()).thenReturn(CoreMetrics.LINES_TO_COVER.getKey());
        when(condition1.getValue()).thenReturn("19");
        when(condition1.getOperator()).thenReturn(QualityGate.Operator.LESS_THAN);
        when(condition1.getErrorThreshold()).thenReturn("20");

        QualityGate.Condition condition2 = mock();
        when(condition2.getStatus()).thenReturn(QualityGate.EvaluationStatus.ERROR);
        when(condition2.getMetricKey()).thenReturn(CoreMetrics.MAINTAINABILITY_ISSUES_KEY);
        when(condition2.getValue()).thenReturn("2");
        when(condition2.getOperator()).thenReturn(QualityGate.Operator.GREATER_THAN);
        when(condition2.getErrorThreshold()).thenReturn("0");

        QualityGate.Condition condition3 = mock();
        when(condition3.getStatus()).thenReturn(QualityGate.EvaluationStatus.ERROR);
        when(condition3.getMetricKey()).thenReturn(CoreMetrics.LINE_COVERAGE.getKey());
        when(condition3.getValue()).thenReturn("68");
        when(condition3.getOperator()).thenReturn(QualityGate.Operator.LESS_THAN);
        when(condition3.getErrorThreshold()).thenReturn("80");

        QualityGate.Condition condition4 = mock();
        when(condition4.getStatus()).thenReturn(QualityGate.EvaluationStatus.ERROR);
        when(condition4.getMetricKey()).thenReturn(CoreMetrics.NEW_SECURITY_RATING.getKey());
        when(condition4.getValue()).thenReturn("5");
        when(condition4.getOperator()).thenReturn(QualityGate.Operator.GREATER_THAN);
        when(condition4.getErrorThreshold()).thenReturn("4");

        QualityGate.Condition condition5 = mock();
        when(condition5.getStatus()).thenReturn(QualityGate.EvaluationStatus.ERROR);
        when(condition5.getMetricKey()).thenReturn(CoreMetrics.RELIABILITY_RATING.getKey());
        when(condition5.getValue()).thenReturn("1");
        when(condition5.getOperator()).thenReturn(QualityGate.Operator.LESS_THAN);
        when(condition5.getErrorThreshold()).thenReturn("3");

        QualityGate.Condition condition6 = mock();
        when(condition6.getStatus()).thenReturn(QualityGate.EvaluationStatus.ERROR);
        when(condition6.getMetricKey()).thenReturn(CoreMetrics.NEW_COVERAGE.getKey());
        when(condition6.getValue()).thenReturn(coverage);
        when(condition6.getOperator()).thenReturn(QualityGate.Operator.GREATER_THAN);
        when(condition6.getErrorThreshold()).thenReturn("15");

        QualityGate.Condition condition7 = mock();
        when(condition7.getStatus()).thenReturn(QualityGate.EvaluationStatus.OK);
        when(condition7.getMetricKey()).thenReturn(CoreMetrics.NEW_RELIABILITY_ISSUES_KEY);
        when(condition7.getValue()).thenReturn("0");
        when(condition7.getOperator()).thenReturn(QualityGate.Operator.LESS_THAN);
        when(condition7.getErrorThreshold()).thenReturn("1");

        QualityGate.Condition condition8 = mock();
        when(condition8.getStatus()).thenReturn(QualityGate.EvaluationStatus.OK);
        when(condition8.getMetricKey()).thenReturn(CoreMetrics.NEW_DUPLICATED_LINES_DENSITY.getKey());
        when(condition8.getValue()).thenReturn(duplications);
        when(condition8.getOperator()).thenReturn(QualityGate.Operator.GREATER_THAN);
        when(condition8.getErrorThreshold()).thenReturn("1");

        when(analysisDetails.findFailedQualityGateConditions()).thenReturn(Arrays.asList(condition1, condition2, condition3, condition4));
        when(analysisDetails.findQualityGateCondition(any())).thenAnswer(i -> Stream.of(condition1, condition2, condition3, condition4, condition5, condition6, condition7, condition8)
            .filter(condition -> condition.getMetricKey().equals(i.getArgument(0, String.class)))
            .findFirst());
        when(analysisDetails.getQualityGateStatus()).thenReturn(QualityGate.Status.ERROR);

        Project project = mock();
        when(project.getKey()).thenReturn("Project Key");

        Component rootComponent = mock();
        when(treeRootHolder.getRoot()).thenReturn(rootComponent);

        MetricRepository metricRepository = mock();
        Metric covergeMetric = mock();
        when(metricRepository.getByKey(CoreMetrics.COVERAGE_KEY)).thenReturn(covergeMetric);
        Metric duplicationsMetric = mock();
        when(metricRepository.getByKey(CoreMetrics.DUPLICATED_LINES_DENSITY_KEY)).thenReturn(duplicationsMetric);

        MeasureRepository measureRepository = mock();
        if (coverage != null) {
            when(measureRepository.getRawMeasure(rootComponent, covergeMetric))
                .thenReturn(Optional.of(Measure.newMeasureBuilder().create(Double.parseDouble(coverage), 2, "data")));
        }
        if (duplications != null) {
            when(measureRepository.getRawMeasure(rootComponent, duplicationsMetric))
                .thenReturn(Optional.of(Measure.newMeasureBuilder().create(Double.parseDouble(duplications), 2, "data")));
        }
        
        Server server = mock();
        when(server.getPublicRootUrl()).thenReturn("http://localhost:9000");
        Configuration configuration = mock();
        ReportGenerator underTest = new ReportGenerator(server, configuration, measureRepository, metricRepository, treeRootHolder);

        AnalysisSummary expected = AnalysisSummary.builder()
                        .withCoverage(new AnalysisSummary.UrlIconMetric<>("http://localhost:9000/component_measures?id=projectKey&metric=new_coverage&pullRequest=5&view=list", "http://localhost:9000/static/communityBranchPlugin/common/" + coverageImage, null == coverage ? null : new BigDecimal(coverage)))
                        .withNewCoverage(null == coverage ? null : new BigDecimal(coverage))
                        .withDuplications(new AnalysisSummary.UrlIconMetric<>("http://localhost:9000/component_measures?id=projectKey&metric=new_duplicated_lines_density&pullRequest=5&view=list", "http://localhost:9000/static/communityBranchPlugin/common/" + duplicationsImage, null == duplications ? null : new BigDecimal(duplications).setScale(1, RoundingMode.CEILING)))
                        .withNewDuplications(null == duplications ? null : new BigDecimal(duplications))
                        .withDashboardUrl("http://localhost:9000/dashboard?id=projectKey&pullRequest=5")
                        .withProjectKey("projectKey")
                        .withSummaryImageUrl("http://localhost:9000/static/communityBranchPlugin/common/icon.png")
                        .withSecurityHotspots(new AnalysisSummary.UrlIconMetric<>("http://localhost:9000/security_hotspots?id=projectKey&pullRequest=5", "http://localhost:9000/static/communityBranchPlugin/common/passed-16px.png", 0))
                        .withStatusDescription("Failed")
                        .withStatusImageUrl("http://localhost:9000/static/communityBranchPlugin/checks/QualityGateBadge/failed-16px.png")
                        .withAcceptedIssues(new AnalysisSummary.UrlIconMetric<>("http://localhost:9000/project/issues?id=projectKey&pullRequest=5&issueStatus=ACCEPTED", "http://localhost:9000/static/communityBranchPlugin/common/accepted-16px.png", 0))
                        .withNewIssues(new AnalysisSummary.UrlIconMetric<>("http://localhost:9000/project/issues?id=projectKey&pullRequest=5&resolved=false", "http://localhost:9000/static/communityBranchPlugin/common/passed-16px.png", 5L))
                        .withFixedIssues(new AnalysisSummary.UrlIconMetric<>("http://localhost:9000/project/issues?id=projectKey&fixedInPullRequest=5", "http://localhost:9000/static/communityBranchPlugin/common/fixed-16px.png", 0))
                        .withFailedQualityGateConditions(List.of("19 Lines to Cover (is less than 20)",
                                "2 Maintainability Issues (is greater than 0)",
                                "68.00% Line Coverage (is less than 80.00%)",
                                "E Security Rating on New Code (is worse than D)"))
                        .build();

        assertThat(underTest.createAnalysisSummary(analysisDetails))
                .usingRecursiveComparison()
                .isEqualTo(expected);
    }

    @CsvSource({"SECURITY, security_hotspots?id=project-key&pullRequest=pull-request-id&hotspots=issue-key",
            "MAINTAINABILITY, project/issues?id=project-key&pullRequest=pull-request-id&issues=issue-key&open=issue-key"})
    @ParameterizedTest
    void shouldProduceCorrectAnalysisIssueSummary(SoftwareQuality ruleType, String issueUrlPostfix) {
        MeasureRepository measureRepository = mock();
        MetricRepository metricRepository = mock();
        TreeRootHolder treeRootHolder = mock();

        Server server = mock();
        when(server.getPublicRootUrl()).thenReturn("http://target.host:port/path/to/root");
        Configuration configuration = mock();
        ReportGenerator underTest = new ReportGenerator(server, configuration, measureRepository, metricRepository, treeRootHolder);

        AnalysisIssueSummary expected = AnalysisIssueSummary.builder()
                .withIssueUrl("http://target.host:port/path/to/root/" + issueUrlPostfix)
                .withMessage("message")
                .build();

        PostAnalysisIssueVisitor.LightIssue lightIssue = mock();
        when(lightIssue.key()).thenReturn("issue-key");
        when(lightIssue.impacts()).thenReturn(Map.of(ruleType, Severity.HIGH));
        when(lightIssue.getMessage()).thenReturn("message");
        when(lightIssue.resolution()).thenReturn("FIXED");
        PostAnalysisIssueVisitor.ComponentIssue componentIssue = mock();
        when(componentIssue.getIssue()).thenReturn(lightIssue);

        AnalysisDetails analysisDetails = mock();
        when(analysisDetails.getAnalysisProjectKey()).thenReturn("project-key");
        when(analysisDetails.getPullRequestId()).thenReturn("pull-request-id");

        assertThat(underTest.createAnalysisIssueSummary(componentIssue, analysisDetails))
                .usingRecursiveComparison()
                .isEqualTo(expected);
    }

}
