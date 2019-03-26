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
package com.github.mc1arke.sonarqube.plugin.scanner;

import org.apache.commons.lang.StringUtils;
import org.sonar.api.CoreProperties;
import org.sonar.api.utils.MessageException;
import org.sonar.core.config.ScannerProperties;
import org.sonar.scanner.scan.branch.BranchConfiguration;
import org.sonar.scanner.scan.branch.BranchConfigurationLoader;
import org.sonar.scanner.scan.branch.BranchInfo;
import org.sonar.scanner.scan.branch.BranchType;
import org.sonar.scanner.scan.branch.DefaultBranchConfiguration;
import org.sonar.scanner.scan.branch.ProjectBranches;
import org.sonar.scanner.scan.branch.ProjectPullRequests;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

/**
 * @author Michael Clarke
 */
public class CommunityBranchConfigurationLoader implements BranchConfigurationLoader {

    public static final String DEFAULT_BRANCH_REGEX = "(branch|release).*";


    private static final Set<String> BRANCH_ANALYSIS_PARAMETERS =
            new HashSet<>(Arrays.asList(ScannerProperties.BRANCH_NAME, ScannerProperties.BRANCH_TARGET));

    private static final Set<String> PULL_REQUEST_ANALYSIS_PARAMETERS = new HashSet<>(
            Arrays.asList(ScannerProperties.PULL_REQUEST_BRANCH, ScannerProperties.PULL_REQUEST_KEY,
                          ScannerProperties.PULL_REQUEST_BASE));

    @Override
    public BranchConfiguration load(Map<String, String> localSettings, Supplier<Map<String, String>> supplier,
                                    ProjectBranches projectBranches, ProjectPullRequests projectPullRequests) {
        if (projectBranches.isEmpty()) {
            if (isTargetingDefaultBranch(localSettings)) {
                return new DefaultBranchConfiguration();
            } else {
                // it would be nice to identify the 'primary' branch directly, but different projects work differently: using any of master, develop, main etc as primary
                // A project/global configuration entry could be used to drive this in the future, but the current documented SonarQube parameters need followed for now
                throw MessageException
                        .of("No branches currently exist in this project. Please scan the main branch without passing any branch parameters.");
            }
        }
        if (BRANCH_ANALYSIS_PARAMETERS.stream().anyMatch(localSettings::containsKey)) {
            return createBranchConfiguration(localSettings.get(ScannerProperties.BRANCH_NAME),
                                             localSettings.get(ScannerProperties.BRANCH_TARGET), supplier,
                                             projectBranches);
        } else if (PULL_REQUEST_ANALYSIS_PARAMETERS.stream().anyMatch(localSettings::containsKey)) {
            return createPullRequestConfiguration(localSettings.get(ScannerProperties.PULL_REQUEST_KEY),
                                                  localSettings.get(ScannerProperties.PULL_REQUEST_BRANCH),
                                                  localSettings.get(ScannerProperties.PULL_REQUEST_BASE),
                                                  projectBranches);
        }

        return new DefaultBranchConfiguration();
    }

    private static boolean isTargetingDefaultBranch(Map<String, String> localSettings) {
        String name = StringUtils.trimToNull(localSettings.get(ScannerProperties.BRANCH_NAME));
        String target = StringUtils.trimToNull(localSettings.get(ScannerProperties.BRANCH_TARGET));

        return (null == name || "master".equals(name)) && (null == target || target.equals(name));
    }

    private static BranchConfiguration createBranchConfiguration(String branchName, String branchTarget,
                                                                 Supplier<Map<String, String>> settingsSupplier,
                                                                 ProjectBranches branches) {
        if (null == branchTarget || branchTarget.isEmpty()) {
            branchTarget = branches.defaultBranchName();
        }

        BranchInfo existingBranch = branches.get(branchName);

        if (null == existingBranch) {
            final BranchType branchType = computeBranchType(settingsSupplier, branchName);
            final BranchInfo targetBranch = findTargetBranch(branchTarget, branches);
            return new CommunityBranchConfiguration(branchName, branchType, targetBranch.name(), branchTarget, null);
        }

        if (BranchType.LONG == existingBranch.type()) {
            return new CommunityBranchConfiguration(branchName, existingBranch.type(), branchName, null, null);
        } else {
            return new CommunityBranchConfiguration(branchName, existingBranch.type(), branchTarget, branchTarget,
                                                    null);
        }
    }

    private static BranchType computeBranchType(Supplier<Map<String, String>> settingsSupplier, String branchName) {
        String longLivedBranchesRegex = settingsSupplier.get().get(CoreProperties.LONG_LIVED_BRANCHES_REGEX);
        if (null == longLivedBranchesRegex) {
            longLivedBranchesRegex = DEFAULT_BRANCH_REGEX;
        }
        if (branchName.matches(longLivedBranchesRegex)) {
            return BranchType.LONG;
        } else {
            return BranchType.SHORT;
        }
    }

    private static BranchConfiguration createPullRequestConfiguration(String pullRequestKey, String pullRequestBranch,
                                                                      String pullRequestBase,
                                                                      ProjectBranches branches) {
        if (null == pullRequestBase || pullRequestBase.isEmpty()) {
            pullRequestBase = branches.defaultBranchName();
        }

        findTargetBranch(pullRequestBase, branches);
        return new CommunityBranchConfiguration(pullRequestBranch, BranchType.PULL_REQUEST, pullRequestBase,
                                                pullRequestBase, pullRequestKey);
    }

    private static BranchInfo findTargetBranch(String targetBranch, ProjectBranches branches) {
        final BranchInfo target = branches.get(targetBranch);

        if (null == target) {
            throw MessageException.of("Could not target requested branch", new IllegalStateException(
                    String.format("Target branch '%s' does not exist", targetBranch)));
        }

        if (BranchType.LONG == target.type()) {
            return target;
        } else {
            throw MessageException.of("Could not target requested branch", new IllegalStateException(
                    String.format("Expected branch type of %s but got %s", BranchType.LONG.name(),
                                  target.type().name())));
        }
    }


}
