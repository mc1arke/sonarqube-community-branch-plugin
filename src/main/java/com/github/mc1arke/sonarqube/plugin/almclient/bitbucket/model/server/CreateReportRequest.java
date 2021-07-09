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
package com.github.mc1arke.sonarqube.plugin.almclient.bitbucket.model.server;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.mc1arke.sonarqube.plugin.almclient.bitbucket.model.CodeInsightsReport;
import com.github.mc1arke.sonarqube.plugin.almclient.bitbucket.model.ReportData;

import java.time.Instant;
import java.util.List;

public class CreateReportRequest extends CodeInsightsReport {

    private final Instant createdDate;
    private final String logoUrl;

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
        super(data, details, title, reporter, link, result);
        this.createdDate = createdDate;
        this.logoUrl = logoUrl;
    }

    public Instant getCreatedDate() {
        return createdDate;
    }

    public String getLogoUrl() {
        return logoUrl;
    }
}
