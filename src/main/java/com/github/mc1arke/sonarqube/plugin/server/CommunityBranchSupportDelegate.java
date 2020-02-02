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
package com.github.mc1arke.sonarqube.plugin.server;

import org.apache.commons.lang.StringUtils;
import org.sonar.core.util.UuidFactory;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.ce.CeTaskCharacteristicDto;
import org.sonar.db.component.BranchDto;
import org.sonar.db.component.BranchType;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.server.ce.queue.BranchSupport;
import org.sonar.server.ce.queue.BranchSupportDelegate;

import java.time.Clock;
import java.util.Date;
import java.util.Map;
import java.util.Optional;

/**
 * @author Michael Clarke
 */
public class CommunityBranchSupportDelegate implements BranchSupportDelegate {

    private final UuidFactory uuidFactory;
    private final DbClient dbClient;
    private final Clock clock;

    public CommunityBranchSupportDelegate(UuidFactory uuidFactory, DbClient dbClient, Clock clock) {
        super();
        this.uuidFactory = uuidFactory;
        this.dbClient = dbClient;
        this.clock = clock;
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
                return new CommunityComponentKey(projectKey,
                                                 ComponentDto.generatePullRequestKey(projectKey, pullRequest), null,
                                                 pullRequest, null);
            }
        }

        String branch = StringUtils.trimToNull(characteristics.get(CeTaskCharacteristicDto.BRANCH_KEY));

        try {
            BranchType branchType = BranchType.valueOf(branchTypeParam);
            return new CommunityComponentKey(projectKey, ComponentDto.generateBranchKey(projectKey, branch),
                                             new BranchSupport.Branch(branch, branchType), null, null);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException(String.format("Unsupported branch type '%s'", branchTypeParam), ex);
        }

    }

    @Override
    public ComponentDto createBranchComponent(DbSession dbSession, BranchSupport.ComponentKey componentKey,
                                              OrganizationDto organization, ComponentDto mainComponentDto,
                                              BranchDto mainComponentBranchDto) {
        if (!componentKey.getKey().equals(mainComponentDto.getKey())) {
            throw new IllegalStateException("Component Key and Main Component Key do not match");
        }

        Optional<BranchSupport.Branch> branchOptional = componentKey.getBranch();
        if (branchOptional.isPresent() && branchOptional.get().getName().equals(mainComponentBranchDto.getKey()) &&
            mainComponentBranchDto.getBranchType() == branchOptional.get().getType()) {
            return mainComponentDto;
        }

        String branchUuid = uuidFactory.create();

        // borrowed from https://github.com/SonarSource/sonarqube/blob/e80c0f3d1e5cd459f88b7e0c41a2d9a7519e260f/server/sonar-ce-task-projectanalysis/src/main/java/org/sonar/ce/task/projectanalysis/component/BranchPersisterImpl.java
        ComponentDto branchDto = mainComponentDto.copy();
        branchDto.setUuid(branchUuid);
        branchDto.setProjectUuid(branchUuid);
        branchDto.setRootUuid(branchUuid);
        branchDto.setUuidPath(ComponentDto.UUID_PATH_OF_ROOT);
        branchDto.setModuleUuidPath(ComponentDto.UUID_PATH_SEPARATOR + branchUuid + ComponentDto.UUID_PATH_SEPARATOR);
        branchDto.setMainBranchProjectUuid(mainComponentDto.uuid());
        branchDto.setDbKey(componentKey.getDbKey());
        branchDto.setCreatedAt(new Date(clock.millis()));
        dbClient.componentDao().insert(dbSession, branchDto);
        return branchDto;
    }

}
