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
package com.github.mc1arke.sonarqube.plugin.scanner;

import org.sonar.scanner.scan.branch.BranchConfiguration;
import org.sonar.scanner.scan.branch.BranchType;

/**
 * @author Michael Clarke
 */
public class CommunityBranchConfiguration implements BranchConfiguration {

    private final String branchName;
    private final BranchType branchType;
    private final String referenceBranchName;
    private final String targetScmBranch;
    private final String pullRequestKey;

    /*package*/ CommunityBranchConfiguration(String branchName, BranchType branchType, String referenceBranchName,
                                             String targetScmBranch,
                                             String pullRequestKey) {
        this.branchName = branchName;
        this.branchType = branchType;
        this.referenceBranchName = referenceBranchName;
        this.targetScmBranch = targetScmBranch;
        this.pullRequestKey = pullRequestKey;
    }

    @Override
    public BranchType branchType() {
        return branchType;
    }

    @Override
    public String branchName() {
        return branchName;
    }

    @Override
    public String referenceBranchName() {
        return referenceBranchName;
    }

    @Override
    public String targetBranchName() {
        return targetScmBranch;
    }

    @Override
    public String pullRequestKey() {
        if (BranchType.PULL_REQUEST != branchType) {
            throw new IllegalStateException("Only a branch of type PULL_REQUEST can have a Pull Request key");
        }

        return pullRequestKey;
    }
}
