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

import java.io.Serializable;

public class ServerProperties implements Serializable {

    public static final String CODE_INSIGHT_VERSION = "5.15";

    private final String version;

    @JsonCreator
    public ServerProperties(@JsonProperty("version") String version) {
        this.version = version;
    }

    public String getVersion() {
        return version;
    }

    public boolean hasCodeInsightsApi() {
        return compareTo(CODE_INSIGHT_VERSION) >= 0;
    }

    private int compareTo(String other) {
        String[] current = semver(version);
        String[] minimum = semver(other);

        int length = Math.max(current.length, minimum.length);
        for (int i = 0; i < length; i++) {
            int thisPart = i < current.length ?
                    Integer.parseInt(current[i]) : 0;
            int thatPart = i < minimum.length ?
                    Integer.parseInt(minimum[i]) : 0;
            if (thisPart < thatPart)
                return -1;
            if (thisPart > thatPart)
                return 1;
        }
        return 0;
    }

    private String[] semver(String v) {
        return v.split("\\.");
    }
}
