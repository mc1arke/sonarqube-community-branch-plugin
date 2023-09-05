/*
 * Copyright (C) 2020-2023 Michael Clarke
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
package com.github.mc1arke.sonarqube.plugin.server;

import java.time.Clock;
import java.util.Arrays;
import java.util.Date;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.sonar.api.config.Configuration;
import org.sonar.core.config.PurgeConstants;
import org.sonar.core.util.UuidFactory;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.ce.CeTaskCharacteristicDto;
import org.sonar.db.component.BranchDto;
import org.sonar.db.component.BranchType;
import org.sonar.db.component.ComponentDto;
import org.sonar.server.ce.queue.BranchSupport;
import org.sonar.server.ce.queue.BranchSupportDelegate;
import org.sonar.server.setting.ProjectConfigurationLoader;

/**
 * @author Michael Clarke
 */
public class CommunityBranchSupportDelegate implements BranchSupportDelegate {

    private final UuidFactory uuidFactory;
    private final DbClient dbClient;
    private final Clock clock;
    private final ProjectConfigurationLoader projectConfigurationLoader;

    public CommunityBranchSupportDelegate(UuidFactory uuidFactory, DbClient dbClient, Clock clock,
                                          ProjectConfigurationLoader projectConfigurationLoader) {
        super();
        this.uuidFactory = uuidFactory;
        this.dbClient = dbClient;
        this.clock = clock;
        this.projectConfigurationLoader = projectConfigurationLoader;
    }

    @Override
    public CommunityComponentKey createComponentKey(String projectKey, Map<String, String> characteristics) {
        String branchTypeParam = StringUtils.trimToNull(characteristics.get(CeTaskCharacteristicDto.BRANCH_TYPE_KEY));

        if (null == branchTypeParam) {
            String pullRequest = StringUtils.trimToNull(characteristics.get(CeTaskCharacteristicDto.PULL_REQUEST));
            if (null == pullRequest) {
                throw new IllegalArgumentException(String.format("One of '%s' or '%s' parameters must be specified",
                                                                 CeTaskCharacteristicDto.BRANCH_TYPE_KEY,
                                                                 CeTaskCharacteristicDto.PULL_REQUEST));
            } else {
                return new CommunityComponentKey(projectKey, null, pullRequest);
            }
        }

        String branch = StringUtils.trimToNull(characteristics.get(CeTaskCharacteristicDto.BRANCH_KEY));

        try {
            BranchType.valueOf(branchTypeParam);
            return new CommunityComponentKey(projectKey, branch, null);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException(String.format("Unsupported branch type '%s'", branchTypeParam), ex);
        }

    }

    @Override
    public ComponentDto createBranchComponent(DbSession dbSession, BranchSupport.ComponentKey componentKey,
                                              ComponentDto mainComponentDto, BranchDto mainComponentBranchDto) {
        if (!componentKey.getKey().equals(mainComponentDto.getKey())) {
            throw new IllegalStateException("Component Key and Main Component Key do not match");
        }

        String branchUuid = uuidFactory.create();

        ComponentDto componentDto = mainComponentDto.copy()
            .setUuid(branchUuid)
            .setBranchUuid(branchUuid)
            .setUuidPath(ComponentDto.UUID_PATH_OF_ROOT)
            .setCreatedAt(new Date(clock.millis()));
        dbClient.componentDao().insert(dbSession, componentDto, false);

        BranchDto branchDto = new BranchDto()
            .setProjectUuid(mainComponentDto.uuid())
            .setUuid(branchUuid);
        componentKey.getPullRequestKey().ifPresent(pullRequestKey -> branchDto.setBranchType(BranchType.PULL_REQUEST)
            .setExcludeFromPurge(false)
            .setKey(pullRequestKey)
            .setIsMain(false));
        componentKey.getBranchName().ifPresent(branchName -> branchDto.setBranchType(BranchType.BRANCH)
            .setExcludeFromPurge(isBranchExcludedFromPurge(projectConfigurationLoader.loadProjectConfiguration(dbSession, branchDto.getProjectUuid()), branchName))
            .setKey(branchName)
            .setIsMain(false));
        dbClient.branchDao().insert(dbSession, branchDto);

        return componentDto;
    }

    private static boolean isBranchExcludedFromPurge(Configuration projectConfiguration, String branchName) {
        return Arrays.stream(projectConfiguration.getStringArray(PurgeConstants.BRANCHES_TO_KEEP_WHEN_INACTIVE))
            .map(Pattern::compile)
            .map(Pattern::asMatchPredicate)
            .anyMatch(p -> p.test(branchName));
    }

}
