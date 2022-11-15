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
package com.github.mc1arke.sonarqube.plugin.ce.pullrequest.github;

import com.github.mc1arke.sonarqube.plugin.almclient.github.GithubClient;
import com.github.mc1arke.sonarqube.plugin.almclient.github.GithubClientFactory;
import com.github.mc1arke.sonarqube.plugin.almclient.github.model.Annotation;
import com.github.mc1arke.sonarqube.plugin.almclient.github.model.CheckRunDetails;
import com.github.mc1arke.sonarqube.plugin.almclient.github.v4.model.CheckAnnotationLevel;
import com.github.mc1arke.sonarqube.plugin.almclient.github.v4.model.CheckConclusionState;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.AnalysisDetails;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.DecorationResult;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.PostAnalysisIssueVisitor;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.markup.MarkdownFormatterFactory;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.report.AnalysisSummary;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.report.ReportGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.sonar.api.ce.posttask.QualityGate;
import org.sonar.api.rule.Severity;
import org.sonar.ce.task.projectanalysis.component.Component;
import org.sonar.db.alm.setting.ALM;
import org.sonar.db.alm.setting.AlmSettingDto;
import org.sonar.db.alm.setting.ProjectAlmSettingDto;

import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Collections;
import java.util.Date;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class GithubPullRequestDecoratorTest {

    private final GithubClient githubClient = mock(GithubClient.class);
    private final AnalysisDetails analysisDetails = mock(AnalysisDetails.class);
    private final GithubClientFactory githubClientFactory = mock(GithubClientFactory.class);
    private final ReportGenerator reportGenerator = mock(ReportGenerator.class);
    private final MarkdownFormatterFactory markdownFormatterFactory = mock(MarkdownFormatterFactory.class);
    private final Clock clock = Clock.fixed(Instant.ofEpochSecond(102030405), ZoneId.of("UTC"));
    private final GithubPullRequestDecorator testCase = new GithubPullRequestDecorator(githubClientFactory, reportGenerator, markdownFormatterFactory, clock);
    private final ProjectAlmSettingDto projectAlmSettingDto = mock(ProjectAlmSettingDto.class);
    private final AlmSettingDto almSettingDto = mock(AlmSettingDto.class);
    private final AnalysisSummary analysisSummary = mock(AnalysisSummary.class);

    @BeforeEach
    void setUp() {
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
    }

    @Test
    void verifyCorrectNameReturned() {
        assertThat(testCase.alm()).isEqualTo(Collections.singletonList(ALM.GITHUB));
    }

    @Test
    void verifyClientExceptionPropagated() throws IOException {
        Exception dummyException = new IOException("Dummy Exception");
        doReturn(githubClient).when(githubClientFactory).createClient(any(), any());
        doThrow(dummyException).when(githubClient).createCheckRun(any(), anyBoolean());

        assertThatThrownBy(() -> testCase.decorateQualityGateStatus(analysisDetails, almSettingDto, projectAlmSettingDto))
                .hasMessage("Could not decorate Pull Request on Github")
                .isExactlyInstanceOf(IllegalStateException.class).hasCause(dummyException);
    }

    @Test
    void verifyCorrectArgumentsAndReturnValuesUsed() throws IOException {
        doReturn(true).when(projectAlmSettingDto).getSummaryCommentEnabled();
        DecorationResult expectedResult = DecorationResult.builder().withPullRequestUrl("http://github.url/repo/path/pull/123").build();
        doReturn(githubClient).when(githubClientFactory).createClient(any(), any());
        doReturn("checkRunId").when(githubClient).createCheckRun(any(), anyBoolean());
        doReturn("http://github.url/repo/path").when(githubClient).getRepositoryUrl();
        DecorationResult decorationResult = testCase.decorateQualityGateStatus(analysisDetails, almSettingDto, projectAlmSettingDto);

        ArgumentCaptor<CheckRunDetails> checkRunDetailsArgumentCaptor = ArgumentCaptor.forClass(CheckRunDetails.class);
        verify(githubClient).createCheckRun(checkRunDetailsArgumentCaptor.capture(), eq(true));

        assertThat(checkRunDetailsArgumentCaptor.getValue())
                .usingRecursiveComparison()
                        .isEqualTo(CheckRunDetails.builder()
                                .withTitle("Quality Gate success")
                                .withName("Project Name Sonarqube Results")
                                .withExternalId("analysis-id")
                                .withPullRequestId(123)
                                .withStartTime(clock.instant().atZone(ZoneId.of("UTC")))
                                .withEndTime(clock.instant().atZone(ZoneId.of("UTC")))
                                .withDashboardUrl("dashboard-url")
                                .withSummary("report summary")
                                .withCommitId("commit-sha")
                                .withAnnotations(IntStream.range(0, 20).mapToObj(i -> Annotation.builder()
                                            .withScmPath("path" + i)
                                            .withLine(i)
                                            .withMessage("issue message " + i)
                                            .withSeverity(i % 5 < 1 ? CheckAnnotationLevel.NOTICE : i % 5 > 2 ? CheckAnnotationLevel.FAILURE : CheckAnnotationLevel.WARNING)
                                            .build()).collect(Collectors.toList()))
                                .withCheckConclusionState(CheckConclusionState.SUCCESS)
                                .build());
        assertThat(decorationResult).usingRecursiveComparison().isEqualTo(expectedResult);
    }
}
