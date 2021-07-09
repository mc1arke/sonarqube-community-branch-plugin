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
import com.github.mc1arke.sonarqube.plugin.almclient.bitbucket.model.CodeInsightsAnnotation;

public class Annotation extends CodeInsightsAnnotation {
    private final String externalId;

    private final String link;

    private final String type;

    @JsonCreator
    public Annotation(@JsonProperty("externalId") String externalId,
                      @JsonProperty("line") int line,
                      String link,
                      String message,
                      String path,
                      String severity,
                      @JsonProperty("type") String type) {
        super(line, message, path, severity);
        this.externalId = externalId;
        this.link = link;
        this.type = type;
    }

    public String getExternalId() {
        return externalId;
    }

    public String getLink() {
        return link;
    }

    public String getType() {
        return type;
    }
}
