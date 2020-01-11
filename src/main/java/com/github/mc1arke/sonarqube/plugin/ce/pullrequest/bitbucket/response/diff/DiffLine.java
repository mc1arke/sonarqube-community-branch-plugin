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

public class DiffLine implements Serializable {
    private final int source;

    private final int destination;

    private final String line;

    private final boolean truncated;

    private final List<Integer> commentIds;

    @JsonCreator
    public DiffLine(@JsonProperty("source") int source, @JsonProperty("destination") int destination, @JsonProperty("line") String line, @JsonProperty("truncated") boolean truncated, @JsonProperty("commentIds") List<Integer> commentIds) {
        this.source = source;
        this.destination = destination;
        this.line = line;
        this.truncated = truncated;
        this.commentIds = commentIds;
    }

    public int getSource() {
        return source;
    }

    public int getDestination() {
        return destination;
    }

    public String getLine() {
        return line;
    }

    public boolean isTruncated() {
        return truncated;
    }

    public List<Integer> getCommentIds() {
        return commentIds;
    }
}
