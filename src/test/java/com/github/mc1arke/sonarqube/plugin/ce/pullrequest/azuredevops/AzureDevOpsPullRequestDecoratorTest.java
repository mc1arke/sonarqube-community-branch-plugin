/*
 * Copyright (C) 2024 Michael Clarke
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.sonar.api.ce.posttask.QualityGate;
import org.sonar.ce.task.projectanalysis.scm.ScmInfoRepository;
import org.sonar.db.alm.setting.ALM;
import org.sonar.db.alm.setting.AlmSettingDto;
import org.sonar.db.alm.setting.ProjectAlmSettingDto;

import com.github.mc1arke.sonarqube.plugin.almclient.azuredevops.AzureDevopsClient;
import com.github.mc1arke.sonarqube.plugin.almclient.azuredevops.AzureDevopsClientFactory;
import com.github.mc1arke.sonarqube.plugin.almclient.azuredevops.model.Comment;
import com.github.mc1arke.sonarqube.plugin.almclient.azuredevops.model.CommentThread;
import com.github.mc1arke.sonarqube.plugin.almclient.azuredevops.model.ConnectionData;
import com.github.mc1arke.sonarqube.plugin.almclient.azuredevops.model.CreateCommentRequest;
import com.github.mc1arke.sonarqube.plugin.almclient.azuredevops.model.IdentityRef;
import com.github.mc1arke.sonarqube.plugin.almclient.azuredevops.model.Project;
import com.github.mc1arke.sonarqube.plugin.almclient.azuredevops.model.PullRequest;
import com.github.mc1arke.sonarqube.plugin.almclient.azuredevops.model.Repository;
import com.github.mc1arke.sonarqube.plugin.almclient.azuredevops.model.enums.CommentType;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.AnalysisDetails;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.markup.MarkdownFormatterFactory;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.report.AnalysisSummary;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.report.ReportGenerator;

class AzureDevOpsPullRequestDecoratorTest {

    private final AlmSettingDto almSettingDto = mock();
    private final ProjectAlmSettingDto projectAlmSettingDto = mock();
    private final AnalysisDetails analysisDetails = mock();
    private final ScmInfoRepository scmInfoRepository = mock();
    private final AzureDevopsClientFactory azureDevopsClientFactory = mock();
    private final ReportGenerator reportGenerator = mock();
    private final MarkdownFormatterFactory markdownFormatterFactory = mock();


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
    void shouldAddNoteToSummaryCommentThreadIfOtherCommentsInDiscussion() throws IOException {
        String azureProject = "azure-project";
        String azureRepository = "azure-repo";
        int pullRequestId = 321;

        AnalysisSummary analysisSummary = mock();
        when(reportGenerator.createAnalysisSummary(any())).thenReturn(analysisSummary);

        when(analysisDetails.getPullRequestId()).thenReturn(Integer.toString(pullRequestId));
        when(projectAlmSettingDto.getAlmSlug()).thenReturn(azureProject);
        when(projectAlmSettingDto.getAlmRepo()).thenReturn(azureRepository);

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
        assertThat(commentRequestArgumentCaptor.getValue()).usingRecursiveComparison().isEqualTo(new CreateCommentRequest("This summary note is outdated, but due to other comments being present in this discussion, the discussion is not being being removed. Please manually resolve this discussion once the other comments have been reviewed."));
        verify(azureDevopsClient, never()).deletePullRequestThreadComment(any(), any(), anyInt(), anyInt(), anyInt());
        verify(azureDevopsClient).retrieveThreads(azureProject, azureRepository, pullRequestId);
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

}
