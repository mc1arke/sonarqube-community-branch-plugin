package com.github.mc1arke.sonarqube.plugin.server;

import java.util.Optional;

import com.github.mc1arke.sonarqube.plugin.SonarqubeCompatibility;

public interface ComponentKeyCompatibility extends SonarqubeCompatibility {

    interface ComponentKeyCompatibilityMajor7 extends ComponentKeyCompatibility, SonarqubeCompatibility.Major7 {

        interface ComponentKeyCompatibilityMinor9 extends ComponentKeyCompatibilityMajor7, SonarqubeCompatibility.Major7.Minor9 {

            @Deprecated
            Optional<String> getDeprecatedBranchName();
        }
    }
}
