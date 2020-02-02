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
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.alm.setting.ALM;
import org.sonar.db.alm.setting.AlmSettingDao;
import org.sonar.db.alm.setting.AlmSettingDto;
import org.sonar.db.alm.setting.ProjectAlmSettingDao;
import org.sonar.db.alm.setting.ProjectAlmSettingDto;

import java.util.ArrayList;
import java.util.Date;
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

    @Test
    public void testFinishedNonPullRequest() {
        PostProjectAnalysisTask.ProjectAnalysis projectAnalysis = mock(PostProjectAnalysisTask.ProjectAnalysis.class);
        Branch branch = mock(Branch.class);
        doReturn(Branch.Type.BRANCH).when(branch).getType();
        doReturn(Optional.of(branch)).when(projectAnalysis).getBranch();

        PostProjectAnalysisTask.Context context = mock(PostProjectAnalysisTask.Context.class);
        doReturn(projectAnalysis).when(context).getProjectAnalysis();

        Server server = mock(Server.class);
        ConfigurationRepository configurationRepository = mock(ConfigurationRepository.class);
        List<PullRequestBuildStatusDecorator> pullRequestBuildStatusDecorators = new ArrayList<>();
        PostAnalysisIssueVisitor postAnalysisIssueVisitor = mock(PostAnalysisIssueVisitor.class);
        MetricRepository metricRepository = mock(MetricRepository.class);
        MeasureRepository measureRepository = mock(MeasureRepository.class);
        TreeRootHolder treeRootHolder = mock(TreeRootHolder.class);

        DbClient dbClient = mock(DbClient.class);

        PullRequestPostAnalysisTask testCase =
                new PullRequestPostAnalysisTask(server, configurationRepository, pullRequestBuildStatusDecorators,
                                                postAnalysisIssueVisitor, metricRepository, measureRepository,
                                                treeRootHolder, dbClient);
        testCase.finished(context);

        verify(branch).getType();
        verify(branch, never()).getName();
    }

    @Test
    public void testFinishedNoBranchName() {
        PostProjectAnalysisTask.ProjectAnalysis projectAnalysis = mock(PostProjectAnalysisTask.ProjectAnalysis.class);
        Branch branch = mock(Branch.class);
        doReturn(Branch.Type.PULL_REQUEST).when(branch).getType();
        doReturn(Optional.empty()).when(branch).getName();
        doReturn(Optional.of(branch)).when(projectAnalysis).getBranch();

        PostProjectAnalysisTask.Context context = mock(PostProjectAnalysisTask.Context.class);
        doReturn(projectAnalysis).when(context).getProjectAnalysis();

        Server server = mock(Server.class);
        ConfigurationRepository configurationRepository = mock(ConfigurationRepository.class);
        List<PullRequestBuildStatusDecorator> pullRequestBuildStatusDecorators = new ArrayList<>();
        PostAnalysisIssueVisitor postAnalysisIssueVisitor = mock(PostAnalysisIssueVisitor.class);
        MetricRepository metricRepository = mock(MetricRepository.class);
        MeasureRepository measureRepository = mock(MeasureRepository.class);
        TreeRootHolder treeRootHolder = mock(TreeRootHolder.class);

        DbClient dbClient = mock(DbClient.class);

        PullRequestPostAnalysisTask testCase =
                new PullRequestPostAnalysisTask(server, configurationRepository, pullRequestBuildStatusDecorators,
                                                postAnalysisIssueVisitor, metricRepository, measureRepository,
                                                treeRootHolder, dbClient);
        testCase.finished(context);

        verify(branch).getName();
        verify(configurationRepository, never()).getConfiguration();
    }

    @Test
    public void testFinishedNoProviderSet() {
        PostProjectAnalysisTask.ProjectAnalysis projectAnalysis = mock(PostProjectAnalysisTask.ProjectAnalysis.class);
        Branch branch = mock(Branch.class);
        doReturn(Branch.Type.PULL_REQUEST).when(branch).getType();
        doReturn(Optional.of("branchName")).when(branch).getName();
        doReturn(Optional.of(branch)).when(projectAnalysis).getBranch();

        Project project = mock(Project.class);
        doReturn("projectUuid").when(project).getUuid();
        doReturn(project).when(projectAnalysis).getProject();

        PostProjectAnalysisTask.Context context = mock(PostProjectAnalysisTask.Context.class);
        doReturn(projectAnalysis).when(context).getProjectAnalysis();

        Server server = mock(Server.class);
        ConfigurationRepository configurationRepository = mock(ConfigurationRepository.class);
        List<PullRequestBuildStatusDecorator> pullRequestBuildStatusDecorators = new ArrayList<>();
        PostAnalysisIssueVisitor postAnalysisIssueVisitor = mock(PostAnalysisIssueVisitor.class);
        MetricRepository metricRepository = mock(MetricRepository.class);
        MeasureRepository measureRepository = mock(MeasureRepository.class);
        TreeRootHolder treeRootHolder = mock(TreeRootHolder.class);

        Configuration configuration = mock(Configuration.class);
        doReturn(configuration).when(configurationRepository).getConfiguration();

        DbClient dbClient = mock(DbClient.class);

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


        PullRequestPostAnalysisTask testCase =
                new PullRequestPostAnalysisTask(server, configurationRepository, pullRequestBuildStatusDecorators,
                                                postAnalysisIssueVisitor, metricRepository, measureRepository,
                                                treeRootHolder, dbClient);
        testCase.finished(context);

        verify(configurationRepository).getConfiguration();
        verify(projectAnalysis, never()).getAnalysis();
    }

    @Test
    public void testFinishedNoProviderMatchingName() {
        PostProjectAnalysisTask.ProjectAnalysis projectAnalysis = mock(PostProjectAnalysisTask.ProjectAnalysis.class);
        Branch branch = mock(Branch.class);
        doReturn(Branch.Type.PULL_REQUEST).when(branch).getType();
        doReturn(Optional.of("branchName")).when(branch).getName();
        doReturn(Optional.of(branch)).when(projectAnalysis).getBranch();

        Project project = mock(Project.class);
        doReturn("projectUuid").when(project).getUuid();
        doReturn(project).when(projectAnalysis).getProject();

        PostProjectAnalysisTask.Context context = mock(PostProjectAnalysisTask.Context.class);
        doReturn(projectAnalysis).when(context).getProjectAnalysis();

        Server server = mock(Server.class);
        ConfigurationRepository configurationRepository = mock(ConfigurationRepository.class);
        List<PullRequestBuildStatusDecorator> pullRequestBuildStatusDecorators = new ArrayList<>();
        PostAnalysisIssueVisitor postAnalysisIssueVisitor = mock(PostAnalysisIssueVisitor.class);
        MetricRepository metricRepository = mock(MetricRepository.class);
        MeasureRepository measureRepository = mock(MeasureRepository.class);
        TreeRootHolder treeRootHolder = mock(TreeRootHolder.class);

        PullRequestBuildStatusDecorator decorator1 = mock(PullRequestBuildStatusDecorator.class);
        doReturn("decorator-name-1").when(decorator1).name();
        pullRequestBuildStatusDecorators.add(decorator1);

        PullRequestBuildStatusDecorator decorator2 = mock(PullRequestBuildStatusDecorator.class);
        doReturn("decorator-name-2").when(decorator2).name();
        doReturn(ALM.GITHUB).when(decorator2).alm();
        pullRequestBuildStatusDecorators.add(decorator2);

        Configuration configuration = mock(Configuration.class);
        doReturn(configuration).when(configurationRepository).getConfiguration();

        DbClient dbClient = mock(DbClient.class);

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

        ScannerContext scannerContext = mock(ScannerContext.class);
        doReturn(scannerContext).when(projectAnalysis).getScannerContext();


        PullRequestPostAnalysisTask testCase =
                new PullRequestPostAnalysisTask(server, configurationRepository, pullRequestBuildStatusDecorators,
                                                postAnalysisIssueVisitor, metricRepository, measureRepository,
                                                treeRootHolder, dbClient);
        testCase.finished(context);

        verify(configurationRepository).getConfiguration();
        verify(decorator1).alm();
        verify(decorator2).alm();
        verify(projectAnalysis, never()).getAnalysis();
    }

    @Test
    public void testFinishedNoAnalysis() {
        PostProjectAnalysisTask.ProjectAnalysis projectAnalysis = mock(PostProjectAnalysisTask.ProjectAnalysis.class);
        Branch branch = mock(Branch.class);
        doReturn(Branch.Type.PULL_REQUEST).when(branch).getType();
        doReturn(Optional.of("pull-request")).when(branch).getName();
        doReturn(Optional.of(branch)).when(projectAnalysis).getBranch();

        Project project = mock(Project.class);
        doReturn("projectUuid").when(project).getUuid();
        doReturn(project).when(projectAnalysis).getProject();

        PostProjectAnalysisTask.Context context = mock(PostProjectAnalysisTask.Context.class);
        doReturn(projectAnalysis).when(context).getProjectAnalysis();

        Server server = mock(Server.class);
        ConfigurationRepository configurationRepository = mock(ConfigurationRepository.class);
        List<PullRequestBuildStatusDecorator> pullRequestBuildStatusDecorators = new ArrayList<>();
        PostAnalysisIssueVisitor postAnalysisIssueVisitor = mock(PostAnalysisIssueVisitor.class);
        MetricRepository metricRepository = mock(MetricRepository.class);
        MeasureRepository measureRepository = mock(MeasureRepository.class);
        TreeRootHolder treeRootHolder = mock(TreeRootHolder.class);

        doReturn(Optional.empty()).when(projectAnalysis).getAnalysis();

        PullRequestBuildStatusDecorator decorator1 = mock(PullRequestBuildStatusDecorator.class);
        doReturn("decorator-name-1").when(decorator1).name();
        pullRequestBuildStatusDecorators.add(decorator1);

        PullRequestBuildStatusDecorator decorator2 = mock(PullRequestBuildStatusDecorator.class);
        doReturn("decorator-name-2").when(decorator2).name();
        doReturn(ALM.GITHUB).when(decorator2).alm();
        pullRequestBuildStatusDecorators.add(decorator2);

        Configuration configuration = mock(Configuration.class);
        doReturn(configuration).when(configurationRepository).getConfiguration();

        DbClient dbClient = mock(DbClient.class);

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

        PullRequestPostAnalysisTask testCase =
                new PullRequestPostAnalysisTask(server, configurationRepository, pullRequestBuildStatusDecorators,
                                                postAnalysisIssueVisitor, metricRepository, measureRepository,
                                                treeRootHolder, dbClient);
        testCase.finished(context);

        verify(configurationRepository).getConfiguration();
        verify(projectAnalysis).getAnalysis();
        verify(decorator2, never()).decorateQualityGateStatus(any(), any(), any());
    }


    @Test
    public void testFinishedAnalysisWithNoRevision() {
        PostProjectAnalysisTask.ProjectAnalysis projectAnalysis = mock(PostProjectAnalysisTask.ProjectAnalysis.class);
        Branch branch = mock(Branch.class);
        doReturn(Branch.Type.PULL_REQUEST).when(branch).getType();
        doReturn(Optional.of("pull-request")).when(branch).getName();
        doReturn(Optional.of(branch)).when(projectAnalysis).getBranch();

        Project project = mock(Project.class);
        doReturn("projectUuid").when(project).getUuid();
        doReturn(project).when(projectAnalysis).getProject();

        PostProjectAnalysisTask.Context context = mock(PostProjectAnalysisTask.Context.class);
        doReturn(projectAnalysis).when(context).getProjectAnalysis();

        Server server = mock(Server.class);
        ConfigurationRepository configurationRepository = mock(ConfigurationRepository.class);
        List<PullRequestBuildStatusDecorator> pullRequestBuildStatusDecorators = new ArrayList<>();
        PostAnalysisIssueVisitor postAnalysisIssueVisitor = mock(PostAnalysisIssueVisitor.class);
        MetricRepository metricRepository = mock(MetricRepository.class);
        MeasureRepository measureRepository = mock(MeasureRepository.class);
        TreeRootHolder treeRootHolder = mock(TreeRootHolder.class);

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

        Configuration configuration = mock(Configuration.class);
        doReturn(configuration).when(configurationRepository).getConfiguration();

        DbClient dbClient = mock(DbClient.class);

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


        PullRequestPostAnalysisTask testCase =
                new PullRequestPostAnalysisTask(server, configurationRepository, pullRequestBuildStatusDecorators,
                                                postAnalysisIssueVisitor, metricRepository, measureRepository,
                                                treeRootHolder, dbClient);
        testCase.finished(context);

        verify(configurationRepository).getConfiguration();
        verify(projectAnalysis).getAnalysis();
        verify(projectAnalysis, never()).getQualityGate();
        verify(decorator2, never()).decorateQualityGateStatus(any(), any(), any());
    }

    @Test
    public void testFinishedAnalysisWithNoQualityGate() {
        PostProjectAnalysisTask.ProjectAnalysis projectAnalysis = mock(PostProjectAnalysisTask.ProjectAnalysis.class);
        Branch branch = mock(Branch.class);
        doReturn(Branch.Type.PULL_REQUEST).when(branch).getType();
        doReturn(Optional.of("pull-request")).when(branch).getName();
        doReturn(Optional.of(branch)).when(projectAnalysis).getBranch();

        Project project = mock(Project.class);
        doReturn("projectUuid").when(project).getUuid();
        doReturn(project).when(projectAnalysis).getProject();

        PostProjectAnalysisTask.Context context = mock(PostProjectAnalysisTask.Context.class);
        doReturn(projectAnalysis).when(context).getProjectAnalysis();

        Server server = mock(Server.class);
        ConfigurationRepository configurationRepository = mock(ConfigurationRepository.class);
        List<PullRequestBuildStatusDecorator> pullRequestBuildStatusDecorators = new ArrayList<>();
        PostAnalysisIssueVisitor postAnalysisIssueVisitor = mock(PostAnalysisIssueVisitor.class);
        MetricRepository metricRepository = mock(MetricRepository.class);
        MeasureRepository measureRepository = mock(MeasureRepository.class);
        TreeRootHolder treeRootHolder = mock(TreeRootHolder.class);

        Analysis analysis = mock(Analysis.class);
        doReturn(Optional.of("revision")).when(analysis).getRevision();
        doReturn(Optional.of(analysis)).when(projectAnalysis).getAnalysis();

        DbClient dbClient = mock(DbClient.class);
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

        Configuration configuration = mock(Configuration.class);
        doReturn(configuration).when(configurationRepository).getConfiguration();

        PullRequestPostAnalysisTask testCase =
                new PullRequestPostAnalysisTask(server, configurationRepository, pullRequestBuildStatusDecorators,
                                                postAnalysisIssueVisitor, metricRepository, measureRepository,
                                                treeRootHolder, dbClient);
        testCase.finished(context);

        verify(configurationRepository).getConfiguration();
        verify(projectAnalysis).getAnalysis();
        verify(projectAnalysis).getQualityGate();
        verify(decorator2, never()).decorateQualityGateStatus(any(), any(), any());
    }

    @Test
    public void testFinishedAnalysisDecorationRequest() {
        PostProjectAnalysisTask.ProjectAnalysis projectAnalysis = mock(PostProjectAnalysisTask.ProjectAnalysis.class);
        Branch branch = mock(Branch.class);
        doReturn(Branch.Type.PULL_REQUEST).when(branch).getType();
        doReturn(Optional.of("pull-request")).when(branch).getName();
        doReturn(Optional.of(branch)).when(projectAnalysis).getBranch();

        PostProjectAnalysisTask.Context context = mock(PostProjectAnalysisTask.Context.class);
        doReturn(projectAnalysis).when(context).getProjectAnalysis();

        Project project = mock(Project.class);
        doReturn("uuid").when(project).getUuid();
        doReturn(project).when(projectAnalysis).getProject();

        Server server = mock(Server.class);
        ConfigurationRepository configurationRepository = mock(ConfigurationRepository.class);
        List<PullRequestBuildStatusDecorator> pullRequestBuildStatusDecorators = new ArrayList<>();
        PostAnalysisIssueVisitor postAnalysisIssueVisitor = mock(PostAnalysisIssueVisitor.class);
        MetricRepository metricRepository = mock(MetricRepository.class);
        MeasureRepository measureRepository = mock(MeasureRepository.class);
        TreeRootHolder treeRootHolder = mock(TreeRootHolder.class);

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

        Configuration configuration = mock(Configuration.class);
        doReturn(configuration).when(configurationRepository).getConfiguration();

        DbClient dbClient = mock(DbClient.class);

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

        PullRequestPostAnalysisTask testCase =
                new PullRequestPostAnalysisTask(server, configurationRepository, pullRequestBuildStatusDecorators,
                                                postAnalysisIssueVisitor, metricRepository, measureRepository,
                                                treeRootHolder, dbClient);
        testCase.finished(context);

        ArgumentCaptor<AnalysisDetails> analysisDetailsArgumentCaptor = ArgumentCaptor.forClass(AnalysisDetails.class);
        ArgumentCaptor<AlmSettingDto> almSettingDtoArgumentCaptor = ArgumentCaptor.forClass(AlmSettingDto.class);
        ArgumentCaptor<ProjectAlmSettingDto> projectAlmSettingDtoArgumentCaptor =
                ArgumentCaptor.forClass(ProjectAlmSettingDto.class);

        verify(configurationRepository).getConfiguration();
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
                                    configuration, null, scannerContext);
        assertThat(analysisDetailsArgumentCaptor.getValue()).usingRecursiveComparison().isEqualTo(analysisDetails);
    }

    @Test
    public void testCorrectDescriptionReturnedForTask() {
        assertThat(new PullRequestPostAnalysisTask(mock(Server.class), mock(ConfigurationRepository.class), new ArrayList<>(),
                                                   mock(PostAnalysisIssueVisitor.class), mock(MetricRepository.class),
                                                   mock(MeasureRepository.class), mock(TreeRootHolder.class),
                                                   mock(DbClient.class))
                           .getDescription()).isEqualTo("Pull Request Decoration");
    }
}
