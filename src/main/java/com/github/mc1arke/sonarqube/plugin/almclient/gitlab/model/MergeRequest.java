/*
 * Copyright (C) 2021 Michael Clarke
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

import com.fasterxml.jackson.annotation.JsonProperty;

public class MergeRequest {
    private final long iid;
    private final DiffRefs diffRefs;
    private final long sourceProjectId;
    private final long targetProjectId;
    private final String webUrl;

    public MergeRequest(@JsonProperty("iid") long iid, @JsonProperty("diff_refs") DiffRefs diffRefs,
                        @JsonProperty("source_project_id") long sourceProjectId,
                        @JsonProperty("target_project_id") long targetProjectId,
                        @JsonProperty("web_url") String webUrl) {
        this.iid = iid;
        this.diffRefs = diffRefs;
        this.sourceProjectId = sourceProjectId;
        this.webUrl = webUrl;
        this.targetProjectId = targetProjectId;
    }

    public long getIid() {
        return iid;
    }

    public DiffRefs getDiffRefs() {
        return diffRefs;
    }

    public long getSourceProjectId() {
        return sourceProjectId;
    }

    public long getTargetProjectId() {
        return targetProjectId;
    }

    public String getWebUrl() {
        return webUrl;
    }
}
