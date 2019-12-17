package com.github.mc1arke.sonarqube.plugin.scanner;

import java.util.List;

import com.github.mc1arke.sonarqube.plugin.SonarqubeCompatibility;

public interface BranchParamsValidatorCompatibility extends SonarqubeCompatibility {

    interface BranchParamsValidatorCompatibilityMajor7 extends BranchParamsValidatorCompatibility, SonarqubeCompatibility.Major7 {

        interface BranchParamsValidatorCompatibilityMinor9 extends BranchParamsValidatorCompatibilityMajor7, SonarqubeCompatibility.Major7.Minor9 {

            @Deprecated
            void validate(List<String> validationMessages, String deprecatedBranchName);

        }

    }

    interface BranchParamsValidatorCompatibilityMajor8 extends SonarqubeCompatibility.Major8, BranchParamsValidatorCompatibility {

        interface BranchParamsValidatorCompatibilityMinor0 extends BranchParamsValidatorCompatibilityMajor8, SonarqubeCompatibility.Major8.Minor0 {

            // forward compatibility for SonarQube 8.0
            void validate(List<String> validationMessages);
        }
    }
}
