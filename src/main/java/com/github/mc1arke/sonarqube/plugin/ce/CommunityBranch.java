/*
 * Copyright (C) 2020 Michael Clarke
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

/**
 * @author Michael Clarke
 */
public class CommunityBranch implements Branch {

    private final String name;
    private final BranchType branchType;
    private final boolean main;
    private final String referenceBranchUuid;
    private final String pullRequestKey;
    private final String targetBranchName;

    public CommunityBranch(String name, BranchType branchType, boolean main, String referenceBranchUuid,
                           String pullRequestKey, String targetBranchName) {
        super();
        this.name = name;
        this.branchType = branchType;
        this.main = main;
        this.referenceBranchUuid = referenceBranchUuid;
        this.pullRequestKey = pullRequestKey;
        this.targetBranchName = targetBranchName;
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
    public String getReferenceBranchUuid() {
        return referenceBranchUuid;
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

    @Override
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

    @Override
    public String getTargetBranchName() {
        return targetBranchName;
    }

}
