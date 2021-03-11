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
import org.sonar.api.server.ws.WebService;
import org.sonar.db.DbClient;
import org.sonar.db.alm.setting.AlmSettingDto;
import org.sonar.server.user.UserSession;

public class UpdateBitbucketCloudAction extends UpdateAction {

    private static final String CLIENT_ID_PARAMETER = "clientId";
    private static final String CLIENT_SECRET_PARAMETER = "clientSecret";
    private static final String WORKSPACE_PARAMETER = "workspace";

    public UpdateBitbucketCloudAction(DbClient dbClient, UserSession userSession) {
        super(dbClient, userSession, "update_bitbucketcloud");
    }

    @Override
    protected void configureAction(WebService.NewAction action) {
        action.createParam(CLIENT_ID_PARAMETER).setRequired(true).setMaximumLength(2000);
        action.createParam(CLIENT_SECRET_PARAMETER).setRequired(true).setMaximumLength(2000);
        action.createParam(WORKSPACE_PARAMETER).setRequired(true);
    }

    @Override
    protected AlmSettingDto updateAlmSettingsDto(AlmSettingDto almSettingDto, Request request) {
        return almSettingDto.setClientSecret(request.mandatoryParam(CLIENT_SECRET_PARAMETER))
                .setClientId(request.mandatoryParam(CLIENT_ID_PARAMETER))
                .setAppId(request.mandatoryParam(WORKSPACE_PARAMETER));
    }

}
