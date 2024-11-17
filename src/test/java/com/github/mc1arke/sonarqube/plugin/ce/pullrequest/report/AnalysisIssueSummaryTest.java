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

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.markup.Document;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.markup.Formatter;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.markup.FormatterFactory;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.markup.Link;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.markup.Paragraph;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.markup.Text;

class AnalysisIssueSummaryTest {

    @Test
    void shouldCreateCorrectOutputDocument() {
        AnalysisIssueSummary underTest = AnalysisIssueSummary.builder()
                .withMessage("message")
                .withIssueUrl("issueUrl")
                .build();

        FormatterFactory formatterFactory = mock();
        Formatter<Document> documentFormatter = mock();
        when(documentFormatter.format(any())).thenReturn("output content");
        when(formatterFactory.documentFormatter()).thenReturn(documentFormatter);

        assertThat(underTest.format(formatterFactory)).isEqualTo("output content");

        ArgumentCaptor<Document> documentArgumentCaptor = ArgumentCaptor.captor();
        verify(documentFormatter).format(documentArgumentCaptor.capture());

        assertThat(documentArgumentCaptor.getValue())
                .usingRecursiveComparison()
                .isEqualTo(
                    new Document(
                            new Paragraph(new Text("message")),
                            new Paragraph(new Link("issueUrl", new Text("View in SonarQube")))
                )
        );
    }

}