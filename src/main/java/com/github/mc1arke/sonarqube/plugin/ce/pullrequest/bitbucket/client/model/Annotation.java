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
package com.github.mc1arke.sonarqube.plugin.ce.pullrequest.bitbucket.client.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;

public class Annotation implements Serializable {
    private final String externalId;
    private final int line;
    private final String link;
    private final String message;
    private final String path;
    private final String severity;
    private final String type;

    @JsonCreator
    public Annotation(@JsonProperty("externalId") String externalId,
                      @JsonProperty("line") int line,
                      @JsonProperty("link") String link,
                      @JsonProperty("message") String message,
                      @JsonProperty("path") String path,
                      @JsonProperty("severity") String severity,
                      @JsonProperty("type") String type) {
        this.externalId = externalId;
        this.line = line;
        this.link = link;
        this.message = message;
        this.path = path;
        this.severity = severity;
        this.type = type;
    }

    public String getExternalId() {
        return externalId;
    }

    public int getLine() {
        return line;
    }

    public String getLink() {
        return link;
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

    public String getType() {
        return type;
    }
}
