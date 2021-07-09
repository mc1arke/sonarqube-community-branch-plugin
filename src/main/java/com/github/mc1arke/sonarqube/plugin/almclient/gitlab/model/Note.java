/*
 * Copyright (C) 2019-2021 Markus Heberling, Michael Clarke
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
package com.github.mc1arke.sonarqube.plugin.almclient.gitlab.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class Note {

    private final long id;
    private final boolean system;
    private final User author;
    private final String body;
    private final boolean resolved;
    private final boolean resolvable;

    @JsonCreator
    public Note(@JsonProperty("id") long id, @JsonProperty("system") boolean system, @JsonProperty("author") User author,
                @JsonProperty("body") String body, @JsonProperty("resolved") boolean resolved,
                @JsonProperty("resolvable") boolean resolvable) {
        this.id = id;
        this.system = system;
        this.author = author;
        this.body = body;
        this.resolved = resolved;
        this.resolvable = resolvable;
    }

    public long getId() {
        return id;
    }

    public boolean isSystem() {
        return system;
    }

    public User getAuthor() {
        return author;
    }

    public String getBody() {
        return body;
    }

    public boolean isResolved() {
        return resolved;
    }

    public boolean isResolvable() {
        return resolvable;
    }
}
