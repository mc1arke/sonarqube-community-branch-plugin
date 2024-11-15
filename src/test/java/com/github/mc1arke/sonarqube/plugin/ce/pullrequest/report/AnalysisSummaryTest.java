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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.markup.Bold;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.markup.Document;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.markup.Formatter;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.markup.FormatterFactory;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.markup.Heading;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.markup.Image;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.markup.Link;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.markup.List;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.markup.ListItem;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.markup.Paragraph;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.markup.Text;

class AnalysisSummaryTest {

    @Test
    void shouldCreateAnalysisSummaryWithNoViolationsListIfQualityGatePasses() {
        AnalysisSummary underTest = AnalysisSummary.builder()
                .withNewDuplications(BigDecimal.valueOf(199))
                .withSummaryImageUrl("summaryImageUrl")
                .withProjectKey("projectKey")
                .withCoverage(new AnalysisSummary.UrlIconMetric<>("codeCoverageUrl", "codeCoverageImageUrl", BigDecimal.valueOf(303)))
                .withDashboardUrl("dashboardUrl")
                .withDuplications(new AnalysisSummary.UrlIconMetric<>("duplicationsUrl", "duplicationsImageUrl", BigDecimal.valueOf(66)))
                .withFailedQualityGateConditions(java.util.List.of())
                .withNewCoverage(BigDecimal.valueOf(99))
                .withSecurityHotspots(new AnalysisSummary.UrlIconMetric<>("securityHotspotsUrl", "securityHotspotsImageUrl", 69))
                .withStatusDescription("status description")
                .withStatusImageUrl("statusImageUrl")
                .withAcceptedIssues(new AnalysisSummary.UrlIconMetric<>("acceptedIssuesUrl", "acceptedIssuesImageUrl", 0))
                .withFixedIssues(new AnalysisSummary.UrlIconMetric<>("fixedIssuesUrl", "fixedIssuesImageUrl", 12))
                .withNewIssues(new AnalysisSummary.UrlIconMetric<>("newIssuesUrl", "newIssuesImageUrl", 666L))
                .build();

        Formatter<Document> formatter = mock();
        when(formatter.format(any())).thenReturn("formatted content");
        FormatterFactory formatterFactory = mock();
        when(formatterFactory.documentFormatter()).thenReturn(formatter);

        assertThat(underTest.format(formatterFactory)).isEqualTo("formatted content");

        ArgumentCaptor<Document> documentArgumentCaptor = ArgumentCaptor.captor();
        verify(formatter).format(documentArgumentCaptor.capture());

        Document expectedDocument = new Document(new Heading(3, new Image("status description", "statusImageUrl"),
            new Text(" "),
            new Text(("Quality Gate passed"))),
                new Heading(4, new Text("Issues")),
                new List(List.Style.BULLET,
                    new ListItem(
                        new Link("newIssuesUrl", new Image("New Issues","newIssuesImageUrl"),
                        new Text(" "),
                        new Text("666 New Issues"))),
                new ListItem(
                        new Link("fixedIssuesUrl", new Image("Fixed Issues","fixedIssuesImageUrl"),
                        new Text(" "),
                        new Text("12 Fixed Issues"))),
                new ListItem(
                        new Link("acceptedIssuesUrl", new Image("Accepted Issues", "acceptedIssuesImageUrl"),
                        new Text(" "),
                        new Text("0 Accepted Issues")))),
                new Heading(4, new Text("Measures")),
                new List(List.Style.BULLET,
                        new ListItem(
                            new Link("securityHotspotsUrl", new Image("Security Hotspots", "securityHotspotsImageUrl"),
                            new Text(" "),
                            new Text("69 Security Hotspots"))),
                        new ListItem(
                                new Link("codeCoverageUrl", new Image("Coverage", "codeCoverageImageUrl"),
                                new Text(" "),
                                new Text("99.00% Coverage (303.00% Estimated after merge)"))),
                    new ListItem(
                        new Link("duplicationsUrl", new Image("Duplications", "duplicationsImageUrl"),
                        new Text(" "),
                        new Text("199.00% Duplicated Code (66.00% Estimated after merge)")))),
                new Paragraph(new Bold(new Text("Project ID:")), new Text(" "), new Text("projectKey")),
                new Paragraph(new Link("dashboardUrl", new Text("View in SonarQube"))));

        assertThat(documentArgumentCaptor.getValue()).usingRecursiveComparison().isEqualTo(expectedDocument);
    }

    @Test
    void shouldReturnNotInfoForExpectedTotalDuplicationsWhereValueIsNull() {
        AnalysisSummary underTest = AnalysisSummary.builder()
            .withNewDuplications(BigDecimal.valueOf(199))
            .withSummaryImageUrl("summaryImageUrl")
            .withProjectKey("projectKey")
            .withCoverage(new AnalysisSummary.UrlIconMetric<>("codeCoverageUrl", "codeCoverageImageUrl", BigDecimal.valueOf(303)))
            .withDashboardUrl("dashboardUrl")
            .withDuplications(new AnalysisSummary.UrlIconMetric<>("duplicationsUrl", "duplicationsImageUrl", null))
            .withFailedQualityGateConditions(java.util.List.of())
            .withNewCoverage(BigDecimal.valueOf(99))
            .withSecurityHotspots(new AnalysisSummary.UrlIconMetric<>("securityHotspotsUrl", "securityHotspotsImageUrl", 69))
            .withStatusDescription("status description")
            .withStatusImageUrl("statusImageUrl")
            .withAcceptedIssues(new AnalysisSummary.UrlIconMetric<>("acceptedIssuesUrl", "acceptedIssuesImageUrl", 0))
            .withFixedIssues(new AnalysisSummary.UrlIconMetric<>("fixedIssuesUrl", "fixedIssuesImageUrl", 12))
            .withNewIssues(new AnalysisSummary.UrlIconMetric<>("newIssuesUrl", "newIssuesImageUrl", 666L))
            .build();

        Formatter<Document> formatter = mock();
        when(formatter.format(any())).thenReturn("formatted content");
        FormatterFactory formatterFactory = mock();
        when(formatterFactory.documentFormatter()).thenReturn(formatter);

        assertThat(underTest.format(formatterFactory)).isEqualTo("formatted content");

        ArgumentCaptor<Document> documentArgumentCaptor = ArgumentCaptor.captor();
        verify(formatter).format(documentArgumentCaptor.capture());

        Document expectedDocument = new Document(new Heading(3, new Image("status description", "statusImageUrl"),
            new Text(" "),
            new Text(("Quality Gate passed"))),
            new Heading(4, new Text("Issues")),
            new List(List.Style.BULLET,
                new ListItem(
                    new Link("newIssuesUrl", new Image("New Issues","newIssuesImageUrl"),
                        new Text(" "),
                        new Text("666 New Issues"))),
                new ListItem(
                    new Link("fixedIssuesUrl", new Image("Fixed Issues","fixedIssuesImageUrl"),
                        new Text(" "),
                        new Text("12 Fixed Issues"))),
                new ListItem(
                    new Link("acceptedIssuesUrl", new Image("Accepted Issues", "acceptedIssuesImageUrl"),
                        new Text(" "),
                        new Text("0 Accepted Issues")))),
            new Heading(4, new Text("Measures")),
            new List(List.Style.BULLET,
                new ListItem(
                    new Link("securityHotspotsUrl", new Image("Security Hotspots", "securityHotspotsImageUrl"),
                        new Text(" "),
                        new Text("69 Security Hotspots"))),
                new ListItem(
                    new Link("codeCoverageUrl", new Image("Coverage", "codeCoverageImageUrl"),
                        new Text(" "),
                        new Text("99.00% Coverage (303.00% Estimated after merge)"))),
                new ListItem(
                    new Link("duplicationsUrl", new Image("Duplications", "duplicationsImageUrl"),
                        new Text(" "),
                        new Text("199.00% Duplicated Code")))),
            new Paragraph(new Bold(new Text("Project ID:")), new Text(" "), new Text("projectKey")),
            new Paragraph(new Link("dashboardUrl", new Text("View in SonarQube"))));

        assertThat(documentArgumentCaptor.getValue()).usingRecursiveComparison().isEqualTo(expectedDocument);
    }


    @Test
    void shouldOnlyShowFailedConditionsAndNotMeasuresOrIssuesMetricsWhenQualityGateFails() {
        AnalysisSummary underTest = AnalysisSummary.builder()
            .withNewDuplications(BigDecimal.valueOf(199))
            .withSummaryImageUrl("summaryImageUrl")
            .withProjectKey("projectKey")
            .withCoverage(new AnalysisSummary.UrlIconMetric<>("codeCoverageUrl", "codeCoverageImageUrl", BigDecimal.valueOf(303)))
            .withDashboardUrl("dashboardUrl")
            .withDuplications(new AnalysisSummary.UrlIconMetric<>("duplicationsUrl", "duplicationsUrl", null))
            .withFailedQualityGateConditions(java.util.List.of("issuea", "issueb", "issuec"))
            .withNewCoverage(BigDecimal.valueOf(99))
            .withSecurityHotspots(new AnalysisSummary.UrlIconMetric<>("securityHotspotsUrl", "securityHotspotsImageUrl", 69))
            .withStatusDescription("status description")
            .withStatusImageUrl("statusImageUrl")
            .withAcceptedIssues(new AnalysisSummary.UrlIconMetric<>("acceptedIssuesUrl", "acceptedIssuesImageUrl", 1))
            .withFixedIssues(new AnalysisSummary.UrlIconMetric<>("fixedIssuesUrl", "fixedIssuesImageUrl", 1))
            .withNewIssues(new AnalysisSummary.UrlIconMetric<>("newIssuesUrl", "newIssuesImageUrl", 1L))
            .build();

        Formatter<Document> formatter = mock();
        when(formatter.format(any())).thenReturn("formatted content");
        FormatterFactory formatterFactory = mock();
        when(formatterFactory.documentFormatter()).thenReturn(formatter);

        assertThat(underTest.format(formatterFactory)).isEqualTo("formatted content");

        ArgumentCaptor<Document> documentArgumentCaptor = ArgumentCaptor.captor();
        verify(formatter).format(documentArgumentCaptor.capture());

        Document expectedDocument = new Document(new Heading(3, new Image("status description", "statusImageUrl"),
            new Text(" "),
            new Text("Quality Gate failed")),
            new List(List.Style.BULLET,
                new ListItem(new Text("issuea")),
                new ListItem(new Text("issueb")),
                new ListItem(new Text("issuec"))),
            new Paragraph(new Bold(new Text("Project ID:")), new Text(" "), new Text("projectKey")),
            new Paragraph(new Link("dashboardUrl", new Text("View in SonarQube"))));

        assertThat(documentArgumentCaptor.getValue()).usingRecursiveComparison().isEqualTo(expectedDocument);

    }

}
