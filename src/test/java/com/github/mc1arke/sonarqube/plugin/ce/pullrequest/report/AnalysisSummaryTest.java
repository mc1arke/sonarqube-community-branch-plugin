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
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.markup.Formatter;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.markup.FormatterFactory;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.markup.Heading;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.markup.Image;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.markup.Link;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.markup.List;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.markup.ListItem;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.markup.Paragraph;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.markup.Text;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class AnalysisSummaryTest {

    @Test
    void testCreateAnalysisSummary() {
        AnalysisSummary underTest = AnalysisSummary.builder()
                .withNewDuplications(BigDecimal.valueOf(199))
                .withSummaryImageUrl("summaryImageUrl")
                .withProjectKey("projectKey")
                .withBugCount(911)
                .withBugImageUrl("bugImageUrl")
                .withCodeSmellCount(1)
                .withCoverage(BigDecimal.valueOf(303))
                .withCodeSmellImageUrl("codeSmellImageUrl")
                .withCoverageImageUrl("codeCoverageImageUrl")
                .withDashboardUrl("dashboardUrl")
                .withDuplications(BigDecimal.valueOf(66))
                .withDuplicationsImageUrl("duplicationsImageUrl")
                .withFailedQualityGateConditions(java.util.List.of("issuea", "issueb", "issuec"))
                .withNewCoverage(BigDecimal.valueOf(99))
                .withSecurityHotspotCount(69)
                .withStatusDescription("status description")
                .withStatusImageUrl("statusImageUrl")
                .withTotalIssueCount(666)
                .withVulnerabilityCount(96)
                .withVulnerabilityImageUrl("vulnerabilityImageUrl")
                .build();

        Formatter<Document> formatter = mock(Formatter.class);
        doReturn("formatted content").when(formatter).format(any());
        FormatterFactory formatterFactory = mock(FormatterFactory.class);
        doReturn(formatter).when(formatterFactory).documentFormatter();

        assertThat(underTest.format(formatterFactory)).isEqualTo("formatted content");

        ArgumentCaptor<Document> documentArgumentCaptor = ArgumentCaptor.forClass(Document.class);
        verify(formatter).format(documentArgumentCaptor.capture());

        Document expectedDocument = new Document(new Paragraph(new Image("status description", "statusImageUrl")),
                new List(List.Style.BULLET,
                        new ListItem(new Text("issuea")),
                        new ListItem(new Text("issueb")),
                        new ListItem(new Text("issuec"))),
                new Heading(1, new Text("Analysis Details")),
                new Heading(2, new Text("666 Issues")),
                new List(List.Style.BULLET,
                    new ListItem(
                        new Image("Bug","bugImageUrl"),
                        new Text(" "),
                        new Text("911 Bugs")),
                new ListItem(
                        new Image("Vulnerability","vulnerabilityImageUrl"),
                        new Text(" "),
                        new Text("165 Vulnerabilities")),
                new ListItem(
                        new Image("Code Smell", "codeSmellImageUrl"),
                        new Text(" "),
                        new Text("1 Code Smell"))),
                new Heading(2, new Text("Coverage and Duplications")),
                new List(List.Style.BULLET,
                        new ListItem(
                            new Image("Coverage", "codeCoverageImageUrl"),
                            new Text(" "),
                            new Text("99.00% Coverage (303.00% Estimated after merge)")),
                        new ListItem(
                                new Image("Duplications", "duplicationsImageUrl"),
                                new Text(" "),
                                new Text("199.00% Duplicated Code (66.00% Estimated after merge)"))),
                new Paragraph(new Text("**Project ID:** projectKey")),
                new Paragraph(new Link("dashboardUrl", new Text("View in SonarQube"))));

        assertThat(documentArgumentCaptor.getValue()).usingRecursiveComparison().isEqualTo(expectedDocument);

    }

}
