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
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.markup.Image;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.markup.Link;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.markup.Paragraph;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.markup.Text;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AnalysisIssueSummaryTest {

    @Test
    void shouldCreateCorrectOutputDocument() {
        AnalysisIssueSummary underTest = AnalysisIssueSummary.builder()
                .withProjectKey("projectKey")
                .withTypeImageUrl("typeImageUrl")
                .withType("type")
                .withSeverityImageUrl("severityImageUrl")
                .withSeverity("severity")
                .withResolution("resolution")
                .withMessage("message")
                .withIssueUrl("issueUrl")
                .withIssueKey("issueKey")
                .withEffortInMinutes(101L)
                .build();

        FormatterFactory formatterFactory = mock(FormatterFactory.class);
        Formatter<Document> documentFormatter = mock(Formatter.class);
        when(documentFormatter.format(any())).thenReturn("output content");
        when(formatterFactory.documentFormatter()).thenReturn(documentFormatter);

        assertThat(underTest.format(formatterFactory)).isEqualTo("output content");

        ArgumentCaptor<Document> documentArgumentCaptor = ArgumentCaptor.forClass(Document.class);
        verify(documentFormatter).format(documentArgumentCaptor.capture());

        assertThat(documentArgumentCaptor.getValue())
                .usingRecursiveComparison()
                .isEqualTo(
                    new Document(
                            new Paragraph(
                                    new Text("**Type:** type "),
                                    new Image("type", "typeImageUrl")
                            ),
                            new Paragraph(
                                    new Text("**Severity:** severity "),
                                    new Image("severity", "severityImageUrl")
                            ),
                            new Paragraph(new Text("**Message:** message")),
                            new Paragraph(new Text("**Duration (min):** 101")),
                            new Paragraph(new Text("**Resolution:** resolution")),
                            new Paragraph(new Text("**Project ID:** projectKey **Issue ID:** issueKey")),
                            new Paragraph(new Link("issueUrl", new Text("View in SonarQube")))
                )
        );
    }

}