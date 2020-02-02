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
import org.sonar.server.user.UserSession;

import static org.apache.commons.lang.StringUtils.isNotBlank;

public abstract class UpdateAction extends AlmSettingsWsAction {

    private static final String KEY_PARAMETER = "key";
    private static final String NEW_KEY_PARAMETER = "newKey";

    private final UserSession userSession;
    private final String actionName;

    protected UpdateAction(DbClient dbClient, UserSession userSession, String actionName) {
        super(dbClient);
        this.userSession = userSession;
        this.actionName = actionName;
    }

    @Override
    public void define(WebService.NewController newController) {
        WebService.NewAction action = newController.createAction(actionName).setPost(true).setHandler(this);
        action.createParam(KEY_PARAMETER).setMaximumLength(200).setRequired(true);
        action.createParam(NEW_KEY_PARAMETER).setMaximumLength(200);
        configureAction(action);
    }

    protected abstract void configureAction(WebService.NewAction action);

    @Override
    public void handle(Request request, Response response) {
        userSession.checkIsSystemAdministrator();

        String key = request.mandatoryParam(KEY_PARAMETER);
        String newKey = request.param(NEW_KEY_PARAMETER);

        try (DbSession dbSession = getDbClient().openSession(false)) {
            AlmSettingDto almSettingDto = getAlmSetting(dbSession, key);
            if (isNotBlank(newKey) && !newKey.equals(key)) {
                checkAlmSettingDoesNotAlreadyExist(dbSession, newKey);
            }
            getDbClient().almSettingDao().update(dbSession, updateAlmSettingsDto(almSettingDto.setKey(isNotBlank(newKey) ? newKey : key), request));
            dbSession.commit();
        }

        response.noContent();
    }

    protected abstract AlmSettingDto updateAlmSettingsDto(AlmSettingDto almSettingDto, Request request);
}
