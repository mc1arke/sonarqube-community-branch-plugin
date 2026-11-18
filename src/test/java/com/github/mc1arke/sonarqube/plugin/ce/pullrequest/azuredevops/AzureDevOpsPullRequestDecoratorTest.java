/*
 * Copyright (C) 2024-2026 Michael Clarke, Sebastiaan Speck
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
package com.github.mc1arke.sonarqube.plugin.ce.pullrequest.azuredevops;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.sonar.api.ce.posttask.QualityGate;
import org.sonar.ce.task.projectanalysis.scm.ScmInfoRepository;
import org.sonar.db.alm.setting.ALM;
import org.sonar.db.alm.setting.AlmSettingDto;
import org.sonar.db.alm.setting.ProjectAlmSettingDto;
import org.sonar.db.protobuf.DbCommons;
import org.sonar.db.protobuf.DbIssues;

import com.github.mc1arke.sonarqube.plugin.almclient.azuredevops.AzureDevopsClient;
import com.github.mc1arke.sonarqube.plugin.almclient.azuredevops.AzureDevopsClientFactory;
import com.github.mc1arke.sonarqube.plugin.almclient.azuredevops.model.Comment;
import com.github.mc1arke.sonarqube.plugin.almclient.azuredevops.model.CommentThread;
import com.github.mc1arke.sonarqube.plugin.almclient.azuredevops.model.ConnectionData;
import com.github.mc1arke.sonarqube.plugin.almclient.azuredevops.model.CreateCommentRequest;
import com.github.mc1arke.sonarqube.plugin.almclient.azuredevops.model.GitPullRequestStatus;
import com.github.mc1arke.sonarqube.plugin.almclient.azuredevops.model.GitStatusContext;
import com.github.mc1arke.sonarqube.plugin.almclient.azuredevops.model.IdentityRef;
import com.github.mc1arke.sonarqube.plugin.almclient.azuredevops.model.Project;
import com.github.mc1arke.sonarqube.plugin.almclient.azuredevops.model.PullRequest;
import com.github.mc1arke.sonarqube.plugin.almclient.azuredevops.model.Repository;
import com.github.mc1arke.sonarqube.plugin.almclient.azuredevops.model.enums.CommentType;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.AnalysisDetails;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.PostAnalysisIssueVisitor;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.report.AnalysisIssueSummary;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.markup.MarkdownFormatterFactory;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.report.AnalysisSummary;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.report.ReportGenerator;
import tools.jackson.core.JacksonException;

class AzureDevOpsPullRequestDecoratorTest {

    private final AlmSettingDto almSettingDto = mock();
    private final ProjectAlmSettingDto projectAlmSettingDto = mock();
    private final AnalysisDetails analysisDetails = mock();
    private final ScmInfoRepository scmInfoRepository = mock();
    private final AzureDevopsClientFactory azureDevopsClientFactory = mock();
    private final ReportGenerator reportGenerator = mock();
    private final MarkdownFormatterFactory markdownFormatterFactory = mock();

    private PullRequest mockPullRequest(AzureDevopsClient azureDevopsClient, String azureProject, String azureRepository, int pullRequestId) throws IOException {
        PullRequest pullRequest = mock();
        when(pullRequest.getId()).thenReturn(pullRequestId);
        when(pullRequest.doesSupportIterations()).thenReturn(true);
        when(azureDevopsClient.retrievePullRequest(any(), any(), anyInt())).thenReturn(pullRequest);
        Repository repository = mock();
        Project project = mock();
        when(pullRequest.getRepository()).thenReturn(repository);
        when(repository.getProject()).thenReturn(project);
        when(project.getName()).thenReturn(azureProject);
        when(repository.getRemoteUrl()).thenReturn("https://remote.url/path/to/repo");
        when(repository.getName()).thenReturn(azureRepository);
        return pullRequest;
    }

    @Test
    void testDecorateQualityGateRepoSlugException() {
        when(almSettingDto.getUrl()).thenReturn("almUrl");
        when(almSettingDto.getDecryptedPersonalAccessToken(any())).thenReturn("personalAccessToken");
        when(analysisDetails.getPullRequestId()).thenReturn("123");
        when(projectAlmSettingDto.getAlmRepo()).thenReturn("repo");

        AzureDevOpsPullRequestDecorator pullRequestDecorator = new AzureDevOpsPullRequestDecorator(scmInfoRepository, azureDevopsClientFactory, reportGenerator, markdownFormatterFactory);

        assertThatThrownBy(() -> pullRequestDecorator.decorateQualityGateStatus(analysisDetails, almSettingDto, projectAlmSettingDto))
            .hasMessage("Repository slug must be provided")
            .isExactlyInstanceOf(IllegalStateException.class);
    }

    @Test
    void testDecorateQualityGateProjectIDException() {
        when(almSettingDto.getUrl()).thenReturn("almUrl");
        when(almSettingDto.getDecryptedPersonalAccessToken(any())).thenReturn("personalAccessToken");
        when(projectAlmSettingDto.getAlmRepo()).thenReturn("repo");
        when(projectAlmSettingDto.getAlmSlug()).thenReturn("slug");

        AzureDevOpsPullRequestDecorator pullRequestDecorator = new AzureDevOpsPullRequestDecorator(scmInfoRepository, azureDevopsClientFactory, reportGenerator, markdownFormatterFactory);

        assertThatThrownBy(() -> pullRequestDecorator.decorateQualityGateStatus(analysisDetails, almSettingDto, projectAlmSettingDto))
            .hasMessage("Could not parse Pull Request Key")
            .isExactlyInstanceOf(IllegalStateException.class);
    }

    @Test
    void testDecorateQualityGatePRBranchException() {
        when(almSettingDto.getUrl()).thenReturn("almUrl");
        when(almSettingDto.getDecryptedPersonalAccessToken(any())).thenReturn("personalAccessToken");
        when(analysisDetails.getPullRequestId()).thenReturn("NON-NUMERIC");
        when(projectAlmSettingDto.getAlmSlug()).thenReturn("prj");
        when(projectAlmSettingDto.getAlmRepo()).thenReturn("repo");

        AzureDevOpsPullRequestDecorator pullRequestDecorator = new AzureDevOpsPullRequestDecorator(scmInfoRepository, azureDevopsClientFactory, reportGenerator, markdownFormatterFactory);

        assertThatThrownBy(() -> pullRequestDecorator.decorateQualityGateStatus(analysisDetails, almSettingDto, projectAlmSettingDto))
            .hasMessage("Could not parse Pull Request Key")
            .isExactlyInstanceOf(IllegalStateException.class);
    }

    @Test
    void shouldRemoveUserInfoFromRepositoryUrlForLinking() {
        AzureDevOpsPullRequestDecorator underTest = new AzureDevOpsPullRequestDecorator(scmInfoRepository, azureDevopsClientFactory, reportGenerator, markdownFormatterFactory);

        Repository repository = mock();
        when(repository.getRemoteUrl()).thenReturn("https://user@domain.com/path/to/repo");
        PullRequest pullRequest = mock();
        when(pullRequest.getRepository()).thenReturn(repository);
        when(pullRequest.getId()).thenReturn(999);

        assertThat(underTest.createFrontEndUrl(pullRequest, analysisDetails)).contains("https://domain.com/path/to/repo/pullRequest/999");
    }


    @Test
    void testName() {
        assertThat(new AzureDevOpsPullRequestDecorator(mock(), mock(), mock(), mock()).alm()).isEqualTo(Collections.singletonList(ALM.AZURE_DEVOPS));
    }

    @Test
    void testDecorateQualityGateRepoNameException() {
        when(almSettingDto.getUrl()).thenReturn("almUrl");
        when(almSettingDto.getDecryptedPersonalAccessToken(any())).thenReturn("personalAccessToken");
        when(analysisDetails.getPullRequestId()).thenReturn("123");
        when(projectAlmSettingDto.getAlmSlug()).thenReturn("prj");

        AzureDevOpsPullRequestDecorator pullRequestDecorator = new AzureDevOpsPullRequestDecorator(scmInfoRepository, azureDevopsClientFactory, reportGenerator, markdownFormatterFactory);

        assertThatThrownBy(() -> pullRequestDecorator.decorateQualityGateStatus(analysisDetails, almSettingDto, projectAlmSettingDto))
            .hasMessage("Repository name must be provided")
            .isExactlyInstanceOf(IllegalStateException.class);
    }

    @Test
    void shouldDeleteSummaryCommentIfNoOtherCommentsInDiscussion() throws IOException {
        String azureProject = "azure-project";
        String azureRepository = "azure-repo";
        int pullRequestId = 321;

        AnalysisSummary analysisSummary = mock();
        when(reportGenerator.createAnalysisSummary(any())).thenReturn(analysisSummary);

        when(analysisDetails.getPullRequestId()).thenReturn(Integer.toString(pullRequestId));
        when(projectAlmSettingDto.getAlmSlug()).thenReturn(azureProject);
        when(projectAlmSettingDto.getAlmRepo()).thenReturn(azureRepository);
        when(projectAlmSettingDto.getInlineAnnotationsEnabled()).thenReturn(true);

        when(analysisDetails.getQualityGateStatus()).thenReturn(QualityGate.Status.OK);

        AzureDevopsClient azureDevopsClient = mock();
        when(azureDevopsClientFactory.createClient(any(), any())).thenReturn(azureDevopsClient);

        PullRequest pullRequest = mock();
        when(pullRequest.getId()).thenReturn(pullRequestId);
        when(azureDevopsClient.retrievePullRequest(any(), any(), anyInt())).thenReturn(pullRequest);
        Repository repository = mock();
        Project project = mock();
        when(pullRequest.getRepository()).thenReturn(repository);
        when(repository.getProject()).thenReturn(project);
        when(project.getName()).thenReturn(azureProject);
        when(repository.getRemoteUrl()).thenReturn("https://remote.url/path/to/repo");
        when(repository.getName()).thenReturn(azureRepository);

        ConnectionData connectionData = mock();
        ConnectionData.Identity authenticatedUser = mock();
        when(authenticatedUser.getId()).thenReturn("sonarqube");
        when(connectionData.getAuthenticatedUser()).thenReturn(authenticatedUser);
        when(azureDevopsClient.getConnectionData()).thenReturn(connectionData);

        AzureDevOpsPullRequestDecorator underTest = new AzureDevOpsPullRequestDecorator(scmInfoRepository, azureDevopsClientFactory, reportGenerator, markdownFormatterFactory);

        IdentityRef sonarqubeUser = mock();
        when(sonarqubeUser.getId()).thenReturn("sonarqube");

        Comment comment1 = mock();
        when(comment1.getId()).thenReturn(999);
        when(comment1.getAuthor()).thenReturn(sonarqubeUser);
        when(comment1.getContent()).thenReturn("Summary comment" + System.lineSeparator() + "[View in SonarQube](http://host.domain/dashboard?id=projectKey&pullRequest=123)");
        when(comment1.getCommentType()).thenReturn(CommentType.TEXT);

        CommentThread discussion = mock();
        when(discussion.getId()).thenReturn(99);
        when(discussion.getComments()).thenReturn(List.of(comment1));

        CommentThread newSummaryThread = mock();
        when(azureDevopsClient.createThread(any(), any(), anyInt(), any())).thenReturn(newSummaryThread);

        when(azureDevopsClient.retrieveThreads(any(), any(), anyInt())).thenReturn(List.of(discussion));

        underTest.decorateQualityGateStatus(analysisDetails, almSettingDto, projectAlmSettingDto);

        verify(azureDevopsClient).deletePullRequestThreadComment(azureProject, azureRepository, pullRequestId, 99, 999);
        verify(azureDevopsClient).retrieveThreads(azureProject, azureRepository, pullRequestId);
    }

    @Test
    void shouldAddNoteToSummaryCommentThreadIfOtherCommentsInDiscussionAndInlineCommentsEnabled() throws IOException {
        String azureProject = "azure-project";
        String azureRepository = "azure-repo";
        int pullRequestId = 321;

        AnalysisSummary analysisSummary = mock();
        when(reportGenerator.createAnalysisSummary(any())).thenReturn(analysisSummary);

        when(analysisDetails.getPullRequestId()).thenReturn(Integer.toString(pullRequestId));
        when(projectAlmSettingDto.getAlmSlug()).thenReturn(azureProject);
        when(projectAlmSettingDto.getAlmRepo()).thenReturn(azureRepository);
        when(projectAlmSettingDto.getInlineAnnotationsEnabled()).thenReturn(true);

        when(analysisDetails.getQualityGateStatus()).thenReturn(QualityGate.Status.OK);

        AzureDevopsClient azureDevopsClient = mock();
        when(azureDevopsClientFactory.createClient(any(), any())).thenReturn(azureDevopsClient);

        PullRequest pullRequest = mock();
        when(pullRequest.getId()).thenReturn(pullRequestId);
        when(azureDevopsClient.retrievePullRequest(any(), any(), anyInt())).thenReturn(pullRequest);
        Repository repository = mock();
        Project project = mock();
        when(pullRequest.getRepository()).thenReturn(repository);
        when(repository.getProject()).thenReturn(project);
        when(project.getName()).thenReturn(azureProject);
        when(repository.getRemoteUrl()).thenReturn("https://remote.url/path/to/repo");
        when(repository.getName()).thenReturn(azureRepository);

        AzureDevOpsPullRequestDecorator underTest = new AzureDevOpsPullRequestDecorator(scmInfoRepository, azureDevopsClientFactory, reportGenerator, markdownFormatterFactory);

        IdentityRef sonarqubeUser = mock();
        when(sonarqubeUser.getId()).thenReturn("sonarqube");

        ConnectionData connectionData = mock();
        ConnectionData.Identity authenticatedUser = mock();
        when(authenticatedUser.getId()).thenReturn("sonarqube");
        when(connectionData.getAuthenticatedUser()).thenReturn(authenticatedUser);
        when(azureDevopsClient.getConnectionData()).thenReturn(connectionData);

        Comment comment1 = mock();
        when(comment1.getId()).thenReturn(101);
        when(comment1.getAuthor()).thenReturn(sonarqubeUser);
        when(comment1.getContent()).thenReturn("Summary comment" + System.lineSeparator() + "[View in SonarQube](http://host.domain/dashboard?id=projectKey&pullRequest=123)");
        when(comment1.getCommentType()).thenReturn(CommentType.TEXT);

        IdentityRef otherUser = mock();
        when(otherUser.getId()).thenReturn("username");
        Comment comment2 = mock();
        when(comment2.getId()).thenReturn(102);
        when(comment2.getAuthor()).thenReturn(otherUser);
        when(comment2.getContent()).thenReturn("Another comment");
        when(comment2.getCommentType()).thenReturn(CommentType.TEXT);

        CommentThread discussion = mock();
        when(discussion.getId()).thenReturn(101);
        when(discussion.getComments()).thenReturn(List.of(comment1, comment2));

        CommentThread newSummaryThread = mock();
        when(azureDevopsClient.createThread(any(), any(), anyInt(), any())).thenReturn(newSummaryThread);

        when(azureDevopsClient.retrieveThreads(any(), any(), anyInt())).thenReturn(List.of(discussion));

        underTest.decorateQualityGateStatus(analysisDetails, almSettingDto, projectAlmSettingDto);

        ArgumentCaptor<CreateCommentRequest> commentRequestArgumentCaptor = ArgumentCaptor.captor();
        verify(azureDevopsClient).addCommentToThread(eq(azureProject), eq(azureRepository), eq(pullRequestId), eq(101), commentRequestArgumentCaptor.capture());
        assertThat(commentRequestArgumentCaptor.getValue()).usingRecursiveComparison().isEqualTo(new CreateCommentRequest("This summary note is outdated, but due to other comments being present in this discussion, the discussion is not being removed. Please manually resolve this discussion once the other comments have been reviewed."));
        verify(azureDevopsClient, never()).deletePullRequestThreadComment(any(), any(), anyInt(), anyInt(), anyInt());
        verify(azureDevopsClient).retrieveThreads(azureProject, azureRepository, pullRequestId);
    }

    @Test
    void shouldNotAddNoteToSummaryCommentThreadIfOtherCommentsInDiscussionAndInlineCommentsEnabledButCloseNoteAlreadyPresent() throws IOException {
        String azureProject = "azure-project";
        String azureRepository = "azure-repo";
        int pullRequestId = 321;

        AnalysisSummary analysisSummary = mock();
        when(reportGenerator.createAnalysisSummary(any())).thenReturn(analysisSummary);

        when(analysisDetails.getPullRequestId()).thenReturn(Integer.toString(pullRequestId));
        when(projectAlmSettingDto.getAlmSlug()).thenReturn(azureProject);
        when(projectAlmSettingDto.getAlmRepo()).thenReturn(azureRepository);
        when(projectAlmSettingDto.getInlineAnnotationsEnabled()).thenReturn(true);

        when(analysisDetails.getQualityGateStatus()).thenReturn(QualityGate.Status.OK);

        AzureDevopsClient azureDevopsClient = mock();
        when(azureDevopsClientFactory.createClient(any(), any())).thenReturn(azureDevopsClient);

        PullRequest pullRequest = mock();
        when(pullRequest.getId()).thenReturn(pullRequestId);
        when(azureDevopsClient.retrievePullRequest(any(), any(), anyInt())).thenReturn(pullRequest);
        Repository repository = mock();
        Project project = mock();
        when(pullRequest.getRepository()).thenReturn(repository);
        when(repository.getProject()).thenReturn(project);
        when(project.getName()).thenReturn(azureProject);
        when(repository.getRemoteUrl()).thenReturn("https://remote.url/path/to/repo");
        when(repository.getName()).thenReturn(azureRepository);

        AzureDevOpsPullRequestDecorator underTest = new AzureDevOpsPullRequestDecorator(scmInfoRepository, azureDevopsClientFactory, reportGenerator, markdownFormatterFactory);

        IdentityRef sonarqubeUser = mock();
        when(sonarqubeUser.getId()).thenReturn("sonarqube");

        ConnectionData connectionData = mock();
        ConnectionData.Identity authenticatedUser = mock();
        when(authenticatedUser.getId()).thenReturn("sonarqube");
        when(connectionData.getAuthenticatedUser()).thenReturn(authenticatedUser);
        when(azureDevopsClient.getConnectionData()).thenReturn(connectionData);

        Comment comment1 = mock();
        when(comment1.getId()).thenReturn(101);
        when(comment1.getAuthor()).thenReturn(sonarqubeUser);
        when(comment1.getContent()).thenReturn("Summary comment" + System.lineSeparator() + "[View in SonarQube](http://host.domain/dashboard?id=projectKey&pullRequest=123)");
        when(comment1.getCommentType()).thenReturn(CommentType.TEXT);

        IdentityRef otherUser = mock();
        when(otherUser.getId()).thenReturn("username");
        Comment comment2 = mock();
        when(comment2.getId()).thenReturn(102);
        when(comment2.getAuthor()).thenReturn(otherUser);
        when(comment2.getContent()).thenReturn("Another comment");
        when(comment2.getCommentType()).thenReturn(CommentType.TEXT);

        Comment comment3 = mock();
        when(comment3.getId()).thenReturn(101);
        when(comment3.getAuthor()).thenReturn(sonarqubeUser);
        when(comment3.getContent()).thenReturn("This summary note is outdated, but due to other comments being present in this discussion, the discussion is not being removed. Please manually resolve this discussion once the other comments have been reviewed.");
        when(comment3.getCommentType()).thenReturn(CommentType.TEXT);


        CommentThread discussion = mock();
        when(discussion.getId()).thenReturn(101);
        when(discussion.getComments()).thenReturn(List.of(comment1, comment2, comment3));

        CommentThread newSummaryThread = mock();
        when(azureDevopsClient.createThread(any(), any(), anyInt(), any())).thenReturn(newSummaryThread);

        when(azureDevopsClient.retrieveThreads(any(), any(), anyInt())).thenReturn(List.of(discussion));

        underTest.decorateQualityGateStatus(analysisDetails, almSettingDto, projectAlmSettingDto);

        verify(azureDevopsClient, never()).addCommentToThread(any(), any(), anyInt(), anyInt(), any());
        verify(azureDevopsClient, never()).deletePullRequestThreadComment(any(), any(), anyInt(), anyInt(), anyInt());
        verify(azureDevopsClient).retrieveThreads(azureProject, azureRepository, pullRequestId);
    }

    @Test
    void shouldNotAddNoteToSummaryCommentThreadIfOtherCommentsInDiscussionAndInlineCommentsNotEnabled() throws IOException {
        String azureProject = "azure-project";
        String azureRepository = "azure-repo";
        int pullRequestId = 321;

        AnalysisSummary analysisSummary = mock();
        when(reportGenerator.createAnalysisSummary(any())).thenReturn(analysisSummary);

        when(analysisDetails.getPullRequestId()).thenReturn(Integer.toString(pullRequestId));
        when(projectAlmSettingDto.getAlmSlug()).thenReturn(azureProject);
        when(projectAlmSettingDto.getAlmRepo()).thenReturn(azureRepository);
        when(projectAlmSettingDto.getInlineAnnotationsEnabled()).thenReturn(false);

        when(analysisDetails.getQualityGateStatus()).thenReturn(QualityGate.Status.OK);

        AzureDevopsClient azureDevopsClient = mock();
        when(azureDevopsClientFactory.createClient(any(), any())).thenReturn(azureDevopsClient);

        PullRequest pullRequest = mock();
        when(pullRequest.getId()).thenReturn(pullRequestId);
        when(azureDevopsClient.retrievePullRequest(any(), any(), anyInt())).thenReturn(pullRequest);
        Repository repository = mock();
        Project project = mock();
        when(pullRequest.getRepository()).thenReturn(repository);
        when(repository.getProject()).thenReturn(project);
        when(project.getName()).thenReturn(azureProject);
        when(repository.getRemoteUrl()).thenReturn("https://remote.url/path/to/repo");
        when(repository.getName()).thenReturn(azureRepository);

        AzureDevOpsPullRequestDecorator underTest = new AzureDevOpsPullRequestDecorator(scmInfoRepository, azureDevopsClientFactory, reportGenerator, markdownFormatterFactory);

        IdentityRef sonarqubeUser = mock();
        when(sonarqubeUser.getId()).thenReturn("sonarqube");

        ConnectionData connectionData = mock();
        ConnectionData.Identity authenticatedUser = mock();
        when(authenticatedUser.getId()).thenReturn("sonarqube");
        when(connectionData.getAuthenticatedUser()).thenReturn(authenticatedUser);
        when(azureDevopsClient.getConnectionData()).thenReturn(connectionData);

        Comment comment1 = mock();
        when(comment1.getId()).thenReturn(101);
        when(comment1.getAuthor()).thenReturn(sonarqubeUser);
        when(comment1.getContent()).thenReturn("Summary comment" + System.lineSeparator() + "[View in SonarQube](http://host.domain/dashboard?id=projectKey&pullRequest=123)");
        when(comment1.getCommentType()).thenReturn(CommentType.TEXT);

        IdentityRef otherUser = mock();
        when(otherUser.getId()).thenReturn("username");
        Comment comment2 = mock();
        when(comment2.getId()).thenReturn(102);
        when(comment2.getAuthor()).thenReturn(otherUser);
        when(comment2.getContent()).thenReturn("Another comment");
        when(comment2.getCommentType()).thenReturn(CommentType.TEXT);

        CommentThread discussion = mock();
        when(discussion.getId()).thenReturn(101);
        when(discussion.getComments()).thenReturn(List.of(comment1, comment2));

        CommentThread newSummaryThread = mock();
        when(azureDevopsClient.createThread(any(), any(), anyInt(), any())).thenReturn(newSummaryThread);

        when(azureDevopsClient.retrieveThreads(any(), any(), anyInt())).thenReturn(List.of(discussion));

        underTest.decorateQualityGateStatus(analysisDetails, almSettingDto, projectAlmSettingDto);

        verify(azureDevopsClient, never()).addCommentToThread(any(), any(), anyInt(), anyInt(), any());
        verify(azureDevopsClient, never()).deletePullRequestThreadComment(any(), any(), anyInt(), anyInt(), anyInt());
        verify(azureDevopsClient, never()).retrieveThreads(any(), any(), anyInt());
    }

    @Test
    void shouldNotTryAndCleanupNonSummaryNote() throws IOException {
        String azureProject = "azure-project";
        String azureRepository = "azure-repo";
        int pullRequestId = 321;

        AnalysisSummary analysisSummary = mock();
        when(reportGenerator.createAnalysisSummary(any())).thenReturn(analysisSummary);

        when(analysisDetails.getPullRequestId()).thenReturn(Integer.toString(pullRequestId));
        when(projectAlmSettingDto.getAlmSlug()).thenReturn(azureProject);
        when(projectAlmSettingDto.getAlmRepo()).thenReturn(azureRepository);
        when(projectAlmSettingDto.getInlineAnnotationsEnabled()).thenReturn(true);

        when(analysisDetails.getQualityGateStatus()).thenReturn(QualityGate.Status.ERROR);

        AzureDevopsClient azureDevopsClient = mock();
        when(azureDevopsClientFactory.createClient(any(), any())).thenReturn(azureDevopsClient);

        when(azureDevopsClient.getConnectionData()).thenThrow(new IOException("Dummy"));

        PullRequest pullRequest = mock();
        when(pullRequest.getId()).thenReturn(pullRequestId);
        when(azureDevopsClient.retrievePullRequest(any(), any(), anyInt())).thenReturn(pullRequest);
        Repository repository = mock();
        Project project = mock();
        when(pullRequest.getRepository()).thenReturn(repository);
        when(repository.getProject()).thenReturn(project);
        when(project.getName()).thenReturn(azureProject);
        when(repository.getRemoteUrl()).thenReturn("https://remote.url/path/to/repo");
        when(repository.getName()).thenReturn(azureRepository);

        AzureDevOpsPullRequestDecorator underTest = new AzureDevOpsPullRequestDecorator(scmInfoRepository, azureDevopsClientFactory, reportGenerator, markdownFormatterFactory);

        IdentityRef sonarqubeUser = mock();
        when(sonarqubeUser.getId()).thenReturn("sonarqube");

        Comment comment1 = mock();
        when(comment1.getId()).thenReturn(101);
        when(comment1.getAuthor()).thenReturn(sonarqubeUser);
        when(comment1.getContent()).thenReturn("Not Summary comment" + System.lineSeparator() + "[Don't View in SonarQube](http://host.domain/dashboard?id=projectKey&pullRequest=123)");
        when(comment1.getCommentType()).thenReturn(CommentType.TEXT);

        IdentityRef otherUser = mock();
        when(otherUser.getId()).thenReturn("username");
        Comment comment2 = mock();
        when(comment2.getId()).thenReturn(102);
        when(comment2.getAuthor()).thenReturn(otherUser);
        when(comment2.getContent()).thenReturn("Another comment");
        when(comment2.getCommentType()).thenReturn(CommentType.TEXT);

        CommentThread discussion = mock();
        when(discussion.getId()).thenReturn(101);
        when(discussion.getComments()).thenReturn(List.of(comment1, comment2));

        when(azureDevopsClient.retrieveThreads(any(), any(), anyInt())).thenReturn(List.of(discussion));

        underTest.decorateQualityGateStatus(analysisDetails, almSettingDto, projectAlmSettingDto);

        verify(azureDevopsClient, never()).addCommentToThread(any(), any(), anyInt(), anyInt(), any());
        verify(azureDevopsClient, never()).deletePullRequestThreadComment(any(), any(), anyInt(), anyInt(), anyInt());
        verify(azureDevopsClient).retrieveThreads(azureProject, azureRepository, pullRequestId);
    }

    @Test
    void shouldSubmitPullRequestStatusWithIterationIdForCommit() throws IOException {
        String azureProject = "azure-project";
        String azureRepository = "azure-repo";
        int pullRequestId = 321;
        int iterationId = 2;
        String commitSha = "abc123def456";

        when(analysisDetails.getPullRequestId()).thenReturn(Integer.toString(pullRequestId));
        when(analysisDetails.getQualityGateStatus()).thenReturn(QualityGate.Status.OK);
        when(analysisDetails.getCommitSha()).thenReturn(commitSha);
        when(projectAlmSettingDto.getAlmSlug()).thenReturn(azureProject);
        when(projectAlmSettingDto.getAlmRepo()).thenReturn(azureRepository);
        when(projectAlmSettingDto.getInlineAnnotationsEnabled()).thenReturn(false);

        AnalysisSummary analysisSummary = mock();
        when(analysisSummary.getDashboardUrl()).thenReturn("http://sonar/dashboard");
        when(reportGenerator.createAnalysisSummary(any())).thenReturn(analysisSummary);

        AzureDevopsClient azureDevopsClient = mock();
        when(azureDevopsClientFactory.createClient(any(), any())).thenReturn(azureDevopsClient);
        mockPullRequest(azureDevopsClient, azureProject, azureRepository, pullRequestId);

        when(azureDevopsClient.retrievePullRequestIterationIdForCommit(azureProject, azureRepository, pullRequestId, commitSha)).thenReturn(iterationId);
        when(azureDevopsClient.createThread(any(), any(), anyInt(), any())).thenReturn(mock());

        AzureDevOpsPullRequestDecorator underTest = new AzureDevOpsPullRequestDecorator(scmInfoRepository, azureDevopsClientFactory, reportGenerator, markdownFormatterFactory);
        underTest.decorateQualityGateStatus(analysisDetails, almSettingDto, projectAlmSettingDto);

        ArgumentCaptor<GitPullRequestStatus> statusCaptor = ArgumentCaptor.captor();
        verify(azureDevopsClient).submitPullRequestStatus(eq(azureProject), eq(azureRepository), eq(pullRequestId), statusCaptor.capture());
        assertThat(statusCaptor.getValue().getIterationId()).isEqualTo(iterationId);
    }

    @Test
    void shouldThrowIfIterationIdForCommitCallFails() throws IOException {
        String azureProject = "azure-project";
        String azureRepository = "azure-repo";
        int pullRequestId = 321;

        when(analysisDetails.getPullRequestId()).thenReturn(Integer.toString(pullRequestId));
        when(analysisDetails.getQualityGateStatus()).thenReturn(QualityGate.Status.OK);
        when(analysisDetails.getCommitSha()).thenReturn("abc123");
        when(projectAlmSettingDto.getAlmSlug()).thenReturn(azureProject);
        when(projectAlmSettingDto.getAlmRepo()).thenReturn(azureRepository);
        when(projectAlmSettingDto.getInlineAnnotationsEnabled()).thenReturn(false);

        AnalysisSummary analysisSummary = mock();
        when(analysisSummary.getDashboardUrl()).thenReturn("http://sonar/dashboard");
        when(reportGenerator.createAnalysisSummary(any())).thenReturn(analysisSummary);

        AzureDevopsClient azureDevopsClient = mock();
        when(azureDevopsClientFactory.createClient(any(), any())).thenReturn(azureDevopsClient);
        mockPullRequest(azureDevopsClient, azureProject, azureRepository, pullRequestId);

        when(azureDevopsClient.retrievePullRequestIterationIdForCommit(any(), any(), anyInt(), any())).thenThrow(new IOException("403 Forbidden"));
        when(azureDevopsClient.createThread(any(), any(), anyInt(), any())).thenReturn(mock());

        AzureDevOpsPullRequestDecorator underTest = new AzureDevOpsPullRequestDecorator(scmInfoRepository, azureDevopsClientFactory, reportGenerator, markdownFormatterFactory);

        assertThatThrownBy(() -> underTest.decorateQualityGateStatus(analysisDetails, almSettingDto, projectAlmSettingDto))
                .isExactlyInstanceOf(IllegalStateException.class)
                .hasMessage("Could not update pipeline status in Gitlab");

        verify(azureDevopsClient, never()).submitPullRequestStatus(any(), any(), anyInt(), any());
    }

    @Test
    void shouldUseIterationIdOneWhenPullRequestDoesNotSupportIterations() throws IOException {
        String azureProject = "azure-project";
        String azureRepository = "azure-repo";
        int pullRequestId = 321;

        when(analysisDetails.getPullRequestId()).thenReturn(Integer.toString(pullRequestId));
        when(analysisDetails.getQualityGateStatus()).thenReturn(QualityGate.Status.OK);
        when(projectAlmSettingDto.getAlmSlug()).thenReturn(azureProject);
        when(projectAlmSettingDto.getAlmRepo()).thenReturn(azureRepository);
        when(projectAlmSettingDto.getInlineAnnotationsEnabled()).thenReturn(false);

        AnalysisSummary analysisSummary = mock();
        when(analysisSummary.getDashboardUrl()).thenReturn("http://sonar/dashboard");
        when(reportGenerator.createAnalysisSummary(any())).thenReturn(analysisSummary);

        AzureDevopsClient azureDevopsClient = mock();
        when(azureDevopsClientFactory.createClient(any(), any())).thenReturn(azureDevopsClient);
        PullRequest pullRequest = mockPullRequest(azureDevopsClient, azureProject, azureRepository, pullRequestId);
        when(pullRequest.doesSupportIterations()).thenReturn(false);
        when(azureDevopsClient.createThread(any(), any(), anyInt(), any())).thenReturn(mock());

        AzureDevOpsPullRequestDecorator underTest = new AzureDevOpsPullRequestDecorator(scmInfoRepository, azureDevopsClientFactory, reportGenerator, markdownFormatterFactory);
        underTest.decorateQualityGateStatus(analysisDetails, almSettingDto, projectAlmSettingDto);

        ArgumentCaptor<GitPullRequestStatus> statusCaptor = ArgumentCaptor.captor();
        verify(azureDevopsClient).submitPullRequestStatus(eq(azureProject), eq(azureRepository), eq(pullRequestId), statusCaptor.capture());
        assertThat(statusCaptor.getValue().getIterationId()).isEqualTo(1);
        verify(azureDevopsClient, never()).retrievePullRequestIterationIdForCommit(any(), any(), anyInt(), any());
    }

    @Test
    void shouldUseGenericContextNameWhenNotMonorepo() throws IOException {
        String azureProject = "azure-project";
        String azureRepository = "azure-repo";
        int pullRequestId = 321;

        when(analysisDetails.getPullRequestId()).thenReturn(Integer.toString(pullRequestId));
        when(analysisDetails.getQualityGateStatus()).thenReturn(QualityGate.Status.OK);
        when(projectAlmSettingDto.getAlmSlug()).thenReturn(azureProject);
        when(projectAlmSettingDto.getAlmRepo()).thenReturn(azureRepository);
        when(projectAlmSettingDto.getInlineAnnotationsEnabled()).thenReturn(false);
        when(projectAlmSettingDto.getMonorepo()).thenReturn(false);

        AnalysisSummary analysisSummary = mock();
        when(analysisSummary.getDashboardUrl()).thenReturn("http://sonar/dashboard");
        when(reportGenerator.createAnalysisSummary(any())).thenReturn(analysisSummary);

        AzureDevopsClient azureDevopsClient = mock();
        when(azureDevopsClientFactory.createClient(any(), any())).thenReturn(azureDevopsClient);
        PullRequest pullRequest = mockPullRequest(azureDevopsClient, azureProject, azureRepository, pullRequestId);
        when(pullRequest.doesSupportIterations()).thenReturn(false);
        when(azureDevopsClient.createThread(any(), any(), anyInt(), any())).thenReturn(mock());

        AzureDevOpsPullRequestDecorator underTest = new AzureDevOpsPullRequestDecorator(scmInfoRepository, azureDevopsClientFactory, reportGenerator, markdownFormatterFactory);
        underTest.decorateQualityGateStatus(analysisDetails, almSettingDto, projectAlmSettingDto);

        ArgumentCaptor<GitPullRequestStatus> statusCaptor = ArgumentCaptor.captor();
        verify(azureDevopsClient).submitPullRequestStatus(eq(azureProject), eq(azureRepository), eq(pullRequestId), statusCaptor.capture());
        GitStatusContext context = statusCaptor.getValue().getContext();
        assertThat(context.getGenre()).isEqualTo("SonarQube");
        assertThat(context.getName()).isEqualTo("quality gate");
    }

    @Test
    void shouldIncludeProjectKeyInContextNameWhenMonorepo() throws IOException {
        String azureProject = "azure-project";
        String azureRepository = "azure-repo";
        int pullRequestId = 321;

        when(analysisDetails.getPullRequestId()).thenReturn(Integer.toString(pullRequestId));
        when(analysisDetails.getQualityGateStatus()).thenReturn(QualityGate.Status.OK);
        when(analysisDetails.getAnalysisProjectKey()).thenReturn("monorepo-project-fe");
        when(projectAlmSettingDto.getAlmSlug()).thenReturn(azureProject);
        when(projectAlmSettingDto.getAlmRepo()).thenReturn(azureRepository);
        when(projectAlmSettingDto.getInlineAnnotationsEnabled()).thenReturn(false);
        when(projectAlmSettingDto.getMonorepo()).thenReturn(true);

        AnalysisSummary analysisSummary = mock();
        when(analysisSummary.getDashboardUrl()).thenReturn("http://sonar/dashboard");
        when(reportGenerator.createAnalysisSummary(any())).thenReturn(analysisSummary);

        AzureDevopsClient azureDevopsClient = mock();
        when(azureDevopsClientFactory.createClient(any(), any())).thenReturn(azureDevopsClient);
        PullRequest pullRequest = mockPullRequest(azureDevopsClient, azureProject, azureRepository, pullRequestId);
        when(pullRequest.doesSupportIterations()).thenReturn(false);
        when(azureDevopsClient.createThread(any(), any(), anyInt(), any())).thenReturn(mock());

        AzureDevOpsPullRequestDecorator underTest = new AzureDevOpsPullRequestDecorator(scmInfoRepository, azureDevopsClientFactory, reportGenerator, markdownFormatterFactory);
        underTest.decorateQualityGateStatus(analysisDetails, almSettingDto, projectAlmSettingDto);

        ArgumentCaptor<GitPullRequestStatus> statusCaptor = ArgumentCaptor.captor();
        verify(azureDevopsClient).submitPullRequestStatus(eq(azureProject), eq(azureRepository), eq(pullRequestId), statusCaptor.capture());
        GitStatusContext context = statusCaptor.getValue().getContext();
        assertThat(context.getGenre()).isEqualTo("SonarQube");
        assertThat(context.getName()).isEqualTo("quality gate - monorepo-project-fe");
    }

    static Stream<Exception> clientFailures() {
        return Stream.of(new IOException("io failure"), new JacksonException("json failure") {});
    }

    private AzureDevOpsPullRequestDecorator decorator() {
        return new AzureDevOpsPullRequestDecorator(scmInfoRepository, azureDevopsClientFactory, reportGenerator, markdownFormatterFactory);
    }

    @ParameterizedTest
    @MethodSource("clientFailures")
    void shouldWrapFailureRetrievingPullRequest(Exception failure) throws IOException {
        AzureDevopsClient client = mock();
        when(analysisDetails.getPullRequestId()).thenReturn("123");
        when(projectAlmSettingDto.getAlmSlug()).thenReturn("project");
        when(projectAlmSettingDto.getAlmRepo()).thenReturn("repo");
        when(client.retrievePullRequest("project", "repo", 123)).thenThrow(failure);

        AzureDevOpsPullRequestDecorator underTest = decorator();
        assertThatThrownBy(() -> underTest.getPullRequest(client, almSettingDto, projectAlmSettingDto, analysisDetails))
                .isExactlyInstanceOf(IllegalStateException.class)
                .hasMessage("Could not retrieve Pull Request details")
                .hasCause(failure);
    }

    @ParameterizedTest
    @MethodSource("clientFailures")
    void shouldWrapFailureRetrievingPullRequestCommits(Exception failure) throws IOException {
        AzureDevopsClient client = mock();
        PullRequest pullRequest = mockPullRequest(client, "project", "repo", 123);
        when(client.getPullRequestCommits("project", "repo", 123)).thenThrow(failure);

        AzureDevOpsPullRequestDecorator underTest = decorator();
        assertThatThrownBy(() -> underTest.getCommitIdsForPullRequest(client, pullRequest))
                .isExactlyInstanceOf(IllegalStateException.class)
                .hasMessage("Could not retrieve commit details for Pull Request")
                .hasCause(failure);
    }

    @ParameterizedTest
    @MethodSource("clientFailures")
    void shouldWrapFailureSubmittingCommitNote(Exception failure) throws IOException {
        AzureDevopsClient client = mock();
        PullRequest pullRequest = mockPullRequest(client, "project", "repo", 123);
        when(client.createThread(any(), any(), anyInt(), any())).thenThrow(failure);

        PostAnalysisIssueVisitor.LightIssue lightIssue = mock();
        when(lightIssue.getLocations()).thenReturn(DbIssues.Locations.newBuilder()
                .setTextRange(DbCommons.TextRange.newBuilder().setStartLine(1).setStartOffset(2).setEndLine(3).setEndOffset(4))
                .build());
        PostAnalysisIssueVisitor.ComponentIssue componentIssue = mock();
        when(componentIssue.getIssue()).thenReturn(lightIssue);
        AnalysisIssueSummary analysisIssueSummary = mock();
        when(analysisIssueSummary.format(any())).thenReturn("issue summary");

        AzureDevOpsPullRequestDecorator underTest = decorator();
        assertThatThrownBy(() -> underTest.submitCommitNoteForIssue(client, pullRequest, componentIssue, "src/File.java", analysisDetails, analysisIssueSummary))
                .isExactlyInstanceOf(IllegalStateException.class)
                .hasMessage("Could not submit commit comment to Azure Devops")
                .hasCause(failure);
    }

    @ParameterizedTest
    @MethodSource("clientFailures")
    void shouldWrapFailureSubmittingSummaryNote(Exception failure) throws IOException {
        AzureDevopsClient client = mock();
        PullRequest pullRequest = mockPullRequest(client, "project", "repo", 123);
        when(client.createThread(any(), any(), anyInt(), any())).thenThrow(failure);
        AnalysisSummary analysisSummary = mock();
        when(analysisSummary.format(any())).thenReturn("summary");

        AzureDevOpsPullRequestDecorator underTest = decorator();
        assertThatThrownBy(() -> underTest.submitSummaryNote(client, pullRequest, analysisDetails, analysisSummary))
                .isExactlyInstanceOf(IllegalStateException.class)
                .hasMessage("Could not submit summary comment to Azure Devops")
                .hasCause(failure);
    }

    @ParameterizedTest
    @MethodSource("clientFailures")
    void shouldWrapFailureRetrievingDiscussions(Exception failure) throws IOException {
        AzureDevopsClient client = mock();
        PullRequest pullRequest = mockPullRequest(client, "project", "repo", 123);
        when(client.retrieveThreads("project", "repo", 123)).thenThrow(failure);

        AzureDevOpsPullRequestDecorator underTest = decorator();
        assertThatThrownBy(() -> underTest.getDiscussions(client, pullRequest))
                .isExactlyInstanceOf(IllegalStateException.class)
                .hasMessage("Could not retrieve discussions from Azure Devops")
                .hasCause(failure);
    }

    @ParameterizedTest
    @MethodSource("clientFailures")
    void shouldWrapFailureAddingNoteToDiscussion(Exception failure) throws IOException {
        AzureDevopsClient client = mock();
        PullRequest pullRequest = mockPullRequest(client, "project", "repo", 123);
        CommentThread discussion = mock();
        when(discussion.getId()).thenReturn(7);
        doThrow(failure).when(client).addCommentToThread(eq("project"), eq("repo"), eq(123), eq(7), any());

        AzureDevOpsPullRequestDecorator underTest = decorator();
        assertThatThrownBy(() -> underTest.addNoteToDiscussion(client, discussion, pullRequest, "note"))
                .isExactlyInstanceOf(IllegalStateException.class)
                .hasMessage("Could not add note to Pull Request comment thread on Azure Devops")
                .hasCause(failure);
    }

    @ParameterizedTest
    @MethodSource("clientFailures")
    void shouldWrapFailureResolvingDiscussion(Exception failure) throws IOException {
        AzureDevopsClient client = mock();
        PullRequest pullRequest = mockPullRequest(client, "project", "repo", 123);
        CommentThread discussion = mock();
        when(discussion.getId()).thenReturn(7);
        doThrow(failure).when(client).resolvePullRequestThread("project", "repo", 123, 7);

        AzureDevOpsPullRequestDecorator underTest = decorator();
        assertThatThrownBy(() -> underTest.resolveDiscussion(client, discussion, pullRequest))
                .isExactlyInstanceOf(IllegalStateException.class)
                .hasMessage("Could not resolve Pull Request comment thread on Azure Devops")
                .hasCause(failure);
    }

    @ParameterizedTest
    @MethodSource("clientFailures")
    void shouldWrapFailureDeletingDiscussion(Exception failure) throws IOException {
        AzureDevopsClient client = mock();
        PullRequest pullRequest = mockPullRequest(client, "project", "repo", 123);
        CommentThread discussion = mock();
        when(discussion.getId()).thenReturn(7);
        Comment note = mock();
        when(note.getId()).thenReturn(9);
        doThrow(failure).when(client).deletePullRequestThreadComment("project", "repo", 123, 7, 9);

        AzureDevOpsPullRequestDecorator underTest = decorator();
        List<Comment> notes = List.of(note);
        assertThatThrownBy(() -> underTest.deleteDiscussion(client, discussion, pullRequest, notes))
                .isExactlyInstanceOf(IllegalStateException.class)
                .hasMessage("Could not delete Pull Request comment thread on Azure Devops")
                .hasCause(failure);
    }
}
