/*
 * Copyright (C) 2022 Michael Clarke
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

import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.AnalysisDetails;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.PostAnalysisIssueVisitor;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.sonar.api.ce.posttask.Project;
import org.sonar.api.ce.posttask.QualityGate;
import org.sonar.api.config.Configuration;
import org.sonar.api.issue.Issue;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.platform.Server;
import org.sonar.api.rules.RuleType;
import org.sonar.ce.task.projectanalysis.component.Component;
import org.sonar.ce.task.projectanalysis.component.TreeRootHolder;
import org.sonar.ce.task.projectanalysis.measure.Measure;
import org.sonar.ce.task.projectanalysis.measure.MeasureRepository;
import org.sonar.ce.task.projectanalysis.metric.Metric;
import org.sonar.ce.task.projectanalysis.metric.MetricRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

class ReportGeneratorTest {

    @CsvSource({"12, 0.png, 21, 20plus.png",
            "98, 90.png, 1, 3.png",
            ",NoCoverageInfo.png,,NoDuplicationInfo.png"})
    @ParameterizedTest
    void shouldProduceCorrectAnalysisSummary(String coverage, String coverageImage, String duplications, String duplicationsImage) {
        AnalysisDetails analysisDetails = mock(AnalysisDetails.class);
        doReturn("5").when(analysisDetails).getPullRequestId();
        doReturn("projectKey").when(analysisDetails).getAnalysisProjectKey();

        TreeRootHolder treeRootHolder = mock(TreeRootHolder.class);

        PostAnalysisIssueVisitor.LightIssue issue1 = mock(PostAnalysisIssueVisitor.LightIssue.class);
        doReturn(Issue.STATUS_CLOSED).when(issue1).status();

        PostAnalysisIssueVisitor.LightIssue issue2 = mock(PostAnalysisIssueVisitor.LightIssue.class);
        doReturn(Issue.STATUS_OPEN).when(issue2).status();
        doReturn(RuleType.BUG).when(issue2).type();

        PostAnalysisIssueVisitor.LightIssue issue3 = mock(PostAnalysisIssueVisitor.LightIssue.class);
        doReturn(Issue.STATUS_OPEN).when(issue3).status();
        doReturn(RuleType.SECURITY_HOTSPOT).when(issue3).type();

        PostAnalysisIssueVisitor.LightIssue issue4 = mock(PostAnalysisIssueVisitor.LightIssue.class);
        doReturn(Issue.STATUS_OPEN).when(issue4).status();
        doReturn(RuleType.CODE_SMELL).when(issue4).type();

        PostAnalysisIssueVisitor.LightIssue issue5 = mock(PostAnalysisIssueVisitor.LightIssue.class);
        doReturn(Issue.STATUS_OPEN).when(issue5).status();
        doReturn(RuleType.VULNERABILITY).when(issue5).type();

        PostAnalysisIssueVisitor.LightIssue issue6 = mock(PostAnalysisIssueVisitor.LightIssue.class);
        doReturn(Issue.STATUS_OPEN).when(issue6).status();
        doReturn(RuleType.BUG).when(issue6).type();

        doReturn(Stream.of(issue1, issue2, issue3, issue4, issue5, issue6).map(i -> {
            PostAnalysisIssueVisitor.ComponentIssue componentIssue =
                    mock(PostAnalysisIssueVisitor.ComponentIssue.class);
            doReturn(i).when(componentIssue).getIssue();
            return componentIssue;
        }).collect(Collectors.toList())).when(analysisDetails).getIssues();

        QualityGate.Condition condition1 = mock(QualityGate.Condition.class);
        doReturn(QualityGate.EvaluationStatus.ERROR).when(condition1).getStatus();
        doReturn(CoreMetrics.LINES_TO_COVER.getKey()).when(condition1).getMetricKey();
        doReturn("19").when(condition1).getValue();
        doReturn(QualityGate.Operator.LESS_THAN).when(condition1).getOperator();
        doReturn("20").when(condition1).getErrorThreshold();

        QualityGate.Condition condition2 = mock(QualityGate.Condition.class);
        doReturn(QualityGate.EvaluationStatus.ERROR).when(condition2).getStatus();
        doReturn(CoreMetrics.CODE_SMELLS.getKey()).when(condition2).getMetricKey();
        doReturn("2").when(condition2).getValue();
        doReturn(QualityGate.Operator.GREATER_THAN).when(condition2).getOperator();
        doReturn("0").when(condition2).getErrorThreshold();

        QualityGate.Condition condition3 = mock(QualityGate.Condition.class);
        doReturn(QualityGate.EvaluationStatus.ERROR).when(condition3).getStatus();
        doReturn(CoreMetrics.LINE_COVERAGE.getKey()).when(condition3).getMetricKey();
        doReturn("68").when(condition3).getValue();
        doReturn(QualityGate.Operator.LESS_THAN).when(condition3).getOperator();
        doReturn("80").when(condition3).getErrorThreshold();

        QualityGate.Condition condition4 = mock(QualityGate.Condition.class);
        doReturn(QualityGate.EvaluationStatus.ERROR).when(condition4).getStatus();
        doReturn(CoreMetrics.NEW_SECURITY_RATING.getKey()).when(condition4).getMetricKey();
        doReturn("5").when(condition4).getValue();
        doReturn(QualityGate.Operator.GREATER_THAN).when(condition4).getOperator();
        doReturn("4").when(condition4).getErrorThreshold();

        QualityGate.Condition condition5 = mock(QualityGate.Condition.class);
        doReturn(QualityGate.EvaluationStatus.ERROR).when(condition5).getStatus();
        doReturn(CoreMetrics.RELIABILITY_RATING.getKey()).when(condition5).getMetricKey();
        doReturn("1").when(condition5).getValue();
        doReturn(QualityGate.Operator.LESS_THAN).when(condition5).getOperator();
        doReturn("3").when(condition5).getErrorThreshold();

        QualityGate.Condition condition6 = mock(QualityGate.Condition.class);
        doReturn(QualityGate.EvaluationStatus.ERROR).when(condition6).getStatus();
        doReturn(CoreMetrics.NEW_COVERAGE.getKey()).when(condition6).getMetricKey();
        doReturn(coverage).when(condition6).getValue();
        doReturn(QualityGate.Operator.GREATER_THAN).when(condition6).getOperator();
        doReturn("15").when(condition6).getErrorThreshold();

        QualityGate.Condition condition7 = mock(QualityGate.Condition.class);
        doReturn(QualityGate.EvaluationStatus.OK).when(condition7).getStatus();
        doReturn(CoreMetrics.NEW_BUGS.getKey()).when(condition7).getMetricKey();
        doReturn("0").when(condition7).getValue();
        doReturn(QualityGate.Operator.LESS_THAN).when(condition7).getOperator();
        doReturn("1").when(condition7).getErrorThreshold();

        QualityGate.Condition condition8 = mock(QualityGate.Condition.class);
        doReturn(QualityGate.EvaluationStatus.OK).when(condition8).getStatus();
        doReturn(CoreMetrics.NEW_DUPLICATED_LINES_DENSITY.getKey()).when(condition8).getMetricKey();
        doReturn(duplications).when(condition8).getValue();
        doReturn(QualityGate.Operator.GREATER_THAN).when(condition8).getOperator();
        doReturn("1").when(condition8).getErrorThreshold();


        doReturn(Arrays.asList(condition1, condition2, condition3, condition4))
                .when(analysisDetails).findFailedQualityGateConditions();
        doAnswer(i -> Stream.of(condition1, condition2, condition3, condition4, condition5, condition6, condition7, condition8).filter(condition -> condition.getMetricKey().equals(i.getArgument(0, String.class))).findFirst()).when(analysisDetails).findQualityGateCondition(any());
        doReturn(QualityGate.Status.ERROR).when(analysisDetails).getQualityGateStatus();

        Project project = mock(Project.class);
        doReturn("Project Key").when(project).getKey();

        Component rootComponent = mock(Component.class);
        doReturn(rootComponent).when(treeRootHolder).getRoot();

        MeasureRepository measureRepository = mock(MeasureRepository.class);
        if (coverage != null) {
            doReturn(Optional.of(Measure.newMeasureBuilder().create(Double.parseDouble(coverage), 2, "data")),
                    Optional.of(Measure.newMeasureBuilder().create(Double.parseDouble(duplications), 2, "data"))).when(measureRepository)
                    .getRawMeasure(eq(rootComponent), any(Metric.class));
        }

        MetricRepository metricRepository = mock(MetricRepository.class);
        doReturn(mock(Metric.class)).when(metricRepository).getByKey(anyString());

        Server server = mock(Server.class);
        doReturn("http://localhost:9000").when(server).getPublicRootUrl();
        Configuration configuration = mock(Configuration.class);
        ReportGenerator underTest = new ReportGenerator(server, configuration, measureRepository, metricRepository, treeRootHolder);

        AnalysisSummary expected = AnalysisSummary.builder()
                        .withBugCount(2)
                        .withBugUrl("http://localhost:9000/project/issues?pullRequest=5&resolved=false&types=BUG&inNewCodePeriod=true&id=projectKey")
                        .withBugImageUrl("http://localhost:9000/static/communityBranchPlugin/common/bug.png")
                        .withCoverage(null == coverage ? null : new BigDecimal(coverage))
                        .withCoverageUrl("http://localhost:9000/component_measures?id=projectKey&metric=new_coverage&pullRequest=5&view=list")
                        .withCoverageImageUrl("http://localhost:9000/static/communityBranchPlugin/checks/CoverageChart/" + coverageImage)
                        .withNewCoverage(null == coverage ? null : new BigDecimal(coverage))
                        .withDuplications(null == duplications ? null : new BigDecimal(duplications).setScale(1, RoundingMode.CEILING))
                        .withDuplicationsUrl("http://localhost:9000/component_measures?id=projectKey&metric=new_duplicated_lines_density&pullRequest=5&view=list")
                        .withDuplicationsImageUrl("http://localhost:9000/static/communityBranchPlugin/checks/Duplications/" + duplicationsImage)
                        .withNewDuplications(null == duplications ? null : new BigDecimal(duplications))
                        .withCodeSmellCount(1)
                        .withCodeSmellUrl("http://localhost:9000/project/issues?pullRequest=5&resolved=false&types=CODE_SMELL&inNewCodePeriod=true&id=projectKey")
                        .withCodeSmellImageUrl("http://localhost:9000/static/communityBranchPlugin/common/code_smell.png")
                        .withDashboardUrl("http://localhost:9000/dashboard?id=projectKey&pullRequest=5")
                        .withProjectKey("projectKey")
                        .withSummaryImageUrl("http://localhost:9000/static/communityBranchPlugin/common/icon.png")
                        .withSecurityHotspotCount(1)
                        .withVulnerabilityCount(1)
                        .withVulnerabilityUrl("http://localhost:9000/project/issues?pullRequest=5&resolved=false&types=VULNERABILITY&inNewCodePeriod=true&id=projectKey")
                        .withVulnerabilityImageUrl("http://localhost:9000/static/communityBranchPlugin/common/vulnerability.png")
                        .withStatusDescription("Failed")
                        .withStatusImageUrl("http://localhost:9000/static/communityBranchPlugin/checks/QualityGateBadge/failed.png")
                        .withTotalIssueCount(5)
                        .withFailedQualityGateConditions(List.of("19 Lines to Cover (is less than 20)",
                                "2 Code Smells (is greater than 0)",
                                "68.00% Line Coverage (is less than 80.00%)",
                                "E Security Rating on New Code (is worse than D)"))
                        .build();

        assertThat(underTest.createAnalysisSummary(analysisDetails))
                .usingRecursiveComparison()
                .isEqualTo(expected);
    }

    @CsvSource({"SECURITY_HOTSPOT, security_hotspots?id=project-key&pullRequest=pull-request-id&hotspots=issue-key",
            "BUG, project/issues?id=project-key&pullRequest=pull-request-id&issues=issue-key&open=issue-key"})
    @ParameterizedTest
    void shouldProduceCorrectAnalysisIssueSummary(RuleType ruleType, String issueUrlPostfix) {
        MeasureRepository measureRepository = mock(MeasureRepository.class);
        MetricRepository metricRepository = mock(MetricRepository.class);
        TreeRootHolder treeRootHolder = mock(TreeRootHolder.class);

        Server server = mock(Server.class);
        doReturn("http://target.host:port/path/to/root").when(server).getPublicRootUrl();
        Configuration configuration = mock(Configuration.class);
        ReportGenerator underTest = new ReportGenerator(server, configuration, measureRepository, metricRepository, treeRootHolder);

        AnalysisIssueSummary expected = AnalysisIssueSummary.builder()
                .withEffortInMinutes(101L)
                .withIssueKey("issue-key")
                .withIssueUrl("http://target.host:port/path/to/root/" + issueUrlPostfix)
                .withMessage("message")
                .withResolution("FIXED")
                .withSeverity("CRITICAL")
                .withSeverityImageUrl("http://target.host:port/path/to/root/static/communityBranchPlugin/checks/Severity/critical.png")
                .withType(ruleType.name())
                .withTypeImageUrl("http://target.host:port/path/to/root/static/communityBranchPlugin/checks/IssueType/" + ruleType.name().toLowerCase(Locale.ENGLISH) + ".png")
                .withProjectKey("project-key")
                .build();

        PostAnalysisIssueVisitor.LightIssue lightIssue = mock(PostAnalysisIssueVisitor.LightIssue.class);
        doReturn("issue-key").when(lightIssue).key();
        doReturn("CRITICAL").when(lightIssue).severity();
        doReturn("message").when(lightIssue).getMessage();
        doReturn("FIXED").when(lightIssue).resolution();
        doReturn(ruleType).when(lightIssue).type();
        doReturn(101L).when(lightIssue).effortInMinutes();
        PostAnalysisIssueVisitor.ComponentIssue componentIssue = mock(PostAnalysisIssueVisitor.ComponentIssue.class);
        doReturn(lightIssue).when(componentIssue).getIssue();

        AnalysisDetails analysisDetails = mock(AnalysisDetails.class);
        doReturn("project-key").when(analysisDetails).getAnalysisProjectKey();
        doReturn("pull-request-id").when(analysisDetails).getPullRequestId();

        assertThat(underTest.createAnalysisIssueSummary(componentIssue, analysisDetails))
                .usingRecursiveComparison()
                .isEqualTo(expected);
    }

}
