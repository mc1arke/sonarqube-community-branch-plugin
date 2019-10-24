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
package com.github.mc1arke.sonarqube.plugin.server;

import org.sonar.server.ce.queue.BranchSupport;

import java.util.Optional;

/**
 * @author Michael Clarke
 */
/*package*/ class CommunityComponentKey extends BranchSupport.ComponentKey {

    private final String key;
    private final String dbKey;
    private final String deprecatedBranchName;
    private final BranchSupport.Branch branch;
    private final String pullRequestKey;

    /*package*/ CommunityComponentKey(String key, String dbKey, BranchSupport.Branch branch, String pullRequestKey,
                                      String deprecatedBranchName) {
        this.key = key;
        this.dbKey = dbKey;
        this.deprecatedBranchName = deprecatedBranchName;
        this.branch = branch;
        this.pullRequestKey = pullRequestKey;
    }

    @Override
    public String getKey() {
        return key;
    }

    @Override
    public String getDbKey() {
        return dbKey;
    }

    //Can be removed when Support for SonarQube 7.9 is removed
    @Override
    public Optional<String> getDeprecatedBranchName() {
        return Optional.ofNullable(deprecatedBranchName);
    }

    @Override
    public Optional<BranchSupport.Branch> getBranch() {
        return Optional.ofNullable(branch);
    }

    @Override
    public Optional<String> getPullRequestKey() {
        return Optional.ofNullable(pullRequestKey);
    }

    @Override
    public CommunityComponentKey getMainBranchComponentKey() {
        if (key.equals(dbKey)) {
            return this;
        }
        return new CommunityComponentKey(key, key, null, null, deprecatedBranchName);
    }
}
