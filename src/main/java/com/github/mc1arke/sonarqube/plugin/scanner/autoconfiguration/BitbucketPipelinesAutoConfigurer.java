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

public class BitbucketPipelinesAutoConfigurer implements BranchAutoConfigurer {

    private final BranchConfigurationFactory branchConfigurationFactory;

    public BitbucketPipelinesAutoConfigurer(BranchConfigurationFactory branchConfigurationFactory) {
        this.branchConfigurationFactory = branchConfigurationFactory;
    }

    @Override
    public Optional<BranchConfiguration> detectConfiguration(System2 system2, ProjectBranches projectBranches) {
        if (!Boolean.parseBoolean(system2.envVariable("CI"))) {
            return Optional.empty();
        }

        String branch = system2.envVariable("BITBUCKET_BRANCH");
        if (StringUtils.isEmpty(branch)) {
            return Optional.empty();
        }

        String prId = system2.envVariable("BITBUCKET_PR_ID");
        String targetBranch = system2.envVariable("BITBUCKET_PR_DESTINATION_BRANCH");
        if (StringUtils.isEmpty(prId)) {
            return Optional.of(branchConfigurationFactory.createBranchConfiguration(branch, projectBranches));
        } else {
            return Optional.of(branchConfigurationFactory.createPullRequestConfiguration(prId, branch, targetBranch, projectBranches));
        }
    }
}
