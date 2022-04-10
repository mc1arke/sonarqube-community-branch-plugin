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

import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.markup.Document;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.markup.FormatterFactory;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.markup.Heading;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.markup.Image;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.markup.Link;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.markup.ListItem;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.markup.Paragraph;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.markup.Text;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public final class AnalysisSummary {

    private final String summaryImageUrl;
    private final String projectKey;

    private final String statusDescription;
    private final String statusImageUrl;
    private final List<String> failedQualityGateConditions;
    private final String dashboardUrl;

    private final BigDecimal newCoverage;
    private final BigDecimal coverage;
    private final String coverageImageUrl;

    private final BigDecimal newDuplications;
    private final BigDecimal duplications;
    private final String duplicationsImageUrl;

    private final long totalIssueCount;

    private final long bugCount;
    private final String bugImageUrl;

    private final long securityHotspotCount;
    private final long vulnerabilityCount;
    private final String vulnerabilityImageUrl;

    private final long codeSmellCount;
    private final String codeSmellImageUrl;

    private AnalysisSummary(Builder builder) {
        this.summaryImageUrl = builder.summaryImageUrl;
        this.projectKey = builder.projectKey;
        this.statusDescription = builder.statusDescription;
        this.statusImageUrl = builder.statusImageUrl;
        this.failedQualityGateConditions = builder.failedQualityGateConditions;
        this.dashboardUrl = builder.dashboardUrl;
        this.newCoverage = builder.newCoverage;
        this.coverage = builder.coverage;
        this.coverageImageUrl = builder.coverageImageUrl;
        this.newDuplications = builder.newDuplications;
        this.duplications = builder.duplications;
        this.duplicationsImageUrl = builder.duplicationsImageUrl;
        this.totalIssueCount = builder.totalIssueCount;
        this.bugCount = builder.bugCount;
        this.bugImageUrl = builder.bugImageUrl;
        this.securityHotspotCount = builder.securityHotspotCount;
        this.vulnerabilityCount = builder.vulnerabilityCount;
        this.vulnerabilityImageUrl = builder.vulnerabilityImageUrl;
        this.codeSmellCount = builder.codeSmellCount;
        this.codeSmellImageUrl = builder.codeSmellImageUrl;
    }

    public String getSummaryImageUrl() {
        return summaryImageUrl;
    }

    public String getProjectKey() {
        return projectKey;
    }

    public String getStatusDescription() {
        return statusDescription;
    }

    public String getStatusImageUrl() {
        return statusImageUrl;
    }

    public List<String> getFailedQualityGateConditions() {
        return failedQualityGateConditions;
    }

    public String getDashboardUrl() {
        return dashboardUrl;
    }

    public BigDecimal getNewCoverage() {
        return newCoverage;
    }

    public BigDecimal getCoverage() {
        return coverage;
    }

    public String getCoverageImageUrl() {
        return coverageImageUrl;
    }

    public BigDecimal getNewDuplications() {
        return newDuplications;
    }

    public BigDecimal getDuplications() {
        return duplications;
    }

    public String getDuplicationsImageUrl() {
        return duplicationsImageUrl;
    }

    public long getTotalIssueCount() {
        return totalIssueCount;
    }

    public long getBugCount() {
        return bugCount;
    }

    public String getBugImageUrl() {
        return bugImageUrl;
    }

    public long getSecurityHotspotCount() {
        return securityHotspotCount;
    }

    public long getVulnerabilityCount() {
        return vulnerabilityCount;
    }

    public String getVulnerabilityImageUrl() {
        return vulnerabilityImageUrl;
    }

    public long getCodeSmellCount() {
        return codeSmellCount;
    }

    public String getCodeSmellImageUrl() {
        return codeSmellImageUrl;
    }

    public String format(FormatterFactory formatterFactory) {
        NumberFormat decimalFormat = new DecimalFormat("#0.00", DecimalFormatSymbols.getInstance(Locale.ENGLISH));

        List<String> failedConditions = getFailedQualityGateConditions();

        Document document = new Document(new Paragraph(new Image(getStatusDescription(), getStatusImageUrl())),
                failedConditions.isEmpty() ? new Text("") :
                        new com.github.mc1arke.sonarqube.plugin.ce.pullrequest.markup.List(
                                com.github.mc1arke.sonarqube.plugin.ce.pullrequest.markup.List.Style.BULLET,
                                failedConditions.stream()
                                        .map(Text::new)
                                        .map(ListItem::new)
                                        .toArray(ListItem[]::new)),
                new Heading(1, new Text("Analysis Details")),
                new Heading(2, new Text(pluralOf(getTotalIssueCount(), "Issue", "Issues"))),
                new com.github.mc1arke.sonarqube.plugin.ce.pullrequest.markup.List(
                        com.github.mc1arke.sonarqube.plugin.ce.pullrequest.markup.List.Style.BULLET,
                        new ListItem(new Image("Bug", getBugImageUrl()),
                                new Text(" "),
                                new Text(pluralOf(getBugCount(), "Bug", "Bugs"))),
                        new ListItem(new Image("Vulnerability", getVulnerabilityImageUrl()),
                                new Text(" "),
                                new Text(pluralOf(getVulnerabilityCount() + getSecurityHotspotCount(), "Vulnerability", "Vulnerabilities"))),
                        new ListItem(new Image("Code Smell", getCodeSmellImageUrl()),
                                new Text(" "),
                                new Text(pluralOf(getCodeSmellCount(), "Code Smell", "Code Smells")))),
                new Heading(2, new Text("Coverage and Duplications")),
                new com.github.mc1arke.sonarqube.plugin.ce.pullrequest.markup.List(
                        com.github.mc1arke.sonarqube.plugin.ce.pullrequest.markup.List.Style.BULLET,
                        new ListItem(new Image("Coverage", getCoverageImageUrl()),
                                new Text(" "), new Text(
                                Optional.ofNullable(getNewCoverage())
                                        .map(decimalFormat::format)
                                        .map(i -> i + "% Coverage")
                                        .orElse("No coverage information") + " (" +
                                        decimalFormat.format(Optional.ofNullable(getCoverage()).orElse(BigDecimal.valueOf(0))) + "% Estimated after merge)")),
                        new ListItem(new Image("Duplications", getDuplicationsImageUrl()),
                                new Text(" "),
                                new Text(Optional.ofNullable(getNewDuplications())
                                        .map(decimalFormat::format)
                                        .map(i -> i + "% Duplicated Code")
                                        .orElse("No duplication information") + " (" + decimalFormat.format(getDuplications()) + "% Estimated after merge)"))),
                new Paragraph(new Text(String.format("**Project ID:** %s", getProjectKey()))),
                new Paragraph(new Link(getDashboardUrl(), new Text("View in SonarQube"))));

        return formatterFactory.documentFormatter().format(document);
    }

    private static String pluralOf(long value, String singleLabel, String multiLabel) {
        return value + " " + (1 == value ? singleLabel : multiLabel);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private String summaryImageUrl;
        private String projectKey;

        private String statusDescription;
        private String statusImageUrl;
        private List<String> failedQualityGateConditions;
        private String dashboardUrl;

        private BigDecimal newCoverage;
        private BigDecimal coverage;
        private String coverageImageUrl;

        private BigDecimal newDuplications;
        private BigDecimal duplications;
        private String duplicationsImageUrl;

        private long totalIssueCount;

        private long bugCount;
        private String bugImageUrl;

        private long securityHotspotCount;
        private long vulnerabilityCount;
        private String vulnerabilityImageUrl;

        private long codeSmellCount;
        private String codeSmellImageUrl;

        private Builder() {
            super();
        }

        public Builder withSummaryImageUrl(String summaryImageUrl) {
            this.summaryImageUrl = summaryImageUrl;
            return this;
        }

        public Builder withProjectKey(String projectKey) {
            this.projectKey = projectKey;
            return this;
        }

        public Builder withStatusDescription(String statusDescription) {
            this.statusDescription = statusDescription;
            return this;
        }

        public Builder withStatusImageUrl(String statusImageUrl) {
            this.statusImageUrl = statusImageUrl;
            return this;
        }

        public Builder withFailedQualityGateConditions(List<String> failedQualityGateConditions) {
            this.failedQualityGateConditions = failedQualityGateConditions;
            return this;
        }

        public Builder withDashboardUrl(String dashboardUrl) {
            this.dashboardUrl = dashboardUrl;
            return this;
        }

        public Builder withNewCoverage(BigDecimal newCoverage) {
            this.newCoverage = newCoverage;
            return this;
        }

        public Builder withCoverage(BigDecimal coverage) {
            this.coverage = coverage;
            return this;
        }

        public Builder withCoverageImageUrl(String coverageImageUrl) {
            this.coverageImageUrl = coverageImageUrl;
            return this;
        }

        public Builder withNewDuplications(BigDecimal newDuplications) {
            this.newDuplications = newDuplications;
            return this;
        }

        public Builder withDuplications(BigDecimal duplications) {
            this.duplications = duplications;
            return this;
        }

        public Builder withDuplicationsImageUrl(String duplicationsImageUrl) {
            this.duplicationsImageUrl = duplicationsImageUrl;
            return this;
        }

        public Builder withTotalIssueCount(long totalIssueCount) {
            this.totalIssueCount = totalIssueCount;
            return this;
        }

        public Builder withBugCount(long bugCount) {
            this.bugCount = bugCount;
            return this;
        }

        public Builder withBugImageUrl(String bugImageUrl) {
            this.bugImageUrl = bugImageUrl;
            return this;
        }

        public Builder withSecurityHotspotCount(long securityHotspotCount) {
            this.securityHotspotCount = securityHotspotCount;
            return this;
        }

        public Builder withVulnerabilityCount(long vulnerabilityCount) {
            this.vulnerabilityCount = vulnerabilityCount;
            return this;
        }

        public Builder withVulnerabilityImageUrl(String vulnerabilityImageUrl) {
            this.vulnerabilityImageUrl = vulnerabilityImageUrl;
            return this;
        }

        public Builder withCodeSmellCount(long codeSmellCount) {
            this.codeSmellCount = codeSmellCount;
            return this;
        }

        public Builder withCodeSmellImageUrl(String codeSmellImageUrl) {
            this.codeSmellImageUrl = codeSmellImageUrl;
            return this;
        }

        public AnalysisSummary build() {
            return new AnalysisSummary(this);
        }
    }

}
