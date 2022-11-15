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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.sonar.api.ce.posttask.Analysis;
import org.sonar.api.ce.posttask.Branch;
import org.sonar.api.ce.posttask.PostProjectAnalysisTask;
import org.sonar.api.ce.posttask.Project;
import org.sonar.api.ce.posttask.QualityGate;
import org.sonar.api.ce.posttask.ScannerContext;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.alm.setting.ALM;
import org.sonar.db.alm.setting.AlmSettingDao;
import org.sonar.db.alm.setting.AlmSettingDto;
import org.sonar.db.alm.setting.ProjectAlmSettingDao;
import org.sonar.db.alm.setting.ProjectAlmSettingDto;
import org.sonar.db.component.BranchDao;
import org.sonar.db.component.BranchDto;
import org.sonar.db.protobuf.DbProjectBranches;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PullRequestPostAnalysisTaskTest {

    private final PostProjectAnalysisTask.ProjectAnalysis projectAnalysis = mock(PostProjectAnalysisTask.ProjectAnalysis.class);
    private final Branch branch = mock(Branch.class);
    private final ScannerContext scannerContext = mock(ScannerContext.class);

    private final List<PullRequestBuildStatusDecorator> pullRequestBuildStatusDecorators = new ArrayList<>();
    private final PostAnalysisIssueVisitor postAnalysisIssueVisitor = mock(PostAnalysisIssueVisitor.class);
    private final PostProjectAnalysisTask.Context context = mock(PostProjectAnalysisTask.Context.class);
    private final DbClient dbClient = mock(DbClient.class);
    private final Project project = mock(Project.class);
    private final List<PostAnalysisIssueVisitor.ComponentIssue> componentIssues = List.of(mock(PostAnalysisIssueVisitor.ComponentIssue.class));

    private final PullRequestPostAnalysisTask testCase =
            new PullRequestPostAnalysisTask(pullRequestBuildStatusDecorators,
                    postAnalysisIssueVisitor, dbClient);

    @BeforeEach
    void init() {
        doReturn(Optional.of(branch)).when(projectAnalysis).getBranch();
        doReturn(scannerContext).when(projectAnalysis).getScannerContext();
        doReturn(new HashMap<>()).when(scannerContext).getProperties();
        doReturn(projectAnalysis).when(context).getProjectAnalysis();
        doReturn(project).when(projectAnalysis).getProject();
        doReturn("uuid").when(project).getUuid();
        doReturn(componentIssues).when(postAnalysisIssueVisitor).getIssues();
    }

    @Test
    void testFinishedNonPullRequest() {
        doReturn(Branch.Type.BRANCH).when(branch).getType();

        testCase.finished(context);

        verify(branch).getType();
        verify(branch, never()).getName();
    }

    @Test
    void testFinishedNoBranchName() {
        doReturn(Branch.Type.PULL_REQUEST).when(branch).getType();
        doReturn(Optional.empty()).when(branch).getName();

        testCase.finished(context);

        verify(branch).getName();
    }

    @Test
    void testFinishedNoProviderSet() {
        doReturn(Branch.Type.PULL_REQUEST).when(branch).getType();
        doReturn(Optional.of("branchName")).when(branch).getName();

        ProjectAlmSettingDto projectAlmSettingDto = mock(ProjectAlmSettingDto.class);
        when(projectAlmSettingDto.getAlmSlug()).thenReturn("dummy/repo");
        when(projectAlmSettingDto.getAlmSettingUuid()).thenReturn("almUuid");
        AlmSettingDto almSettingDto = mock(AlmSettingDto.class);
        when(almSettingDto.getUrl()).thenReturn("http://host.name");
        when(almSettingDto.getAppId()).thenReturn("app id");
        when(almSettingDto.getDecryptedPrivateKey(any())).thenReturn("private key");
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
    void testFinishedNoProviderMatchingName() {
        doReturn(Branch.Type.PULL_REQUEST).when(branch).getType();
        doReturn(Optional.of("branchName")).when(branch).getName();

        PullRequestBuildStatusDecorator decorator1 = mock(PullRequestBuildStatusDecorator.class);
        doReturn(Collections.singletonList(ALM.BITBUCKET)).when(decorator1).alm();
        pullRequestBuildStatusDecorators.add(decorator1);

        PullRequestBuildStatusDecorator decorator2 = mock(PullRequestBuildStatusDecorator.class);
        doReturn(Collections.singletonList(ALM.GITHUB)).when(decorator2).alm();
        pullRequestBuildStatusDecorators.add(decorator2);

        ProjectAlmSettingDto projectAlmSettingDto = mock(ProjectAlmSettingDto.class);
        when(projectAlmSettingDto.getAlmSlug()).thenReturn("dummy/repo");
        when(projectAlmSettingDto.getAlmSettingUuid()).thenReturn("almUuid");
        AlmSettingDto almSettingDto = mock(AlmSettingDto.class);
        when(almSettingDto.getUrl()).thenReturn("http://host.name");
        when(almSettingDto.getAppId()).thenReturn("app id");
        when(almSettingDto.getDecryptedPrivateKey(any())).thenReturn("private key");
        when(almSettingDto.getAlm()).thenReturn(ALM.AZURE_DEVOPS);
        when(dbClient.openSession(anyBoolean())).thenReturn(mock(DbSession.class));
        AlmSettingDao almSettingDao = mock(AlmSettingDao.class);
        when(almSettingDao.selectByUuid(any(), any())).thenReturn(Optional.of(almSettingDto));
        when(dbClient.almSettingDao()).thenReturn(almSettingDao);
        ProjectAlmSettingDao projectAlmSettingDao = mock(ProjectAlmSettingDao.class);
        when(projectAlmSettingDao.selectByProject(any(), anyString())).thenReturn(Optional.of(projectAlmSettingDto));
        when(dbClient.projectAlmSettingDao()).thenReturn(projectAlmSettingDao);

        testCase.finished(context);

        verify(decorator1).alm();
        verify(decorator2).alm();
        verify(projectAnalysis, never()).getAnalysis();
    }

    @Test
    void testFinishedNoAnalysis() {
        doReturn(Branch.Type.PULL_REQUEST).when(branch).getType();
        doReturn(Optional.of("pull-request")).when(branch).getName();

        doReturn(Optional.empty()).when(projectAnalysis).getAnalysis();

        PullRequestBuildStatusDecorator decorator1 = mock(PullRequestBuildStatusDecorator.class);
        doReturn(Collections.singletonList(ALM.AZURE_DEVOPS)).when(decorator1).alm();
        pullRequestBuildStatusDecorators.add(decorator1);

        PullRequestBuildStatusDecorator decorator2 = mock(PullRequestBuildStatusDecorator.class);
        doReturn(Collections.singletonList(ALM.GITHUB)).when(decorator2).alm();
        pullRequestBuildStatusDecorators.add(decorator2);

        ProjectAlmSettingDto projectAlmSettingDto = mock(ProjectAlmSettingDto.class);
        when(projectAlmSettingDto.getAlmSlug()).thenReturn("dummy/repo");
        when(projectAlmSettingDto.getAlmSettingUuid()).thenReturn("almUuid");
        AlmSettingDto almSettingDto = mock(AlmSettingDto.class);
        when(almSettingDto.getUrl()).thenReturn("http://host.name");
        when(almSettingDto.getAppId()).thenReturn("app id");
        when(almSettingDto.getDecryptedPrivateKey(any())).thenReturn("private key");
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
        verify(decorator2, never()).decorateQualityGateStatus(any(), any(), any());
    }


    @Test
    void testFinishedAnalysisWithNoRevision() {
        doReturn(Branch.Type.PULL_REQUEST).when(branch).getType();
        doReturn(Optional.of("pull-request")).when(branch).getName();

        Analysis analysis = mock(Analysis.class);
        doReturn(Optional.empty()).when(analysis).getRevision();
        doReturn(Optional.of(analysis)).when(projectAnalysis).getAnalysis();

        PullRequestBuildStatusDecorator decorator1 = mock(PullRequestBuildStatusDecorator.class);
        doReturn(Collections.singletonList(ALM.BITBUCKET)).when(decorator1).alm();
        pullRequestBuildStatusDecorators.add(decorator1);

        PullRequestBuildStatusDecorator decorator2 = mock(PullRequestBuildStatusDecorator.class);
        doReturn(Collections.singletonList(ALM.GITHUB)).when(decorator2).alm();
        pullRequestBuildStatusDecorators.add(decorator2);

        ProjectAlmSettingDto projectAlmSettingDto = mock(ProjectAlmSettingDto.class);
        when(projectAlmSettingDto.getAlmSlug()).thenReturn("dummy/repo");
        when(projectAlmSettingDto.getAlmSettingUuid()).thenReturn("almUuid");
        AlmSettingDto almSettingDto = mock(AlmSettingDto.class);
        when(almSettingDto.getUrl()).thenReturn("http://host.name");
        when(almSettingDto.getAppId()).thenReturn("app id");
        when(almSettingDto.getDecryptedPrivateKey(any())).thenReturn("private key");
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
    void testFinishedAnalysisWithNoQualityGate() {
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
        when(almSettingDto.getDecryptedPrivateKey(any())).thenReturn("private key");
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
        doReturn(Collections.singletonList(ALM.GITLAB)).when(decorator1).alm();
        pullRequestBuildStatusDecorators.add(decorator1);

        PullRequestBuildStatusDecorator decorator2 = mock(PullRequestBuildStatusDecorator.class);
        doReturn(Collections.singletonList(ALM.GITHUB)).when(decorator2).alm();
        pullRequestBuildStatusDecorators.add(decorator2);

        testCase.finished(context);

        verify(projectAnalysis).getAnalysis();
        verify(projectAnalysis).getQualityGate();
        verify(decorator2, never()).decorateQualityGateStatus(any(), any(), any());
    }

    @Test
    void testFinishedAnalysisDecorationRequest() {
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
        doReturn(Collections.singletonList(ALM.BITBUCKET)).when(decorator1).alm();
        pullRequestBuildStatusDecorators.add(decorator1);

        PullRequestBuildStatusDecorator decorator2 = mock(PullRequestBuildStatusDecorator.class);
        doReturn(Collections.singletonList(ALM.GITHUB)).when(decorator2).alm();
        doReturn(DecorationResult.builder().build()).when(decorator2).decorateQualityGateStatus(any(), any(), any());
        pullRequestBuildStatusDecorators.add(decorator2);

        ProjectAlmSettingDto projectAlmSettingDto = mock(ProjectAlmSettingDto.class);
        AlmSettingDto almSettingDto = mock(AlmSettingDto.class);
        doReturn(ALM.GITHUB).when(almSettingDto).getAlm();

        when(dbClient.openSession(anyBoolean())).thenReturn(mock(DbSession.class));
        AlmSettingDao almSettingDao = mock(AlmSettingDao.class);
        when(almSettingDao.selectByUuid(any(), any())).thenReturn(Optional.of(almSettingDto));
        when(dbClient.almSettingDao()).thenReturn(almSettingDao);
        ProjectAlmSettingDao projectAlmSettingDao = mock(ProjectAlmSettingDao.class);
        when(projectAlmSettingDao.selectByProject(any(), anyString())).thenReturn(Optional.of(projectAlmSettingDto));
        when(dbClient.projectAlmSettingDao()).thenReturn(projectAlmSettingDao);

        DbSession dbSession = mock(DbSession.class);
        doReturn(dbSession).when(dbClient).openSession(anyBoolean());
        BranchDao branchDao = mock(BranchDao.class);
        doReturn(branchDao).when(dbClient).branchDao();
        BranchDto branchDto = mock(BranchDto.class);
        doReturn(Optional.empty()).when(branchDao).selectByPullRequestKey(any(), any(), any());
        doReturn(DbProjectBranches.PullRequestData.newBuilder().build()).when(branchDto).getPullRequestData();


        testCase.finished(context);

        ArgumentCaptor<AnalysisDetails> analysisDetailsArgumentCaptor = ArgumentCaptor.forClass(AnalysisDetails.class);

        verify(projectAnalysis).getAnalysis();
        verify(projectAnalysis).getQualityGate();
        verify(dbClient, never()).branchDao();
        verify(decorator2).decorateQualityGateStatus(analysisDetailsArgumentCaptor.capture(), eq(almSettingDto), eq(projectAlmSettingDto));

        AnalysisDetails analysisDetails =
                new AnalysisDetails("pull-request", "revision", componentIssues, qualityGate, projectAnalysis);
        assertThat(analysisDetailsArgumentCaptor.getValue()).usingRecursiveComparison().isEqualTo(analysisDetails);
    }

    @Test
    void testFinishedAnalysisDecorationRequestPullRequestLinkSaved() {
        doReturn(Branch.Type.PULL_REQUEST).when(branch).getType();
        doReturn(Optional.of("pull-request")).when(branch).getName();

        Project project = mock(Project.class);
        doReturn("uuid").when(project).getUuid();
        doReturn(project).when(projectAnalysis).getProject();

        Analysis analysis = mock(Analysis.class);
        doReturn(Optional.of("revision")).when(analysis).getRevision();
        doReturn(new Date()).when(analysis).getDate();
        doReturn(Optional.of(analysis)).when(projectAnalysis).getAnalysis();

        QualityGate qualityGate = mock(QualityGate.class);
        doReturn(qualityGate).when(projectAnalysis).getQualityGate();

        PullRequestBuildStatusDecorator decorator1 = mock(PullRequestBuildStatusDecorator.class);
        doReturn(Collections.singletonList(ALM.GITHUB)).when(decorator1).alm();
        pullRequestBuildStatusDecorators.add(decorator1);

        PullRequestBuildStatusDecorator decorator2 = mock(PullRequestBuildStatusDecorator.class);
        doReturn(Collections.singletonList(ALM.BITBUCKET)).when(decorator2).alm();
        doReturn(DecorationResult.builder().withPullRequestUrl("pullRequestUrl").build()).when(decorator2).decorateQualityGateStatus(any(), any(), any());
        pullRequestBuildStatusDecorators.add(decorator2);

        ProjectAlmSettingDto projectAlmSettingDto = mock(ProjectAlmSettingDto.class);
        AlmSettingDto almSettingDto = mock(AlmSettingDto.class);
        doReturn(ALM.BITBUCKET).when(almSettingDto).getAlm();

        DbSession dbSession = mock(DbSession.class);
        doReturn(dbSession).when(dbClient).openSession(anyBoolean());
        BranchDao branchDao = mock(BranchDao.class);
        doReturn(branchDao).when(dbClient).branchDao();
        BranchDto branchDto = mock(BranchDto.class);
        doReturn(Optional.of(branchDto)).when(branchDao).selectByPullRequestKey(any(), any(), any());
        doReturn(DbProjectBranches.PullRequestData.newBuilder().build()).when(branchDto).getPullRequestData();

        ProjectAlmSettingDao projectAlmSettingDao = mock(ProjectAlmSettingDao.class);
        doReturn(Optional.of(projectAlmSettingDto)).when(projectAlmSettingDao).selectByProject(dbSession, "uuid");
        doReturn("setting-uuid").when(projectAlmSettingDto).getAlmSettingUuid();
        AlmSettingDao almSettingDao = mock(AlmSettingDao.class);
        doReturn(Optional.of(almSettingDto)).when(almSettingDao).selectByUuid(dbSession, "setting-uuid");

        doReturn(projectAlmSettingDao).when(dbClient).projectAlmSettingDao();
        doReturn(almSettingDao).when(dbClient).almSettingDao();

        testCase.finished(context);

        ArgumentCaptor<AnalysisDetails> analysisDetailsArgumentCaptor = ArgumentCaptor.forClass(AnalysisDetails.class);
        verify(projectAnalysis).getAnalysis();
        verify(projectAnalysis).getQualityGate();
        verify(dbClient, times(2)).openSession(false);
        verify(dbClient).branchDao();
        verify(branchDao).selectByPullRequestKey(dbSession, "uuid", "pull-request");
        verify(decorator2).decorateQualityGateStatus(analysisDetailsArgumentCaptor.capture(), eq(almSettingDto), eq(projectAlmSettingDto));

        ArgumentCaptor<DbProjectBranches.PullRequestData> pullRequestDataArgumentCaptor = ArgumentCaptor.forClass(
                DbProjectBranches.PullRequestData.class);
        verify(branchDto).setPullRequestData(pullRequestDataArgumentCaptor.capture());
        assertThat(pullRequestDataArgumentCaptor.getValue().getUrl()).isEqualTo("pullRequestUrl");

        verify(dbSession).commit();
        verify(branchDao).upsert(dbSession, branchDto);

        AnalysisDetails analysisDetails =
                new AnalysisDetails("pull-request", "revision",
                                    componentIssues, qualityGate, projectAnalysis);
        assertThat(analysisDetailsArgumentCaptor.getValue()).usingRecursiveComparison().isEqualTo(analysisDetails);
    }

    @Test
    void testFinishedAnalysisDecorationRequestPullRequestLinkNotSavedIfBranchDtoMissing() {
        doReturn(Branch.Type.PULL_REQUEST).when(branch).getType();
        doReturn(Optional.of("pull-request")).when(branch).getName();

        Project project = mock(Project.class);
        doReturn("uuid").when(project).getUuid();
        doReturn(project).when(projectAnalysis).getProject();

        Analysis analysis = mock(Analysis.class);
        doReturn(Optional.of("revision")).when(analysis).getRevision();
        doReturn(new Date()).when(analysis).getDate();
        doReturn(Optional.of(analysis)).when(projectAnalysis).getAnalysis();

        QualityGate qualityGate = mock(QualityGate.class);
        doReturn(qualityGate).when(projectAnalysis).getQualityGate();

        PullRequestBuildStatusDecorator decorator1 = mock(PullRequestBuildStatusDecorator.class);
        doReturn(Collections.singletonList(ALM.GITHUB)).when(decorator1).alm();
        doReturn(DecorationResult.builder().withPullRequestUrl("pullRequestUrl").build()).when(decorator1).decorateQualityGateStatus(any(), any(), any());
        pullRequestBuildStatusDecorators.add(decorator1);

        PullRequestBuildStatusDecorator decorator2 = mock(PullRequestBuildStatusDecorator.class);
        pullRequestBuildStatusDecorators.add(decorator2);

        ProjectAlmSettingDto projectAlmSettingDto = mock(ProjectAlmSettingDto.class);
        when(projectAlmSettingDto.getAlmSlug()).thenReturn("dummy/repo");
        when(projectAlmSettingDto.getAlmSettingUuid()).thenReturn("almUuid");
        AlmSettingDto almSettingDto = mock(AlmSettingDto.class);
        when(almSettingDto.getUrl()).thenReturn("http://host.name");
        when(almSettingDto.getAppId()).thenReturn("app id");
        when(almSettingDto.getDecryptedPrivateKey(any())).thenReturn("private key");
        when(almSettingDto.getAlm()).thenReturn(ALM.GITHUB);
        when(dbClient.openSession(anyBoolean())).thenReturn(mock(DbSession.class));
        AlmSettingDao almSettingDao = mock(AlmSettingDao.class);
        when(almSettingDao.selectByUuid(any(), any())).thenReturn(Optional.of(almSettingDto));
        when(dbClient.almSettingDao()).thenReturn(almSettingDao);
        ProjectAlmSettingDao projectAlmSettingDao = mock(ProjectAlmSettingDao.class);
        when(projectAlmSettingDao.selectByProject(any(), anyString())).thenReturn(Optional.of(projectAlmSettingDto));
        when(dbClient.projectAlmSettingDao()).thenReturn(projectAlmSettingDao);

        DbSession dbSession = mock(DbSession.class);
        doReturn(dbSession).when(dbClient).openSession(anyBoolean());
        BranchDao branchDao = mock(BranchDao.class);
        doReturn(branchDao).when(dbClient).branchDao();
        BranchDto branchDto = mock(BranchDto.class);
        doReturn(Optional.empty()).when(branchDao).selectByPullRequestKey(any(), any(), any());
        doReturn(DbProjectBranches.PullRequestData.newBuilder().build()).when(branchDto).getPullRequestData();

        testCase.finished(context);

        ArgumentCaptor<AnalysisDetails> analysisDetailsArgumentCaptor = ArgumentCaptor.forClass(AnalysisDetails.class);
        ArgumentCaptor<AlmSettingDto> almSettingDtoArgumentCaptor = ArgumentCaptor.forClass(AlmSettingDto.class);
        ArgumentCaptor<ProjectAlmSettingDto> projectAlmSettingDtoArgumentCaptor =
                ArgumentCaptor.forClass(ProjectAlmSettingDto.class);

        verify(projectAnalysis).getAnalysis();
        verify(projectAnalysis).getQualityGate();
        verify(dbClient, times(2)).openSession(false);
        verify(dbClient).branchDao();
        verify(branchDao).selectByPullRequestKey(dbSession, "uuid", "pull-request");
        verify(decorator1).decorateQualityGateStatus(analysisDetailsArgumentCaptor.capture(),
                                                     almSettingDtoArgumentCaptor.capture(),
                                                     projectAlmSettingDtoArgumentCaptor.capture());
        assertThat(almSettingDtoArgumentCaptor.getValue()).isSameAs(almSettingDto);
        assertThat(projectAlmSettingDtoArgumentCaptor.getValue()).isSameAs(projectAlmSettingDto);

        verify(branchDto, never()).setPullRequestData(any());
        verify(dbSession, never()).commit();
        verify(branchDao, never()).upsert(any(), any());

        AnalysisDetails analysisDetails =
                new AnalysisDetails("pull-request", "revision",
                                    componentIssues, qualityGate, projectAnalysis);
        assertThat(analysisDetailsArgumentCaptor.getValue()).usingRecursiveComparison().isEqualTo(analysisDetails);
    }

    @Test
    void testCorrectDescriptionReturnedForTask() {
        assertThat(testCase.getDescription()).isEqualTo("Pull Request Decoration");
    }
}
