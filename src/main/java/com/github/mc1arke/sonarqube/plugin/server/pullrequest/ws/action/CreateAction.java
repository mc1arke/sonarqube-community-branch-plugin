/*
 * Copyright (C) 2020-2021 Michael Clarke
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

public abstract class CreateAction extends AlmSettingsWebserviceAction {

    private static final String KEY_PARAMETER = "key";

    private final UserSession userSession;
    private final String actionName;

    protected CreateAction(DbClient dbClient, UserSession userSession, String actionName) {
        super(dbClient);
        this.userSession = userSession;
        this.actionName = actionName;
    }

    @Override
    public void define(WebService.NewController newController) {
        WebService.NewAction action = newController.createAction(actionName).setPost(true).setHandler(this);
        action.createParam(KEY_PARAMETER).setMaximumLength(200).setRequired(true);
        configureAction(action);
    }

    protected abstract void configureAction(WebService.NewAction action);

    @Override
    public void handle(Request request, Response response) {
        userSession.checkIsSystemAdministrator();

        String key = request.mandatoryParam(KEY_PARAMETER);

        try (DbSession dbSession = getDbClient().openSession(false)) {
            checkAlmSettingDoesNotAlreadyExist(dbSession, key);
            getDbClient().almSettingDao().insert(dbSession, createAlmSettingDto(key, request));
            dbSession.commit();
        }

        response.noContent();
    }

    protected abstract AlmSettingDto createAlmSettingDto(String key, Request request);
}
