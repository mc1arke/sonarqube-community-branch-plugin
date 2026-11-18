/*
 * Copyright (C) 2022-2026 Michael Clarke
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
package com.github.mc1arke.sonarqube.plugin.scanner.autoconfiguration;

import com.github.mc1arke.sonarqube.plugin.scanner.BranchAutoConfigurer;
import com.github.mc1arke.sonarqube.plugin.scanner.BranchConfigurationFactory;
import org.apache.commons.lang3.StringUtils;
import org.sonar.api.utils.System2;
import org.sonar.scanner.scan.branch.BranchConfiguration;
import org.sonar.scanner.scan.branch.ProjectBranches;

import java.util.List;
import java.util.Optional;

public class GithubActionsAutoConfigurer implements BranchAutoConfigurer {

    private static final String PULL_REQUEST_REF_PREFIX = "refs/pull/";
    // GitHub runs pull request workflows on the merge ref; Forgejo and Gitea Actions, which expose the same
    // GITHUB_* variables, run them on the head ref.
    private static final List<String> PULL_REQUEST_REF_POSTFIXES = List.of("/merge", "/head");

    private final BranchConfigurationFactory branchConfigurationFactory;

    public GithubActionsAutoConfigurer(BranchConfigurationFactory branchConfigurationFactory) {
        this.branchConfigurationFactory = branchConfigurationFactory;
    }

    @Override
    public Optional<BranchConfiguration> detectConfiguration(System2 system, ProjectBranches projectBranches) {
        if (!Boolean.parseBoolean(system.envVariable("GITHUB_ACTIONS"))) {
            return Optional.empty();
        }

        String ref = system.envVariable("GITHUB_REF");
        if (StringUtils.isEmpty(ref)) {
            return Optional.empty();
        }

        String sourceBranch = system.envVariable("GITHUB_HEAD_REF");
        Optional<String> pullRequestKey = findPullRequestKey(ref);
        if (pullRequestKey.isPresent() && StringUtils.isNotEmpty(sourceBranch)) {
            String targetBranch = system.envVariable("GITHUB_BASE_REF");
            return Optional.of(branchConfigurationFactory.createPullRequestConfiguration(pullRequestKey.get(), sourceBranch, targetBranch, projectBranches));
        } else {
            String branch = system.envVariable("GITHUB_REF_NAME");
            return Optional.of(branchConfigurationFactory.createBranchConfiguration(branch, projectBranches));
        }
    }

    private static Optional<String> findPullRequestKey(String ref) {
        if (!ref.startsWith(PULL_REQUEST_REF_PREFIX)) {
            return Optional.empty();
        }
        return PULL_REQUEST_REF_POSTFIXES.stream()
                .filter(postfix -> ref.endsWith(postfix) && ref.length() > PULL_REQUEST_REF_PREFIX.length() + postfix.length())
                .map(postfix -> ref.substring(PULL_REQUEST_REF_PREFIX.length(), ref.length() - postfix.length()))
                .filter(StringUtils::isNotEmpty)
                .findFirst();
    }
}
