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
package com.github.mc1arke.sonarqube.plugin.almclient.bitbucket.model.cloud;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.mc1arke.sonarqube.plugin.almclient.bitbucket.model.CodeInsightsAnnotation;

public class CloudAnnotation extends CodeInsightsAnnotation {
    private final String externalId;
    private final String link;
    private final String annotationType;

    @JsonCreator
    public CloudAnnotation(String externalId,
                           int line,
                           String link,
                           String message,
                           String path,
                           String severity,
                           String annotationType) {
        super(line, message, path, severity);
        this.externalId = externalId;
        this.link = link;
        this.annotationType = annotationType;
    }

    @Override
    @JsonProperty("summary")
    public String getMessage() {
        return super.getMessage();
    }

    @JsonProperty("external_id")
    public String getExternalId() {
        return externalId;
    }

    @JsonProperty("link")
    public String getLink() {
        return link;
    }

    @JsonProperty("annotation_type")
    public String getAnnotationType() {
        return annotationType;
    }
}
