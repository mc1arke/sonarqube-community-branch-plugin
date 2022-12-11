/*
 * Copyright (C) 2020-2022 Michael Clarke
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
package com.github.mc1arke.sonarqube.plugin.server.pullrequest.ws.binding.action;

import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.WebService;
import org.sonar.db.DbClient;
import org.sonar.db.alm.setting.ProjectAlmSettingDto;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.user.UserSession;

public class SetBitbucketBindingAction extends SetBindingAction {

    private static final String REPOSITORY_PARAMETER = "repository";
    private static final String SLUG_PARAMETER = "slug";

    public SetBitbucketBindingAction(DbClient dbClient, ComponentFinder componentFinder, UserSession userSession) {
        super(dbClient, componentFinder, userSession, "set_bitbucket_binding");
    }

    @Override
    protected void configureAction(WebService.NewAction action) {
        super.configureAction(action);
        action.createParam(REPOSITORY_PARAMETER).setRequired(true);
        action.createParam(SLUG_PARAMETER).setRequired(true);
    }

    @Override
    protected ProjectAlmSettingDto createProjectAlmSettingDto(String projectUuid, String settingsUuid,
                                                              boolean monoRepo, Request request) {
        return new ProjectAlmSettingDto()
                .setProjectUuid(projectUuid)
                .setAlmSettingUuid(settingsUuid)
                .setAlmRepo(request.mandatoryParam(REPOSITORY_PARAMETER))
                .setAlmSlug(request.mandatoryParam(SLUG_PARAMETER))
                .setMonorepo(monoRepo);
    }

}
