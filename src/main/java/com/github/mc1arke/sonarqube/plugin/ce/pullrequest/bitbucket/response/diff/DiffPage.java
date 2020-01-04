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

public class DiffPage implements Serializable {
    private final String fromHash;

    private final String toHash;

    private final boolean truncated;

    private final List<Diff> diffs;

    @JsonCreator
    public DiffPage(@JsonProperty("fromHash") final String fromHash, @JsonProperty("toHash") final String toHash, @JsonProperty("truncated") final boolean truncated, @JsonProperty("diffs") final List<Diff> diffs) {
        this.fromHash = fromHash;
        this.toHash = toHash;
        this.truncated = truncated;
        this.diffs = diffs;
    }

    public String getFromHash() {
        return fromHash;
    }

    public String getToHash() {
        return toHash;
    }

    public boolean isTruncated() {
        return truncated;
    }

    public List<Diff> getDiffs() {
        return diffs;
    }
}
