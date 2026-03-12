/*
 * Copyright (C) 2020-2026 Michael Clarke, Sebastiaan Speck
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
package com.github.mc1arke.sonarqube.plugin.almclient.azuredevops.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.mc1arke.sonarqube.plugin.almclient.azuredevops.model.enums.GitStatusState;

public class GitPullRequestStatus {

    private final GitStatusState state;
    private final String description;
    private final GitStatusContext context;
    private final String targetUrl;
    private final int iterationId;

    @JsonCreator
    public GitPullRequestStatus(
            @JsonProperty("state") GitStatusState state,
            @JsonProperty("description") String description,
            @JsonProperty("context") GitStatusContext context,
            @JsonProperty("targetUrl") String targetUrl,
            @JsonProperty("iterationId") int iterationId) {
        this.state = state;
        this.description = description;
        this.context = context;
        this.targetUrl = targetUrl;
        this.iterationId = iterationId;
    }

    /**
     * State of the status.
     */
    public GitStatusState getState() {
        return this.state;
    }

    /**
     * Description of the status
     */
    public String getDescription() {
        return this.description;
    }

    /**
     * Status context that uniquely identifies the status.
     */
    public GitStatusContext getContext() {
        return this.context;
    }

    /**
     * TargetUrl of the status
     */
    public String getTargetUrl() {
        return this.targetUrl;
    }

    /**
     * ID of the iteration to associate status with. Minimum value is 1.
     */
    public int getIterationId() {
        return this.iterationId;
    }
}
