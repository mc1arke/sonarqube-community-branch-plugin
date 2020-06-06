/*
 * Copyright (C) 2020 Mathias Åhsberg
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
package com.github.mc1arke.sonarqube.plugin.ce.pullrequest.bitbucket.client.model.server;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.bitbucket.client.model.ReportData;

import java.time.Instant;
import java.util.List;

public class CreateReportRequest {
    private final List<ReportData> data;
    private final String details;
    private final String title;
    private final String reporter;
    private final Instant createdDate;
    private final String link;
    private final String logoUrl;
    private final String result;

    @JsonCreator
    public CreateReportRequest(
            @JsonProperty("data") List<ReportData> data,
            @JsonProperty("details") String details,
            @JsonProperty("title") String title,
            @JsonProperty("reporter") String reporter,
            @JsonProperty("createdDate") Instant createdDate,
            @JsonProperty("link") String link,
            @JsonProperty("logoUrl") String logoUrl,
            @JsonProperty("result") String result) {
        this.data = data;
        this.details = details;
        this.title = title;
        this.reporter = reporter;
        this.createdDate = createdDate;
        this.link = link;
        this.logoUrl = logoUrl;
        this.result = result;
    }

    public List<ReportData> getData() {
        return data;
    }

    public String getDetails() {
        return details;
    }

    public String getTitle() {
        return title;
    }

    public String getReporter() {
        return reporter;
    }

    public Instant getCreatedDate() {
        return createdDate;
    }

    public String getLink() {
        return link;
    }

    public String getLogoUrl() {
        return logoUrl;
    }

    public String getResult() {
        return result;
    }
}
