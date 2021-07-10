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

import java.math.BigDecimal;
import java.util.Optional;

public class PipelineStatus {

    private final String pipelineName;
    private final String pipelineDescription;
    private final State state;
    private final String targetUrl;
    private final BigDecimal coverage;
    private final Long pipelineId;

    public PipelineStatus(String pipelineName, String pipelineDescription, State state, String targetUrl, BigDecimal coverage, Long pipelineId) {
        this.pipelineName = pipelineName;
        this.pipelineDescription = pipelineDescription;
        this.state = state;
        this.targetUrl = targetUrl;
        this.coverage = coverage;
        this.pipelineId = pipelineId;
    }

    public String getPipelineName() {
        return pipelineName;
    }

    public String getPipelineDescription() {
        return pipelineDescription;
    }

    public State getState() {
        return state;
    }

    public String getTargetUrl() {
        return targetUrl;
    }

    public Optional<BigDecimal> getCoverage() {
        return Optional.ofNullable(coverage);
    }

    public Optional<Long> getPipelineId() {
        return Optional.ofNullable(pipelineId);
    }

    public enum State {
        SUCCESS("success"),
        FAILED("failed");

        private final String label;

        State(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }
    }
}
