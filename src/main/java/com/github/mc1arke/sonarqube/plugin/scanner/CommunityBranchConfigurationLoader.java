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

import org.sonar.api.notifications.AnalysisWarnings;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.core.config.ScannerProperties;
import org.sonar.scanner.scan.branch.BranchConfiguration;
import org.sonar.scanner.scan.branch.BranchConfigurationLoader;
import org.sonar.scanner.scan.branch.BranchInfo;
import org.sonar.scanner.scan.branch.BranchType;
import org.sonar.scanner.scan.branch.DefaultBranchConfiguration;
import org.sonar.scanner.scan.branch.ProjectBranches;
import org.sonar.scanner.scan.branch.ProjectPullRequests;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * @author Michael Clarke
 */
public class CommunityBranchConfigurationLoader implements BranchConfigurationLoader {

    private static final Logger LOGGER = Loggers.get(CommunityBranchConfigurationLoader.class);

    private static final Set<String> BRANCH_ANALYSIS_PARAMETERS =
            new HashSet<>(Collections.singletonList(ScannerProperties.BRANCH_NAME));

    private static final Set<String> PULL_REQUEST_ANALYSIS_PARAMETERS = new HashSet<>(
            Arrays.asList(ScannerProperties.PULL_REQUEST_BRANCH, ScannerProperties.PULL_REQUEST_KEY,
                          ScannerProperties.PULL_REQUEST_BASE));

    private final AnalysisWarnings analysisWarnings;

    public CommunityBranchConfigurationLoader(AnalysisWarnings analysisWarnings) {
        super();
        this.analysisWarnings = analysisWarnings;
    }

    @Override
    public BranchConfiguration load(Map<String, String> localSettings, ProjectBranches projectBranches,
                                    ProjectPullRequests pullRequests) {
        if (null != localSettings.get(ScannerProperties.BRANCH_TARGET)) { //NOSONAR - purposefully checking for a deprecated parameter
            String warning = String.format("Property '%s' is no longer supported", ScannerProperties.BRANCH_TARGET); //NOSONAR - reporting use of deprecated parameter
            analysisWarnings.addUnique(warning);
            LOGGER.warn(warning);
        }
        if (BRANCH_ANALYSIS_PARAMETERS.stream().anyMatch(localSettings::containsKey)) {
            return createBranchConfiguration(localSettings.get(ScannerProperties.BRANCH_NAME),
                                             projectBranches);
        } else if (PULL_REQUEST_ANALYSIS_PARAMETERS.stream().anyMatch(localSettings::containsKey)) {
            return createPullRequestConfiguration(localSettings.get(ScannerProperties.PULL_REQUEST_KEY),
                                                  localSettings.get(ScannerProperties.PULL_REQUEST_BRANCH),
                                                  localSettings.get(ScannerProperties.PULL_REQUEST_BASE),
                                                  projectBranches);
        }

        return new DefaultBranchConfiguration();
    }

    private static BranchConfiguration createBranchConfiguration(String branchName, ProjectBranches branches) {
        BranchInfo existingBranch = branches.get(branchName);

        if (null == existingBranch) {
            return new CommunityBranchConfiguration(branchName, BranchType.BRANCH, branches.defaultBranchName(), null, null);
        }

        return new CommunityBranchConfiguration(branchName, existingBranch.type(), existingBranch.name(), null, null);
    }

    private static BranchConfiguration createPullRequestConfiguration(String pullRequestKey, String pullRequestBranch,
                                                                      String pullRequestBase,
                                                                      ProjectBranches branches) {
        if (null == pullRequestBase || pullRequestBase.isEmpty()) {
            return new CommunityBranchConfiguration(pullRequestBranch, BranchType.PULL_REQUEST, branches.defaultBranchName(),
                                                    branches.defaultBranchName(), pullRequestKey);
        } else {
            return new CommunityBranchConfiguration(pullRequestBranch, BranchType.PULL_REQUEST,
                                                    Optional.ofNullable(branches.get(pullRequestBase))
                                                            .map(b -> pullRequestBase)
                                                            .orElse(null),
                                                    pullRequestBase, pullRequestKey);
        }
    }

}
