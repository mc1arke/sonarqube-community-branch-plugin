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
import static org.mockito.Mockito.doThrow;
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
import org.sonar.api.rule.Severity;
import org.sonar.ce.task.projectanalysis.component.Component;
import org.sonar.db.alm.setting.ALM;
import org.sonar.db.alm.setting.AlmSettingDto;
import org.sonar.db.alm.setting.ProjectAlmSettingDto;

import com.github.mc1arke.sonarqube.plugin.almclient.github.GithubClientFactory;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.AnalysisDetails;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.DecorationResult;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.PostAnalysisIssueVisitor;
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
        doReturn("alm-repo").when(projectAlmSettingDto).getAlmRepo();
        doReturn("123").when(analysisDetails).getPullRequestId();
        doReturn(Date.from(clock.instant())).when(analysisDetails).getAnalysisDate();
        doReturn("analysis-id").when(analysisDetails).getAnalysisId();
        doReturn("project-key").when(analysisDetails).getAnalysisProjectKey();
        doReturn("Project Name").when(analysisDetails).getAnalysisProjectName();
        doReturn(QualityGate.Status.OK).when(analysisDetails).getQualityGateStatus();
        doReturn("commit-sha").when(analysisDetails).getCommitSha();
        doReturn(IntStream.range(0, 20).mapToObj(i -> {
            PostAnalysisIssueVisitor.ComponentIssue componentIssue = mock(PostAnalysisIssueVisitor.ComponentIssue.class);
            Component component = mock(Component.class);
            doReturn(Optional.of("path" + i)).when(componentIssue).getScmPath();
            doReturn(component).when(componentIssue).getComponent();
            PostAnalysisIssueVisitor.LightIssue lightIssue = mock(PostAnalysisIssueVisitor.LightIssue.class);
            doReturn("issue message " + i).when(lightIssue).getMessage();
            doReturn(i).when(lightIssue).getLine();
            doReturn(Severity.ALL.get(i % Severity.ALL.size())).when(lightIssue).severity();
            doReturn(lightIssue).when(componentIssue).getIssue();
            return componentIssue;
        }).collect(Collectors.toList())).when(analysisDetails).getScmReportableIssues();

        doReturn(analysisSummary).when(reportGenerator).createAnalysisSummary(any());
        doReturn("dashboard-url").when(analysisSummary).getDashboardUrl();
        doReturn("report summary").when(analysisSummary).format(any());
        doReturn(gitHub).when(githubClientFactory).createClient(any(), any());
    }

    @Test
    void shouldReturnCorrectAlms() {
        assertThat(testCase.alm()).isEqualTo(List.of(ALM.GITHUB));
    }

    @Test
    void shouldThrowExceptionIfClientCreationFails() throws IOException {
        Exception dummyException = new IOException("Dummy Exception");
        doThrow(dummyException).when(githubClientFactory).createClient(any(), any());

        assertThatThrownBy(() -> testCase.decorateQualityGateStatus(analysisDetails, almSettingDto, projectAlmSettingDto))
                .hasMessage("Could not decorate Pull Request on Github")
                .isExactlyInstanceOf(IllegalStateException.class).hasCause(dummyException);
    }

    @Test
    void shouldDecoratePullRequestWithCorrectAnalysisAndSummaryCommentWhenEnabled() throws IOException {
        doReturn(true).when(projectAlmSettingDto).getSummaryCommentEnabled();
        GHRepository repository = mock();
        doReturn(repository).when(gitHub).getRepository(any());
        GHCheckRunBuilder checkRunBuilder = mock(InvocationOnMock::getMock);
        doReturn(null).when(checkRunBuilder).create();
        doReturn(checkRunBuilder).when(repository).createCheckRun(any(), any());
        GHPullRequest pullRequest = mock();
        GHIssueComment comment1 = createComment("summary comment from current bot user, no project ID", "Bot", 123, 1);
        GHIssueComment comment2 = createComment("summary comment from non bot user with no project ID", "User", 321, 2);
        GHIssueComment comment3 = createComment("summary comment from current bot user, with project ID. **Project ID:** project-key\n", "Bot", 123, 3);
        GHIssueComment comment4 = createComment("summary comment from other bot user, with project ID. **Project ID:** project-key\n", "Bot", 999, 4);
        GHIssueComment comment5 = createComment("summary comment from other bot user, with project ID. **Project ID:** project-key\n", "User", 111, 5);
        GHIssueComment summaryComment = createComment("summary comment from current bot user, with project ID. **Project ID:** project-key\r", "Bot", 123, 6);
        doReturn(List.of(comment1, comment2, comment3, comment4, comment5, summaryComment)).when(pullRequest).getComments();
        doReturn(summaryComment).when(pullRequest).comment(any(String.class));
        doReturn(pullRequest).when(repository).getPullRequest(anyInt());
        doReturn(new URL("http://url.of/pull/request")).when(pullRequest).getHtmlUrl();
        DecorationResult decorationResult = testCase.decorateQualityGateStatus(analysisDetails, almSettingDto, projectAlmSettingDto);

        verify(gitHub).getRepository("alm-repo");
        verify(repository).createCheckRun("Project Name Sonarqube Results", "commit-sha");

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
                i % 5 < 1 ? GHCheckRun.AnnotationLevel.NOTICE : i % 5 > 2 ? GHCheckRun.AnnotationLevel.FAILURE : GHCheckRun.AnnotationLevel.WARNING,
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
        doReturn(false).when(projectAlmSettingDto).getSummaryCommentEnabled();
        GHRepository repository = mock();
        doReturn(repository).when(gitHub).getRepository(any());
        GHCheckRunBuilder checkRunBuilder = mock(InvocationOnMock::getMock);
        doReturn(null).when(checkRunBuilder).create();
        doReturn(checkRunBuilder).when(repository).createCheckRun(any(), any());
        GHPullRequest pullRequest = mock();
        doReturn(pullRequest).when(repository).getPullRequest(anyInt());
        doReturn(new URL("http://url.of/pull/request")).when(pullRequest).getHtmlUrl();
        doReturn(QualityGate.Status.ERROR).when(analysisDetails).getQualityGateStatus();

        DecorationResult decorationResult = testCase.decorateQualityGateStatus(analysisDetails, almSettingDto, projectAlmSettingDto);

        verify(gitHub).getRepository("alm-repo");
        verify(repository).createCheckRun("Project Name Sonarqube Results", "commit-sha");

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
                i % 5 < 1 ? GHCheckRun.AnnotationLevel.NOTICE : i % 5 > 2 ? GHCheckRun.AnnotationLevel.FAILURE : GHCheckRun.AnnotationLevel.WARNING,
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
