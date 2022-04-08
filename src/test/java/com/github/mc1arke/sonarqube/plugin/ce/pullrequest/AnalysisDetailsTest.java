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

import com.github.mc1arke.sonarqube.plugin.CommunityBranchPlugin;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.markup.Document;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.markup.Formatter;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.markup.FormatterFactory;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.markup.Heading;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.markup.Image;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.markup.Link;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.markup.List;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.markup.ListItem;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.markup.Paragraph;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.markup.Text;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.sonar.api.ce.posttask.Analysis;
import org.sonar.api.ce.posttask.Project;
import org.sonar.api.ce.posttask.QualityGate;
import org.sonar.api.ce.posttask.ScannerContext;
import org.sonar.api.config.Configuration;
import org.sonar.api.issue.Issue;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.rules.RuleType;
import org.sonar.ce.task.projectanalysis.component.Component;
import org.sonar.ce.task.projectanalysis.component.ReportAttributes;
import org.sonar.ce.task.projectanalysis.component.TreeRootHolder;
import org.sonar.ce.task.projectanalysis.measure.Measure;
import org.sonar.ce.task.projectanalysis.measure.MeasureRepository;
import org.sonar.ce.task.projectanalysis.metric.Metric;
import org.sonar.ce.task.projectanalysis.metric.MetricRepository;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class AnalysisDetailsTest {

    @Test
    public void testGetBranchName() {
        AnalysisDetails.BranchDetails branchDetails = mock(AnalysisDetails.BranchDetails.class);
        doReturn("branchName").when(branchDetails).getBranchName();

        AnalysisDetails.MeasuresHolder measuresHolder = mock(AnalysisDetails.MeasuresHolder.class);
        PostAnalysisIssueVisitor postAnalysisIssueVisitor = mock(PostAnalysisIssueVisitor.class);
        QualityGate qualityGate = mock(QualityGate.class);
        Analysis analysis = mock(Analysis.class);
        Project project = mock(Project.class);
        Configuration configuration = mock(Configuration.class);
        ScannerContext scannerContext = mock(ScannerContext.class);

        AnalysisDetails testCase =
                new AnalysisDetails(branchDetails, postAnalysisIssueVisitor, qualityGate, measuresHolder, analysis,
                                    project, configuration, null, scannerContext);

        assertEquals("branchName", testCase.getBranchName());
    }

    @Test
    public void testGetCommitSha() {
        AnalysisDetails.BranchDetails branchDetails = mock(AnalysisDetails.BranchDetails.class);
        doReturn("commitId").when(branchDetails).getCommitId();

        AnalysisDetails.MeasuresHolder measuresHolder = mock(AnalysisDetails.MeasuresHolder.class);
        PostAnalysisIssueVisitor postAnalysisIssueVisitor = mock(PostAnalysisIssueVisitor.class);
        QualityGate qualityGate = mock(QualityGate.class);
        Analysis analysis = mock(Analysis.class);
        Project project = mock(Project.class);
        ScannerContext scannerContext = mock(ScannerContext.class);
        Configuration configuration = mock(Configuration.class);

        AnalysisDetails testCase =
                new AnalysisDetails(branchDetails, postAnalysisIssueVisitor, qualityGate, measuresHolder, analysis,
                                    project, configuration, null, scannerContext);

        assertEquals("commitId", testCase.getCommitSha());
    }

    @Test
    public void testGetQualityGateStatus() {
        AnalysisDetails.BranchDetails branchDetails = mock(AnalysisDetails.BranchDetails.class);
        AnalysisDetails.MeasuresHolder measuresHolder = mock(AnalysisDetails.MeasuresHolder.class);
        PostAnalysisIssueVisitor postAnalysisIssueVisitor = mock(PostAnalysisIssueVisitor.class);
        QualityGate qualityGate = mock(QualityGate.class);
        doReturn(QualityGate.Status.ERROR).when(qualityGate).getStatus();
        Analysis analysis = mock(Analysis.class);
        Project project = mock(Project.class);
        ScannerContext scannerContext = mock(ScannerContext.class);
        Configuration configuration = mock(Configuration.class);

        AnalysisDetails testCase =
                new AnalysisDetails(branchDetails, postAnalysisIssueVisitor, qualityGate, measuresHolder, analysis,
                                    project, configuration, null, scannerContext);

        assertEquals(QualityGate.Status.ERROR, testCase.getQualityGateStatus());
    }

    @Test
    public void testGetAnalysisDate() {
        AnalysisDetails.BranchDetails branchDetails = mock(AnalysisDetails.BranchDetails.class);
        AnalysisDetails.MeasuresHolder measuresHolder = mock(AnalysisDetails.MeasuresHolder.class);
        PostAnalysisIssueVisitor postAnalysisIssueVisitor = mock(PostAnalysisIssueVisitor.class);
        QualityGate qualityGate = mock(QualityGate.class);
        Analysis analysis = mock(Analysis.class);
        doReturn(new Date()).when(analysis).getDate();
        Project project = mock(Project.class);
        ScannerContext scannerContext = mock(ScannerContext.class);
        Configuration configuration = mock(Configuration.class);

        AnalysisDetails testCase =
                new AnalysisDetails(branchDetails, postAnalysisIssueVisitor, qualityGate, measuresHolder, analysis,
                                    project, configuration, null, scannerContext);

        assertEquals(analysis.getDate(), testCase.getAnalysisDate());
    }

    @Test
    public void testGetAnalysisId() {
        AnalysisDetails.BranchDetails branchDetails = mock(AnalysisDetails.BranchDetails.class);
        AnalysisDetails.MeasuresHolder measuresHolder = mock(AnalysisDetails.MeasuresHolder.class);
        PostAnalysisIssueVisitor postAnalysisIssueVisitor = mock(PostAnalysisIssueVisitor.class);
        QualityGate qualityGate = mock(QualityGate.class);
        Analysis analysis = mock(Analysis.class);
        doReturn("Analysis ID").when(analysis).getAnalysisUuid();
        Project project = mock(Project.class);
        ScannerContext scannerContext = mock(ScannerContext.class);
        Configuration configuration = mock(Configuration.class);

        AnalysisDetails testCase =
                new AnalysisDetails(branchDetails, postAnalysisIssueVisitor, qualityGate, measuresHolder, analysis,
                                    project, configuration, null, scannerContext);

        assertEquals("Analysis ID", testCase.getAnalysisId());
    }

    @Test
    public void testGetAnalysisProjectKey() {
        AnalysisDetails.BranchDetails branchDetails = mock(AnalysisDetails.BranchDetails.class);
        AnalysisDetails.MeasuresHolder measuresHolder = mock(AnalysisDetails.MeasuresHolder.class);
        PostAnalysisIssueVisitor postAnalysisIssueVisitor = mock(PostAnalysisIssueVisitor.class);
        QualityGate qualityGate = mock(QualityGate.class);
        Analysis analysis = mock(Analysis.class);
        Project project = mock(Project.class);
        doReturn("Project Key").when(project).getKey();
        ScannerContext scannerContext = mock(ScannerContext.class);
        Configuration configuration = mock(Configuration.class);

        AnalysisDetails testCase =
                new AnalysisDetails(branchDetails, postAnalysisIssueVisitor, qualityGate, measuresHolder, analysis,
                                    project, configuration, null, scannerContext);

        assertEquals("Project Key", testCase.getAnalysisProjectKey());
    }

    @Test
    public void testCreateAnalysisSummary() {
        AnalysisDetails.BranchDetails branchDetails = mock(AnalysisDetails.BranchDetails.class);
        doReturn("5").when(branchDetails).getBranchName();

        TreeRootHolder treeRootHolder = mock(TreeRootHolder.class);
        AnalysisDetails.MeasuresHolder measuresHolder = mock(AnalysisDetails.MeasuresHolder.class);
        doReturn(treeRootHolder).when(measuresHolder).getTreeRootHolder();

        PostAnalysisIssueVisitor postAnalysisIssueVisitor = mock(PostAnalysisIssueVisitor.class);
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
        }).collect(Collectors.toList())).when(postAnalysisIssueVisitor).getIssues();

        QualityGate.Condition condition1 = mock(QualityGate.Condition.class);
        doReturn(QualityGate.EvaluationStatus.ERROR).when(condition1).getStatus();
        doReturn(CoreMetrics.LINES_TO_COVER.getKey()).when(condition1).getMetricKey();
        doReturn("12").when(condition1).getValue();
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
        doReturn(CoreMetrics.BRANCH_COVERAGE.getKey()).when(condition6).getMetricKey();
        doReturn("16").when(condition6).getValue();
        doReturn(QualityGate.Operator.GREATER_THAN).when(condition6).getOperator();
        doReturn("15").when(condition6).getErrorThreshold();

        QualityGate.Condition condition7 = mock(QualityGate.Condition.class);
        doReturn(QualityGate.EvaluationStatus.OK).when(condition7).getStatus();
        doReturn(CoreMetrics.NEW_BUGS.getKey()).when(condition7).getMetricKey();
        doReturn("0").when(condition7).getValue();
        doReturn(QualityGate.Operator.LESS_THAN).when(condition7).getOperator();
        doReturn("1").when(condition7).getErrorThreshold();

        QualityGate qualityGate = mock(QualityGate.class);
        doReturn(Arrays.asList(condition1, condition2, condition3, condition4, condition5, condition6, condition7))
                .when(qualityGate).getConditions();

        Analysis analysis = mock(Analysis.class);
        Project project = mock(Project.class);
        doReturn("Project Key").when(project).getKey();

        Component rootComponent = mock(Component.class);
        doReturn(rootComponent).when(treeRootHolder).getRoot();

        MeasureRepository measureRepository = mock(MeasureRepository.class);
        doReturn(Optional.of(Measure.newMeasureBuilder().create(12.3, 2, "data"))).when(measureRepository)
                .getRawMeasure(eq(rootComponent), any(Metric.class));
        doReturn(measureRepository).when(measuresHolder).getMeasureRepository();

        MetricRepository metricRepository = mock(MetricRepository.class);
        doReturn(mock(Metric.class)).when(metricRepository).getByKey(anyString());
        doReturn(metricRepository).when(measuresHolder).getMetricRepository();

        ScannerContext scannerContext = mock(ScannerContext.class);
        Configuration configuration = mock(Configuration.class);

        AnalysisDetails testCase =
                new AnalysisDetails(branchDetails, postAnalysisIssueVisitor, qualityGate, measuresHolder, analysis,
                                    project, configuration, "http://localhost:9000", scannerContext);

        Formatter<Document> formatter = mock(Formatter.class);
        doReturn("formatted content").when(formatter).format(any(), any());
        FormatterFactory formatterFactory = mock(FormatterFactory.class);
        doReturn(formatter).when(formatterFactory).documentFormatter();

        assertEquals("formatted content", testCase.createAnalysisSummary(formatterFactory));

        ArgumentCaptor<Document> documentArgumentCaptor = ArgumentCaptor.forClass(Document.class);
        verify(formatter).format(documentArgumentCaptor.capture(), eq(formatterFactory));

        Document expectedDocument = new Document(new Paragraph(new Image("Failed",
                                                                         "http://localhost:9000/static/communityBranchPlugin/checks/QualityGateBadge/failed.svg?sanitize=true")),
                                                 new List(List.Style.BULLET,
                                                          new ListItem(new Text("12 Lines to Cover (is less than 20)")),
                                                          new ListItem(new Text("2 Code Smells (is greater than 0)")),
                                                          new ListItem(new Text(
                                                                  "68.00% Line Coverage (is less than 80.00%)")),
                                                          new ListItem(new Text(
                                                                  "E Security Rating on New Code (is worse than D)")),
                                                          new ListItem(
                                                                  new Text("A Reliability Rating (is better than C)")),
                                                          new ListItem(new Text(
                                                                  "16.00% Condition Coverage (is greater than 15.00%)"))),
                                                 new Heading(1, new Text("Analysis Details")),
                                                 new Heading(2, new Text("5 Issues")), new List(List.Style.BULLET,
                                                                                                new ListItem(
                                                                                                        new Image("Bug",
                                                                                                                  "http://localhost:9000/static/communityBranchPlugin/common/bug.svg?sanitize=true"),
                                                                                                        new Text(" "),
                                                                                                        new Text(
                                                                                                                "2 Bugs")),
                                                                                                new ListItem(new Image(
                                                                                                        "Vulnerability",
                                                                                                        "http://localhost:9000/static/communityBranchPlugin/common/vulnerability.svg?sanitize=true"),
                                                                                                             new Text(
                                                                                                                     " "),
                                                                                                             new Text(
                                                                                                                     "2 Vulnerabilities")),
                                                                                                new ListItem(new Image(
                                                                                                        "Code Smell",
                                                                                                        "http://localhost:9000/static/communityBranchPlugin/common/code_smell.svg?sanitize=true"),
                                                                                                             new Text(
                                                                                                                     " "),
                                                                                                             new Text(
                                                                                                                     "1 Code Smell"))),
                                                 new Heading(2, new Text("Coverage and Duplications")),
                                                 new List(List.Style.BULLET, new ListItem(
                                                         new Image("No coverage information",
                                                                   "http://localhost:9000/static/communityBranchPlugin/checks/CoverageChart/NoCoverageInfo.svg?sanitize=true"),
                                                         new Text(" "), new Text(
                                                         "No coverage information (12.30% Estimated after merge)")),
                                                          new ListItem(new Image("No duplication information",
                                                                                 "http://localhost:9000/static/communityBranchPlugin/checks/Duplications/NoDuplicationInfo.svg?sanitize=true"),
                                                                       new Text(" "), new Text(
                                                                  "No duplication information (12.30% Estimated after merge)"))),
                                                 new Paragraph(new Text("**Project ID:** Project Key")),
                                                 new Paragraph(new Link("http://localhost:9000/dashboard?id=Project+Key&pullRequest=5", new Text("View in SonarQube"))));

        assertThat(documentArgumentCaptor.getValue()).usingRecursiveComparison().isEqualTo(expectedDocument);

    }


    @Test
    public void testCreateAnalysisSummary2() {
        AnalysisDetails.BranchDetails branchDetails = mock(AnalysisDetails.BranchDetails.class);
        doReturn("5").when(branchDetails).getBranchName();

        TreeRootHolder treeRootHolder = mock(TreeRootHolder.class);
        AnalysisDetails.MeasuresHolder measuresHolder = mock(AnalysisDetails.MeasuresHolder.class);
        doReturn(treeRootHolder).when(measuresHolder).getTreeRootHolder();

        PostAnalysisIssueVisitor postAnalysisIssueVisitor = mock(PostAnalysisIssueVisitor.class);
        doReturn(new ArrayList<>()).when(postAnalysisIssueVisitor).getIssues();

        QualityGate.Condition duplicationsCondition = mock(QualityGate.Condition.class);
        doReturn("18").when(duplicationsCondition).getValue();
        doReturn(CoreMetrics.NEW_DUPLICATED_LINES_DENSITY_KEY).when(duplicationsCondition).getMetricKey();


        QualityGate.Condition coverageCondition = mock(QualityGate.Condition.class);
        doReturn("33").when(coverageCondition).getValue();
        doReturn(CoreMetrics.NEW_COVERAGE_KEY).when(coverageCondition).getMetricKey();

        QualityGate qualityGate = mock(QualityGate.class);
        doReturn(QualityGate.Status.OK).when(qualityGate).getStatus();
        doReturn(Arrays.asList(coverageCondition, duplicationsCondition)).when(qualityGate).getConditions();

        Analysis analysis = mock(Analysis.class);
        Project project = mock(Project.class);
        doReturn("Project Key").when(project).getKey();

        Component rootComponent = mock(Component.class);
        doReturn(rootComponent).when(treeRootHolder).getRoot();

        MeasureRepository measureRepository = mock(MeasureRepository.class);
        doReturn(Optional.of(Measure.newMeasureBuilder().create(21.782, 2, "data"))).when(measureRepository)
                .getRawMeasure(eq(rootComponent), any(Metric.class));
        doReturn(measureRepository).when(measuresHolder).getMeasureRepository();

        MetricRepository metricRepository = mock(MetricRepository.class);
        doReturn(mock(Metric.class)).when(metricRepository).getByKey(anyString());
        doReturn(metricRepository).when(measuresHolder).getMetricRepository();

        ScannerContext scannerContext = mock(ScannerContext.class);

        Configuration configuration = mock(Configuration.class);

        AnalysisDetails testCase =
                new AnalysisDetails(branchDetails, postAnalysisIssueVisitor, qualityGate, measuresHolder, analysis,
                                    project, configuration, "http://localhost:9000", scannerContext);

        Formatter<Document> formatter = mock(Formatter.class);
        doReturn("formatted content").when(formatter).format(any(), any());
        FormatterFactory formatterFactory = mock(FormatterFactory.class);
        doReturn(formatter).when(formatterFactory).documentFormatter();

        assertEquals("formatted content", testCase.createAnalysisSummary(formatterFactory));

        ArgumentCaptor<Document> documentArgumentCaptor = ArgumentCaptor.forClass(Document.class);
        verify(formatter).format(documentArgumentCaptor.capture(), eq(formatterFactory));

        Document expectedDocument = new Document(new Paragraph(new Image("Passed",
                                                                         "http://localhost:9000/static/communityBranchPlugin/checks/QualityGateBadge/passed.svg?sanitize=true")),
                                                 new Text(""), new Heading(1, new Text("Analysis Details")),
                                                 new Heading(2, new Text("0 Issues")), new List(List.Style.BULLET,
                                                                                                new ListItem(
                                                                                                        new Image("Bug",
                                                                                                                  "http://localhost:9000/static/communityBranchPlugin/common/bug.svg?sanitize=true"),
                                                                                                        new Text(" "),
                                                                                                        new Text(
                                                                                                                "0 Bugs")),
                                                                                                new ListItem(new Image(
                                                                                                        "Vulnerability",
                                                                                                        "http://localhost:9000/static/communityBranchPlugin/common/vulnerability.svg?sanitize=true"),
                                                                                                             new Text(
                                                                                                                     " "),
                                                                                                             new Text(
                                                                                                                     "0 Vulnerabilities")),
                                                                                                new ListItem(new Image(
                                                                                                        "Code Smell",
                                                                                                        "http://localhost:9000/static/communityBranchPlugin/common/code_smell.svg?sanitize=true"),
                                                                                                             new Text(
                                                                                                                     " "),
                                                                                                             new Text(
                                                                                                                     "0 Code Smells"))),
                                                 new Heading(2, new Text("Coverage and Duplications")),
                                                 new List(List.Style.BULLET, new ListItem(
                                                         new Image("25 percent coverage",
                                                                   "http://localhost:9000/static/communityBranchPlugin/checks/CoverageChart/25.svg?sanitize=true"),
                                                         new Text(" "),
                                                         new Text("33.00% Coverage (21.78% Estimated after merge)")),
                                                          new ListItem(new Image("20 percent duplication",
                                                                                 "http://localhost:9000/static/communityBranchPlugin/checks/Duplications/20.svg?sanitize=true"),
                                                                       new Text(" "), new Text(
                                                                  "18.00% Duplicated Code (21.78% Estimated after merge)"))),
                                                 new Paragraph(new Text("**Project ID:** Project Key")),
                                                 new Paragraph(new Link("http://localhost:9000/dashboard?id=Project+Key&pullRequest=5", new Text("View in SonarQube"))));

        assertThat(documentArgumentCaptor.getValue()).usingRecursiveComparison().isEqualTo(expectedDocument);

    }

    @Test
    public void testCreateAnalysisSummary3() {
        AnalysisDetails.BranchDetails branchDetails = mock(AnalysisDetails.BranchDetails.class);
        doReturn("5").when(branchDetails).getBranchName();

        TreeRootHolder treeRootHolder = mock(TreeRootHolder.class);
        AnalysisDetails.MeasuresHolder measuresHolder = mock(AnalysisDetails.MeasuresHolder.class);
        doReturn(treeRootHolder).when(measuresHolder).getTreeRootHolder();

        PostAnalysisIssueVisitor.LightIssue issue = mock(PostAnalysisIssueVisitor.LightIssue.class);
        doReturn(Issue.STATUS_OPEN).when(issue).status();
        doReturn(RuleType.BUG).when(issue).type();
        PostAnalysisIssueVisitor postAnalysisIssueVisitor = mock(PostAnalysisIssueVisitor.class);

        PostAnalysisIssueVisitor.ComponentIssue componentIssue = mock(PostAnalysisIssueVisitor.ComponentIssue.class);
        doReturn(issue).when(componentIssue).getIssue();
        doReturn(Collections.singletonList(componentIssue)).when(postAnalysisIssueVisitor).getIssues();

        QualityGate.Condition duplicationsCondition = mock(QualityGate.Condition.class);
        doReturn("10").when(duplicationsCondition).getValue();
        doReturn(CoreMetrics.NEW_DUPLICATED_LINES_DENSITY_KEY).when(duplicationsCondition).getMetricKey();


        QualityGate.Condition coverageCondition = mock(QualityGate.Condition.class);
        doReturn("25").when(coverageCondition).getValue();
        doReturn(CoreMetrics.NEW_COVERAGE_KEY).when(coverageCondition).getMetricKey();

        QualityGate qualityGate = mock(QualityGate.class);
        doReturn(QualityGate.Status.OK).when(qualityGate).getStatus();
        doReturn(Arrays.asList(coverageCondition, duplicationsCondition)).when(qualityGate).getConditions();

        Analysis analysis = mock(Analysis.class);
        Project project = mock(Project.class);
        doReturn("Project Key").when(project).getKey();

        Component rootComponent = mock(Component.class);
        doReturn(rootComponent).when(treeRootHolder).getRoot();

        MeasureRepository measureRepository = mock(MeasureRepository.class);
        doReturn(Optional.of(Measure.newMeasureBuilder().create(21.782, 2, "data"))).when(measureRepository)
                .getRawMeasure(eq(rootComponent), any(Metric.class));
        doReturn(measureRepository).when(measuresHolder).getMeasureRepository();

        MetricRepository metricRepository = mock(MetricRepository.class);
        doReturn(mock(Metric.class)).when(metricRepository).getByKey(anyString());
        doReturn(metricRepository).when(measuresHolder).getMetricRepository();

        ScannerContext scannerContext = mock(ScannerContext.class);

        Configuration configuration = mock(Configuration.class);
        doReturn(Optional.of("http://host.name/path")).when(configuration)
                .get(eq(CommunityBranchPlugin.IMAGE_URL_BASE));

        AnalysisDetails testCase =
                new AnalysisDetails(branchDetails, postAnalysisIssueVisitor, qualityGate, measuresHolder, analysis,
                                    project, configuration, "http://localhost:9000", scannerContext);

        Formatter<Document> formatter = mock(Formatter.class);
        doReturn("formatted content").when(formatter).format(any(), any());
        FormatterFactory formatterFactory = mock(FormatterFactory.class);
        doReturn(formatter).when(formatterFactory).documentFormatter();

        assertEquals("formatted content", testCase.createAnalysisSummary(formatterFactory));

        ArgumentCaptor<Document> documentArgumentCaptor = ArgumentCaptor.forClass(Document.class);
        verify(formatter).format(documentArgumentCaptor.capture(), eq(formatterFactory));

        Document expectedDocument = new Document(new Paragraph(
                new Image("Passed", "http://host.name/path/checks/QualityGateBadge/passed.svg?sanitize=true")),
                                                 new Text(""), new Heading(1, new Text("Analysis Details")),
                                                 new Heading(2, new Text("1 Issue")), new List(List.Style.BULLET,
                                                                                               new ListItem(
                                                                                                       new Image("Bug",
                                                                                                                 "http://host.name/path/common/bug.svg?sanitize=true"),
                                                                                                       new Text(" "),
                                                                                                       new Text(
                                                                                                               "1 Bug")),
                                                                                               new ListItem(new Image(
                                                                                                       "Vulnerability",
                                                                                                       "http://host.name/path/common/vulnerability.svg?sanitize=true"),
                                                                                                            new Text(
                                                                                                                    " "),
                                                                                                            new Text(
                                                                                                                    "0 Vulnerabilities")),
                                                                                               new ListItem(new Image(
                                                                                                       "Code Smell",
                                                                                                       "http://host.name/path/common/code_smell.svg?sanitize=true"),
                                                                                                            new Text(
                                                                                                                    " "),
                                                                                                            new Text(
                                                                                                                    "0 Code Smells"))),
                                                 new Heading(2, new Text("Coverage and Duplications")),
                                                 new List(List.Style.BULLET, new ListItem(
                                                         new Image("25 percent coverage",
                                                                   "http://host.name/path/checks/CoverageChart/25.svg?sanitize=true"),
                                                         new Text(" "),
                                                         new Text("25.00% Coverage (21.78% Estimated after merge)")),
                                                          new ListItem(new Image("10 percent duplication",
                                                                                 "http://host.name/path/checks/Duplications/10.svg?sanitize=true"),
                                                                       new Text(" "), new Text(
                                                                  "10.00% Duplicated Code (21.78% Estimated after merge)"))),
                                                 new Paragraph(new Text("**Project ID:** Project Key")),
                                                 new Paragraph(new Link("http://localhost:9000/dashboard?id=Project+Key&pullRequest=5", new Text("View in SonarQube"))));

        assertThat(documentArgumentCaptor.getValue()).usingRecursiveComparison().isEqualTo(expectedDocument);

    }

    @Test
    public void testCreateAnalysisSummary4() {
        AnalysisDetails.BranchDetails branchDetails = mock(AnalysisDetails.BranchDetails.class);
        doReturn("5").when(branchDetails).getBranchName();

        TreeRootHolder treeRootHolder = mock(TreeRootHolder.class);
        AnalysisDetails.MeasuresHolder measuresHolder = mock(AnalysisDetails.MeasuresHolder.class);
        doReturn(treeRootHolder).when(measuresHolder).getTreeRootHolder();

        PostAnalysisIssueVisitor postAnalysisIssueVisitor = mock(PostAnalysisIssueVisitor.class);
        doReturn(new ArrayList<>()).when(postAnalysisIssueVisitor).getIssues();

        QualityGate.Condition duplicationsCondition = mock(QualityGate.Condition.class);
        doReturn("30").when(duplicationsCondition).getValue();
        doReturn(CoreMetrics.NEW_DUPLICATED_LINES_DENSITY_KEY).when(duplicationsCondition).getMetricKey();


        QualityGate.Condition coverageCondition = mock(QualityGate.Condition.class);
        doReturn("0").when(coverageCondition).getValue();
        doReturn(CoreMetrics.NEW_COVERAGE_KEY).when(coverageCondition).getMetricKey();

        QualityGate qualityGate = mock(QualityGate.class);
        doReturn(QualityGate.Status.OK).when(qualityGate).getStatus();
        doReturn(Arrays.asList(coverageCondition, duplicationsCondition)).when(qualityGate).getConditions();

        Analysis analysis = mock(Analysis.class);
        Project project = mock(Project.class);
        doReturn("Project Key").when(project).getKey();

        Component rootComponent = mock(Component.class);
        doReturn(rootComponent).when(treeRootHolder).getRoot();

        MeasureRepository measureRepository = mock(MeasureRepository.class);
        doReturn(Optional.of(Measure.newMeasureBuilder().create(21.782, 2, "data"))).when(measureRepository)
                .getRawMeasure(eq(rootComponent), any(Metric.class));
        doReturn(measureRepository).when(measuresHolder).getMeasureRepository();

        MetricRepository metricRepository = mock(MetricRepository.class);
        doReturn(mock(Metric.class)).when(metricRepository).getByKey(anyString());
        doReturn(metricRepository).when(measuresHolder).getMetricRepository();

        ScannerContext scannerContext = mock(ScannerContext.class);
        Configuration configuration = mock(Configuration.class);

        AnalysisDetails testCase =
                new AnalysisDetails(branchDetails, postAnalysisIssueVisitor, qualityGate, measuresHolder, analysis,
                                    project, configuration, "http://localhost:9000", scannerContext);

        Formatter<Document> formatter = mock(Formatter.class);
        doReturn("formatted content").when(formatter).format(any(), any());
        FormatterFactory formatterFactory = mock(FormatterFactory.class);
        doReturn(formatter).when(formatterFactory).documentFormatter();

        assertEquals("formatted content", testCase.createAnalysisSummary(formatterFactory));

        ArgumentCaptor<Document> documentArgumentCaptor = ArgumentCaptor.forClass(Document.class);
        verify(formatter).format(documentArgumentCaptor.capture(), eq(formatterFactory));

        Document expectedDocument = new Document(new Paragraph(new Image("Passed",
                                                                         "http://localhost:9000/static/communityBranchPlugin/checks/QualityGateBadge/passed.svg?sanitize=true")),
                                                 new Text(""), new Heading(1, new Text("Analysis Details")),
                                                 new Heading(2, new Text("0 Issues")), new List(List.Style.BULLET,
                                                                                                new ListItem(
                                                                                                        new Image("Bug",
                                                                                                                  "http://localhost:9000/static/communityBranchPlugin/common/bug.svg?sanitize=true"),
                                                                                                        new Text(" "),
                                                                                                        new Text(
                                                                                                                "0 Bugs")),
                                                                                                new ListItem(new Image(
                                                                                                        "Vulnerability",
                                                                                                        "http://localhost:9000/static/communityBranchPlugin/common/vulnerability.svg?sanitize=true"),
                                                                                                             new Text(
                                                                                                                     " "),
                                                                                                             new Text(
                                                                                                                     "0 Vulnerabilities")),
                                                                                                new ListItem(new Image(
                                                                                                        "Code Smell",
                                                                                                        "http://localhost:9000/static/communityBranchPlugin/common/code_smell.svg?sanitize=true"),
                                                                                                             new Text(
                                                                                                                     " "),
                                                                                                             new Text(
                                                                                                                     "0 Code Smells"))),
                                                 new Heading(2, new Text("Coverage and Duplications")),
                                                 new List(List.Style.BULLET, new ListItem(
                                                         new Image("0 percent coverage",
                                                                   "http://localhost:9000/static/communityBranchPlugin/checks/CoverageChart/0.svg?sanitize=true"),
                                                         new Text(" "),
                                                         new Text("0.00% Coverage (21.78% Estimated after merge)")),
                                                          new ListItem(new Image("20plus percent duplication",
                                                                                 "http://localhost:9000/static/communityBranchPlugin/checks/Duplications/20plus.svg?sanitize=true"),
                                                                       new Text(" "), new Text(
                                                                  "30.00% Duplicated Code (21.78% Estimated after merge)"))),
                                                 new Paragraph(new Text("**Project ID:** Project Key")),
                                                 new Paragraph(new Link("http://localhost:9000/dashboard?id=Project+Key&pullRequest=5", new Text("View in SonarQube"))));

        assertThat(documentArgumentCaptor.getValue()).usingRecursiveComparison().isEqualTo(expectedDocument);

    }

    @Test
    public void testCorrectMeasuresRepositoryReturned() {
        MeasureRepository measureRepository = mock(MeasureRepository.class);
        MetricRepository metricRepository = mock(MetricRepository.class);
        TreeRootHolder treeRootHolder = mock(TreeRootHolder.class);

        AnalysisDetails.MeasuresHolder testCase =
                new AnalysisDetails.MeasuresHolder(metricRepository, measureRepository, treeRootHolder);

        assertEquals(measureRepository, testCase.getMeasureRepository());
    }

    @Test
    public void testCorrectMetricsRepositoryReturned() {
        MeasureRepository measureRepository = mock(MeasureRepository.class);
        MetricRepository metricRepository = mock(MetricRepository.class);
        TreeRootHolder treeRootHolder = mock(TreeRootHolder.class);

        AnalysisDetails.MeasuresHolder testCase =
                new AnalysisDetails.MeasuresHolder(metricRepository, measureRepository, treeRootHolder);

        assertEquals(metricRepository, testCase.getMetricRepository());
    }

    @Test
    public void testCorrectTreeRootHolderReturned() {
        MeasureRepository measureRepository = mock(MeasureRepository.class);
        MetricRepository metricRepository = mock(MetricRepository.class);
        TreeRootHolder treeRootHolder = mock(TreeRootHolder.class);

        AnalysisDetails.MeasuresHolder testCase =
                new AnalysisDetails.MeasuresHolder(metricRepository, measureRepository, treeRootHolder);

        assertEquals(treeRootHolder, testCase.getTreeRootHolder());
    }

    @Test
    public void testCorrectPostAnalysisIssueVisitorReturned() {
        PostAnalysisIssueVisitor postAnalysisIssueVisitor = mock(PostAnalysisIssueVisitor.class);
        AnalysisDetails analysisDetails =
                new AnalysisDetails(mock(AnalysisDetails.BranchDetails.class), postAnalysisIssueVisitor,
                                    mock(QualityGate.class), mock(AnalysisDetails.MeasuresHolder.class),
                                    mock(Analysis.class), mock(Project.class), mock(Configuration.class), null,
                                    mock(ScannerContext.class));
        assertSame(postAnalysisIssueVisitor, analysisDetails.getPostAnalysisIssueVisitor());
    }

    @Test
    public void testCorrectBranchDetailsReturned() {
        AnalysisDetails.BranchDetails branchDetails = new AnalysisDetails.BranchDetails("branchName", "commitId");
        assertEquals("branchName", branchDetails.getBranchName());
        assertEquals("commitId", branchDetails.getCommitId());
    }

    @Test
    public void testGetBaseImageUrlFromConfig() {
        Configuration configuration = mock(Configuration.class);
        doReturn(Optional.of("http://host.name/path")).when(configuration)
                .get(eq(CommunityBranchPlugin.IMAGE_URL_BASE));

        AnalysisDetails analysisDetails =
                new AnalysisDetails(mock(AnalysisDetails.BranchDetails.class), mock(PostAnalysisIssueVisitor.class),
                        mock(QualityGate.class), mock(AnalysisDetails.MeasuresHolder.class),
                        mock(Analysis.class), mock(Project.class), configuration, "http://localhost:9000", mock(ScannerContext.class));

        assertEquals("http://host.name/path", analysisDetails.getBaseImageUrl());
    }

    @Test
    public void testGetBaseImageUrlFromConfigWithTrailingSlash() {
        Configuration configuration = mock(Configuration.class);
        doReturn(Optional.of("http://host.name/path/")).when(configuration)
                .get(eq(CommunityBranchPlugin.IMAGE_URL_BASE));

        AnalysisDetails analysisDetails =
                new AnalysisDetails(mock(AnalysisDetails.BranchDetails.class), mock(PostAnalysisIssueVisitor.class),
                        mock(QualityGate.class), mock(AnalysisDetails.MeasuresHolder.class),
                        mock(Analysis.class), mock(Project.class), configuration, "http://localhost:9000", mock(ScannerContext.class));

        assertEquals("http://host.name/path", analysisDetails.getBaseImageUrl());
    }

    @Test
    public void testGetBaseImageUrlFromRootUrl() {
        AnalysisDetails analysisDetails =
                new AnalysisDetails(mock(AnalysisDetails.BranchDetails.class), mock(PostAnalysisIssueVisitor.class),
                        mock(QualityGate.class), mock(AnalysisDetails.MeasuresHolder.class),
                        mock(Analysis.class), mock(Project.class), mock(Configuration.class), "http://localhost:9000", mock(ScannerContext.class));

        assertEquals("http://localhost:9000/static/communityBranchPlugin", analysisDetails.getBaseImageUrl());
    }

    @Test
    public void testGetIssueUrlBug() {
        Project project = mock(Project.class);
        doReturn("projectKey").when(project).getKey();

        AnalysisDetails.BranchDetails branchDetails = mock(AnalysisDetails.BranchDetails.class);
        doReturn("123").when(branchDetails).getBranchName();

        AnalysisDetails analysisDetails =
                new AnalysisDetails(branchDetails, mock(PostAnalysisIssueVisitor.class),
                        mock(QualityGate.class), mock(AnalysisDetails.MeasuresHolder.class),
                        mock(Analysis.class), project, mock(Configuration.class), "http://localhost:9000", mock(ScannerContext.class));

        PostAnalysisIssueVisitor.LightIssue lightIssue = mock(PostAnalysisIssueVisitor.LightIssue.class);
        when(lightIssue.key()).thenReturn("issueKey");
        when(lightIssue.type()).thenReturn(RuleType.BUG);

        assertEquals("http://localhost:9000/project/issues?id=projectKey&pullRequest=123&issues=issueKey&open=issueKey", analysisDetails.getIssueUrl(lightIssue));
    }

    @Test
    public void testGetIssueUrlSecurityHotspot() {
        Project project = mock(Project.class);
        doReturn("projectKey").when(project).getKey();

        AnalysisDetails.BranchDetails branchDetails = mock(AnalysisDetails.BranchDetails.class);
        doReturn("123").when(branchDetails).getBranchName();

        AnalysisDetails analysisDetails =
                new AnalysisDetails(branchDetails, mock(PostAnalysisIssueVisitor.class),
                        mock(QualityGate.class), mock(AnalysisDetails.MeasuresHolder.class),
                        mock(Analysis.class), project, mock(Configuration.class), "http://localhost:9000", mock(ScannerContext.class));

        PostAnalysisIssueVisitor.LightIssue lightIssue = mock(PostAnalysisIssueVisitor.LightIssue.class);
        when(lightIssue.key()).thenReturn("secondIssueKey");
        when(lightIssue.type()).thenReturn(RuleType.SECURITY_HOTSPOT);

        assertEquals("http://localhost:9000/security_hotspots?id=projectKey&pullRequest=123&hotspots=secondIssueKey", analysisDetails.getIssueUrl(lightIssue));
    }

    @Test
    public void testGetRuleUrlWithRuleKey() {
        AnalysisDetails analysisDetails =
                new AnalysisDetails(mock(AnalysisDetails.BranchDetails.class), mock(PostAnalysisIssueVisitor.class),
                        mock(QualityGate.class), mock(AnalysisDetails.MeasuresHolder.class),
                        mock(Analysis.class), mock(Project.class), mock(Configuration.class), "http://localhost:9000", mock(ScannerContext.class));

        assertEquals("http://localhost:9000/coding_rules?open=ruleKey&rule_key=ruleKey", analysisDetails.getRuleUrlWithRuleKey("ruleKey"));
    }

    @Test
    public void testCreateAnalysisIssueSummary() {
        FormatterFactory formatterFactory = mock(FormatterFactory.class);
        PostAnalysisIssueVisitor.ComponentIssue componentIssue = mock(PostAnalysisIssueVisitor.ComponentIssue.class);

        AnalysisDetails.BranchDetails branchDetails = mock(AnalysisDetails.BranchDetails.class);
        when(branchDetails.getBranchName()).thenReturn("branchName");

        PostAnalysisIssueVisitor.LightIssue lightIssue = mock(PostAnalysisIssueVisitor.LightIssue.class);
        when(lightIssue.type()).thenReturn(RuleType.BUG);
        when(lightIssue.getMessage()).thenReturn("message");
        when(lightIssue.severity()).thenReturn("severity");
        when(lightIssue.key()).thenReturn("issueKey");
        when(lightIssue.effortInMinutes()).thenReturn(123L);
        when(componentIssue.getIssue()).thenReturn(lightIssue);

        Project project = mock(Project.class);
        when(project.getKey()).thenReturn("projectKey");

        Formatter<Document> documentFormatter = mock(Formatter.class);
        when(formatterFactory.documentFormatter()).thenReturn(documentFormatter);

        AnalysisDetails analysisDetails =
                new AnalysisDetails(branchDetails, mock(PostAnalysisIssueVisitor.class),
                        mock(QualityGate.class), mock(AnalysisDetails.MeasuresHolder.class),
                        mock(Analysis.class), project, mock(Configuration.class), "http://localhost:9000", mock(ScannerContext.class));

        ArgumentCaptor<Document> documentArgumentCaptor = ArgumentCaptor.forClass(Document.class);
        analysisDetails.createAnalysisIssueSummary(componentIssue, formatterFactory);
        verify(documentFormatter).format(documentArgumentCaptor.capture(), any());

        assertThat(documentArgumentCaptor.getValue()).usingRecursiveComparison().isEqualTo(
                new Document(
                        new Paragraph(
                                new Text("**Type:** BUG "),
                                new Image("BUG", "http://localhost:9000/static/communityBranchPlugin/checks/IssueType/bug.svg?sanitize=true")
                        ),
                        new Paragraph(
                                new Text("**Severity:** severity "),
                                new Image("severity", "http://localhost:9000/static/communityBranchPlugin/checks/Severity/severity.svg?sanitize=true")
                        ),
                        new Paragraph(new Text("**Message:** message")),
                        new Paragraph(new Text("**Duration (min):** 123")),
                        new Text(""),
                        new Paragraph(new Text("**Project ID:** projectKey **Issue ID:** issueKey")),
                        new Paragraph(new Link("http://localhost:9000/project/issues?id=projectKey&pullRequest=branchName&issues=issueKey&open=issueKey", new Text("View in SonarQube")))
                )
        );
    }

    @Test
    public void testFakeIdReturnedForSummaryComment() {
        AnalysisDetails analysisDetails = new AnalysisDetails(mock(AnalysisDetails.BranchDetails.class), mock(PostAnalysisIssueVisitor.class),
                mock(QualityGate.class), mock(AnalysisDetails.MeasuresHolder.class), mock(Analysis.class), mock(Project.class),
                mock(Configuration.class),"", mock(ScannerContext.class));
        assertThat(analysisDetails.parseIssueIdFromUrl("https://sonarqube.dummy/path/dashboard?id=project&pullRequest=123"))
                .get()
                .usingRecursiveComparison()
                .isEqualTo(new AnalysisDetails.ProjectIssueIdentifier("project", "decorator-summary-comment"));
    }

    @Test
    public void testIssueIdReturnedForHotspotUrl() {
        AnalysisDetails analysisDetails = new AnalysisDetails(mock(AnalysisDetails.BranchDetails.class), mock(PostAnalysisIssueVisitor.class),
                mock(QualityGate.class), mock(AnalysisDetails.MeasuresHolder.class), mock(Analysis.class), mock(Project.class),
                mock(Configuration.class),"", mock(ScannerContext.class));
        assertThat(analysisDetails.parseIssueIdFromUrl("http://subdomain.sonarqube.dummy/path/security_hotspots?id=projectIdentifier&hotspots=A1B2-Z9Y8X7"))
                .get()
                .usingRecursiveComparison()
                .isEqualTo(new AnalysisDetails.ProjectIssueIdentifier("projectIdentifier", "A1B2-Z9Y8X7"));
    }

    @Test
    public void testNoIssueIdReturnedForHotspotUrlWithoutId() {
        AnalysisDetails analysisDetails = new AnalysisDetails(mock(AnalysisDetails.BranchDetails.class), mock(PostAnalysisIssueVisitor.class),
                mock(QualityGate.class), mock(AnalysisDetails.MeasuresHolder.class), mock(Analysis.class), mock(Project.class),
                mock(Configuration.class),"", mock(ScannerContext.class));
        assertThat(analysisDetails.parseIssueIdFromUrl("http://subdomain.sonarqube.dummy/path/security_hotspots?id=projectId&other_parameter=ABC"))
                .isEmpty();
    }

    @Test
    public void testIssueIdReturnedForIssueUrl() {
        AnalysisDetails analysisDetails = new AnalysisDetails(mock(AnalysisDetails.BranchDetails.class), mock(PostAnalysisIssueVisitor.class),
                mock(QualityGate.class), mock(AnalysisDetails.MeasuresHolder.class), mock(Analysis.class), mock(Project.class),
                mock(Configuration.class),"", mock(ScannerContext.class));
        assertThat(analysisDetails.parseIssueIdFromUrl("http://subdomain.sonarqube.dummy/path/issue?id=projectId&issues=XXX-YYY-ZZZ"))
                .get()
                .usingRecursiveComparison()
                .isEqualTo(new AnalysisDetails.ProjectIssueIdentifier("projectId", "XXX-YYY-ZZZ"));
    }

    @Test
    public void testNoIssueIdReturnedForIssueUrlWithoutId() {
        AnalysisDetails analysisDetails = new AnalysisDetails(mock(AnalysisDetails.BranchDetails.class), mock(PostAnalysisIssueVisitor.class),
                mock(QualityGate.class), mock(AnalysisDetails.MeasuresHolder.class), mock(Analysis.class), mock(Project.class),
                mock(Configuration.class),"", mock(ScannerContext.class));
        assertThat(analysisDetails.parseIssueIdFromUrl("http://subdomain.sonarqube.dummy/path/issue?id=projectId&other_parameter=123")).isEmpty();
    }
    
    @Test
    public void shouldOnlyReturnNonClosedFileIssuesWithScmInfo() {
        PostAnalysisIssueVisitor.LightIssue lightIssue1 = mock(PostAnalysisIssueVisitor.LightIssue.class);
        when(lightIssue1.status()).thenReturn(Issue.STATUS_OPEN);
        Component component1 = mock(Component.class);
        when(component1.getType()).thenReturn(Component.Type.FILE);
        ReportAttributes reportAttributes1 = mock(ReportAttributes.class);
        when(reportAttributes1.getScmPath()).thenReturn(Optional.of("path"));
        when(component1.getReportAttributes()).thenReturn(reportAttributes1);
        PostAnalysisIssueVisitor.ComponentIssue componentIssue1 = new PostAnalysisIssueVisitor.ComponentIssue(component1, lightIssue1);

        PostAnalysisIssueVisitor.LightIssue lightIssue2 = mock(PostAnalysisIssueVisitor.LightIssue.class);
        when(lightIssue2.status()).thenReturn(Issue.STATUS_OPEN);
        Component component2 = mock(Component.class);
        when(component2.getType()).thenReturn(Component.Type.FILE);
        ReportAttributes reportAttributes2 = mock(ReportAttributes.class);
        when(reportAttributes2.getScmPath()).thenReturn(Optional.empty());
        when(component2.getReportAttributes()).thenReturn(reportAttributes2);
        PostAnalysisIssueVisitor.ComponentIssue componentIssue2 = new PostAnalysisIssueVisitor.ComponentIssue(component2, lightIssue2);

        PostAnalysisIssueVisitor.LightIssue lightIssue3 = mock(PostAnalysisIssueVisitor.LightIssue.class);
        when(lightIssue3.status()).thenReturn(Issue.STATUS_OPEN);
        Component component3 = mock(Component.class);
        when(component3.getType()).thenReturn(Component.Type.PROJECT);
        ReportAttributes reportAttributes3 = mock(ReportAttributes.class);
        when(reportAttributes3.getScmPath()).thenReturn(Optional.of("path"));
        when(component3.getReportAttributes()).thenReturn(reportAttributes3);
        PostAnalysisIssueVisitor.ComponentIssue componentIssue3 = new PostAnalysisIssueVisitor.ComponentIssue(component3, lightIssue3);

        PostAnalysisIssueVisitor.LightIssue lightIssue4 = mock(PostAnalysisIssueVisitor.LightIssue.class);
        when(lightIssue4.status()).thenReturn(Issue.STATUS_CLOSED);
        Component component4 = mock(Component.class);
        when(component4.getType()).thenReturn(Component.Type.FILE);
        ReportAttributes reportAttributes4 = mock(ReportAttributes.class);
        when(reportAttributes4.getScmPath()).thenReturn(Optional.of("path"));
        when(component4.getReportAttributes()).thenReturn(reportAttributes4);
        PostAnalysisIssueVisitor.ComponentIssue componentIssue4 = new PostAnalysisIssueVisitor.ComponentIssue(component4, lightIssue4);

        PostAnalysisIssueVisitor postAnalysisIssueVisitor = mock(PostAnalysisIssueVisitor.class);
        when(postAnalysisIssueVisitor.getIssues()).thenReturn(Arrays.asList(componentIssue1, componentIssue2, componentIssue3, componentIssue4));
        
        AnalysisDetails underTest = new AnalysisDetails(mock(AnalysisDetails.BranchDetails.class), postAnalysisIssueVisitor,
                mock(QualityGate.class), mock(AnalysisDetails.MeasuresHolder.class), mock(Analysis.class), mock(Project.class),
                mock(Configuration.class),"", mock(ScannerContext.class));
        
        assertThat(underTest.getScmReportableIssues()).containsOnly(componentIssue1);
    }
}
