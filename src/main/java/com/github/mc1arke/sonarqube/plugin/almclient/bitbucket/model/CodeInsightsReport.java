/*
 * Copyright (C) 2020 Marvin Wichmann
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
package com.github.mc1arke.sonarqube.plugin.almclient.bitbucket.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Interface for reusing models between the cloud and the server version
 */
public class CodeInsightsReport {
    @JsonProperty("data")
    private final List<ReportData> data;
    @JsonProperty("details")
    private final String details;
    @JsonProperty("title")
    private final String title;
    @JsonProperty("reporter")
    private final String reporter;
    @JsonProperty("link")
    private final String link;
    @JsonProperty("result")
    private final String result;

    public CodeInsightsReport(List<ReportData> data, String details, String title, String reporter, String link, String result) {
        this.data = data;
        this.details = details;
        this.title = title;
        this.reporter = reporter;
        this.link = link;
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

    public String getLink() {
        return link;
    }

    public String getResult() {
        return result;
    }
}
