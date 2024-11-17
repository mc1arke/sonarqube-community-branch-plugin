/*
 * Copyright (C) 2020-2024 Michael Clarke
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;
import org.sonar.api.ce.posttask.Analysis;
import org.sonar.api.ce.posttask.PostProjectAnalysisTask;
import org.sonar.api.ce.posttask.Project;
import org.sonar.api.ce.posttask.QualityGate;
import org.sonar.api.ce.posttask.ScannerContext;
import org.sonar.api.issue.IssueStatus;
import org.sonar.ce.task.projectanalysis.component.Component;
import org.sonar.ce.task.projectanalysis.component.ReportAttributes;

class AnalysisDetailsTest {

    @Test
    void shouldReturnStatusFromQualityGate() {
        QualityGate qualityGate = mock();
        doReturn(QualityGate.Status.ERROR).when(qualityGate).getStatus();
        PostProjectAnalysisTask.ProjectAnalysis projectAnalysis = mock();

        AnalysisDetails testCase =
                new AnalysisDetails("pullRequestKey", "commitHash", new ArrayList<>(), qualityGate, projectAnalysis);

        assertEquals(QualityGate.Status.ERROR, testCase.getQualityGateStatus());
    }

    @Test
    void shouldGetDateFromAnalysis() {
        QualityGate qualityGate = mock();
        Analysis analysis = mock();
        PostProjectAnalysisTask.ProjectAnalysis projectAnalysis = mock();
        doReturn(Optional.of(analysis)).when(projectAnalysis).getAnalysis();
        doReturn(new Date()).when(analysis).getDate();

        AnalysisDetails testCase =
                new AnalysisDetails("pullRequestKey", "commitHash", new ArrayList<>(), qualityGate, projectAnalysis);

        assertEquals(analysis.getDate(), testCase.getAnalysisDate());
    }

    @Test
    void shouldGetIdFromAnalysis() {
        QualityGate qualityGate = mock();
        Analysis analysis = mock();
        PostProjectAnalysisTask.ProjectAnalysis projectAnalysis = mock();
        doReturn(Optional.of(analysis)).when(projectAnalysis).getAnalysis();
        doReturn("Analysis ID").when(analysis).getAnalysisUuid();

        AnalysisDetails testCase =
                new AnalysisDetails("pullRequestKey", "commitHash", new ArrayList<>(), qualityGate, projectAnalysis);

        assertEquals("Analysis ID", testCase.getAnalysisId());
    }

    @Test
    void shouldGetProjectKeyFromUnderlyingProject() {
        QualityGate qualityGate = mock();
        PostProjectAnalysisTask.ProjectAnalysis projectAnalysis = mock();
        Project project = mock();
        when(project.getKey()).thenReturn("Project Key");
        when(projectAnalysis.getProject()).thenReturn(project);

        AnalysisDetails testCase =
                new AnalysisDetails("pullRequestKey", "commitHash", new ArrayList<>(), qualityGate, projectAnalysis);

        assertEquals("Project Key", testCase.getAnalysisProjectKey());
    }

    @Test
    void shouldGetProjectNameFromUnderlyingProject() {
        QualityGate qualityGate = mock();
        PostProjectAnalysisTask.ProjectAnalysis projectAnalysis = mock();
        Project project = mock();
        when(project.getName()).thenReturn("Project Name");
        when(projectAnalysis.getProject()).thenReturn(project);

        AnalysisDetails testCase =
                new AnalysisDetails("pullRequestKey", "commitHash", new ArrayList<>(), qualityGate, projectAnalysis);

        assertEquals("Project Name", testCase.getAnalysisProjectName());
    }

    @Test
    void shouldOnlyReturnNonClosedFileIssuesWithScmInfo() {
        PostAnalysisIssueVisitor.LightIssue lightIssue1 = mock();
        when(lightIssue1.issueStatus()).thenReturn(IssueStatus.OPEN);
        Component component1 = mock();
        when(component1.getType()).thenReturn(Component.Type.FILE);
        ReportAttributes reportAttributes1 = mock();
        when(reportAttributes1.getScmPath()).thenReturn(Optional.of("path"));
        when(component1.getReportAttributes()).thenReturn(reportAttributes1);
        PostAnalysisIssueVisitor.ComponentIssue componentIssue1 = new PostAnalysisIssueVisitor.ComponentIssue(component1, lightIssue1);

        PostAnalysisIssueVisitor.LightIssue lightIssue2 = mock();
        when(lightIssue2.issueStatus()).thenReturn(IssueStatus.OPEN);
        Component component2 = mock();
        when(component2.getType()).thenReturn(Component.Type.FILE);
        ReportAttributes reportAttributes2 = mock();
        when(reportAttributes2.getScmPath()).thenReturn(Optional.empty());
        when(component2.getReportAttributes()).thenReturn(reportAttributes2);
        PostAnalysisIssueVisitor.ComponentIssue componentIssue2 = new PostAnalysisIssueVisitor.ComponentIssue(component2, lightIssue2);

        PostAnalysisIssueVisitor.LightIssue lightIssue3 = mock();
        when(lightIssue3.issueStatus()).thenReturn(IssueStatus.OPEN);
        Component component3 = mock();
        when(component3.getType()).thenReturn(Component.Type.PROJECT);
        ReportAttributes reportAttributes3 = mock();
        when(reportAttributes3.getScmPath()).thenReturn(Optional.of("path"));
        when(component3.getReportAttributes()).thenReturn(reportAttributes3);
        PostAnalysisIssueVisitor.ComponentIssue componentIssue3 = new PostAnalysisIssueVisitor.ComponentIssue(component3, lightIssue3);

        PostAnalysisIssueVisitor.LightIssue lightIssue4 = mock();
        when(lightIssue4.issueStatus()).thenReturn(IssueStatus.FIXED);
        Component component4 = mock();
        when(component4.getType()).thenReturn(Component.Type.FILE);
        ReportAttributes reportAttributes4 = mock();
        when(reportAttributes4.getScmPath()).thenReturn(Optional.of("path"));
        when(component4.getReportAttributes()).thenReturn(reportAttributes4);
        PostAnalysisIssueVisitor.ComponentIssue componentIssue4 = new PostAnalysisIssueVisitor.ComponentIssue(component4, lightIssue4);

        PostAnalysisIssueVisitor postAnalysisIssueVisitor = mock();
        when(postAnalysisIssueVisitor.getIssues()).thenReturn(Arrays.asList(componentIssue1, componentIssue2, componentIssue3, componentIssue4));
        
        AnalysisDetails underTest = new AnalysisDetails("pullRequest", "commmitId",
                Arrays.asList(componentIssue1, componentIssue2, componentIssue3, componentIssue4),
                mock(), mock());
        
        assertThat(underTest.getScmReportableIssues()).usingRecursiveFieldByFieldElementComparator().containsOnly(componentIssue1);
    }

    @Test
    void shouldOnlyReturnQualityGateConditionsInErrorState() {
        QualityGate qualityGate = mock();

        QualityGate.Condition condition1 = mock();
        when(condition1.getStatus()).thenReturn(QualityGate.EvaluationStatus.OK);
        QualityGate.Condition condition2 = mock();
        when(condition2.getStatus()).thenReturn(QualityGate.EvaluationStatus.ERROR);
        QualityGate.Condition condition3 = mock();
        when(condition3.getStatus()).thenReturn(QualityGate.EvaluationStatus.NO_VALUE);
        QualityGate.Condition condition4 = mock();
        when(condition4.getStatus()).thenReturn(QualityGate.EvaluationStatus.ERROR);

        when(qualityGate.getConditions()).thenReturn(List.of(condition1, condition2, condition3, condition4));

        AnalysisDetails underTest = new AnalysisDetails("pullRequest", "commit", List.of(), qualityGate, mock());

        assertThat(underTest.findFailedQualityGateConditions()).isEqualTo(List.of(condition2, condition4));
    }

    @Test
    void shouldFilterOnQualityGateConditionName() {
        QualityGate qualityGate = mock();

        List<QualityGate.Condition> conditions = IntStream.range(0, 10).mapToObj(i -> {
            QualityGate.Condition condition = mock();
            when(condition.getMetricKey()).thenReturn("key" + i);
            return condition;
        }).collect(Collectors.toList());

        when(qualityGate.getConditions()).thenReturn(conditions);

        AnalysisDetails underTest = new AnalysisDetails("pullRequest", "commit", List.of(), qualityGate, mock());

        assertThat(underTest.findQualityGateCondition("key2")).contains(conditions.get(2));
    }

    @Test
    void shouldRetrievePropertyFromScannerProperties() {
        Map<String, String> scannerProperties = mock();
        when(scannerProperties.get(anyString())).thenReturn("world");

        ScannerContext scannerContext = mock();
        when(scannerContext.getProperties()).thenReturn(scannerProperties);
        PostProjectAnalysisTask.ProjectAnalysis projectAnalysis = mock();
        when(projectAnalysis.getScannerContext()).thenReturn(scannerContext);

        AnalysisDetails underTest = new AnalysisDetails("PullRequest", "Commit", List.of(), mock(), projectAnalysis);

        assertThat(underTest.getScannerProperty("hello")).contains("world");

        verify(scannerProperties).get("hello");
    }

    @Test
    void shouldReturnPullRequestId() {
        AnalysisDetails underTest = new AnalysisDetails("pull-request-id", "commit-id", List.of(), mock(), mock());

        assertThat(underTest.getPullRequestId()).isEqualTo("pull-request-id");
    }


    @Test
    void shouldReturnCommitSha() {
        AnalysisDetails underTest = new AnalysisDetails("pull-request-id", "commit-id", List.of(), mock(), mock());

        assertThat(underTest.getCommitSha()).isEqualTo("commit-id");
    }
}
