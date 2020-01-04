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

public class ActivityPage implements Serializable {
    private final int size;

    private final int limit;

    private final boolean isLastPage;

    private final int start;

    private final int nextPageStart;

    private final Activity[] values;

    @JsonCreator
    public ActivityPage(@JsonProperty("size") final int size, @JsonProperty("limit") final int limit, @JsonProperty("isLastPage") final boolean isLastPage, @JsonProperty("start") final int start, @JsonProperty("nextPageStart") final int nextPageStart, @JsonProperty("values") final Activity[] values) {
        this.size = size;
        this.limit = limit;
        this.isLastPage = isLastPage;
        this.start = start;
        this.nextPageStart = nextPageStart;
        this.values = values;
    }

    public int getSize() {
        return size;
    }

    public int getLimit() {
        return limit;
    }

    public boolean isLastPage() {
        return isLastPage;
    }

    public int getStart() {
        return start;
    }

    public int getNextPageStart() {
        return nextPageStart;
    }

    public Activity[] getValues() {
        return values;
    }
}
