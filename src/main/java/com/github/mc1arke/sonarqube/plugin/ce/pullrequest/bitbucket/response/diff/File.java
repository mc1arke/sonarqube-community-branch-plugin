/*
 * Copyright (C) 2019 Oliver Jedinger
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
package com.github.mc1arke.sonarqube.plugin.ce.pullrequest.bitbucket.response.diff;

import java.io.Serializable;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class File implements Serializable {
    private final String parent;

    private final String name;

    private final String extension;

    private final String toString;

    private final List<String> components;

    @JsonCreator
    public File(@JsonProperty("parent") final String parent, @JsonProperty("name") final String name, @JsonProperty("extension") final String extension, @JsonProperty("toString") final String toString, @JsonProperty("components") final List<String> components) {
        this.parent = parent;
        this.name = name;
        this.extension = extension;
        this.toString = toString;
        this.components = components;
    }

    public String getParent() {
        return parent;
    }

    public String getName() {
        return name;
    }

    public String getExtension() {
        return extension;
    }

    public String getToString() {
        return toString;
    }

    public List<String> getComponents() {
        return components;
    }
}
