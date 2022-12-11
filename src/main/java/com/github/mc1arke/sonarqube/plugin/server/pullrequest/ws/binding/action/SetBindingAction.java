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
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.alm.setting.AlmSettingDto;
import org.sonar.db.alm.setting.ProjectAlmSettingDto;
import org.sonar.db.project.ProjectDto;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.user.UserSession;

import static java.lang.String.format;

public abstract class SetBindingAction extends ProjectWsAction {

    private static final String ALM_SETTING_PARAMETER = "almSetting";
    private static final String MONOREPO_PARAMETER = "monorepo";

    protected SetBindingAction(DbClient dbClient, ComponentFinder componentFinder, UserSession userSession, String actionName) {
        super(actionName, dbClient, componentFinder, userSession);
    }

    @Override
    protected void configureAction(WebService.NewAction action) {
        action.setPost(true)
            .createParam(ALM_SETTING_PARAMETER).setRequired(true);
        action.createParam(MONOREPO_PARAMETER).setRequired(true).setBooleanPossibleValues();
    }

    @Override
    protected void handleProjectRequest(ProjectDto project, Request request, Response response, DbSession dbSession) {
        String almSetting = request.mandatoryParam(ALM_SETTING_PARAMETER);
        boolean monoRepo = request.mandatoryParamAsBoolean(MONOREPO_PARAMETER);

        DbClient dbClient = getDbClient();
        AlmSettingDto almSettingDto = getAlmSetting(dbClient, dbSession, almSetting);
        dbClient.projectAlmSettingDao().insertOrUpdate(dbSession, createProjectAlmSettingDto(project.getUuid(), almSettingDto.getUuid(), monoRepo, request), almSettingDto.getUuid(), project.getName(), project.getKey());
        dbSession.commit();

        response.noContent();
    }

    private static AlmSettingDto getAlmSetting(DbClient dbClient, DbSession dbSession, String almSetting) {
        return dbClient.almSettingDao().selectByKey(dbSession, almSetting)
                .orElseThrow(() -> new NotFoundException(format("ALM setting '%s' could not be found", almSetting)));
    }


    protected abstract ProjectAlmSettingDto createProjectAlmSettingDto(String projectUuid, String settingsUuid, boolean monoRepo, Request request);

}
