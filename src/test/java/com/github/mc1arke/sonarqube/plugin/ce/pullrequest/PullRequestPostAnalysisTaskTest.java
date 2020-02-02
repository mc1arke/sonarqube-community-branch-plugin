/*
 * Copyright (C) 2020 Michael Clarke
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

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.sonar.api.ce.posttask.Analysis;
import org.sonar.api.ce.posttask.Branch;
import org.sonar.api.ce.posttask.PostProjectAnalysisTask;
import org.sonar.api.ce.posttask.Project;
import org.sonar.api.ce.posttask.QualityGate;
import org.sonar.api.ce.posttask.ScannerContext;
import org.sonar.api.platform.Server;
import org.sonar.ce.task.projectanalysis.component.TreeRootHolder;
import org.sonar.ce.task.projectanalysis.measure.MeasureRepository;
import org.sonar.ce.task.projectanalysis.metric.MetricRepository;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.alm.setting.ALM;
import org.sonar.db.alm.setting.AlmSettingDao;
import org.sonar.db.alm.setting.AlmSettingDto;
import org.sonar.db.alm.setting.ProjectAlmSettingDao;
import org.sonar.db.alm.setting.ProjectAlmSettingDto;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class PullRequestPostAnalysisTaskTest {

    private PostProjectAnalysisTask.ProjectAnalysis projectAnalysis = mock(PostProjectAnalysisTask.ProjectAnalysis.class);
    private Branch branch = mock(Branch.class);
    private ScannerContext scannerContext = mock(ScannerContext.class);

    private Server server = mock(Server.class);
    private List<PullRequestBuildStatusDecorator> pullRequestBuildStatusDecorators = new ArrayList<>();
    private PostAnalysisIssueVisitor postAnalysisIssueVisitor = mock(PostAnalysisIssueVisitor.class);
    private MetricRepository metricRepository = mock(MetricRepository.class);
    private MeasureRepository measureRepository = mock(MeasureRepository.class);
    private TreeRootHolder treeRootHolder = mock(TreeRootHolder.class);
    private PostProjectAnalysisTask.Context context = mock(PostProjectAnalysisTask.Context.class);
    private DbClient dbClient = mock(DbClient.class);
    private Project project = mock(Project.class);

    private PullRequestPostAnalysisTask testCase =
            new PullRequestPostAnalysisTask(server, pullRequestBuildStatusDecorators,
                    postAnalysisIssueVisitor, metricRepository, measureRepository,
                    treeRootHolder, dbClient);

    @Before
    public void init() {
        doReturn(Optional.of(branch)).when(projectAnalysis).getBranch();
        doReturn(scannerContext).when(projectAnalysis).getScannerContext();
        doReturn(new HashMap<>()).when(scannerContext).getProperties();
        doReturn(projectAnalysis).when(context).getProjectAnalysis();
        doReturn(project).when(projectAnalysis).getProject();
        doReturn("uuid").when(project).getUuid();


    }

    @Test
    public void testFinishedNonPullRequest() {
        doReturn(Branch.Type.BRANCH).when(branch).getType();

        testCase.finished(context);

        verify(branch).getType();
        verify(branch, never()).getName();
    }

    @Test
    public void testFinishedNoBranchName() {
        doReturn(Branch.Type.PULL_REQUEST).when(branch).getType();
        doReturn(Optional.empty()).when(branch).getName();

        testCase.finished(context);

        verify(branch).getName();
    }

    @Test
    public void testFinishedNoProviderSet() {
        doReturn(Branch.Type.PULL_REQUEST).when(branch).getType();
        doReturn(Optional.of("branchName")).when(branch).getName();

        ProjectAlmSettingDto projectAlmSettingDto = mock(ProjectAlmSettingDto.class);
        when(projectAlmSettingDto.getAlmSlug()).thenReturn("dummy/repo");
        when(projectAlmSettingDto.getAlmSettingUuid()).thenReturn("almUuid");
        AlmSettingDto almSettingDto = mock(AlmSettingDto.class);
        when(almSettingDto.getUrl()).thenReturn("http://host.name");
        when(almSettingDto.getAppId()).thenReturn("app id");
        when(almSettingDto.getPrivateKey()).thenReturn("private key");
        when(almSettingDto.getAlm()).thenReturn(ALM.GITHUB);
        when(dbClient.openSession(anyBoolean())).thenReturn(mock(DbSession.class));
        AlmSettingDao almSettingDao = mock(AlmSettingDao.class);
        when(almSettingDao.selectByUuid(any(), any())).thenReturn(Optional.of(almSettingDto));
        when(dbClient.almSettingDao()).thenReturn(almSettingDao);
        ProjectAlmSettingDao projectAlmSettingDao = mock(ProjectAlmSettingDao.class);
        when(projectAlmSettingDao.selectByProject(any(), anyString())).thenReturn(Optional.of(projectAlmSettingDto));
        when(dbClient.projectAlmSettingDao()).thenReturn(projectAlmSettingDao);

        doReturn(scannerContext).when(projectAnalysis).getScannerContext();

        testCase.finished(context);

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
        doReturn(ALM.GITHUB).when(decorator2).alm();
        pullRequestBuildStatusDecorators.add(decorator2);

        ProjectAlmSettingDto projectAlmSettingDto = mock(ProjectAlmSettingDto.class);
        when(projectAlmSettingDto.getAlmSlug()).thenReturn("dummy/repo");
        when(projectAlmSettingDto.getAlmSettingUuid()).thenReturn("almUuid");
        AlmSettingDto almSettingDto = mock(AlmSettingDto.class);
        when(almSettingDto.getUrl()).thenReturn("http://host.name");
        when(almSettingDto.getAppId()).thenReturn("app id");
        when(almSettingDto.getPrivateKey()).thenReturn("private key");
        when(almSettingDto.getAlm()).thenReturn(ALM.AZURE_DEVOPS);
        when(dbClient.openSession(anyBoolean())).thenReturn(mock(DbSession.class));
        AlmSettingDao almSettingDao = mock(AlmSettingDao.class);
        when(almSettingDao.selectByUuid(any(), any())).thenReturn(Optional.of(almSettingDto));
        when(dbClient.almSettingDao()).thenReturn(almSettingDao);
        ProjectAlmSettingDao projectAlmSettingDao = mock(ProjectAlmSettingDao.class);
        when(projectAlmSettingDao.selectByProject(any(), anyString())).thenReturn(Optional.of(projectAlmSettingDto));
        when(dbClient.projectAlmSettingDao()).thenReturn(projectAlmSettingDao);

        PullRequestPostAnalysisTask testCase =
                new PullRequestPostAnalysisTask(server,  pullRequestBuildStatusDecorators,
                                                postAnalysisIssueVisitor, metricRepository, measureRepository,
                                                treeRootHolder, dbClient);
        testCase.finished(context);

        verify(decorator1).alm();
        verify(decorator2).alm();
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
        doReturn(ALM.GITHUB).when(decorator2).alm();
        pullRequestBuildStatusDecorators.add(decorator2);

        ProjectAlmSettingDto projectAlmSettingDto = mock(ProjectAlmSettingDto.class);
        when(projectAlmSettingDto.getAlmSlug()).thenReturn("dummy/repo");
        when(projectAlmSettingDto.getAlmSettingUuid()).thenReturn("almUuid");
        AlmSettingDto almSettingDto = mock(AlmSettingDto.class);
        when(almSettingDto.getUrl()).thenReturn("http://host.name");
        when(almSettingDto.getAppId()).thenReturn("app id");
        when(almSettingDto.getPrivateKey()).thenReturn("private key");
        when(almSettingDto.getAlm()).thenReturn(ALM.GITHUB);
        when(dbClient.openSession(anyBoolean())).thenReturn(mock(DbSession.class));
        AlmSettingDao almSettingDao = mock(AlmSettingDao.class);
        when(almSettingDao.selectByUuid(any(), any())).thenReturn(Optional.of(almSettingDto));
        when(dbClient.almSettingDao()).thenReturn(almSettingDao);
        ProjectAlmSettingDao projectAlmSettingDao = mock(ProjectAlmSettingDao.class);
        when(projectAlmSettingDao.selectByProject(any(), anyString())).thenReturn(Optional.of(projectAlmSettingDto));
        when(dbClient.projectAlmSettingDao()).thenReturn(projectAlmSettingDao);

        PullRequestPostAnalysisTask testCase =
                new PullRequestPostAnalysisTask(server, pullRequestBuildStatusDecorators,
                                                postAnalysisIssueVisitor, metricRepository, measureRepository,
                                                treeRootHolder, dbClient);
        testCase.finished(context);

        verify(projectAnalysis).getAnalysis();
        verify(decorator2, never()).decorateQualityGateStatus(any(), any(), any());
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
        doReturn(ALM.GITHUB).when(decorator2).alm();
        pullRequestBuildStatusDecorators.add(decorator2);

        ProjectAlmSettingDto projectAlmSettingDto = mock(ProjectAlmSettingDto.class);
        when(projectAlmSettingDto.getAlmSlug()).thenReturn("dummy/repo");
        when(projectAlmSettingDto.getAlmSettingUuid()).thenReturn("almUuid");
        AlmSettingDto almSettingDto = mock(AlmSettingDto.class);
        when(almSettingDto.getUrl()).thenReturn("http://host.name");
        when(almSettingDto.getAppId()).thenReturn("app id");
        when(almSettingDto.getPrivateKey()).thenReturn("private key");
        when(almSettingDto.getAlm()).thenReturn(ALM.GITHUB);
        when(dbClient.openSession(anyBoolean())).thenReturn(mock(DbSession.class));
        AlmSettingDao almSettingDao = mock(AlmSettingDao.class);
        when(almSettingDao.selectByUuid(any(), any())).thenReturn(Optional.of(almSettingDto));
        when(dbClient.almSettingDao()).thenReturn(almSettingDao);
        ProjectAlmSettingDao projectAlmSettingDao = mock(ProjectAlmSettingDao.class);
        when(projectAlmSettingDao.selectByProject(any(), anyString())).thenReturn(Optional.of(projectAlmSettingDto));
        when(dbClient.projectAlmSettingDao()).thenReturn(projectAlmSettingDao);

        testCase.finished(context);

        verify(projectAnalysis).getAnalysis();
        verify(projectAnalysis, never()).getQualityGate();
        verify(decorator2, never()).decorateQualityGateStatus(any(), any(), any());
    }

    @Test
    public void testFinishedAnalysisWithNoQualityGate() {
        doReturn(Branch.Type.PULL_REQUEST).when(branch).getType();
        doReturn(Optional.of("pull-request")).when(branch).getName();

        Analysis analysis = mock(Analysis.class);
        doReturn(Optional.of("revision")).when(analysis).getRevision();
        doReturn(Optional.of(analysis)).when(projectAnalysis).getAnalysis();

        ProjectAlmSettingDto projectAlmSettingDto = mock(ProjectAlmSettingDto.class);
        when(projectAlmSettingDto.getAlmSlug()).thenReturn("dummy/repo");
        when(projectAlmSettingDto.getAlmSettingUuid()).thenReturn("almUuid");
        AlmSettingDto almSettingDto = mock(AlmSettingDto.class);
        when(almSettingDto.getUrl()).thenReturn("http://host.name");
        when(almSettingDto.getAppId()).thenReturn("app id");
        when(almSettingDto.getPrivateKey()).thenReturn("private key");
        when(almSettingDto.getAlm()).thenReturn(ALM.GITHUB);
        when(dbClient.openSession(anyBoolean())).thenReturn(mock(DbSession.class));
        AlmSettingDao almSettingDao = mock(AlmSettingDao.class);
        when(almSettingDao.selectByUuid(any(), any())).thenReturn(Optional.of(almSettingDto));
        when(dbClient.almSettingDao()).thenReturn(almSettingDao);
        ProjectAlmSettingDao projectAlmSettingDao = mock(ProjectAlmSettingDao.class);
        when(projectAlmSettingDao.selectByProject(any(), anyString())).thenReturn(Optional.of(projectAlmSettingDto));
        when(dbClient.projectAlmSettingDao()).thenReturn(projectAlmSettingDao);

        ScannerContext scannerContext = mock(ScannerContext.class);
        doReturn(scannerContext).when(projectAnalysis).getScannerContext();

        PullRequestBuildStatusDecorator decorator1 = mock(PullRequestBuildStatusDecorator.class);
        doReturn("decorator-name-1").when(decorator1).name();
        pullRequestBuildStatusDecorators.add(decorator1);

        PullRequestBuildStatusDecorator decorator2 = mock(PullRequestBuildStatusDecorator.class);
        doReturn("decorator-name-2").when(decorator2).name();
        doReturn(ALM.GITHUB).when(decorator2).alm();
        pullRequestBuildStatusDecorators.add(decorator2);

        testCase.finished(context);

        verify(projectAnalysis).getAnalysis();
        verify(projectAnalysis).getQualityGate();
        verify(decorator2, never()).decorateQualityGateStatus(any(), any(), any());
    }

    @Test
    public void testFinishedAnalysisDecorationRequest() {
        doReturn(Branch.Type.PULL_REQUEST).when(branch).getType();
        doReturn(Optional.of("pull-request")).when(branch).getName();

        PostProjectAnalysisTask.Context context = mock(PostProjectAnalysisTask.Context.class);
        doReturn(projectAnalysis).when(context).getProjectAnalysis();

        Analysis analysis = mock(Analysis.class);
        doReturn(Optional.of("revision")).when(analysis).getRevision();
        doReturn(new Date()).when(analysis).getDate();
        doReturn(Optional.of(analysis)).when(projectAnalysis).getAnalysis();

        QualityGate qualityGate = mock(QualityGate.class);
        doReturn(qualityGate).when(projectAnalysis).getQualityGate();

        PullRequestBuildStatusDecorator decorator1 = mock(PullRequestBuildStatusDecorator.class);
        doReturn("decorator-name-1").when(decorator1).name();
        doReturn(ALM.BITBUCKET).when(decorator1).alm();
        pullRequestBuildStatusDecorators.add(decorator1);

        PullRequestBuildStatusDecorator decorator2 = mock(PullRequestBuildStatusDecorator.class);
        doReturn("decorator-name-2").when(decorator2).name();
        doReturn(ALM.GITHUB).when(decorator2).alm();
        pullRequestBuildStatusDecorators.add(decorator2);

        ProjectAlmSettingDto projectAlmSettingDto = mock(ProjectAlmSettingDto.class);
        when(projectAlmSettingDto.getAlmSlug()).thenReturn("dummy/repo");
        when(projectAlmSettingDto.getAlmSettingUuid()).thenReturn("almUuid");
        AlmSettingDto almSettingDto = mock(AlmSettingDto.class);
        when(almSettingDto.getUrl()).thenReturn("http://host.name");
        when(almSettingDto.getAppId()).thenReturn("app id");
        when(almSettingDto.getPrivateKey()).thenReturn("private key");
        when(almSettingDto.getAlm()).thenReturn(ALM.GITHUB);
        when(dbClient.openSession(anyBoolean())).thenReturn(mock(DbSession.class));
        AlmSettingDao almSettingDao = mock(AlmSettingDao.class);
        when(almSettingDao.selectByUuid(any(), any())).thenReturn(Optional.of(almSettingDto));
        when(dbClient.almSettingDao()).thenReturn(almSettingDao);
        ProjectAlmSettingDao projectAlmSettingDao = mock(ProjectAlmSettingDao.class);
        when(projectAlmSettingDao.selectByProject(any(), anyString())).thenReturn(Optional.of(projectAlmSettingDto));
        when(dbClient.projectAlmSettingDao()).thenReturn(projectAlmSettingDao);

        testCase.finished(context);

        ArgumentCaptor<AnalysisDetails> analysisDetailsArgumentCaptor = ArgumentCaptor.forClass(AnalysisDetails.class);
        ArgumentCaptor<AlmSettingDto> almSettingDtoArgumentCaptor = ArgumentCaptor.forClass(AlmSettingDto.class);
        ArgumentCaptor<ProjectAlmSettingDto> projectAlmSettingDtoArgumentCaptor =
                ArgumentCaptor.forClass(ProjectAlmSettingDto.class);

        verify(projectAnalysis).getAnalysis();
        verify(projectAnalysis).getQualityGate();
        verify(decorator2).decorateQualityGateStatus(analysisDetailsArgumentCaptor.capture(),
                                                     almSettingDtoArgumentCaptor.capture(),
                                                     projectAlmSettingDtoArgumentCaptor.capture());
        assertThat(almSettingDtoArgumentCaptor.getValue()).isSameAs(almSettingDto);
        assertThat(projectAlmSettingDtoArgumentCaptor.getValue()).isSameAs(projectAlmSettingDto);

        AnalysisDetails analysisDetails =
                new AnalysisDetails(new AnalysisDetails.BranchDetails("pull-request", "revision"),
                                    postAnalysisIssueVisitor, qualityGate,
                                    new AnalysisDetails.MeasuresHolder(metricRepository, measureRepository,
                                                                       treeRootHolder), analysis, project,
                                    null, scannerContext);
        assertThat(analysisDetailsArgumentCaptor.getValue()).usingRecursiveComparison().isEqualTo(analysisDetails);
    }

    @Test
    public void testCorrectDescriptionReturnedForTask() {
        assertThat(testCase.getDescription()).isEqualTo("Pull Request Decoration");
    }
}
