/*
 * Copyright (C) 2020-2022 Mathias Åhsberg, Michael Clarke
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
package com.github.mc1arke.sonarqube.plugin.ce.pullrequest.bitbucket;

import com.github.mc1arke.sonarqube.plugin.almclient.bitbucket.BitbucketClient;
import com.github.mc1arke.sonarqube.plugin.almclient.bitbucket.BitbucketClientFactory;
import com.github.mc1arke.sonarqube.plugin.almclient.bitbucket.BitbucketException;
import com.github.mc1arke.sonarqube.plugin.almclient.bitbucket.model.AnnotationUploadLimit;
import com.github.mc1arke.sonarqube.plugin.almclient.bitbucket.model.CodeInsightsAnnotation;
import com.github.mc1arke.sonarqube.plugin.almclient.bitbucket.model.CodeInsightsReport;
import com.github.mc1arke.sonarqube.plugin.almclient.bitbucket.model.DataValue;
import com.github.mc1arke.sonarqube.plugin.almclient.bitbucket.model.ReportData;
import com.github.mc1arke.sonarqube.plugin.almclient.bitbucket.model.ReportStatus;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.report.AnalysisIssueSummary;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.report.AnalysisSummary;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.report.ReportGenerator;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.AnalysisDetails;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.DecorationResult;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.PullRequestBuildStatusDecorator;
import com.google.common.annotations.VisibleForTesting;
import org.sonar.api.ce.posttask.QualityGate;
import org.sonar.api.issue.Issue;
import org.sonar.api.rule.Severity;
import org.sonar.api.rules.RuleType;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.db.alm.setting.ALM;
import org.sonar.db.alm.setting.AlmSettingDto;
import org.sonar.db.alm.setting.ProjectAlmSettingDto;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static java.util.stream.Collectors.toSet;

public class BitbucketPullRequestDecorator implements PullRequestBuildStatusDecorator {

    private static final Logger LOGGER = Loggers.get(BitbucketPullRequestDecorator.class);

    private static final DecorationResult DEFAULT_DECORATION_RESULT = DecorationResult.builder().build();

    private final BitbucketClientFactory bitbucketClientFactory;
    private final ReportGenerator reportGenerator;

    public BitbucketPullRequestDecorator(BitbucketClientFactory bitbucketClientFactory, ReportGenerator reportGenerator) {
        this.bitbucketClientFactory = bitbucketClientFactory;
        this.reportGenerator = reportGenerator;
    }

    @Override
    public DecorationResult decorateQualityGateStatus(AnalysisDetails analysisDetails, AlmSettingDto almSettingDto, ProjectAlmSettingDto projectAlmSettingDto) {
        BitbucketClient client = bitbucketClientFactory.createClient(projectAlmSettingDto, almSettingDto);
        try {
            if (!client.supportsCodeInsights()) {
                LOGGER.warn("Your Bitbucket instance does not support the Code Insights API.");
                return DEFAULT_DECORATION_RESULT;
            }

            AnalysisSummary analysisSummary = reportGenerator.createAnalysisSummary(analysisDetails);

            CodeInsightsReport codeInsightsReport = client.createCodeInsightsReport(
                    toReport(client, analysisSummary),
                    reportDescription(analysisDetails, analysisSummary),
                    analysisDetails.getAnalysisDate().toInstant(),
                    analysisSummary.getDashboardUrl(),
                    analysisSummary.getSummaryImageUrl(),
                    analysisDetails.getQualityGateStatus() == QualityGate.Status.OK ? ReportStatus.PASSED : ReportStatus.FAILED
            );

            client.uploadReport(analysisDetails.getCommitSha(), codeInsightsReport, analysisDetails.getAnalysisProjectKey());

            updateAnnotations(client, analysisDetails);
        } catch (IOException e) {
            LOGGER.error("Could not decorate pull request for project {}", analysisDetails.getAnalysisProjectKey(), e);
        }

        return DEFAULT_DECORATION_RESULT;
    }

    @Override
    public List<ALM> alm() {
        return Arrays.asList(ALM.BITBUCKET, ALM.BITBUCKET_CLOUD);
    }

    private static List<ReportData> toReport(BitbucketClient client, AnalysisSummary analysisSummary) {
        List<ReportData> reportData = new ArrayList<>();
        reportData.add(reliabilityReport(analysisSummary.getBugCount()));
        reportData.add(new ReportData("Code coverage", new DataValue.Percentage(Optional.ofNullable(analysisSummary.getNewCoverage()).orElse(BigDecimal.ZERO))));
        reportData.add(securityReport(analysisSummary.getVulnerabilityCount(), analysisSummary.getSecurityHotspotCount()));
        reportData.add(new ReportData("Duplication", new DataValue.Percentage(Optional.ofNullable(analysisSummary.getNewDuplications()).orElse(BigDecimal.ZERO))));
        reportData.add(maintainabilityReport(analysisSummary.getCodeSmellCount()));
        reportData.add(new ReportData("Analysis details", client.createLinkDataValue(analysisSummary.getDashboardUrl())));

        return reportData;
    }

    private void updateAnnotations(BitbucketClient client, AnalysisDetails analysisDetails) throws IOException {
        final AtomicInteger chunkCounter = new AtomicInteger(0);

        client.deleteAnnotations(analysisDetails.getCommitSha(), analysisDetails.getAnalysisProjectKey());

        AnnotationUploadLimit uploadLimit = client.getAnnotationUploadLimit();

        Map<Integer, Set<CodeInsightsAnnotation>> annotationChunks = analysisDetails.getScmReportableIssues().stream()
                .filter(i -> !(i.getIssue().type() == RuleType.SECURITY_HOTSPOT && Issue.SECURITY_HOTSPOT_RESOLUTIONS
                        .contains(i.getIssue().resolution())))
                .sorted(Comparator.comparing(a -> Severity.ALL.indexOf(a.getIssue().severity())))
                .map(componentIssue -> {
                    String path = componentIssue.getComponent().getReportAttributes().getScmPath().orElseThrow();
                    AnalysisIssueSummary analysisIssueSummary = reportGenerator.createAnalysisIssueSummary(componentIssue, analysisDetails);
                    return client.createCodeInsightsAnnotation(componentIssue.getIssue().key(),
                            Optional.ofNullable(componentIssue.getIssue().getLine()).orElse(0),
                            analysisIssueSummary.getIssueUrl(),
                            componentIssue.getIssue().getMessage(),
                            path,
                            toBitbucketSeverity(componentIssue.getIssue().severity()),
                            toBitbucketType(componentIssue.getIssue().type()));
                }).collect(Collectors.groupingBy(s -> chunkCounter.getAndIncrement() / uploadLimit.getAnnotationBatchSize(), toSet()));

        int totalAnnotationsCounter = 1;
        for (Set<CodeInsightsAnnotation> annotations : annotationChunks.values()) {
            try {
                if (exceedsMaximumNumberOfAnnotations(totalAnnotationsCounter++, uploadLimit)) {
                    LOGGER.warn("This project has too many issues. The provider only supports {}." +
                            " The remaining annotations will be truncated.", uploadLimit.getTotalAllowedAnnotations());
                    break;
                }

                client.uploadAnnotations(analysisDetails.getCommitSha(), annotations, analysisDetails.getAnalysisProjectKey());
            } catch (BitbucketException e) {
                if (e.isError(BitbucketException.PAYLOAD_TOO_LARGE)) {
                    LOGGER.warn("The annotations will be truncated since the maximum number of annotations for this report has been reached.");
                } else {
                    throw e;
                }
            }
        }
    }

    @VisibleForTesting
    static boolean exceedsMaximumNumberOfAnnotations(int chunkCounter, AnnotationUploadLimit uploadLimit) {
        return (chunkCounter * uploadLimit.getAnnotationBatchSize()) > uploadLimit.getTotalAllowedAnnotations();
    }

    private static String toBitbucketSeverity(String severity) {
        if (severity == null) {
            return "LOW";
        }
        switch (severity) {
            case Severity.BLOCKER:
            case Severity.CRITICAL:
                return "HIGH";
            case Severity.MAJOR:
                return "MEDIUM";
            default:
                return "LOW";
        }
    }

    private static String toBitbucketType(RuleType sonarqubeType) {
        switch (sonarqubeType) {
            case SECURITY_HOTSPOT:
            case VULNERABILITY:
                return "VULNERABILITY";
            case CODE_SMELL:
                return "CODE_SMELL";
            case BUG:
                return "BUG";
            default:
                throw new IllegalStateException(format("%s is not a valid ruleType.", sonarqubeType));
        }
    }

    private static ReportData securityReport(Long vulnerabilities, Long hotspots) {
        String vulnerabilityDescription = vulnerabilities == 1 ? "Vulnerability" : "Vulnerabilities";
        String hotspotDescription = hotspots == 1 ? "Hotspot" : "Hotspots";
        String security = format("%d %s (and %d %s)", vulnerabilities, vulnerabilityDescription, hotspots, hotspotDescription);
        return new ReportData("Security", new DataValue.Text(security));
    }

    private static ReportData reliabilityReport(Long bugs) {
        String description = bugs == 1 ? "Bug" : "Bugs";
        return new ReportData("Reliability", new DataValue.Text(format("%d %s", bugs, description)));
    }

    private static ReportData maintainabilityReport(Long codeSmells) {
        String description = codeSmells == 1 ? "Code Smell" : "Code Smells";
        return new ReportData("Maintainability", new DataValue.Text(format("%d %s", codeSmells, description)));
    }

    private static String reportDescription(AnalysisDetails details, AnalysisSummary analysisSummary) {
        String header = details.getQualityGateStatus() == QualityGate.Status.OK ? "Quality Gate passed" : "Quality Gate failed";
        String body = analysisSummary.getFailedQualityGateConditions().stream()
                .map(s -> format("- %s", s))
                .collect(Collectors.joining(System.lineSeparator()));
        return format("%s%n%s", header, body);
    }
}
