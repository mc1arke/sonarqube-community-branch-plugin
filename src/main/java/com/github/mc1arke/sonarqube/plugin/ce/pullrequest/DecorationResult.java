/*
 * Copyright (C) 2020 Michael Clarke
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
package com.github.mc1arke.sonarqube.plugin.ce.pullrequest;

import java.util.Optional;

public final class DecorationResult {

    private final String pullRequestUrl;

    private DecorationResult(Builder builder) {
        super();
        this.pullRequestUrl = builder.pullRequestUrl;
    }

    public Optional<String> getPullRequestUrl() {
        return Optional.ofNullable(pullRequestUrl);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {

        private String pullRequestUrl;

        private Builder() {
            super();
        }

        public Builder withPullRequestUrl(String pullRequestUrl) {
            this.pullRequestUrl = pullRequestUrl;
            return this;
        }

        public DecorationResult build() {
            return new DecorationResult(this);
        }
    }
}
