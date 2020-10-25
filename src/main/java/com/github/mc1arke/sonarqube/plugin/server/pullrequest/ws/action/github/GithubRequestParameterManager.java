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

import org.sonar.api.server.ws.WebService;

final class GithubRequestParameterManager {

    static final String URL_PARAMETER = "url";
    static final String APP_ID_PARAMETER = "appId";
    static final String PRIVATE_KEY_PARAMETER = "privateKey";
    static final String CLIENT_ID = "clientId";
    static final String CLIENT_SECRET = "clientSecret";

    private GithubRequestParameterManager() {
        super();
    }

    static void createRequestParameters(WebService.NewAction action) {
        action.createParam(URL_PARAMETER).setRequired(true).setMaximumLength(2000);
        action.createParam(APP_ID_PARAMETER).setRequired(true).setMaximumLength(80);
        action.createParam(PRIVATE_KEY_PARAMETER).setRequired(true).setMaximumLength(2000);
        action.createParam(CLIENT_ID).setRequired(true).setMaximumLength(80);
        action.createParam(CLIENT_SECRET).setRequired(true).setMaximumLength(80);
    }

}
