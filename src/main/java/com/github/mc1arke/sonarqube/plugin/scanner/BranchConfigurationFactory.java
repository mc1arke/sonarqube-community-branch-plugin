/*
 * Copyright (C) 2022-2024 Michael Clarke
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

import org.sonar.api.scanner.ScannerSide;
import org.sonar.api.utils.MessageException;
import org.sonar.scanner.scan.branch.BranchConfiguration;
import org.sonar.scanner.scan.branch.BranchInfo;
import org.sonar.scanner.scan.branch.BranchType;
import org.sonar.scanner.scan.branch.ProjectBranches;

import java.util.Optional;

@ScannerSide
public class BranchConfigurationFactory {

    public BranchConfiguration createBranchConfiguration(String branchName, ProjectBranches branches) {
        if (branches.isEmpty()) {
            return new CommunityBranchConfiguration(branchName, BranchType.BRANCH, null, null, null);
        }

        String targetBranchName = branches.get(branchName) == null ? branches.defaultBranchName() : branchName;
        return new CommunityBranchConfiguration(branchName, BranchType.BRANCH, targetBranchName, null, null);
    }

    public BranchConfiguration createPullRequestConfiguration(String pullRequestKey, String pullRequestBranch, String pullRequestBase, ProjectBranches branches) {
        String targetBranch = Optional.ofNullable(pullRequestBase).orElse(branches.defaultBranchName());
        String referenceBranch = findReferenceBranch(targetBranch, branches);
        return new CommunityBranchConfiguration(pullRequestBranch, BranchType.PULL_REQUEST, referenceBranch, targetBranch, pullRequestKey);
    }

    private static String findReferenceBranch(String targetBranch, ProjectBranches branches) {
        BranchInfo target = Optional.ofNullable(branches.get(targetBranch))
                .orElseThrow(() -> MessageException.of("No branch exists in Sonarqube with the name " + targetBranch));

        if (target.type() == BranchType.BRANCH) {
            return targetBranch;
        }

        String targetBranchTarget = target.branchTargetName();
        if (targetBranchTarget == null) {
            throw MessageException.of(String.format("The branch '%s' of type %s does not have a target", target.name(), target.type()));
        }

        return findReferenceBranch(targetBranchTarget, branches);
    }
}
