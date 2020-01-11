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
package com.github.mc1arke.sonarqube.plugin.ce.pullrequest.bitbucket;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;

public class Anchor implements Serializable {
    private final int line;

    private final String lineType;

    private final String path;

    private final String fileType;

    @JsonCreator
    public Anchor(@JsonProperty("line") int line, @JsonProperty("lineType") String lineType, @JsonProperty("path") String path, @JsonProperty("fileType") String fileType) {
        this.line = line;
        this.lineType = lineType;
        this.path = path;
        this.fileType = fileType;
    }

    public int getLine() {
        return line;
    }

    public String getLineType() {
        return lineType;
    }

    public String getPath() {
        return path;
    }

    public String getFileType() {
        return fileType;
    }
}
