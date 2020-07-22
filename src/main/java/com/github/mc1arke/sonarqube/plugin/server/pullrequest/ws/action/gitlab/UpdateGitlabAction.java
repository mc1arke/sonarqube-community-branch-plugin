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
package com.github.mc1arke.sonarqube.plugin.server.pullrequest.ws.action.gitlab;

import com.github.mc1arke.sonarqube.plugin.server.pullrequest.ws.action.UpdateAction;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.WebService;
import org.sonar.db.DbClient;
import org.sonar.db.alm.setting.AlmSettingDto;
import org.sonar.server.user.UserSession;

public class UpdateGitlabAction extends UpdateAction {

    private static final String URL_PARAMETER = "url";
    private static final String PERSONAL_ACCESS_TOKEN_PARAMETER = "personalAccessToken";

    public UpdateGitlabAction(DbClient dbClient, UserSession userSession) {
        super(dbClient, userSession, "update_gitlab");
    }

    @Override
    protected void configureAction(WebService.NewAction action) {
        action.createParam(URL_PARAMETER).setMaximumLength(2000);
        action.createParam(PERSONAL_ACCESS_TOKEN_PARAMETER).setRequired(true).setMaximumLength(2000);
    }

    @Override
    protected AlmSettingDto updateAlmSettingsDto(AlmSettingDto almSettingDto, Request request) {
        return almSettingDto.setPersonalAccessToken(request.mandatoryParam(PERSONAL_ACCESS_TOKEN_PARAMETER))
            .setUrl(request.param(URL_PARAMETER));
    }

}
