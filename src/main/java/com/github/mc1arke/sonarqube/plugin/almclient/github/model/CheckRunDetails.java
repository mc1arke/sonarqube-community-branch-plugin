/*
 * Copyright (C) 2022 Michael Clarke
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
package com.github.mc1arke.sonarqube.plugin.almclient.github.model;

import com.github.mc1arke.sonarqube.plugin.almclient.github.v4.model.CheckConclusionState;

import java.time.ZonedDateTime;
import java.util.List;

public class CheckRunDetails {

    private final String summary;
    private final String title;
    private final String name;
    private final String dashboardUrl;
    private final ZonedDateTime startTime;
    private final ZonedDateTime endTime;
    private final String externalId;
    private final String commitId;
    private final List<Annotation> annotations;
    private final CheckConclusionState checkConclusionState;
    private final int pullRequestId;
    private final String projectKey;

    private CheckRunDetails(Builder builder) {
        summary = builder.summary;
        title = builder.title;
        name = builder.name;
        dashboardUrl = builder.dashboardUrl;
        startTime = builder.startTime;
        endTime = builder.endTime;
        externalId = builder.externalId;
        commitId = builder.commitId;
        annotations = builder.annotations;
        checkConclusionState = builder.checkConclusionState;
        pullRequestId = builder.pullRequestId;
        projectKey = builder.projectKey;
    }

    public String getSummary() {
        return summary;
    }

    public String getTitle() {
        return title;
    }

    public String getName() {
        return name;
    }

    public String getDashboardUrl() {
        return dashboardUrl;
    }

    public ZonedDateTime getStartTime() {
        return startTime;
    }

    public ZonedDateTime getEndTime() {
        return endTime;
    }

    public String getExternalId() {
        return externalId;
    }

    public String getCommitId() {
        return commitId;
    }

    public List<Annotation> getAnnotations() {
        return annotations;
    }

    public CheckConclusionState getCheckConclusionState() {
        return checkConclusionState;
    }

    public int getPullRequestId() {
        return pullRequestId;
    }

    public String getProjectKey() {
        return projectKey;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String summary;
        private String title;
        private String name;
        private String dashboardUrl;
        private ZonedDateTime startTime;
        private ZonedDateTime endTime;
        private String externalId;
        private String commitId;
        private List<Annotation> annotations;
        private CheckConclusionState checkConclusionState;
        private int pullRequestId;
        private String projectKey;

        private Builder() {
            super();
        }

        public Builder withSummary(String summary) {
            this.summary = summary;
            return this;
        }

        public Builder withTitle(String title) {
            this.title = title;
            return this;
        }

        public Builder withName(String name) {
            this.name = name;
            return this;
        }

        public Builder withDashboardUrl(String dashboardUrl) {
            this.dashboardUrl = dashboardUrl;
            return this;
        }

        public Builder withStartTime(ZonedDateTime startTime) {
            this.startTime = startTime;
            return this;
        }

        public Builder withEndTime(ZonedDateTime endTime) {
            this.endTime = endTime;
            return this;
        }

        public Builder withExternalId(String externalId) {
            this.externalId = externalId;
            return this;
        }

        public Builder withCommitId(String commitId) {
            this.commitId = commitId;
            return this;
        }

        public Builder withAnnotations(List<Annotation> annotations) {
            this.annotations = annotations;
            return this;
        }

        public Builder withCheckConclusionState(CheckConclusionState checkConclusionState) {
            this.checkConclusionState = checkConclusionState;
            return this;
        }

        public Builder withPullRequestId(int pullRequestId) {
            this.pullRequestId = pullRequestId;
            return this;
        }

        public Builder withProjectKey(String projectKey) {
            this.projectKey = projectKey;
            return this;
        }

        public CheckRunDetails build() {
            return new CheckRunDetails(this);
        }
    }
}
