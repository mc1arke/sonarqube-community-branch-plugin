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
package com.github.mc1arke.sonarqube.plugin.server.pullrequest.ws.action;

import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.alm.setting.AlmSettingDto;
import org.sonar.db.alm.setting.ProjectAlmSettingDto;
import org.sonar.db.component.ComponentDto;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.user.UserSession;

public abstract class SetBindingAction extends ProjectWsAction {

    private static final String ALM_SETTING_PARAMETER = "almSetting";

    protected SetBindingAction(DbClient dbClient, ComponentFinder componentFinder, UserSession userSession, String actionName) {
        super(actionName, dbClient, componentFinder, userSession);
    }

    protected void configureAction(WebService.NewAction action) {
        action.createParam(ALM_SETTING_PARAMETER).setRequired(true);
    }

    protected void handleProjectRequest(ComponentDto project, Request request, Response response, DbSession dbSession) {
        String almSetting = request.mandatoryParam(ALM_SETTING_PARAMETER);

        AlmSettingDto almSettingDto = getAlmSetting(dbSession, almSetting);
        getDbClient().projectAlmSettingDao().insertOrUpdate(dbSession, createProjectAlmSettingDto(project.uuid(), almSettingDto.getUuid(), request));
        dbSession.commit();

        response.noContent();

    }


    protected abstract ProjectAlmSettingDto createProjectAlmSettingDto(String projectUuid, String settingsUuid, Request request);

}
