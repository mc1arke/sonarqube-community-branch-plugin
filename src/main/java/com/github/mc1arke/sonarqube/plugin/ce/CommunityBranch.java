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
package com.github.mc1arke.sonarqube.plugin.ce;

import org.apache.logging.log4j.util.Strings;
import org.sonar.ce.task.projectanalysis.analysis.Branch;
import org.sonar.core.component.ComponentKeys;
import org.sonar.db.component.BranchType;
import org.sonar.db.component.ComponentDto;
import org.sonar.scanner.protocol.output.ScannerReport;

import java.util.Optional;

/**
 * @author Michael Clarke
 */
public class CommunityBranch implements Branch {

    private final String name;
    private final BranchType branchType;
    private final boolean main;
    private final String mergeBranchUuid;
    private final String pullRequestKey;

    public CommunityBranch(String name, BranchType branchType, boolean main, String mergeBranchUuid,
                           String pullRequestKey) {
        super();
        this.name = name;
        this.branchType = branchType;
        this.main = main;
        this.mergeBranchUuid = mergeBranchUuid;
        this.pullRequestKey = pullRequestKey;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public BranchType getType() {
        return branchType;
    }

    @Override
    public boolean isMain() {
        return main;
    }

    @Override
    public boolean isLegacyFeature() {
        return false;
    }

    @Override
    public Optional<String> getMergeBranchUuid() {
        return Optional.ofNullable(mergeBranchUuid);
    }

    @Override
    public boolean supportsCrossProjectCpd() {
        return main;
    }

    @Override
    public String getPullRequestKey() {
        if (BranchType.PULL_REQUEST != branchType) {
            throw new IllegalStateException("Only a branch of type PULL_REQUEST can have a pull request ID");
        }
        return pullRequestKey;
    }

    // This method can be removed when removing support for all SonarQube versions before 7.6
    @Override
    public String generateKey(ScannerReport.Component projectKey, ScannerReport.Component fileOrDirPath) {
        return generateKey(projectKey.getKey(), null == fileOrDirPath ? null : fileOrDirPath.getPath());
    }

    //@Override for SonarQube 7.6
    public String generateKey(String projectKey, String fileOrDirPath) {
        String effectiveKey;
        if (null == fileOrDirPath) {
            effectiveKey = projectKey;
        } else {
            effectiveKey = ComponentKeys.createEffectiveKey(projectKey, Strings.trimToNull(fileOrDirPath));
        }

        if (main) {
            return effectiveKey;
        } else if (BranchType.PULL_REQUEST == branchType) {
            return ComponentDto.generatePullRequestKey(effectiveKey, pullRequestKey);
        } else {
            return ComponentDto.generateBranchKey(effectiveKey, name);
        }
    }

}
