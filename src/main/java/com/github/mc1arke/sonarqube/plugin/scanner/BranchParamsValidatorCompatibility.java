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

import java.util.List;

public interface BranchParamsValidatorCompatibility extends SonarqubeCompatibility {

    interface BranchParamsValidatorCompatibilityMajor7 extends BranchParamsValidatorCompatibility, SonarqubeCompatibility.Major7 {

        interface BranchParamsValidatorCompatibilityMinor9 extends BranchParamsValidatorCompatibilityMajor7, SonarqubeCompatibility.Major7.Minor9 {

            void validate(List<String> validationMessages, String deprecatedBranchName);

        }

    }

    interface BranchParamsValidatorCompatibilityMajor8 extends SonarqubeCompatibility.Major8, BranchParamsValidatorCompatibility {

        interface BranchParamsValidatorCompatibilityMinor0 extends BranchParamsValidatorCompatibilityMajor8, SonarqubeCompatibility.Major8.Minor0 {

            void validate(List<String> validationMessages);
        }
    }
}
