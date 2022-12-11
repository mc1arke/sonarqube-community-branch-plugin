/*
 * Copyright (C) 2020-2022 Michael Clarke
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
package com.github.mc1arke.sonarqube.plugin.server;

import org.sonar.server.ce.queue.BranchSupport;

import java.util.Optional;

/**
 * @author Michael Clarke
 */
/*package*/ class CommunityComponentKey extends BranchSupport.ComponentKey {

    private final String key;
    private final String branchName;
    private final String pullRequestKey;

    /*package*/ CommunityComponentKey(String key, String branchName, String pullRequestKey) {
        this.key = key;
        this.branchName = branchName;
        this.pullRequestKey = pullRequestKey;
    }

    @Override
    public String getKey() {
        return key;
    }

    @Override
    public Optional<String> getBranchName() {
        return Optional.ofNullable(branchName);
    }

    @Override
    public Optional<String> getPullRequestKey() {
        return Optional.ofNullable(pullRequestKey);
    }

}
