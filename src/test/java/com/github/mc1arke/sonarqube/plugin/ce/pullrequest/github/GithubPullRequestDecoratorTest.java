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
package com.github.mc1arke.sonarqube.plugin.ce.pullrequest.github;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.URL;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.kohsuke.github.GHCheckRun;
import org.kohsuke.github.GHCheckRunBuilder;
import org.kohsuke.github.GHIssueComment;
import org.kohsuke.github.GHPullRequest;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GHUser;
import org.kohsuke.github.GitHub;
import org.mockito.ArgumentCaptor;
import org.mockito.invocation.InvocationOnMock;
import org.sonar.api.ce.posttask.QualityGate;
import org.sonar.api.issue.impact.Severity;
import org.sonar.api.issue.impact.SoftwareQuality;
import org.sonar.ce.task.projectanalysis.component.Component;
import org.sonar.db.alm.setting.ALM;
import org.sonar.db.alm.setting.AlmSettingDto;
import org.sonar.db.alm.setting.ProjectAlmSettingDto;

import com.github.mc1arke.sonarqube.plugin.almclient.github.GithubClientFactory;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.AnalysisDetails;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.DecorationResult;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.PostAnalysisIssueVisitor;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.markup.Document;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.markup.Formatter;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.markup.MarkdownFormatterFactory;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.report.AnalysisSummary;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.report.ReportGenerator;

class GithubPullRequestDecoratorTest {

    private final AnalysisDetails analysisDetails = mock();
    private final GithubClientFactory githubClientFactory = mock();
    private final ReportGenerator reportGenerator = mock();
    private final MarkdownFormatterFactory markdownFormatterFactory = mock();
    private final Clock clock = Clock.fixed(Instant.ofEpochSecond(102030405), ZoneId.of("UTC"));
    private final GithubPullRequestDecorator testCase = new GithubPullRequestDecorator(githubClientFactory, reportGenerator, markdownFormatterFactory, clock);
    private final ProjectAlmSettingDto projectAlmSettingDto = mock();
    private final AlmSettingDto almSettingDto = mock();
    private final AnalysisSummary analysisSummary = mock();
    private final GitHub gitHub = mock();

    @BeforeEach
    void setUp() throws IOException {
        when(projectAlmSettingDto.getAlmRepo()).thenReturn("alm-repo");
        when(projectAlmSettingDto.getMonorepo()).thenReturn(false);
        when(analysisDetails.getPullRequestId()).thenReturn("123");
        when(analysisDetails.getAnalysisDate()).thenReturn(Date.from(clock.instant()));
        when(analysisDetails.getAnalysisId()).thenReturn("analysis-id");
        when(analysisDetails.getAnalysisProjectKey()).thenReturn("project-key");
        when(analysisDetails.getAnalysisProjectName()).thenReturn("Project Name");
        when(analysisDetails.getQualityGateStatus()).thenReturn(QualityGate.Status.OK);
        when(analysisDetails.getCommitSha()).thenReturn("commit-sha");
        List<PostAnalysisIssueVisitor.ComponentIssue> reportableIssues = IntStream.range(0, 20).mapToObj(i -> {
            PostAnalysisIssueVisitor.ComponentIssue componentIssue = mock();
            Component component = mock();
            when(componentIssue.getScmPath()).thenReturn(Optional.of("path" + i));
            when(componentIssue.getComponent()).thenReturn(component);
            PostAnalysisIssueVisitor.LightIssue lightIssue = mock();
            when(lightIssue.getMessage()).thenReturn("issue message " + i);
            when(lightIssue.getLine()).thenReturn(i);
            when(lightIssue.impacts()).thenReturn(Map.of(SoftwareQuality.values()[i % SoftwareQuality.values().length], Severity.values()[i % Severity.values().length]));
            when(componentIssue.getIssue()).thenReturn(lightIssue);
            return componentIssue;
        }).collect(Collectors.toList());
        when(analysisDetails.getScmReportableIssues()).thenReturn(reportableIssues);

        when(reportGenerator.createAnalysisSummary(any())).thenReturn(analysisSummary);
        when(analysisSummary.getDashboardUrl()).thenReturn("dashboard-url");
        when(analysisSummary.format(any())).thenReturn("report summary");
        when(githubClientFactory.createClient(any(), any())).thenReturn(gitHub);
    }

    @Test
    void shouldReturnCorrectAlms() {
        assertThat(testCase.alm()).isEqualTo(List.of(ALM.GITHUB));
    }

    @Test
    void shouldThrowExceptionIfClientCreationFails() throws IOException {
        Exception dummyException = new IOException("Dummy Exception");
        when(githubClientFactory.createClient(any(), any())).thenThrow(dummyException);

        assertThatThrownBy(() -> testCase.decorateQualityGateStatus(analysisDetails, almSettingDto, projectAlmSettingDto))
                .hasMessage("Could not decorate Pull Request on Github")
                .isExactlyInstanceOf(IllegalStateException.class).hasCause(dummyException);
    }

    @Test
    void shouldDecoratePullRequestWithCorrectAnalysisAndSummaryCommentWhenEnabled() throws IOException {
        when(projectAlmSettingDto.getSummaryCommentEnabled()).thenReturn(true);
        GHRepository repository = mock();
        when(gitHub.getRepository(any())).thenReturn(repository);
        GHCheckRunBuilder checkRunBuilder = mock(InvocationOnMock::getMock);
        doReturn(null).when(checkRunBuilder).create();
        when(repository.createCheckRun(any(), any())).thenReturn(checkRunBuilder);
        GHPullRequest pullRequest = mock();
        GHIssueComment comment1 = createComment("summary comment from current bot user, no project ID", "Bot", 123, 1);
        GHIssueComment comment2 = createComment("summary comment from non bot user with no project ID", "User", 321, 2);
        GHIssueComment comment3 = createComment("summary comment from current bot user, with project ID. **Project ID:** project-key\n", "Bot", 123, 3);
        GHIssueComment comment4 = createComment("summary comment from other bot user, with project ID. **Project ID:** project-key\n", "Bot", 999, 4);
        GHIssueComment comment5 = createComment("summary comment from other bot user, with project ID. **Project ID:** project-key\n", "User", 111, 5);
        GHIssueComment summaryComment = createComment("summary comment from current bot user, with project ID. **Project ID:** project-key\r", "Bot", 123, 6);
        when(pullRequest.getComments()).thenReturn(List.of(comment1, comment2, comment3, comment4, comment5, summaryComment));
        when(pullRequest.comment(any(String.class))).thenReturn(summaryComment);
        when(repository.getPullRequest(anyInt())).thenReturn(pullRequest);
        when(pullRequest.getHtmlUrl()).thenReturn(new URL("http://url.of/pull/request"));
        Formatter<Document> documentFormatter = mock();
        when(documentFormatter.format(any())).thenReturn("**Project ID:** project-key");
        when(markdownFormatterFactory.documentFormatter()).thenReturn(documentFormatter);
        DecorationResult decorationResult = testCase.decorateQualityGateStatus(analysisDetails, almSettingDto, projectAlmSettingDto);

        verify(gitHub).getRepository("alm-repo");
        verify(repository).createCheckRun("SonarQube Code Analysis", "commit-sha");

        ArgumentCaptor<GHCheckRunBuilder.Output> outputCaptor = ArgumentCaptor.captor();

        verify(checkRunBuilder).add(outputCaptor.capture());
        verify(checkRunBuilder).withExternalID("analysis-id");
        verify(checkRunBuilder).withStartedAt(Date.from(clock.instant()));
        verify(checkRunBuilder).withCompletedAt(Date.from(clock.instant()));
        verify(checkRunBuilder).withDetailsURL("dashboard-url");
        verify(checkRunBuilder).withStatus(GHCheckRun.Status.COMPLETED);
        verify(checkRunBuilder).withConclusion(GHCheckRun.Conclusion.SUCCESS);
        verify(checkRunBuilder).create();

        verifyNoMoreInteractions(checkRunBuilder);

        GHCheckRunBuilder.Output output = new GHCheckRunBuilder.Output("Quality Gate success", "report summary");
        for (int i = 0; i < 20; i++) {
            output.add(new GHCheckRunBuilder.Annotation(
                "path" + i,
                i,
                GHCheckRun.AnnotationLevel.values()[i % Severity.values().length < 2 ? 0 : i % Severity.values().length > 2 ? 2 : 1],
                "issue message " + i));
        }

        assertThat(outputCaptor.getValue()).usingRecursiveComparison().isEqualTo(output);

        DecorationResult expectedResult = DecorationResult.builder().withPullRequestUrl("http://url.of/pull/request").build();
        assertThat(decorationResult).usingRecursiveComparison().isEqualTo(expectedResult);

        verifyNoMoreInteractions(gitHub);

        verify(comment1, never()).delete();
        verify(comment2, never()).delete();
        verify(comment3).delete();
        verify(comment4, never()).delete();
        verify(comment5, never()).delete();
        verify(summaryComment, never()).delete();

        verify(pullRequest).comment("report summary");
        verify(pullRequest).getHtmlUrl();
        verify(pullRequest).getComments();
        verifyNoMoreInteractions(pullRequest);
    }


    @Test
    void shouldDecoratePullRequestWithCorrectAnalysisAndNoSummaryCommentWhenDisabled() throws IOException {
        when(projectAlmSettingDto.getSummaryCommentEnabled()).thenReturn(false);
        GHRepository repository = mock();
        when(gitHub.getRepository(any())).thenReturn(repository);
        GHCheckRunBuilder checkRunBuilder = mock(InvocationOnMock::getMock);
        doReturn(null).when(checkRunBuilder).create();
        when(repository.createCheckRun(any(), any())).thenReturn(checkRunBuilder);
        GHPullRequest pullRequest = mock();
        when(repository.getPullRequest(anyInt())).thenReturn(pullRequest);
        when(pullRequest.getHtmlUrl()).thenReturn(new URL("http://url.of/pull/request"));
        when(analysisDetails.getQualityGateStatus()).thenReturn(QualityGate.Status.ERROR);

        DecorationResult decorationResult = testCase.decorateQualityGateStatus(analysisDetails, almSettingDto, projectAlmSettingDto);

        verify(gitHub).getRepository("alm-repo");
        verify(repository).createCheckRun("SonarQube Code Analysis", "commit-sha");

        ArgumentCaptor<GHCheckRunBuilder.Output> outputCaptor = ArgumentCaptor.captor();

        verify(checkRunBuilder).add(outputCaptor.capture());
        verify(checkRunBuilder).withExternalID("analysis-id");
        verify(checkRunBuilder).withStartedAt(Date.from(clock.instant()));
        verify(checkRunBuilder).withCompletedAt(Date.from(clock.instant()));
        verify(checkRunBuilder).withDetailsURL("dashboard-url");
        verify(checkRunBuilder).withStatus(GHCheckRun.Status.COMPLETED);
        verify(checkRunBuilder).withConclusion(GHCheckRun.Conclusion.FAILURE);
        verify(checkRunBuilder).create();

        verifyNoMoreInteractions(checkRunBuilder);

        GHCheckRunBuilder.Output output = new GHCheckRunBuilder.Output("Quality Gate failed", "report summary");
        for (int i = 0; i < 20; i++) {
            output.add(new GHCheckRunBuilder.Annotation(
                "path" + i,
                i,
                GHCheckRun.AnnotationLevel.values()[i % Severity.values().length < 2 ? 0 : i % Severity.values().length > 2 ? 2 : 1],
                "issue message " + i));
        }

        assertThat(outputCaptor.getValue()).usingRecursiveComparison().isEqualTo(output);

        DecorationResult expectedResult = DecorationResult.builder().withPullRequestUrl("http://url.of/pull/request").build();
        assertThat(decorationResult).usingRecursiveComparison().isEqualTo(expectedResult);

        verifyNoMoreInteractions(gitHub);

        verify(pullRequest).getHtmlUrl();
        verifyNoMoreInteractions(pullRequest);
    }

    private static GHIssueComment createComment(String body, String userType, long userId, long commentId) throws IOException {
        GHIssueComment comment = mock();
        when(comment.getBody()).thenReturn(body);
        when(comment.getId()).thenReturn(commentId);
        GHUser user = mock();
        when(user.getId()).thenReturn(userId);
        when(user.getType()).thenReturn(userType);
        when(comment.getUser()).thenReturn(user);
        return comment;
    }
}
