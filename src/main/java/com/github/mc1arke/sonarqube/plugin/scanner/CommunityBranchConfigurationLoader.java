/*
 * Copyright (C) 2020-2023 Michael Clarke
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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;
import org.sonar.api.utils.MessageException;
import org.sonar.api.utils.System2;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.core.config.ScannerProperties;
import org.sonar.scanner.scan.branch.BranchConfiguration;
import org.sonar.scanner.scan.branch.BranchConfigurationLoader;
import org.sonar.scanner.scan.branch.DefaultBranchConfiguration;
import org.sonar.scanner.scan.branch.ProjectBranches;

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

    private final System2 system2;
    private final BranchConfigurationFactory branchConfigurationFactory;
    private final List<BranchAutoConfigurer> autoConfigurers;

    public CommunityBranchConfigurationLoader(System2 system2, BranchConfigurationFactory branchConfigurationFactory,
                                              List<BranchAutoConfigurer> autoConfigurers) {
        super();
        this.system2 = system2;
        this.branchConfigurationFactory = branchConfigurationFactory;
        this.autoConfigurers = autoConfigurers;
    }

    @Override
    public BranchConfiguration load(Map<String, String> localSettings, ProjectBranches projectBranches) {
        List<String> nonEmptyParameters = localSettings.entrySet().stream()
                .filter(e -> StringUtils.isNotEmpty(e.getValue()))
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
        boolean hasBranchParameters = BRANCH_ANALYSIS_PARAMETERS.stream()
                .anyMatch(nonEmptyParameters::contains);
        boolean hasPullRequestParameters = PULL_REQUEST_ANALYSIS_PARAMETERS.stream()
                .anyMatch(nonEmptyParameters::contains);

        if (hasPullRequestParameters && hasBranchParameters) {
            throw MessageException.of("sonar.pullrequest and sonar.branch parameters should not be specified in the same scan");
        }

        if (!hasBranchParameters && !hasPullRequestParameters) {
            for (BranchAutoConfigurer branchAutoConfigurer : autoConfigurers) {
                Optional<BranchConfiguration> optionalBranchConfiguration = branchAutoConfigurer.detectConfiguration(system2, projectBranches);
                if (optionalBranchConfiguration.isPresent()) {
                    BranchConfiguration branchConfiguration = optionalBranchConfiguration.get();
                    LOGGER.info("Auto detected {} configuration with source {} using {}", branchConfiguration.branchType(), branchConfiguration.branchName(), branchAutoConfigurer.getClass().getName());
                    return branchConfiguration;
                }
            }
        }

        if (hasBranchParameters) {
            String branch = StringUtils.trimToNull(localSettings.get(ScannerProperties.BRANCH_NAME));
            return branchConfigurationFactory.createBranchConfiguration(branch, projectBranches);
        }

        if (hasPullRequestParameters) {
            String key = Optional.ofNullable(StringUtils.trimToNull(localSettings.get(ScannerProperties.PULL_REQUEST_KEY)))
                    .orElseThrow(() -> MessageException.of(ScannerProperties.PULL_REQUEST_KEY + " is required for a pull request analysis"));
            String branch = Optional.ofNullable(StringUtils.trimToNull(localSettings.get(ScannerProperties.PULL_REQUEST_BRANCH)))
                    .orElseThrow(() -> MessageException.of(ScannerProperties.PULL_REQUEST_BRANCH + " is required for a pull request analysis"));
            String target = StringUtils.trimToNull(localSettings.get(ScannerProperties.PULL_REQUEST_BASE));
            return branchConfigurationFactory.createPullRequestConfiguration(key, branch, target, projectBranches);
        }

        return new DefaultBranchConfiguration();
    }

}
