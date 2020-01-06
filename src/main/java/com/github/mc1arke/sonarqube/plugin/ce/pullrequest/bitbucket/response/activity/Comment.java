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
package com.github.mc1arke.sonarqube.plugin.ce.pullrequest.bitbucket.response.activity;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class Comment implements Serializable {
    private final int id;

    private final int version;

    private final String text;

    private final User author;

    @JsonCreator
    public Comment(@JsonProperty("id") final int id, @JsonProperty("version") final int version, @JsonProperty("text") final String text, @JsonProperty("author") final User author) {
        this.id = id;
        this.version = version;
        this.text = text;
        this.author = author;
    }

    public int getId() {
        return id;
    }

    public int getVersion() {
        return version;
    }

    public String getText() {
        return text;
    }

    public User getAuthor() {
        return author;
    }
}
