/*
 * Copyright (C) 2019-2022 Michael Clarke
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
package com.github.mc1arke.sonarqube.plugin.almclient.github.v3.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class Repository {

    private final String nodeId;
    private final String fullName;
    private final String htmlUrl;
    private final String name;
    private final Owner owner;

    @JsonCreator
    public Repository(@JsonProperty("node_id") String nodeId, @JsonProperty("full_name") String fullName, @JsonProperty("html_url") String htmlUrl, @JsonProperty("name") String name, @JsonProperty("owner") Owner owner) {
        this.nodeId = nodeId;
        this.fullName = fullName;
        this.htmlUrl = htmlUrl;
        this.name = name;
        this.owner = owner;
    }

    public String getFullName() {
        return fullName;
    }

    public String getNodeId() {
        return nodeId;
    }

    public String getHtmlUrl() {
        return htmlUrl;
    }

    public String getName() {
        return name;
    }

    public Owner getOwner() {
        return owner;
    }
}
