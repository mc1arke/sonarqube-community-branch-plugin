package com.github.mc1arke.sonarqube.plugin.scanner;

import java.util.Map;
import java.util.function.Supplier;

import org.sonar.scanner.scan.branch.BranchConfiguration;
import org.sonar.scanner.scan.branch.ProjectBranches;
import org.sonar.scanner.scan.branch.ProjectPullRequests;

import com.github.mc1arke.sonarqube.plugin.SonarqubeCompatibility;

public interface BranchConfigurationLoaderCompatibility extends SonarqubeCompatibility {

    interface BranchConfigurationLoaderCompatibilityMajor7 extends SonarqubeCompatibility.Major7, BranchConfigurationLoaderCompatibility {

        interface BranchConfigurationLoaderCompatibilityMinor8 extends BranchConfigurationLoaderCompatibilityMajor7, SonarqubeCompatibility.Major7.Minor8 {

            @Deprecated
            BranchConfiguration load(Map<String, String> localSettings, Supplier<Map<String, String>> supplier,
                                     ProjectBranches projectBranches, ProjectPullRequests projectPullRequests);

        }

        interface BranchConfigurationLoaderCompatibilityMinor9 extends BranchParamsValidatorCompatibility.BranchParamsValidatorCompatibilityMajor7, SonarqubeCompatibility.Major7.Minor9 {

            //Forward compatibility for SQ 7.9
            BranchConfiguration load(Map<String, String> localSettings, ProjectBranches projectBranches,
                                     ProjectPullRequests pullRequests);
        }

    }


}
