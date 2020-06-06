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
package com.github.mc1arke.sonarqube.plugin.ce.pullrequest.bitbucket.client.model.cloud;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.bitbucket.client.model.IAnnotation;

import java.io.Serializable;

public class CloudAnnotation implements Serializable, IAnnotation {
    @JsonProperty("external_id")
    private final String externalId;
    @JsonProperty("line")
    private final int line;
    @JsonProperty("summary")
    private final String link;
    @JsonProperty("message")
    private final String message;
    @JsonProperty("path")
    private final String path;
    @JsonProperty("severity")
    private final String severity;
    @JsonProperty("annotation_type")
    private final String annotationType;

    @JsonCreator
    public CloudAnnotation(String externalId,
                           int line,
                           String link,
                           String message,
                           String path,
                           String severity,
                           String annotationType) {
        this.externalId = externalId;
        this.line = line;
        this.link = link;
        this.message = message;
        this.path = path;
        this.severity = severity;
        this.annotationType = annotationType;
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

    public String getAnnotationType() {
        return annotationType;
    }
}
