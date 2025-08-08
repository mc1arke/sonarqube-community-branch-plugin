/*
 * Copyright (C) 2022-2024 Michael Clarke
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

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.ce.posttask.QualityGate;
import org.sonar.api.config.Configuration;
import org.sonar.api.issue.IssueStatus;
import org.sonar.api.issue.impact.SoftwareQuality;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Metric;
import org.sonar.api.platform.Server;
import org.sonar.ce.task.projectanalysis.component.TreeRootHolder;
import org.sonar.ce.task.projectanalysis.measure.Measure;
import org.sonar.ce.task.projectanalysis.measure.MeasureRepository;
import org.sonar.ce.task.projectanalysis.metric.MetricRepository;
import org.sonar.server.measure.Rating;

import com.github.mc1arke.sonarqube.plugin.CommunityBranchPlugin;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.AnalysisDetails;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.PostAnalysisIssueVisitor;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.bitbucket.BitbucketPullRequestDecorator;

import org.sonar.server.metric.StandardToMQRMetrics;

public class ReportGenerator {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReportGenerator.class);

    private static final String NO_DATA_IMAGE_PATH = "common/no-data-16px.png";
    private static final String PASSED_IMAGE_PATH = "common/passed-16px.png";

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

        return AnalysisIssueSummary.builder()
                .withIssueUrl(getIssueUrl(issue, analysisDetails))
                .withMessage(issue.getMessage())
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

        int fixedIssues = findMeasure(CoreMetrics.PULL_REQUEST_FIXED_ISSUES_KEY)
            .map(Measure::getIntValue)
            .orElse(0);
        long newIssues = analysisDetails.getIssues().stream().filter(i -> i.getIssue().issueStatus() == IssueStatus.OPEN).count();
        int acceptedIssues = findMeasure(CoreMetrics.ACCEPTED_ISSUES_KEY)
            .map(Measure::getIntValue)
            .orElse(0);

        List<QualityGate.Condition> failedConditions = analysisDetails.findFailedQualityGateConditions();

        String baseImageUrl = getBaseImageUrl();

        return AnalysisSummary.builder()
                .withProjectKey(analysisDetails.getAnalysisProjectKey())
                .withSummaryImageUrl(baseImageUrl + "/common/icon.png")
                .withCoverage(new AnalysisSummary.UrlIconMetric<>(getComponentMeasuresUrlForCodeMetrics(analysisDetails, CoreMetrics.NEW_COVERAGE_KEY), baseImageUrl + "/" + (newCoverage == null ? NO_DATA_IMAGE_PATH : PASSED_IMAGE_PATH), coverage))
                .withNewCoverage(newCoverage)
                .withDashboardUrl(getDashboardUrl(analysisDetails))
                .withDuplications(new AnalysisSummary.UrlIconMetric<>(getComponentMeasuresUrlForCodeMetrics(analysisDetails, CoreMetrics.NEW_DUPLICATED_LINES_DENSITY_KEY), baseImageUrl + "/" + (newDuplications == null ? NO_DATA_IMAGE_PATH : PASSED_IMAGE_PATH), duplications))
                .withNewDuplications(newDuplications)
                .withFailedQualityGateConditions(failedConditions.stream()
                        .map(ReportGenerator::formatQualityGateCondition)
                        .filter(Optional::isPresent)
                        .map(Optional::get)
                        .collect(Collectors.toList()))
                .withStatusDescription(QualityGate.Status.OK == analysisDetails.getQualityGateStatus() ? "Passed" : "Failed")
                .withStatusImageUrl(QualityGate.Status.OK == analysisDetails.getQualityGateStatus()
                        ? baseImageUrl + "/checks/QualityGateBadge/passed-16px.png"
                        : baseImageUrl + "/checks/QualityGateBadge/failed-16px.png")
                .withSecurityHotspots(new AnalysisSummary.UrlIconMetric<>(String.format("%s/security_hotspots?id=%s&pullRequest=%s", server.getPublicRootUrl(), URLEncoder.encode(analysisDetails.getAnalysisProjectKey(), StandardCharsets.UTF_8), URLEncoder.encode(analysisDetails.getPullRequestId(), StandardCharsets.UTF_8)),
                    baseImageUrl + "/" + PASSED_IMAGE_PATH,
                    findMeasure(CoreMetrics.SECURITY_HOTSPOTS_KEY).map(Measure::getIntValue).orElse(0)))
                .withFixedIssues(new AnalysisSummary.UrlIconMetric<>(String.format("%s/project/issues?id=%s&fixedInPullRequest=%s", server.getPublicRootUrl(), URLEncoder.encode(analysisDetails.getAnalysisProjectKey(), StandardCharsets.UTF_8), URLEncoder.encode(analysisDetails.getPullRequestId(), StandardCharsets.UTF_8)), baseImageUrl + "/common/fixed-16px.png", fixedIssues))
                .withNewIssues(new AnalysisSummary.UrlIconMetric<>(String.format("%s/project/issues?id=%s&pullRequest=%s&resolved=false", server.getPublicRootUrl(), URLEncoder.encode(analysisDetails.getAnalysisProjectKey(), StandardCharsets.UTF_8), URLEncoder.encode(analysisDetails.getPullRequestId(), StandardCharsets.UTF_8)), baseImageUrl + "/" + PASSED_IMAGE_PATH, newIssues))
                .withAcceptedIssues(new AnalysisSummary.UrlIconMetric<>(String.format("%s/project/issues?id=%s&pullRequest=%s&issueStatus=ACCEPTED", server.getPublicRootUrl(), URLEncoder.encode(analysisDetails.getAnalysisProjectKey(), StandardCharsets.UTF_8), URLEncoder.encode(analysisDetails.getPullRequestId(), StandardCharsets.UTF_8)), baseImageUrl + "/common/accepted-16px.png", acceptedIssues))
                .build();
    }

    public boolean isPublishBuildStatus() {
    	return configuration.getBoolean(BitbucketPullRequestDecorator.PUBLISH_BUILD_STATUS).orElse(true);
    }
    
    private String getComponentMeasuresUrlForCodeMetrics(AnalysisDetails analysisDetails, String codeMetricsKey) {
        // https://my-server:port/component_measures?id=some-key&metric=new_coverage&pullRequest=341&view=list
        return server.getPublicRootUrl() +
                "/component_measures?id=" + URLEncoder.encode(analysisDetails.getAnalysisProjectKey(), StandardCharsets.UTF_8) +
                "&metric=" + codeMetricsKey +
                "&pullRequest=" + analysisDetails.getPullRequestId() +
                "&view=list";
    }

    private String getBaseImageUrl() {
        return configuration.get(CommunityBranchPlugin.IMAGE_URL_BASE)
                .orElse(server.getPublicRootUrl() + "/static/communityBranchPlugin")
                .replaceAll("/*$", "");
    }

    private String getIssueUrl(PostAnalysisIssueVisitor.LightIssue issue, AnalysisDetails analysisDetails) {
        if (issue.impacts().containsKey(SoftwareQuality.SECURITY)) {
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

    private static Optional<String> formatQualityGateCondition(QualityGate.Condition condition) {
        String key = condition.getMetricKey();
        Optional<Metric> optionalMetric = findMetric(key);
        if (optionalMetric.isEmpty()) {
            LOGGER.info("No metric found for key {}, trying to map from MQR to Core equivalent", key);
            Optional<String> alternativeKey = StandardToMQRMetrics.getEquivalentMetric(key);
            if (alternativeKey.isPresent()) {
                String alternative = alternativeKey.get();
                LOGGER.info("Found alternative metric {} for key {}", alternative, key);
                optionalMetric = findMetric(alternative);
            }
        }
        if (optionalMetric.isEmpty()) {
            LOGGER.warn("No alternative metric found for key {}", key);
            return Optional.empty();
        }
        Metric<?> metric = optionalMetric.get();
        if (metric.getType() == Metric.ValueType.RATING) {
            return Optional.of(String
                    .format("%s %s (%s %s)", Rating.valueOf(Integer.parseInt(condition.getValue())), metric.getName(),
                            condition.getOperator() == QualityGate.Operator.GREATER_THAN ? "is worse than" :
                                    "is better than", Rating.valueOf(Integer.parseInt(condition.getErrorThreshold()))));
        } else if (metric.getType() == Metric.ValueType.PERCENT) {
            NumberFormat numberFormat = new DecimalFormat("#0.00", DecimalFormatSymbols.getInstance(Locale.ENGLISH));
            return Optional.of(String.format("%s%% %s (%s %s%%)", numberFormat.format(new BigDecimal(condition.getValue())),
                    metric.getName(),
                    condition.getOperator() == QualityGate.Operator.GREATER_THAN ? "is greater than" :
                            "is less than", numberFormat.format(new BigDecimal(condition.getErrorThreshold()))));
        } else {
            return Optional.of(String.format("%s %s (%s %s)", condition.getValue(), metric.getName(),
                    condition.getOperator() == QualityGate.Operator.GREATER_THAN ? "is greater than" :
                            "is less than", condition.getErrorThreshold()));
        }
    }

    private static Optional<Metric> findMetric(String key) {
        return CoreMetrics.getMetrics().stream().filter(metric -> metric.getKey().equals(key)).findFirst();
    }

}
