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
package com.github.mc1arke.sonarqube.plugin.ce.pullrequest.bitbucket.client.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Class for reusing models between the cloud and the server version
 */
public class CodeInsightsAnnotation {
    @JsonProperty("line")
    private final int line;
    @JsonProperty("summary")
    private final String message;
    @JsonProperty("path")
    private final String path;
    @JsonProperty("severity")
    private final String severity;

    public CodeInsightsAnnotation(int line, String message, String path, String severity) {
        this.line = line;
        this.message = message;
        this.path = path;
        this.severity = severity;
    }

    public int getLine() {
        return line;
    }

    public String getMessage() {
        return message;
    }

    public String getPath() {
        return path;
    }

    public String getSeverity() {
        return severity;
    }

}
