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

import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.markup.Bold;
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
    private final UrlIconMetric<BigDecimal> coverage;

    private final BigDecimal newDuplications;
    private final UrlIconMetric<BigDecimal> duplications;

    private final UrlIconMetric<Long> newIssues;
    private final UrlIconMetric<Integer> acceptedIssues;
    private final UrlIconMetric<Integer> fixedIssues;
    private final UrlIconMetric<Integer> securityHotspots;

    private AnalysisSummary(Builder builder) {
        this.summaryImageUrl = builder.summaryImageUrl;
        this.projectKey = builder.projectKey;
        this.statusDescription = builder.statusDescription;
        this.statusImageUrl = builder.statusImageUrl;
        this.failedQualityGateConditions = builder.failedQualityGateConditions;
        this.dashboardUrl = builder.dashboardUrl;
        this.newCoverage = builder.newCoverage;
        this.coverage = builder.coverage;
        this.newDuplications = builder.newDuplications;
        this.duplications = builder.duplications;

        this.securityHotspots = builder.securityHotspots;
        this.newIssues = builder.newIssues;
        this.acceptedIssues = builder.acceptedIssues;
        this.fixedIssues = builder.fixedIssues;
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

    public UrlIconMetric<BigDecimal> getCoverage() {
        return coverage;
    }

    public BigDecimal getNewDuplications() {
        return newDuplications;
    }

    public UrlIconMetric<BigDecimal> getDuplications() {
        return duplications;
    }

    public UrlIconMetric<Integer> getSecurityHotspots() {
        return securityHotspots;
    }

    public UrlIconMetric<Integer> getAcceptedIssues() {
        return acceptedIssues;
    }

    public UrlIconMetric<Integer> getFixedIssues() {
        return fixedIssues;
    }

    public UrlIconMetric<Long> getNewIssues() {
        return newIssues;
    }

    public String format(FormatterFactory formatterFactory) {
        NumberFormat decimalFormat = new DecimalFormat("#0.00", DecimalFormatSymbols.getInstance(Locale.ENGLISH));

        List<String> failedConditions = getFailedQualityGateConditions();

        if (failedConditions.isEmpty()) {
            Document document = new Document(new Heading(3, new Image(getStatusDescription(), getStatusImageUrl()),
                new Text(" "),
                new Text("Quality Gate passed")),
                new Heading(4, new Text("Issues")),
                new com.github.mc1arke.sonarqube.plugin.ce.pullrequest.markup.List(
                    com.github.mc1arke.sonarqube.plugin.ce.pullrequest.markup.List.Style.BULLET,
                    new ListItem(new Link(getNewIssues().getUrl(), new Image("New Issues", getNewIssues().getIconUrl()), new Text(" "), new Text(pluralOf(getNewIssues().getValue(), "New Issue", "New Issues")))),
                    new ListItem(new Link(getFixedIssues().getUrl(), new Image("Fixed Issues", getFixedIssues().getIconUrl()), new Text(" "), new Text(pluralOf(getFixedIssues().getValue(), "Fixed Issue", "Fixed Issues")))),
                    new ListItem(new Link(getAcceptedIssues().getUrl(), new Image("Accepted Issues", getAcceptedIssues().getIconUrl()), new Text(" "), new Text(pluralOf(getAcceptedIssues().getValue(), "Accepted Issue", "Accepted Issues"))))
                ),
                new Heading(4, new Text("Measures")),
                new com.github.mc1arke.sonarqube.plugin.ce.pullrequest.markup.List(
                    com.github.mc1arke.sonarqube.plugin.ce.pullrequest.markup.List.Style.BULLET,
                    new ListItem(new Link(getSecurityHotspots().getUrl(), new Image("Security Hotspots", getSecurityHotspots().getIconUrl()), new Text(" "), new Text(pluralOf(getSecurityHotspots().getValue(), "Security Hotspot", "Security Hotspots")))),
                    new ListItem(new Link(getCoverage().getUrl(), new Image("Coverage", getCoverage().getIconUrl()), new Text(" "), new Text(
                        Optional.ofNullable(getNewCoverage())
                            .map(decimalFormat::format)
                            .map(i -> i + "% Coverage")
                            .orElse("No data about coverage")
                            + Optional.ofNullable(getCoverage().getValue())
                            .map(decimalFormat::format)
                            .map( i -> " (" + i + "% Estimated after merge)")
                            .orElse("")))),
                    new ListItem(new Link(getDuplications().getUrl(), new Image("Duplications", getDuplications().getIconUrl()), new Text(" "), new Text(Optional.ofNullable(getNewDuplications())
                        .map(decimalFormat::format)
                        .map(i -> i + "% Duplicated Code")
                        .orElse("No data about duplications")
                        + Optional.ofNullable(getDuplications().getValue())
                        .map(decimalFormat::format)
                        .map(i -> " (" + i + "% Estimated after merge)")
                        .orElse(""))))),
                new Paragraph(new Bold(new Text("Project ID:")), new Text(" "), new Text(getProjectKey())),
                new Paragraph(new Link(getDashboardUrl(), new Text("View in SonarQube"))));

            return formatterFactory.documentFormatter().format(document);
        } else {
            Document document = new Document(new Heading(3, new Image(getStatusDescription(), getStatusImageUrl()),
                new Text(" "),
                new Text("Quality Gate failed")),
                new com.github.mc1arke.sonarqube.plugin.ce.pullrequest.markup.List(
                    com.github.mc1arke.sonarqube.plugin.ce.pullrequest.markup.List.Style.BULLET,
                    failedConditions.stream()
                        .map(Text::new)
                        .map(ListItem::new)
                        .toArray(ListItem[]::new)),
                new Paragraph(new Bold(new Text("Project ID:")), new Text(" "), new Text(getProjectKey())),
                new Paragraph(new Link(getDashboardUrl(), new Text("View in SonarQube"))));

            return formatterFactory.documentFormatter().format(document);
        }
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
        private UrlIconMetric<BigDecimal> coverage;

        private BigDecimal newDuplications;
        private UrlIconMetric<BigDecimal> duplications;

        private UrlIconMetric<Long> newIssues;
        private UrlIconMetric<Integer> fixedIssues;
        private UrlIconMetric<Integer> acceptedIssues;
        private UrlIconMetric<Integer> securityHotspots;

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

        public Builder withCoverage(UrlIconMetric<BigDecimal> coverage) {
            this.coverage = coverage;
            return this;
        }

        public Builder withNewDuplications(BigDecimal newDuplications) {
            this.newDuplications = newDuplications;
            return this;
        }

        public Builder withDuplications(UrlIconMetric<BigDecimal> duplications) {
            this.duplications = duplications;
            return this;
        }

        public Builder withSecurityHotspots(UrlIconMetric<Integer> securityHotspots) {
            this.securityHotspots = securityHotspots;
            return this;
        }

        public Builder withNewIssues(UrlIconMetric<Long> newIssues) {
            this.newIssues = newIssues;
            return this;
        }

        public Builder withFixedIssues(UrlIconMetric<Integer> fixedIssues) {
            this.fixedIssues = fixedIssues;
            return this;
        }

        public Builder withAcceptedIssues(UrlIconMetric<Integer> acceptedIssues) {
            this.acceptedIssues = acceptedIssues;
            return this;
        }

        public AnalysisSummary build() {
            return new AnalysisSummary(this);
        }
    }

    public static class UrlIconMetric<T extends Number> {

        private final String url;
        private final String iconUrl;
        private final T value;

        public UrlIconMetric(String url, String iconUrl, T value) {
            this.url = url;
            this.iconUrl = iconUrl;
            this.value = value;
        }

        public String getUrl() {
            return url;
        }

        public String getIconUrl() {
            return iconUrl;
        }

        public T getValue() {
            return value;
        }
    }

}
