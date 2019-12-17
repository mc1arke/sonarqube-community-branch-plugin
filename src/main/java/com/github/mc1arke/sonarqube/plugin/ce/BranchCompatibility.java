package com.github.mc1arke.sonarqube.plugin.ce;

import com.github.mc1arke.sonarqube.plugin.SonarqubeCompatibility;

public interface BranchCompatibility extends SonarqubeCompatibility {

    interface BranchCompatibilityMajor7 extends BranchCompatibility, SonarqubeCompatibility.Major7 {

        interface BranchCompatibilityMinor9 extends BranchCompatibilityMajor7, SonarqubeCompatibility.Major7.Minor9 {

            @Deprecated
            boolean isLegacyFeature();
        }
    }
}
