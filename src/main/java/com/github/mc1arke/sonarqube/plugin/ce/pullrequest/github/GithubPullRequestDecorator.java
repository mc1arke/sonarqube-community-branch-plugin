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
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.PullRequestBuildStatusDecorator;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.markup.MarkdownFormatterFactory;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.report.AnalysisSummary;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.report.ReportGenerator;
import org.sonar.api.ce.posttask.QualityGate;
import org.sonar.api.rule.Severity;
import org.sonar.db.alm.setting.ALM;
import org.sonar.db.alm.setting.AlmSettingDto;
import org.sonar.db.alm.setting.ProjectAlmSettingDto;

import java.time.Clock;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class GithubPullRequestDecorator implements PullRequestBuildStatusDecorator {

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
        AnalysisSummary analysisSummary = reportGenerator.createAnalysisSummary(analysisDetails);

        CheckRunDetails checkRunDetails = CheckRunDetails.builder()
                .withAnnotations(analysisDetails.getScmReportableIssues().stream()
                        .map(GithubPullRequestDecorator::createAnnotation)
                        .collect(Collectors.toList()))
                .withCheckConclusionState(analysisDetails.getQualityGateStatus() == QualityGate.Status.OK ? CheckConclusionState.SUCCESS : CheckConclusionState.FAILURE)
                .withCommitId(analysisDetails.getCommitSha())
                .withSummary(analysisSummary.format(markdownFormatterFactory))
                .withDashboardUrl(analysisSummary.getDashboardUrl())
                .withPullRequestId(Integer.parseInt(analysisDetails.getPullRequestId()))
                .withStartTime(analysisDetails.getAnalysisDate().toInstant().atZone(ZoneId.of("UTC")))
                .withEndTime(ZonedDateTime.now(clock))
                .withExternalId(analysisDetails.getAnalysisId())
                .withName(String.format("%s Sonarqube Results", analysisDetails.getAnalysisProjectName()))
                .withTitle("Quality Gate " + (analysisDetails.getQualityGateStatus() == QualityGate.Status.OK ? "success" : "failed"))
                .withProjectKey(analysisDetails.getAnalysisProjectKey())
                .build();

        try {
            GithubClient githubClient = githubClientFactory.createClient(projectAlmSettingDto, almSettingDto);

            githubClient.createCheckRun(checkRunDetails,
                            Optional.ofNullable(projectAlmSettingDto.getSummaryCommentEnabled())
                                    .orElse(false));

            return DecorationResult.builder()
                    .withPullRequestUrl(githubClient.getRepositoryUrl() + "/pull/" + checkRunDetails.getPullRequestId())
                    .build();
        } catch (Exception ex) {
            throw new IllegalStateException("Could not decorate Pull Request on Github", ex);
        }

    }

    @Override
    public List<ALM> alm() {
        return Collections.singletonList(ALM.GITHUB);
    }

    private static Annotation createAnnotation(PostAnalysisIssueVisitor.ComponentIssue componentIssue) {
        return Annotation.builder()
                .withLine(Optional.ofNullable(componentIssue.getIssue().getLine()).orElse(0))
                .withScmPath(componentIssue.getScmPath().orElseThrow())
                .withMessage(Optional.ofNullable(componentIssue.getIssue().getMessage()).orElseThrow().replace("\\","\\\\").replace("\"", "\\\""))
                .withSeverity(mapToGithubAnnotationLevel(componentIssue.getIssue().severity()))
                .build();
    }

    private static CheckAnnotationLevel mapToGithubAnnotationLevel(String sonarqubeSeverity) {
        switch (sonarqubeSeverity) {
            case Severity.INFO:
                return CheckAnnotationLevel.NOTICE;
            case Severity.MINOR:
            case Severity.MAJOR:
                return CheckAnnotationLevel.WARNING;
            case Severity.CRITICAL:
            case Severity.BLOCKER:
                return CheckAnnotationLevel.FAILURE;
            default:
                throw new IllegalArgumentException("Unknown severity value: " + sonarqubeSeverity);
        }
    }

}
