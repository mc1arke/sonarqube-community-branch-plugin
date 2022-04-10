/*
 * Copyright (C) 2022 Michael Clarke
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
package com.github.mc1arke.sonarqube.plugin.ce.pullrequest.report;

import com.github.mc1arke.sonarqube.plugin.CommunityBranchPlugin;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.AnalysisDetails;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.PostAnalysisIssueVisitor;
import org.sonar.api.ce.posttask.QualityGate;
import org.sonar.api.config.Configuration;
import org.sonar.api.issue.Issue;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Metric;
import org.sonar.api.platform.Server;
import org.sonar.api.rules.RuleType;
import org.sonar.ce.task.projectanalysis.component.TreeRootHolder;
import org.sonar.ce.task.projectanalysis.measure.Measure;
import org.sonar.ce.task.projectanalysis.measure.MeasureRepository;
import org.sonar.ce.task.projectanalysis.metric.MetricRepository;
import org.sonar.server.measure.Rating;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class ReportGenerator {

    private static final List<String> CLOSED_ISSUE_STATUS = Arrays.asList(Issue.STATUS_CLOSED, Issue.STATUS_RESOLVED);

    private static final List<BigDecimal> COVERAGE_LEVELS = List.of(BigDecimal.valueOf(100),
                    BigDecimal.valueOf(90),
                    BigDecimal.valueOf(60),
                    BigDecimal.valueOf(50),
                    BigDecimal.valueOf(40),
                    BigDecimal.valueOf(25));

    private static final List<DuplicationMapping> DUPLICATION_LEVELS = List.of(new DuplicationMapping(BigDecimal.valueOf(3), "3"),
                    new DuplicationMapping(BigDecimal.valueOf(5), "5"),
                    new DuplicationMapping(BigDecimal.TEN, "10"),
                    new DuplicationMapping(BigDecimal.valueOf(20), "20"));

    private final Server server;
    private final Configuration configuration;
    private final MeasureRepository measureRepository;
    private final MetricRepository metricRepository;
    private final TreeRootHolder treeRootHolder;

    public ReportGenerator(Server server, Configuration configuration, MeasureRepository measureRepository, MetricRepository metricRepository, TreeRootHolder treeRootHolder) {
        this.server = server;
        this.configuration = configuration;
        this.measureRepository = measureRepository;
        this.metricRepository = metricRepository;
        this.treeRootHolder = treeRootHolder;
    }

    public AnalysisIssueSummary createAnalysisIssueSummary(PostAnalysisIssueVisitor.ComponentIssue componentIssue, AnalysisDetails analysisDetails) {
        final PostAnalysisIssueVisitor.LightIssue issue = componentIssue.getIssue();

        String baseImageUrl = getBaseImageUrl();

        return AnalysisIssueSummary.builder()
                .withEffortInMinutes(issue.effortInMinutes())
                .withIssueKey(issue.key())
                .withIssueUrl(getIssueUrl(issue, analysisDetails))
                .withMessage(issue.getMessage())
                .withProjectKey(analysisDetails.getAnalysisProjectKey())
                .withResolution(issue.resolution())
                .withSeverity(issue.severity())
                .withSeverityImageUrl(String.format("%s/checks/Severity/%s.svg?sanitize=true", baseImageUrl, issue.severity().toLowerCase()))
                .withType(issue.type().name())
                .withTypeImageUrl(String.format("%s/checks/IssueType/%s.svg?sanitize=true", baseImageUrl, issue.type().name().toLowerCase()))
                .build();
    }

    public AnalysisSummary createAnalysisSummary(AnalysisDetails analysisDetails) {
        BigDecimal newCoverage = analysisDetails.findQualityGateCondition(CoreMetrics.NEW_COVERAGE_KEY)
                .filter(condition -> condition.getStatus() != QualityGate.EvaluationStatus.NO_VALUE)
                .map(QualityGate.Condition::getValue)
                .map(BigDecimal::new)
                .orElse(null);

        BigDecimal coverage = findMeasure(CoreMetrics.COVERAGE_KEY)
                .map(Measure::getDoubleValue)
                .map(BigDecimal::new)
                .orElse(null);

        BigDecimal newDuplications = analysisDetails.findQualityGateCondition(CoreMetrics.NEW_DUPLICATED_LINES_DENSITY_KEY)
                .filter(condition -> condition.getStatus() != QualityGate.EvaluationStatus.NO_VALUE)
                .map(QualityGate.Condition::getValue)
                .map(BigDecimal::new)
                .orElse(null);

        BigDecimal duplications = findMeasure(CoreMetrics.DUPLICATED_LINES_DENSITY_KEY)
                .map(Measure::getDoubleValue)
                .map(BigDecimal::valueOf)
                .orElse(null);

        Map<RuleType, Long> issueCounts = countRuleByType(analysisDetails.getIssues());
        long issueTotal = issueCounts.values().stream().mapToLong(l -> l).sum();

        List<QualityGate.Condition> failedConditions = analysisDetails.findFailedQualityGateConditions();

        String baseImageUrl = getBaseImageUrl();

        return AnalysisSummary.builder()
                .withProjectKey(analysisDetails.getAnalysisProjectKey())
                .withSummaryImageUrl(baseImageUrl + "/common/icon.png")
                .withBugCount(issueCounts.get(RuleType.BUG))
                .withBugImageUrl(baseImageUrl + "/common/bug.svg?sanitize=true")
                .withCodeSmellCount(issueCounts.get(RuleType.CODE_SMELL))
                .withCodeSmellImageUrl(baseImageUrl + "/common/code_smell.svg?sanitize=true")
                .withCoverage(coverage)
                .withNewCoverage(newCoverage)
                .withCoverageImageUrl(createCoverageImage(newCoverage, baseImageUrl))
                .withDashboardUrl(getDashboardUrl(analysisDetails))
                .withDuplications(duplications)
                .withDuplicationsImageUrl(createDuplicateImage(newDuplications, baseImageUrl))
                .withNewDuplications(newDuplications)
                .withFailedQualityGateConditions(failedConditions.stream()
                        .map(ReportGenerator::formatQualityGateCondition)
                        .collect(Collectors.toList()))
                .withStatusDescription(QualityGate.Status.OK == analysisDetails.getQualityGateStatus() ? "Passed" : "Failed")
                .withStatusImageUrl(QualityGate.Status.OK == analysisDetails.getQualityGateStatus()
                        ? baseImageUrl + "/checks/QualityGateBadge/passed.svg?sanitize=true"
                        : baseImageUrl + "/checks/QualityGateBadge/failed.svg?sanitize=true")
                .withTotalIssueCount(issueTotal)
                .withVulnerabilityCount(issueCounts.get(RuleType.VULNERABILITY))
                .withSecurityHotspotCount(issueCounts.get(RuleType.SECURITY_HOTSPOT))
                .withVulnerabilityImageUrl(baseImageUrl + "/common/vulnerability.svg?sanitize=true")
                .build();
    }

    private String getBaseImageUrl() {
        return configuration.get(CommunityBranchPlugin.IMAGE_URL_BASE)
                .orElse(server.getPublicRootUrl() + "/static/communityBranchPlugin")
                .replaceAll("/*$", "");
    }

    private String getIssueUrl(PostAnalysisIssueVisitor.LightIssue issue, AnalysisDetails analysisDetails) {
        if (issue.type() == RuleType.SECURITY_HOTSPOT) {
            return String.format("%s/security_hotspots?id=%s&pullRequest=%s&hotspots=%s", server.getPublicRootUrl(), URLEncoder.encode(analysisDetails.getAnalysisProjectKey(), StandardCharsets.UTF_8), analysisDetails.getPullRequestId(), issue.key());
        } else {
            return String.format("%s/project/issues?id=%s&pullRequest=%s&issues=%s&open=%s", server.getPublicRootUrl(), URLEncoder.encode(analysisDetails.getAnalysisProjectKey(), StandardCharsets.UTF_8), analysisDetails.getPullRequestId(), issue.key(), issue.key());
        }
    }

    private Optional<Measure> findMeasure(String metricKey) {
        return measureRepository.getRawMeasure(treeRootHolder.getRoot(), metricRepository.getByKey(metricKey));
    }

    private String getDashboardUrl(AnalysisDetails analysisDetails) {
        return server.getPublicRootUrl() + "/dashboard?id=" + URLEncoder.encode(analysisDetails.getAnalysisProjectKey(), StandardCharsets.UTF_8) + "&pullRequest=" + analysisDetails.getPullRequestId();
    }

    private static String createCoverageImage(BigDecimal coverage, String baseImageUrl) {
        if (null == coverage) {
            return baseImageUrl + "/checks/CoverageChart/NoCoverageInfo.svg?sanitize=true";
        }
        BigDecimal matchedLevel = BigDecimal.ZERO;
        for (BigDecimal level : COVERAGE_LEVELS) {
            if (coverage.compareTo(level) >= 0) {
                matchedLevel = level;
                break;
            }
        }
        return baseImageUrl + "/checks/CoverageChart/" + matchedLevel + ".svg?sanitize=true";
    }

    private static String createDuplicateImage(BigDecimal duplications, String baseImageUrl) {
        if (null == duplications) {
            return baseImageUrl + "/checks/Duplications/NoDuplicationInfo.svg?sanitize=true";
        }
        String matchedLevel = "20plus";
        for (DuplicationMapping level : DUPLICATION_LEVELS) {
            if (level.getDuplicationLevel().compareTo(duplications) >= 0) {
                matchedLevel = level.getImageName();
                break;
            }
        }
        return baseImageUrl + "/checks/Duplications/" + matchedLevel + ".svg?sanitize=true";
    }

    private static String formatQualityGateCondition(QualityGate.Condition condition) {
        Metric<?> metric = CoreMetrics.getMetric(condition.getMetricKey());
        if (metric.getType() == Metric.ValueType.RATING) {
            return String
                    .format("%s %s (%s %s)", Rating.valueOf(Integer.parseInt(condition.getValue())), metric.getName(),
                            condition.getOperator() == QualityGate.Operator.GREATER_THAN ? "is worse than" :
                                    "is better than", Rating.valueOf(Integer.parseInt(condition.getErrorThreshold())));
        } else if (metric.getType() == Metric.ValueType.PERCENT) {
            NumberFormat numberFormat = new DecimalFormat("#0.00", DecimalFormatSymbols.getInstance(Locale.ENGLISH));
            return String.format("%s%% %s (%s %s%%)", numberFormat.format(new BigDecimal(condition.getValue())),
                    metric.getName(),
                    condition.getOperator() == QualityGate.Operator.GREATER_THAN ? "is greater than" :
                            "is less than", numberFormat.format(new BigDecimal(condition.getErrorThreshold())));
        } else {
            return String.format("%s %s (%s %s)", condition.getValue(), metric.getName(),
                    condition.getOperator() == QualityGate.Operator.GREATER_THAN ? "is greater than" :
                            "is less than", condition.getErrorThreshold());
        }
    }

    private static Map<RuleType, Long> countRuleByType(List<PostAnalysisIssueVisitor.ComponentIssue> issues) {
        return Arrays.stream(RuleType.values()).collect(Collectors.toMap(k -> k,
                k -> issues.stream()
                        .map(PostAnalysisIssueVisitor.ComponentIssue::getIssue)
                        .filter(i -> !CLOSED_ISSUE_STATUS.contains(i.status()))
                        .filter(i -> k == i.type())
                        .count()));
    }

    private static class DuplicationMapping {

        private final BigDecimal duplicationLevel;
        private final String imageName;

        DuplicationMapping(BigDecimal duplicationLevel, String imageName) {
            this.duplicationLevel = duplicationLevel;
            this.imageName = imageName;
        }

        private BigDecimal getDuplicationLevel() {
            return duplicationLevel;
        }

        private String getImageName() {
            return imageName;
        }
    }

}
