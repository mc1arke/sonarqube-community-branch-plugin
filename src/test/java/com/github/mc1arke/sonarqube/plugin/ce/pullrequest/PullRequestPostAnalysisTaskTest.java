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

    private final PostProjectAnalysisTask.ProjectAnalysis projectAnalysis = mock();
    private final Branch branch = mock();
    private final ScannerContext scannerContext = mock();

    private final List<PullRequestBuildStatusDecorator> pullRequestBuildStatusDecorators = new ArrayList<>();
    private final PostAnalysisIssueVisitor postAnalysisIssueVisitor = mock();
    private final PostProjectAnalysisTask.Context context = mock();
    private final DbClient dbClient = mock();
    private final Project project = mock();
    private final PostAnalysisIssueVisitor.ComponentIssue componentIssue = mock();
    private final List<PostAnalysisIssueVisitor.ComponentIssue> componentIssues = List.of(componentIssue);

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

        ProjectAlmSettingDto projectAlmSettingDto = mock();
        when(projectAlmSettingDto.getAlmSlug()).thenReturn("dummy/repo");
        when(projectAlmSettingDto.getAlmSettingUuid()).thenReturn("almUuid");
        AlmSettingDto almSettingDto = mock();
        when(almSettingDto.getUrl()).thenReturn("http://host.name");
        when(almSettingDto.getAppId()).thenReturn("app id");
        when(almSettingDto.getDecryptedPrivateKey(any())).thenReturn("private key");
        when(almSettingDto.getAlm()).thenReturn(ALM.GITHUB);
        when(dbClient.openSession(anyBoolean())).thenReturn(mock());
        AlmSettingDao almSettingDao = mock();
        when(almSettingDao.selectByUuid(any(), any())).thenReturn(Optional.of(almSettingDto));
        when(dbClient.almSettingDao()).thenReturn(almSettingDao);
        ProjectAlmSettingDao projectAlmSettingDao = mock();
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

        PullRequestBuildStatusDecorator decorator1 = mock();
        doReturn(Collections.singletonList(ALM.BITBUCKET)).when(decorator1).alm();
        pullRequestBuildStatusDecorators.add(decorator1);

        PullRequestBuildStatusDecorator decorator2 = mock();
        doReturn(Collections.singletonList(ALM.GITHUB)).when(decorator2).alm();
        pullRequestBuildStatusDecorators.add(decorator2);

        ProjectAlmSettingDto projectAlmSettingDto = mock();
        when(projectAlmSettingDto.getAlmSlug()).thenReturn("dummy/repo");
        when(projectAlmSettingDto.getAlmSettingUuid()).thenReturn("almUuid");
        AlmSettingDto almSettingDto = mock();
        when(almSettingDto.getUrl()).thenReturn("http://host.name");
        when(almSettingDto.getAppId()).thenReturn("app id");
        when(almSettingDto.getDecryptedPrivateKey(any())).thenReturn("private key");
        when(almSettingDto.getAlm()).thenReturn(ALM.AZURE_DEVOPS);
        when(dbClient.openSession(anyBoolean())).thenReturn(mock());
        AlmSettingDao almSettingDao = mock();
        when(almSettingDao.selectByUuid(any(), any())).thenReturn(Optional.of(almSettingDto));
        when(dbClient.almSettingDao()).thenReturn(almSettingDao);
        ProjectAlmSettingDao projectAlmSettingDao = mock();
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

        PullRequestBuildStatusDecorator decorator1 = mock();
        doReturn(Collections.singletonList(ALM.AZURE_DEVOPS)).when(decorator1).alm();
        pullRequestBuildStatusDecorators.add(decorator1);

        PullRequestBuildStatusDecorator decorator2 = mock();
        doReturn(Collections.singletonList(ALM.GITHUB)).when(decorator2).alm();
        pullRequestBuildStatusDecorators.add(decorator2);

        ProjectAlmSettingDto projectAlmSettingDto = mock();
        when(projectAlmSettingDto.getAlmSlug()).thenReturn("dummy/repo");
        when(projectAlmSettingDto.getAlmSettingUuid()).thenReturn("almUuid");
        AlmSettingDto almSettingDto = mock();
        when(almSettingDto.getUrl()).thenReturn("http://host.name");
        when(almSettingDto.getAppId()).thenReturn("app id");
        when(almSettingDto.getDecryptedPrivateKey(any())).thenReturn("private key");
        when(almSettingDto.getAlm()).thenReturn(ALM.GITHUB);
        when(dbClient.openSession(anyBoolean())).thenReturn(mock());
        AlmSettingDao almSettingDao = mock();
        when(almSettingDao.selectByUuid(any(), any())).thenReturn(Optional.of(almSettingDto));
        when(dbClient.almSettingDao()).thenReturn(almSettingDao);
        ProjectAlmSettingDao projectAlmSettingDao = mock();
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

        Analysis analysis = mock();
        doReturn(Optional.empty()).when(analysis).getRevision();
        doReturn(Optional.of(analysis)).when(projectAnalysis).getAnalysis();

        PullRequestBuildStatusDecorator decorator1 = mock();
        doReturn(Collections.singletonList(ALM.BITBUCKET)).when(decorator1).alm();
        pullRequestBuildStatusDecorators.add(decorator1);

        PullRequestBuildStatusDecorator decorator2 = mock();
        doReturn(Collections.singletonList(ALM.GITHUB)).when(decorator2).alm();
        pullRequestBuildStatusDecorators.add(decorator2);

        ProjectAlmSettingDto projectAlmSettingDto = mock();
        when(projectAlmSettingDto.getAlmSlug()).thenReturn("dummy/repo");
        when(projectAlmSettingDto.getAlmSettingUuid()).thenReturn("almUuid");
        AlmSettingDto almSettingDto = mock();
        when(almSettingDto.getUrl()).thenReturn("http://host.name");
        when(almSettingDto.getAppId()).thenReturn("app id");
        when(almSettingDto.getDecryptedPrivateKey(any())).thenReturn("private key");
        when(almSettingDto.getAlm()).thenReturn(ALM.GITHUB);
        when(dbClient.openSession(anyBoolean())).thenReturn(mock());
        AlmSettingDao almSettingDao = mock();
        when(almSettingDao.selectByUuid(any(), any())).thenReturn(Optional.of(almSettingDto));
        when(dbClient.almSettingDao()).thenReturn(almSettingDao);
        ProjectAlmSettingDao projectAlmSettingDao = mock();
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

        Analysis analysis = mock();
        doReturn(Optional.of("revision")).when(analysis).getRevision();
        doReturn(Optional.of(analysis)).when(projectAnalysis).getAnalysis();

        ProjectAlmSettingDto projectAlmSettingDto = mock();
        when(projectAlmSettingDto.getAlmSlug()).thenReturn("dummy/repo");
        when(projectAlmSettingDto.getAlmSettingUuid()).thenReturn("almUuid");
        AlmSettingDto almSettingDto = mock();
        when(almSettingDto.getUrl()).thenReturn("http://host.name");
        when(almSettingDto.getAppId()).thenReturn("app id");
        when(almSettingDto.getDecryptedPrivateKey(any())).thenReturn("private key");
        when(almSettingDto.getAlm()).thenReturn(ALM.GITHUB);
        when(dbClient.openSession(anyBoolean())).thenReturn(mock());
        AlmSettingDao almSettingDao = mock();
        when(almSettingDao.selectByUuid(any(), any())).thenReturn(Optional.of(almSettingDto));
        when(dbClient.almSettingDao()).thenReturn(almSettingDao);
        ProjectAlmSettingDao projectAlmSettingDao = mock();
        when(projectAlmSettingDao.selectByProject(any(), anyString())).thenReturn(Optional.of(projectAlmSettingDto));
        when(dbClient.projectAlmSettingDao()).thenReturn(projectAlmSettingDao);

        doReturn(scannerContext).when(projectAnalysis).getScannerContext();

        PullRequestBuildStatusDecorator decorator1 = mock();
        doReturn(Collections.singletonList(ALM.GITLAB)).when(decorator1).alm();
        pullRequestBuildStatusDecorators.add(decorator1);

        PullRequestBuildStatusDecorator decorator2 = mock();
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

        doReturn(projectAnalysis).when(context).getProjectAnalysis();

        Analysis analysis = mock();
        doReturn(Optional.of("revision")).when(analysis).getRevision();
        doReturn(new Date()).when(analysis).getDate();
        doReturn(Optional.of(analysis)).when(projectAnalysis).getAnalysis();

        QualityGate qualityGate = mock();
        doReturn(qualityGate).when(projectAnalysis).getQualityGate();

        PullRequestBuildStatusDecorator decorator1 = mock();
        doReturn(Collections.singletonList(ALM.BITBUCKET)).when(decorator1).alm();
        pullRequestBuildStatusDecorators.add(decorator1);

        PullRequestBuildStatusDecorator decorator2 = mock();
        doReturn(Collections.singletonList(ALM.GITHUB)).when(decorator2).alm();
        doReturn(DecorationResult.builder().build()).when(decorator2).decorateQualityGateStatus(any(), any(), any());
        pullRequestBuildStatusDecorators.add(decorator2);

        ProjectAlmSettingDto projectAlmSettingDto = mock();
        AlmSettingDto almSettingDto = mock();
        doReturn(ALM.GITHUB).when(almSettingDto).getAlm();

        when(dbClient.openSession(anyBoolean())).thenReturn(mock());
        AlmSettingDao almSettingDao = mock();
        when(almSettingDao.selectByUuid(any(), any())).thenReturn(Optional.of(almSettingDto));
        when(dbClient.almSettingDao()).thenReturn(almSettingDao);
        ProjectAlmSettingDao projectAlmSettingDao = mock();
        when(projectAlmSettingDao.selectByProject(any(), anyString())).thenReturn(Optional.of(projectAlmSettingDto));
        when(dbClient.projectAlmSettingDao()).thenReturn(projectAlmSettingDao);

        DbSession dbSession = mock();
        doReturn(dbSession).when(dbClient).openSession(anyBoolean());
        BranchDao branchDao = mock();
        doReturn(branchDao).when(dbClient).branchDao();
        BranchDto branchDto = mock();
        doReturn(Optional.empty()).when(branchDao).selectByPullRequestKey(any(), any(), any());
        doReturn(DbProjectBranches.PullRequestData.newBuilder().build()).when(branchDto).getPullRequestData();


        testCase.finished(context);

        ArgumentCaptor<AnalysisDetails> analysisDetailsArgumentCaptor = ArgumentCaptor.captor();

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

        doReturn("uuid").when(project).getUuid();
        doReturn(project).when(projectAnalysis).getProject();

        Analysis analysis = mock();
        doReturn(Optional.of("revision")).when(analysis).getRevision();
        doReturn(new Date()).when(analysis).getDate();
        doReturn(Optional.of(analysis)).when(projectAnalysis).getAnalysis();

        QualityGate qualityGate = mock();
        doReturn(qualityGate).when(projectAnalysis).getQualityGate();

        PullRequestBuildStatusDecorator decorator1 = mock();
        doReturn(Collections.singletonList(ALM.GITHUB)).when(decorator1).alm();
        pullRequestBuildStatusDecorators.add(decorator1);

        PullRequestBuildStatusDecorator decorator2 = mock();
        doReturn(Collections.singletonList(ALM.BITBUCKET)).when(decorator2).alm();
        doReturn(DecorationResult.builder().withPullRequestUrl("pullRequestUrl").build()).when(decorator2).decorateQualityGateStatus(any(), any(), any());
        pullRequestBuildStatusDecorators.add(decorator2);

        ProjectAlmSettingDto projectAlmSettingDto = mock();
        AlmSettingDto almSettingDto = mock();
        doReturn(ALM.BITBUCKET).when(almSettingDto).getAlm();

        DbSession dbSession = mock();
        doReturn(dbSession).when(dbClient).openSession(anyBoolean());
        BranchDao branchDao = mock();
        doReturn(branchDao).when(dbClient).branchDao();
        BranchDto branchDto = mock();
        doReturn(Optional.of(branchDto)).when(branchDao).selectByPullRequestKey(any(), any(), any());
        doReturn(DbProjectBranches.PullRequestData.newBuilder().build()).when(branchDto).getPullRequestData();

        ProjectAlmSettingDao projectAlmSettingDao = mock();
        doReturn(Optional.of(projectAlmSettingDto)).when(projectAlmSettingDao).selectByProject(dbSession, "uuid");
        doReturn("setting-uuid").when(projectAlmSettingDto).getAlmSettingUuid();
        AlmSettingDao almSettingDao = mock();
        doReturn(Optional.of(almSettingDto)).when(almSettingDao).selectByUuid(dbSession, "setting-uuid");

        doReturn(projectAlmSettingDao).when(dbClient).projectAlmSettingDao();
        doReturn(almSettingDao).when(dbClient).almSettingDao();

        testCase.finished(context);

        ArgumentCaptor<AnalysisDetails> analysisDetailsArgumentCaptor = ArgumentCaptor.captor();
        verify(projectAnalysis).getAnalysis();
        verify(projectAnalysis).getQualityGate();
        verify(dbClient, times(2)).openSession(false);
        verify(dbClient).branchDao();
        verify(branchDao).selectByPullRequestKey(dbSession, "uuid", "pull-request");
        verify(decorator2).decorateQualityGateStatus(analysisDetailsArgumentCaptor.capture(), eq(almSettingDto), eq(projectAlmSettingDto));

        ArgumentCaptor<DbProjectBranches.PullRequestData> pullRequestDataArgumentCaptor = ArgumentCaptor.captor();
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

        doReturn("uuid").when(project).getUuid();
        doReturn(project).when(projectAnalysis).getProject();

        Analysis analysis = mock();
        doReturn(Optional.of("revision")).when(analysis).getRevision();
        doReturn(new Date()).when(analysis).getDate();
        doReturn(Optional.of(analysis)).when(projectAnalysis).getAnalysis();

        QualityGate qualityGate = mock();
        doReturn(qualityGate).when(projectAnalysis).getQualityGate();

        PullRequestBuildStatusDecorator decorator1 = mock();
        doReturn(Collections.singletonList(ALM.GITHUB)).when(decorator1).alm();
        doReturn(DecorationResult.builder().withPullRequestUrl("pullRequestUrl").build()).when(decorator1).decorateQualityGateStatus(any(), any(), any());
        pullRequestBuildStatusDecorators.add(decorator1);

        PullRequestBuildStatusDecorator decorator2 = mock();
        pullRequestBuildStatusDecorators.add(decorator2);

        ProjectAlmSettingDto projectAlmSettingDto = mock();
        when(projectAlmSettingDto.getAlmSlug()).thenReturn("dummy/repo");
        when(projectAlmSettingDto.getAlmSettingUuid()).thenReturn("almUuid");
        AlmSettingDto almSettingDto = mock();
        when(almSettingDto.getUrl()).thenReturn("http://host.name");
        when(almSettingDto.getAppId()).thenReturn("app id");
        when(almSettingDto.getDecryptedPrivateKey(any())).thenReturn("private key");
        when(almSettingDto.getAlm()).thenReturn(ALM.GITHUB);
        when(dbClient.openSession(anyBoolean())).thenReturn(mock());
        AlmSettingDao almSettingDao = mock();
        when(almSettingDao.selectByUuid(any(), any())).thenReturn(Optional.of(almSettingDto));
        when(dbClient.almSettingDao()).thenReturn(almSettingDao);
        ProjectAlmSettingDao projectAlmSettingDao = mock();
        when(projectAlmSettingDao.selectByProject(any(), anyString())).thenReturn(Optional.of(projectAlmSettingDto));
        when(dbClient.projectAlmSettingDao()).thenReturn(projectAlmSettingDao);

        DbSession dbSession = mock();
        doReturn(dbSession).when(dbClient).openSession(anyBoolean());
        BranchDao branchDao = mock();
        doReturn(branchDao).when(dbClient).branchDao();
        BranchDto branchDto = mock();
        doReturn(Optional.empty()).when(branchDao).selectByPullRequestKey(any(), any(), any());
        doReturn(DbProjectBranches.PullRequestData.newBuilder().build()).when(branchDto).getPullRequestData();

        testCase.finished(context);

        ArgumentCaptor<AnalysisDetails> analysisDetailsArgumentCaptor = ArgumentCaptor.captor();
        ArgumentCaptor<AlmSettingDto> almSettingDtoArgumentCaptor = ArgumentCaptor.captor();
        ArgumentCaptor<ProjectAlmSettingDto> projectAlmSettingDtoArgumentCaptor =
                ArgumentCaptor.captor();

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
