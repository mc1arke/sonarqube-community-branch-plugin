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
package com.github.mc1arke.sonarqube.plugin.ce.pullrequest;


import com.github.mc1arke.sonarqube.plugin.CommunityBranchPlugin;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.markup.Document;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.markup.FormatterFactory;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.markup.Heading;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.markup.Image;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.markup.Link;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.markup.ListItem;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.markup.Node;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.markup.Paragraph;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.markup.Text;
import org.apache.commons.lang.StringUtils;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.sonar.api.ce.posttask.Analysis;
import org.sonar.api.ce.posttask.Project;
import org.sonar.api.ce.posttask.QualityGate;
import org.sonar.api.ce.posttask.QualityGate.EvaluationStatus;
import org.sonar.api.ce.posttask.ScannerContext;
import org.sonar.api.config.Configuration;
import org.sonar.api.issue.Issue;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Metric;
import org.sonar.api.rules.RuleType;
import org.sonar.ce.task.projectanalysis.component.Component;
import org.sonar.ce.task.projectanalysis.component.TreeRootHolder;
import org.sonar.ce.task.projectanalysis.measure.Measure;
import org.sonar.ce.task.projectanalysis.measure.MeasureRepository;
import org.sonar.ce.task.projectanalysis.metric.MetricRepository;
import org.sonar.server.measure.Rating;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class AnalysisDetails {

    private static final List<String> CLOSED_ISSUE_STATUS = Arrays.asList(Issue.STATUS_CLOSED, Issue.STATUS_RESOLVED);
    private static final List<String> OPEN_ISSUE_STATUSES =
            Issue.STATUSES.stream().filter(s -> !CLOSED_ISSUE_STATUS.contains(s))
                    .collect(Collectors.toList());

    private static final List<BigDecimal> COVERAGE_LEVELS =
            Arrays.asList(BigDecimal.valueOf(100), BigDecimal.valueOf(90), BigDecimal.valueOf(60),
                          BigDecimal.valueOf(50), BigDecimal.valueOf(40), BigDecimal.valueOf(25));
    private static final List<DuplicationMapping> DUPLICATION_LEVELS =
            Arrays.asList(new DuplicationMapping(BigDecimal.valueOf(3), "3"),
                          new DuplicationMapping(BigDecimal.valueOf(5), "5"),
                          new DuplicationMapping(BigDecimal.TEN, "10"),
                          new DuplicationMapping(BigDecimal.valueOf(20), "20"));

    private final String publicRootURL;
    private final BranchDetails branchDetails;
    private final MeasuresHolder measuresHolder;
    private final PostAnalysisIssueVisitor postAnalysisIssueVisitor;
    private final QualityGate qualityGate;
    private final Analysis analysis;
    private final Project project;
    private final ScannerContext scannerContext;
    private final Configuration configuration;

    AnalysisDetails(BranchDetails branchDetails, PostAnalysisIssueVisitor postAnalysisIssueVisitor,
                    QualityGate qualityGate, MeasuresHolder measuresHolder, Analysis analysis, Project project,
                    Configuration configuration, String publicRootURL, ScannerContext scannerContext) {
        super();
        this.publicRootURL = publicRootURL;
        this.branchDetails = branchDetails;
        this.measuresHolder = measuresHolder;
        this.postAnalysisIssueVisitor = postAnalysisIssueVisitor;
        this.qualityGate = qualityGate;
        this.analysis = analysis;
        this.project = project;
        this.scannerContext = scannerContext;
        this.configuration = configuration;
    }

    public String getBranchName() {
        return branchDetails.getBranchName();
    }

    public String getCommitSha() {
        return branchDetails.getCommitId();
    }

    public String getDashboardUrl() {
        return publicRootURL + "/dashboard?id=" + encode(project.getKey()) + "&pullRequest=" + branchDetails.getBranchName();
    }

    public String getIssueUrl(PostAnalysisIssueVisitor.LightIssue issue) {
        if (issue.type() == RuleType.SECURITY_HOTSPOT) {
            return String.format("%s/security_hotspots?id=%s&pullRequest=%s&hotspots=%s", publicRootURL, encode(project.getKey()), branchDetails.getBranchName(), issue.key());
        } else {
            return String.format("%s/project/issues?id=%s&pullRequest=%s&issues=%s&open=%s", publicRootURL, encode(project.getKey()), branchDetails.getBranchName(), issue.key(), issue.key());
        }
    }

    public Optional<ProjectIssueIdentifier> parseIssueIdFromUrl(String issueUrl) {
        URI url = URI.create(issueUrl);
        List<NameValuePair> parameters = URLEncodedUtils.parse(url, StandardCharsets.UTF_8);
        Optional<String> optionalProjectId = parameters.stream()
                .filter(parameter -> "id".equals(parameter.getName()))
                .map(NameValuePair::getValue)
                .findFirst();

        if (optionalProjectId.isEmpty()) {
            return Optional.empty();
        }

        String projectId = optionalProjectId.get();

        if (url.getPath().endsWith("/dashboard")) {
            return Optional.of(new ProjectIssueIdentifier(projectId, "decorator-summary-comment"));
        } else if (url.getPath().endsWith("security_hotspots")) {
            return parameters.stream()
                    .filter(parameter -> "hotspots".equals(parameter.getName()))
                    .map(NameValuePair::getValue)
                    .findFirst()
                    .map(issueId -> new ProjectIssueIdentifier(projectId, issueId));
        } else {
            return parameters.stream()
                    .filter(parameter -> "issues".equals(parameter.getName()))
                    .map(NameValuePair::getValue)
                    .findFirst()
                    .map(issueId -> new ProjectIssueIdentifier(projectId, issueId));
        }
    }

    public QualityGate.Status getQualityGateStatus() {
        return qualityGate.getStatus();
    }

    public String getRuleUrlWithRuleKey(String ruleKey) {
        return publicRootURL + "/coding_rules?open=" + encode(ruleKey) + "&rule_key=" + encode(ruleKey);
    }

    public Optional<String> getScannerProperty(String propertyName) {
        return Optional.ofNullable(scannerContext.getProperties().get(propertyName));
    }

    public String createAnalysisSummary(FormatterFactory formatterFactory) {

        BigDecimal newCoverage = getNewCoverage().orElse(null);
        BigDecimal coverage = getCoverage().orElse(null);

        BigDecimal newDuplications = findQualityGateCondition(CoreMetrics.NEW_DUPLICATED_LINES_DENSITY_KEY)
                .filter(condition -> condition.getStatus() != EvaluationStatus.NO_VALUE)
                .map(QualityGate.Condition::getValue)
                .map(BigDecimal::new)
                .orElse(null);

        double duplications =
                findMeasure(CoreMetrics.DUPLICATED_LINES_DENSITY_KEY).map(Measure::getDoubleValue).orElse(0D);

        NumberFormat decimalFormat = new DecimalFormat("#0.00", DecimalFormatSymbols.getInstance(Locale.ENGLISH));

        Map<RuleType, Long> issueCounts = countRuleByType();
        long issueTotal = issueCounts.values().stream().mapToLong(l -> l).sum();

        List<QualityGate.Condition> failedConditions = findFailedConditions();

        String baseImageUrl = getBaseImageUrl();

        Document document = new Document(new Paragraph((QualityGate.Status.OK == getQualityGateStatus() ?
                                                        new Image("Passed", baseImageUrl +
                                                                            "/checks/QualityGateBadge/passed.svg?sanitize=true") :
                                                        new Image("Failed", baseImageUrl +
                                                                            "/checks/QualityGateBadge/failed.svg?sanitize=true"))),
                                         failedConditions.isEmpty() ? new Text("") :
                                         new com.github.mc1arke.sonarqube.plugin.ce.pullrequest.markup.List(
                                                 com.github.mc1arke.sonarqube.plugin.ce.pullrequest.markup.List.Style.BULLET,
                                                 failedConditions.stream().map(c -> new ListItem(new Text(format(c))))
                                                         .toArray(ListItem[]::new)),
                                         new Heading(1, new Text("Analysis Details")), new Heading(2, new Text(
                issueTotal + " Issue" + (issueCounts.values().stream().mapToLong(l -> l).sum() == 1 ? "" : "s"))),
                                         new com.github.mc1arke.sonarqube.plugin.ce.pullrequest.markup.List(
                                                 com.github.mc1arke.sonarqube.plugin.ce.pullrequest.markup.List.Style.BULLET,
                                                 new ListItem(new Image("Bug",
                                                                        baseImageUrl + "/common/bug.svg?sanitize=true"),
                                                              new Text(" "), new Text(
                                                         pluralOf(issueCounts.get(RuleType.BUG), "Bug", "Bugs"))),
                                                 new ListItem(new Image("Vulnerability", baseImageUrl +
                                                                                         "/common/vulnerability.svg?sanitize=true"),
                                                              new Text(" "), new Text(pluralOf(
                                                         issueCounts.get(RuleType.VULNERABILITY) +
                                                         issueCounts.get(RuleType.SECURITY_HOTSPOT), "Vulnerability",
                                                         "Vulnerabilities"))), new ListItem(new Image("Code Smell",
                                                                                                      baseImageUrl +
                                                                                                      "/common/code_smell.svg?sanitize=true"),
                                                                                            new Text(" "), new Text(
                                                 pluralOf(issueCounts.get(RuleType.CODE_SMELL), "Code Smell",
                                                          "Code Smells")))),
                                         new Heading(2, new Text("Coverage and Duplications")),
                                         new com.github.mc1arke.sonarqube.plugin.ce.pullrequest.markup.List(
                                                 com.github.mc1arke.sonarqube.plugin.ce.pullrequest.markup.List.Style.BULLET,
                                                 new ListItem(createCoverageImage(newCoverage, baseImageUrl),
                                                              new Text(" "), new Text(
                                                         Optional.ofNullable(newCoverage).map(decimalFormat::format)
                                                                 .map(i -> i + "% Coverage")
                                                                 .orElse("No coverage information") + " (" +
                                                         decimalFormat.format(Optional.ofNullable(coverage).orElse(BigDecimal.valueOf(0))) + "% Estimated after merge)")),
                                                 new ListItem(createDuplicateImage(newDuplications, baseImageUrl),
                                                              new Text(" "), new Text(
                                                         Optional.ofNullable(newDuplications).map(decimalFormat::format)
                                                                 .map(i -> i + "% Duplicated Code")
                                                                 .orElse("No duplication information") + " (" +
                                                         decimalFormat.format(duplications) +
                                                         "% Estimated after merge)"))),
                                         new Paragraph(new Text(String.format("**Project ID:** %s", project.getKey()))),
                                         new Paragraph(new Link(getDashboardUrl(), new Text("View in SonarQube"))));

        return formatterFactory.documentFormatter().format(document, formatterFactory);
    }

    public String createAnalysisIssueSummary(PostAnalysisIssueVisitor.ComponentIssue componentIssue, FormatterFactory formatterFactory) {
        final PostAnalysisIssueVisitor.LightIssue issue = componentIssue.getIssue();

        String baseImageUrl = getBaseImageUrl();

        Long effort = issue.effortInMinutes();
        Node effortNode = (null == effort ? new Text("") : new Paragraph(new Text(String.format("**Duration (min):** %s", effort))));

        String resolution = issue.resolution();
        Node resolutionNode = (StringUtils.isBlank(resolution) ? new Text("") : new Paragraph(new Text(String.format("**Resolution:** %s ", resolution))));

        Document document = new Document(
                new Paragraph(new Text(String.format("**Type:** %s ", issue.type().name())), new Image(issue.type().name(), String.format("%s/checks/IssueType/%s.svg?sanitize=true", baseImageUrl, issue.type().name().toLowerCase()))),
                new Paragraph(new Text(String.format("**Severity:** %s ", issue.severity())), new Image(issue.severity(), String.format("%s/checks/Severity/%s.svg?sanitize=true", baseImageUrl, issue.severity().toLowerCase()))),
                new Paragraph(new Text(String.format("**Message:** %s", issue.getMessage()))),
                effortNode,
                resolutionNode,
                new Paragraph(new Text(String.format("**Project ID:** %s **Issue ID:** %s", project.getKey(), issue.key()))),
                new Paragraph(new Link(getIssueUrl(issue), new Text("View in SonarQube")))
        );
        return formatterFactory.documentFormatter().format(document, formatterFactory);
    }

    public String getBaseImageUrl() {
        return configuration.get(CommunityBranchPlugin.IMAGE_URL_BASE)
                .orElse(publicRootURL + "/static/communityBranchPlugin")
                .replaceAll("/*$", "");
    }

    public Optional<String> getSCMPathForIssue(PostAnalysisIssueVisitor.ComponentIssue componentIssue) {
        Component component = componentIssue.getComponent();
        if (Component.Type.FILE.equals(component.getType())) {
            return component.getReportAttributes().getScmPath();
        }
        return Optional.empty();
    }

    public PostAnalysisIssueVisitor getPostAnalysisIssueVisitor() {
        return postAnalysisIssueVisitor;
    }

    private static String encode(String original) {
        try {
            return URLEncoder.encode(original, StandardCharsets.UTF_8.name());
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException("Standard charset not found in JVM", e);
        }
    }

    private static Image createCoverageImage(BigDecimal coverage, String baseImageUrl) {
        if (null == coverage) {
            return new Image("No coverage information",
                             baseImageUrl + "/checks/CoverageChart/NoCoverageInfo.svg?sanitize=true");
        }
        BigDecimal matchedLevel = BigDecimal.ZERO;
        for (BigDecimal level : COVERAGE_LEVELS) {
            if (coverage.compareTo(level) >= 0) {
                matchedLevel = level;
                break;
            }
        }
        return new Image(matchedLevel + " percent coverage",
                         baseImageUrl + "/checks/CoverageChart/" + matchedLevel + ".svg?sanitize=true");
    }

    private static Image createDuplicateImage(BigDecimal duplications, String baseImageUrl) {
        if (null == duplications) {
            return new Image("No duplication information",
                             baseImageUrl + "/checks/Duplications/NoDuplicationInfo.svg?sanitize=true");
        }
        String matchedLevel = "20plus";
        for (DuplicationMapping level : DUPLICATION_LEVELS) {
            if (level.getDuplicationLevel().compareTo(duplications) >= 0) {
                matchedLevel = level.getImageName();
                break;
            }
        }
        return new Image(matchedLevel + " percent duplication",
                         baseImageUrl + "/checks/Duplications/" + matchedLevel + ".svg?sanitize=true");
    }


    public Date getAnalysisDate() {
        return analysis.getDate();
    }

    public String getAnalysisId() {
        return analysis.getAnalysisUuid();
    }

    public String getAnalysisProjectKey() {
        return project.getKey();
    }

    public String getAnalysisProjectName() {
        return project.getName();
    }

    public List<QualityGate.Condition> findFailedConditions() {
        return qualityGate.getConditions().stream().filter(c -> c.getStatus() == QualityGate.EvaluationStatus.ERROR)
                .collect(Collectors.toList());
    }

    public Optional<Measure> findMeasure(String metricKey) {
        return measuresHolder.getMeasureRepository().getRawMeasure(measuresHolder.getTreeRootHolder().getRoot(),
                                                                   measuresHolder.getMetricRepository()
                                                                           .getByKey(metricKey));
    }

    public Optional<QualityGate.Condition> findQualityGateCondition(String metricKey) {
        return qualityGate.getConditions().stream().filter(c -> metricKey.equals(c.getMetricKey())).findFirst();
    }

    public Map<RuleType, Long> countRuleByType() {
        return Arrays.stream(RuleType.values()).collect(Collectors.toMap(k -> k,
                                                                         k -> postAnalysisIssueVisitor.getIssues()
                                                                                 .stream()
                                                                                 .map(PostAnalysisIssueVisitor.ComponentIssue::getIssue)
                                                                                 .filter(i -> !CLOSED_ISSUE_STATUS
                                                                                         .contains(i.status()))
                                                                                 .filter(i -> k == i.type()).count()));
    }

    private static String pluralOf(long value, String singleLabel, String multiLabel) {
        return value + " " + (1 == value ? singleLabel : multiLabel);
    }


    public static String format(QualityGate.Condition condition) {
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

    public Optional<BigDecimal> getNewCoverage(){
        return findQualityGateCondition(CoreMetrics.NEW_COVERAGE_KEY)
                .filter(condition -> condition.getStatus() != EvaluationStatus.NO_VALUE)
                .map(QualityGate.Condition::getValue)
                .map(BigDecimal::new);
    }

    public Optional<BigDecimal> getCoverage(){
        return findMeasure(CoreMetrics.COVERAGE_KEY).map(Measure::getDoubleValue).map(BigDecimal::new);
    }

    public List<PostAnalysisIssueVisitor.ComponentIssue> getScmReportableIssues() {
        return postAnalysisIssueVisitor.getIssues().stream()
                .filter(i -> getSCMPathForIssue(i).isPresent())
                .filter(i -> i.getComponent().getType() == Component.Type.FILE)
                .filter(i -> i.getIssue().resolution() == null)
                .filter(i -> OPEN_ISSUE_STATUSES.contains(i.getIssue().status()))
                .collect(Collectors.toList());
    }

    public static class BranchDetails {

        private final String branchName;
        private final String commitId;

        BranchDetails(String branchName, String commitId) {
            this.branchName = branchName;
            this.commitId = commitId;
        }

        public String getBranchName() {
            return branchName;
        }

        public String getCommitId() {
            return commitId;
        }

    }

    public static class MeasuresHolder {

        private final MetricRepository metricRepository;
        private final MeasureRepository measureRepository;
        private final TreeRootHolder treeRootHolder;

        MeasuresHolder(MetricRepository metricRepository, MeasureRepository measureRepository,
                       TreeRootHolder treeRootHolder) {
            this.metricRepository = metricRepository;
            this.measureRepository = measureRepository;
            this.treeRootHolder = treeRootHolder;
        }

        public MetricRepository getMetricRepository() {
            return metricRepository;
        }

        public MeasureRepository getMeasureRepository() {
            return measureRepository;
        }

        public TreeRootHolder getTreeRootHolder() {
            return treeRootHolder;
        }

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

    public static class ProjectIssueIdentifier {

        private final String projectKey;
        private final String issueKey;

        public ProjectIssueIdentifier(String projectKey, String issueKey) {
            this.projectKey = projectKey;
            this.issueKey = issueKey;
        }

        public String getProjectKey() {
            return projectKey;
        }

        public String getIssueKey() {
            return issueKey;
        }
    }

}
