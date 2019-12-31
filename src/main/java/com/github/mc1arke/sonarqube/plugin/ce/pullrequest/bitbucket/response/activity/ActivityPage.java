/*
 * Copyright (C) 2019 Michael Clarke
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
    @JsonProperty
    private int size;

    @JsonProperty
    private int limit;

    @JsonProperty
    private boolean isLastPage;

    @JsonProperty
    private int start;

    @JsonProperty
    private int nextPageStart;

    @JsonProperty
    private Activity[] values;

    @JsonCreator
    public ActivityPage()
    {
        super();
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
