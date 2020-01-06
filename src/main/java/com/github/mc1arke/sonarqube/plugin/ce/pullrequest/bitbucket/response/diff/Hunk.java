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

public class Hunk implements Serializable {
    private final String context;

    private final int sourceLine;

    private final int sourceSpan;

    private final int destinationLine;

    private final int destinationSpan;

    private final List<Segment> segments;

    @JsonCreator
    public Hunk(@JsonProperty("context") final String context, @JsonProperty("sourceLine") final int sourceLine, @JsonProperty("sourceSpan") final int sourceSpan, @JsonProperty("destinationLine") final int destinationLine, @JsonProperty("destinationSpan") final int destinationSpan, @JsonProperty("segments") final List<Segment> segments) {
        this.context = context;
        this.sourceLine = sourceLine;
        this.sourceSpan = sourceSpan;
        this.destinationLine = destinationLine;
        this.destinationSpan = destinationSpan;
        this.segments = segments;
    }

    public String getContext() {
        return context;
    }

    public int getSourceLine() {
        return sourceLine;
    }

    public int getSourceSpan() {
        return sourceSpan;
    }

    public int getDestinationLine() {
        return destinationLine;
    }

    public int getDestinationSpan() {
        return destinationSpan;
    }

    public List<Segment> getSegments() {
        return segments;
    }
}
