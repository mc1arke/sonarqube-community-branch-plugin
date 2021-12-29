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
package com.github.mc1arke.sonarqube.plugin.scanner.autoconfiguration;

import com.github.mc1arke.sonarqube.plugin.scanner.BranchAutoConfigurer;
import com.github.mc1arke.sonarqube.plugin.scanner.BranchConfigurationFactory;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.utils.System2;
import org.sonar.scanner.scan.branch.BranchConfiguration;
import org.sonar.scanner.scan.branch.ProjectBranches;

import java.util.Optional;

public class GithubActionsAutoConfigurer implements BranchAutoConfigurer {

    private static final String PULL_REQUEST_REF_PREFIX = "refs/pull/";
    private static final String PULL_REQUEST_REF_POSTFIX = "/merge";

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
        if (ref.startsWith(PULL_REQUEST_REF_PREFIX) && ref.endsWith(PULL_REQUEST_REF_POSTFIX) && StringUtils.isNotEmpty(sourceBranch)) {
            String key = ref.substring(PULL_REQUEST_REF_PREFIX.length(), ref.length() - PULL_REQUEST_REF_POSTFIX.length());
            String targetBranch = system.envVariable("GITHUB_BASE_REF");
            return Optional.of(branchConfigurationFactory.createPullRequestConfiguration(key, sourceBranch, targetBranch, projectBranches));
        } else {
            String branch = system.envVariable("GITHUB_REF_NAME");
            return Optional.of(branchConfigurationFactory.createBranchConfiguration(branch, projectBranches));
        }
    }
}
