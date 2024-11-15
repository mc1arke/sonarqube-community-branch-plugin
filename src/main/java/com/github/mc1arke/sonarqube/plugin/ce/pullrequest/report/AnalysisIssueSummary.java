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

import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.markup.Document;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.markup.FormatterFactory;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.markup.Link;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.markup.Paragraph;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.markup.Text;

public final class AnalysisIssueSummary {

    private final String issueUrl;
    private final String message;

    private AnalysisIssueSummary(Builder builder) {
        this.issueUrl = builder.issueUrl;
        this.message = builder.message;
    }

    public String getIssueUrl() {
        return issueUrl;
    }

    public String getMessage() {
        return message;
    }

    public String format(FormatterFactory formatterFactory) {
        Document document = new Document(
                new Paragraph(new Text(getMessage())),
                new Paragraph(new Link(getIssueUrl(), new Text("View in SonarQube")))
        );

        return formatterFactory.documentFormatter().format(document);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private String issueUrl;
        private String message;

        private Builder() {
            super();
        }

        public Builder withIssueUrl(String issueUrl) {
            this.issueUrl = issueUrl;
            return this;
        }

        public Builder withMessage(String message) {
            this.message = message;
            return this;
        }

        public AnalysisIssueSummary build() {
            return new AnalysisIssueSummary(this);
        }
    }
}
