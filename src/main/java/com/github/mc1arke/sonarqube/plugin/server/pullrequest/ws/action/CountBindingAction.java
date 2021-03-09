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
import org.sonar.server.ws.WsUtils;
import org.sonarqube.ws.AlmSettings.CountBindingWsResponse;


public class CountBindingAction extends AlmSettingsWebserviceAction {

    private static final String ALM_SETTING_PARAMETER = "almSetting";

    private final DbClient dbClient;
    private final UserSession userSession;
    private final ProtoBufWriter protoBufWriter;

    public CountBindingAction(DbClient dbClient, UserSession userSession) {
        this(dbClient, userSession, WsUtils::writeProtobuf);
    }

    CountBindingAction(DbClient dbClient, UserSession userSession, ProtoBufWriter protoBufWriter) {
        super(dbClient);
        this.dbClient = dbClient;
        this.userSession = userSession;
        this.protoBufWriter = protoBufWriter;
    }

    @Override
    public void define(WebService.NewController context) {
        WebService.NewAction action = context.createAction("count_binding").setHandler(this);

        action.createParam(ALM_SETTING_PARAMETER).setRequired(true);
    }

    @Override
    public void handle(Request request, Response response) {
        userSession.checkIsSystemAdministrator();

        String almSettingKey = request.mandatoryParam(ALM_SETTING_PARAMETER);
        try (DbSession dbSession = dbClient.openSession(false)) {
            AlmSettingDto almSetting = getAlmSetting(dbSession, almSettingKey);
            int projectCount = dbClient.projectAlmSettingDao().countByAlmSetting(dbSession, almSetting);
            CountBindingWsResponse.Builder builder =
                    CountBindingWsResponse.newBuilder().setKey(almSetting.getKey()).setProjects(projectCount);

            protoBufWriter.write(builder.build(), request, response);
        }

    }

}
