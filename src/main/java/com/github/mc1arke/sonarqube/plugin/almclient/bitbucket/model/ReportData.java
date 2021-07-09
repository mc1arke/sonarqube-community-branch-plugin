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
package com.github.mc1arke.sonarqube.plugin.almclient.bitbucket.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class ReportData {
    private final String title;
    private final DataValue value;
    @JsonProperty("type")
    private final String type;

    @JsonCreator
    public ReportData(@JsonProperty("title") String title, @JsonProperty("value") DataValue value) {
        this.title = title;
        this.value = value;
        this.type = typeFrom(value);
    }

    private static String typeFrom(DataValue value) {
        if (value instanceof DataValue.Link || value instanceof DataValue.CloudLink) {
            return "LINK";
        } else if (value instanceof DataValue.Percentage) {
            return "PERCENTAGE";
        } else {
            return "TEXT";
        }
    }

    public String getTitle() {
        return title;
    }

    public DataValue getValue() {
        return value;
    }

    public String getType() {
        return type;
    }
}
