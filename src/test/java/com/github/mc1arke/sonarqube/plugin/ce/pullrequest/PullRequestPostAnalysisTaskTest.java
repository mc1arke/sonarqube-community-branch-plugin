/*
 * Copyright (C) 2019 Michael Clarke
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
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.sonar.api.ce.posttask.Analysis;
import org.sonar.api.ce.posttask.Branch;
import org.sonar.api.ce.posttask.PostProjectAnalysisTask;
import org.sonar.api.ce.posttask.Project;
import org.sonar.api.ce.posttask.QualityGate;
import org.sonar.api.ce.posttask.ScannerContext;
import org.sonar.api.config.Configuration;
import org.sonar.api.platform.Server;
import org.sonar.ce.task.projectanalysis.component.ConfigurationRepository;
import org.sonar.ce.task.projectanalysis.component.TreeRootHolder;
import org.sonar.ce.task.projectanalysis.measure.MeasureRepository;
import org.sonar.ce.task.projectanalysis.metric.MetricRepository;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

public class PullRequestPostAnalysisTaskTest {

    private PostProjectAnalysisTask.ProjectAnalysis projectAnalysis = mock(PostProjectAnalysisTask.ProjectAnalysis.class);
    private Branch branch = mock(Branch.class);
    private ScannerContext scannerContext = mock(ScannerContext.class);

    private Server server = mock(Server.class);
    private ConfigurationRepository configurationRepository = mock(ConfigurationRepository.class);
    private Configuration configuration = mock(Configuration.class);
    private List<PullRequestBuildStatusDecorator> pullRequestBuildStatusDecorators = new ArrayList<>();
    private PostAnalysisIssueVisitor postAnalysisIssueVisitor = mock(PostAnalysisIssueVisitor.class);
    private MetricRepository metricRepository = mock(MetricRepository.class);
    private MeasureRepository measureRepository = mock(MeasureRepository.class);
    private TreeRootHolder treeRootHolder = mock(TreeRootHolder.class);

    private PullRequestPostAnalysisTask testCase =
            new PullRequestPostAnalysisTask(server, configurationRepository, pullRequestBuildStatusDecorators,
                    postAnalysisIssueVisitor, metricRepository, measureRepository,
                    treeRootHolder);

    @Before
    public void init() {
        doReturn(Optional.of(branch)).when(projectAnalysis).getBranch();
        doReturn(scannerContext).when(projectAnalysis).getScannerContext();
        doReturn(new HashMap<>()).when(scannerContext).getProperties();
        doReturn(configuration).when(configurationRepository).getConfiguration();
    }

    @Test
    public void testFinishedNonPullRequest() {
        doReturn(Branch.Type.LONG).when(branch).getType();

        testCase.finished(projectAnalysis);

        verify(branch).getType();
        verify(branch, never()).getName();
    }

    @Test
    public void testFinishedNoBranchName() {
        doReturn(Branch.Type.PULL_REQUEST).when(branch).getType();
        doReturn(Optional.empty()).when(branch).getName();

        testCase.finished(projectAnalysis);

        verify(branch).getName();
        verify(configurationRepository, never()).getConfiguration();
    }

    @Test
    public void testFinishedNoProviderSet() {
        doReturn(Branch.Type.PULL_REQUEST).when(branch).getType();
        doReturn(Optional.of("branchName")).when(branch).getName();

        doReturn(Optional.empty()).when(configuration).get(eq(CommunityBranchPlugin.PULL_REQUEST_PROVIDER));

        testCase.finished(projectAnalysis);

        verify(configurationRepository).getConfiguration();
        verify(configuration).get(CommunityBranchPlugin.PULL_REQUEST_PROVIDER);
        verify(projectAnalysis, never()).getAnalysis();
    }

    @Test
    public void testFinishedNoProviderMatchingName() {
        doReturn(Branch.Type.PULL_REQUEST).when(branch).getType();
        doReturn(Optional.of("branchName")).when(branch).getName();

        PullRequestBuildStatusDecorator decorator1 = mock(PullRequestBuildStatusDecorator.class);
        doReturn("decorator-name-1").when(decorator1).name();
        pullRequestBuildStatusDecorators.add(decorator1);

        PullRequestBuildStatusDecorator decorator2 = mock(PullRequestBuildStatusDecorator.class);
        doReturn("decorator-name-2").when(decorator2).name();
        pullRequestBuildStatusDecorators.add(decorator2);

        doReturn(Optional.of("missing-provider")).when(configuration).get(eq(CommunityBranchPlugin.PULL_REQUEST_PROVIDER));

        testCase.finished(projectAnalysis);

        verify(configurationRepository).getConfiguration();
        verify(configuration).get(CommunityBranchPlugin.PULL_REQUEST_PROVIDER);
        verify(decorator1).name();
        verify(decorator2).name();
        verify(projectAnalysis, never()).getAnalysis();
    }

    @Test
    public void testFinishedNoAnalysis() {
        doReturn(Branch.Type.PULL_REQUEST).when(branch).getType();
        doReturn(Optional.of("pull-request")).when(branch).getName();

        doReturn(Optional.empty()).when(projectAnalysis).getAnalysis();

        PullRequestBuildStatusDecorator decorator1 = mock(PullRequestBuildStatusDecorator.class);
        doReturn("decorator-name-1").when(decorator1).name();
        pullRequestBuildStatusDecorators.add(decorator1);

        PullRequestBuildStatusDecorator decorator2 = mock(PullRequestBuildStatusDecorator.class);
        doReturn("decorator-name-2").when(decorator2).name();
        pullRequestBuildStatusDecorators.add(decorator2);

        doReturn(Optional.of("decorator-name-2")).when(configuration).get(eq(CommunityBranchPlugin.PULL_REQUEST_PROVIDER));

        testCase.finished(projectAnalysis);

        verify(configurationRepository).getConfiguration();
        verify(projectAnalysis).getAnalysis();
        verify(decorator2, never()).decorateQualityGateStatus(any(), any());
    }


    @Test
    public void testFinishedAnalysisWithNoRevision() {
        doReturn(Branch.Type.PULL_REQUEST).when(branch).getType();
        doReturn(Optional.of("pull-request")).when(branch).getName();

        Analysis analysis = mock(Analysis.class);
        doReturn(Optional.empty()).when(analysis).getRevision();
        doReturn(Optional.of(analysis)).when(projectAnalysis).getAnalysis();

        PullRequestBuildStatusDecorator decorator1 = mock(PullRequestBuildStatusDecorator.class);
        doReturn("decorator-name-1").when(decorator1).name();
        pullRequestBuildStatusDecorators.add(decorator1);

        PullRequestBuildStatusDecorator decorator2 = mock(PullRequestBuildStatusDecorator.class);
        doReturn("decorator-name-2").when(decorator2).name();
        pullRequestBuildStatusDecorators.add(decorator2);

        doReturn(Optional.of("decorator-name-2")).when(configuration).get(eq(CommunityBranchPlugin.PULL_REQUEST_PROVIDER));

        testCase.finished(projectAnalysis);

        verify(configurationRepository).getConfiguration();
        verify(projectAnalysis).getAnalysis();
        verify(projectAnalysis, never()).getQualityGate();
        verify(decorator2, never()).decorateQualityGateStatus(any(), any());
    }

    @Test
    public void testFinishedAnalysisWithNoQualityGate() {
        doReturn(Branch.Type.PULL_REQUEST).when(branch).getType();
        doReturn(Optional.of("pull-request")).when(branch).getName();

        Analysis analysis = mock(Analysis.class);
        doReturn(Optional.of("revision")).when(analysis).getRevision();
        doReturn(Optional.of(analysis)).when(projectAnalysis).getAnalysis();

        PullRequestBuildStatusDecorator decorator1 = mock(PullRequestBuildStatusDecorator.class);
        doReturn("decorator-name-1").when(decorator1).name();
        pullRequestBuildStatusDecorators.add(decorator1);

        PullRequestBuildStatusDecorator decorator2 = mock(PullRequestBuildStatusDecorator.class);
        doReturn("decorator-name-2").when(decorator2).name();
        pullRequestBuildStatusDecorators.add(decorator2);

        doReturn(Optional.of("decorator-name-2")).when(configuration).get(eq(CommunityBranchPlugin.PULL_REQUEST_PROVIDER));

        testCase.finished(projectAnalysis);

        verify(configurationRepository).getConfiguration();
        verify(projectAnalysis).getAnalysis();
        verify(projectAnalysis).getQualityGate();
        verify(decorator2, never()).decorateQualityGateStatus(any(), any());
    }

    @Test
    public void testFinishedAnalysisDecorationRequest() {
        doReturn(Branch.Type.PULL_REQUEST).when(branch).getType();
        doReturn(Optional.of("pull-request")).when(branch).getName();

        Project project = mock(Project.class);
        doReturn(project).when(projectAnalysis).getProject();

        Analysis analysis = mock(Analysis.class);
        doReturn(Optional.of("revision")).when(analysis).getRevision();
        doReturn(new Date()).when(analysis).getDate();
        doReturn(Optional.of(analysis)).when(projectAnalysis).getAnalysis();

        QualityGate qualityGate = mock(QualityGate.class);
        doReturn(qualityGate).when(projectAnalysis).getQualityGate();

        PullRequestBuildStatusDecorator decorator1 = mock(PullRequestBuildStatusDecorator.class);
        doReturn("decorator-name-1").when(decorator1).name();
        pullRequestBuildStatusDecorators.add(decorator1);

        PullRequestBuildStatusDecorator decorator2 = mock(PullRequestBuildStatusDecorator.class);
        doReturn("decorator-name-2").when(decorator2).name();
        pullRequestBuildStatusDecorators.add(decorator2);

        doReturn(Optional.of("decorator-name-2")).when(configuration).get(eq(CommunityBranchPlugin.PULL_REQUEST_PROVIDER));

        testCase.finished(projectAnalysis);

        ArgumentCaptor<AnalysisDetails> analysisDetailsArgumentCaptor = ArgumentCaptor.forClass(AnalysisDetails.class);
        ArgumentCaptor<UnifyConfiguration> unifyConfigurationArgumentCaptor = ArgumentCaptor.forClass(UnifyConfiguration.class);

        verify(configurationRepository).getConfiguration();
        verify(projectAnalysis).getAnalysis();
        verify(projectAnalysis).getQualityGate();
        verify(decorator2).decorateQualityGateStatus(analysisDetailsArgumentCaptor.capture(), unifyConfigurationArgumentCaptor.capture());

        AnalysisDetails analysisDetails =
                new AnalysisDetails(new AnalysisDetails.BranchDetails("pull-request", "revision"),
                                    postAnalysisIssueVisitor, qualityGate,
                                    new AnalysisDetails.MeasuresHolder(metricRepository, measureRepository,
                                                                       treeRootHolder), analysis, project,
                                    null);
        assertThat(analysisDetailsArgumentCaptor.getValue()).usingRecursiveComparison().isEqualTo(analysisDetails);
        assertThat(unifyConfigurationArgumentCaptor.getValue()).usingRecursiveComparison().isEqualTo(new UnifyConfiguration(configuration, scannerContext));
    }

    @Test
    public void testCorrectDescriptionReturnedForTask() {
        assertThat(testCase.getDescription()).isEqualTo("Pull Request Decoration");
    }
}
