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
package com.github.mc1arke.sonarqube.plugin.server.pullrequest.ws.action.github;

import com.github.mc1arke.sonarqube.plugin.server.pullrequest.ws.action.AlmSettingsWsAction;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.alm.setting.AlmSettingDto;
import org.sonar.server.user.UserSession;

import static org.sonar.db.alm.setting.ALM.GITHUB;

public class CreateGithubAction extends AlmSettingsWsAction {

    private static final String PARAM_KEY = "key";
    private static final String PARAM_URL = "url";
    private static final String PARAM_APP_ID = "appId";
    private static final String PARAM_PRIVATE_KEY = "privateKey";

    private final DbClient dbClient;
    private final UserSession userSession;

    public CreateGithubAction(DbClient dbClient, UserSession userSession) {
        super(dbClient);
        this.dbClient = dbClient;
        this.userSession = userSession;
    }

    @Override
    public void define(WebService.NewController context) {
        WebService.NewAction action = context.createAction("create_github").setPost(true).setHandler(this);

        action.createParam(PARAM_KEY).setRequired(true).setMaximumLength(200);
        action.createParam(PARAM_URL).setRequired(true).setMaximumLength(2000);
        action.createParam(PARAM_APP_ID).setRequired(true).setMaximumLength(80);
        action.createParam(PARAM_PRIVATE_KEY).setRequired(true).setMaximumLength(2000);
    }

    @Override
    public void handle(Request request, Response response) {
        userSession.checkIsSystemAdministrator();

        String key = request.mandatoryParam(PARAM_KEY);
        String url = request.mandatoryParam(PARAM_URL);
        String appId = request.mandatoryParam(PARAM_APP_ID);
        String privateKey = request.mandatoryParam(PARAM_PRIVATE_KEY);

        try (DbSession dbSession = dbClient.openSession(false)) {
            checkAlmSettingDoesNotAlreadyExist(dbSession, key);
            dbClient.almSettingDao().insert(dbSession,
                                            new AlmSettingDto().setAlm(GITHUB).setKey(key).setUrl(url).setAppId(appId)
                                                    .setPrivateKey(privateKey));
            dbSession.commit();
        }


        response.noContent();
    }

}
