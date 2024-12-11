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

import java.io.IOException;
import java.time.Clock;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.kohsuke.github.GHCheckRun;
import org.kohsuke.github.GHCheckRunBuilder;
import org.kohsuke.github.GHIssueComment;
import org.kohsuke.github.GHPullRequest;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;
import org.sonar.api.ce.posttask.QualityGate;
import org.sonar.api.issue.impact.Severity;
import org.sonar.db.alm.setting.ALM;
import org.sonar.db.alm.setting.AlmSettingDto;
import org.sonar.db.alm.setting.ProjectAlmSettingDto;

import com.github.mc1arke.sonarqube.plugin.almclient.github.GithubClientFactory;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.AnalysisDetails;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.DecorationResult;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.PostAnalysisIssueVisitor;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.PullRequestBuildStatusDecorator;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.markup.Bold;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.markup.Document;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.markup.MarkdownFormatterFactory;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.markup.Text;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.report.AnalysisSummary;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.report.ReportGenerator;

public class GithubPullRequestDecorator implements PullRequestBuildStatusDecorator {

    private static final String DEFAULT_CHECK_RUN_NAME = "SonarQube Code Analysis";
    private final GithubClientFactory githubClientFactory;
    private final ReportGenerator reportGenerator;
    private final MarkdownFormatterFactory markdownFormatterFactory;
    private final Clock clock;

    public GithubPullRequestDecorator(GithubClientFactory githubClientFactory, ReportGenerator reportGenerator,
                                      MarkdownFormatterFactory markdownFormatterFactory, Clock clock) {
        this.githubClientFactory = githubClientFactory;
        this.reportGenerator = reportGenerator;
        this.markdownFormatterFactory = markdownFormatterFactory;
        this.clock = clock;
    }

    @Override
    public DecorationResult decorateQualityGateStatus(AnalysisDetails analysisDetails, AlmSettingDto almSettingDto,
                                          ProjectAlmSettingDto projectAlmSettingDto) {
        try {
            GitHub github = githubClientFactory.createClient(almSettingDto, projectAlmSettingDto);
            GHRepository repository = github.getRepository(projectAlmSettingDto.getAlmRepo());

            GHPullRequest pullRequest = createCheckRun(repository, analysisDetails, projectAlmSettingDto.getMonorepo(),
                    Optional.ofNullable(projectAlmSettingDto.getSummaryCommentEnabled()).orElse(false));

            return DecorationResult.builder()
                    .withPullRequestUrl(pullRequest.getHtmlUrl().toExternalForm())
                    .build();
        } catch (Exception ex) {
            throw new IllegalStateException("Could not decorate Pull Request on Github", ex);
        }

    }

    @Override
    public List<ALM> alm() {
        return List.of(ALM.GITHUB);
    }


    private GHPullRequest createCheckRun(GHRepository repository, AnalysisDetails analysisDetails,
            boolean isMonorepo, boolean postSummaryComment) throws IOException {
        AnalysisSummary analysisSummary = reportGenerator.createAnalysisSummary(analysisDetails);
        String summary = analysisSummary.format(markdownFormatterFactory);

        GHCheckRunBuilder.Output output = new GHCheckRunBuilder.Output("Quality Gate " + (analysisDetails.getQualityGateStatus() == QualityGate.Status.OK ? "success" : "failed"), summary);
        for (PostAnalysisIssueVisitor.ComponentIssue componentIssue : analysisDetails.getScmReportableIssues()) {
            output.add(new GHCheckRunBuilder.Annotation(
                componentIssue.getScmPath().orElseThrow(),
                Optional.ofNullable(componentIssue.getIssue().getLine()).orElse(0),
                mapToGithubAnnotationLevel(componentIssue.getIssue().impacts().values()),
                Optional.ofNullable(componentIssue.getIssue().getMessage()).orElseThrow())
            );
        }

        String checkRunName = isMonorepo
            ? String.format("[%s] %s", analysisDetails.getAnalysisProjectName(), DEFAULT_CHECK_RUN_NAME)
            : DEFAULT_CHECK_RUN_NAME;
        repository.createCheckRun(checkRunName, analysisDetails.getCommitSha())
            .withStartedAt(analysisDetails.getAnalysisDate())
            .withCompletedAt(Date.from(clock.instant()))
            .withStatus(GHCheckRun.Status.COMPLETED)
            .withConclusion(analysisDetails.getQualityGateStatus() == QualityGate.Status.OK ? GHCheckRun.Conclusion.SUCCESS : GHCheckRun.Conclusion.FAILURE)
            .withDetailsURL(analysisSummary.getDashboardUrl())
            .withExternalID(analysisDetails.getAnalysisId())
            .add(output)
            .create();

        GHPullRequest pullRequest = repository.getPullRequest(Integer.parseInt(analysisDetails.getPullRequestId()));
        if (postSummaryComment) {
            postSummaryComment(pullRequest, summary, analysisDetails.getAnalysisProjectKey());
        }
        return pullRequest;
    }

    private void postSummaryComment(GHPullRequest pullRequest, String summary, String projectId) throws IOException {
        String projectCommentMarker = markdownFormatterFactory.documentFormatter().format(new Document(new Bold(new Text("Project ID:")), new Text(" " + projectId)));

        GHIssueComment summaryComment = pullRequest.comment(summary);

        for (GHIssueComment comment : pullRequest.getComments()) {
            if ("Bot".equalsIgnoreCase(comment.getUser().getType())
                && summaryComment.getUser().getId() == comment.getUser().getId()
                && (comment.getBody().contains(projectCommentMarker + "\n") || comment.getBody().contains(projectCommentMarker + "\r"))
                && comment.getId() != summaryComment.getId()) {
                comment.delete();
            }
        }

    }

    private static GHCheckRun.AnnotationLevel mapToGithubAnnotationLevel(Collection<Severity> sonarqubeSeverity) {
        Severity maxSeverity = sonarqubeSeverity.stream().max(Severity::compareTo).orElseThrow();
        switch (maxSeverity) {
            case LOW:
            case INFO:
                return GHCheckRun.AnnotationLevel.NOTICE;
            case MEDIUM:
                return GHCheckRun.AnnotationLevel.WARNING;
            case HIGH:
            case BLOCKER:
                return GHCheckRun.AnnotationLevel.FAILURE;
            default:
                throw new IllegalArgumentException("Unknown severity value: " + sonarqubeSeverity);
        }
    }

}
