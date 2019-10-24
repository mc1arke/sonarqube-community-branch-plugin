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

import org.sonar.core.config.ScannerProperties;
import org.sonar.scanner.bootstrap.GlobalConfiguration;
import org.sonar.scanner.scan.branch.BranchParamsValidator;

import java.util.List;

/**
 * @author Michael Clarke
 */
public class CommunityBranchParamsValidator implements BranchParamsValidator {

    private final GlobalConfiguration globalConfiguration;

    public CommunityBranchParamsValidator(GlobalConfiguration globalConfiguration) {
        super();
        this.globalConfiguration = globalConfiguration;
    }

    //Can be removed when support for SonarQube 7.9 is removed
    @Override
    public void validate(List<String> validationMessages, String deprecatedBranchName) {
        if (null != deprecatedBranchName && (globalConfiguration.hasKey(ScannerProperties.BRANCH_NAME) ||
                                             globalConfiguration.hasKey(ScannerProperties.BRANCH_TARGET))) {
            validationMessages.add(String.format(
                    "The legacy 'sonar.branch' parameter cannot be used at the same time as '%s' or '%s'",
                    ScannerProperties.BRANCH_NAME, ScannerProperties.BRANCH_TARGET));
        }
    }

    //@Override since SonarQube 8.0
    public void validate(List<String> validationMessages) {
        //no-op - nothing to validate
    }
}
