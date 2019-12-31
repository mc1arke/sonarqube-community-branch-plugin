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

import com.github.mc1arke.sonarqube.plugin.SonarqubeCompatibility;
import org.sonar.scanner.scan.branch.BranchConfiguration;
import org.sonar.scanner.scan.branch.ProjectBranches;
import org.sonar.scanner.scan.branch.ProjectPullRequests;

import java.util.Map;
import java.util.function.Supplier;

public interface BranchConfigurationLoaderCompatibility extends SonarqubeCompatibility {

    interface BranchConfigurationLoaderCompatibilityMajor7 extends SonarqubeCompatibility.Major7, BranchConfigurationLoaderCompatibility {

        interface BranchConfigurationLoaderCompatibilityMinor8 extends BranchConfigurationLoaderCompatibilityMajor7, SonarqubeCompatibility.Major7.Minor8 {

            BranchConfiguration load(Map<String, String> localSettings, Supplier<Map<String, String>> supplier,
                                     ProjectBranches projectBranches, ProjectPullRequests projectPullRequests);

        }

        interface BranchConfigurationLoaderCompatibilityMinor9 extends BranchParamsValidatorCompatibility.BranchParamsValidatorCompatibilityMajor7, SonarqubeCompatibility.Major7.Minor9 {

            BranchConfiguration load(Map<String, String> localSettings, ProjectBranches projectBranches,
                                     ProjectPullRequests pullRequests);
        }

    }


}
