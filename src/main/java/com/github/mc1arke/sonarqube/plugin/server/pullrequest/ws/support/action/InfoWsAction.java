/*
 * Copyright (C) 2026 Michael Clarke
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
package com.github.mc1arke.sonarqube.plugin.server.pullrequest.ws.support.action;

import java.io.IOException;
import java.io.OutputStream;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.Response.Stream;
import org.sonar.api.server.ws.WebService.NewController;
import org.sonar.api.utils.DateUtils;
import org.sonar.server.property.InternalProperties;
import org.sonar.server.user.UserSession;

public class InfoWsAction implements SupportWsAction {

    private final UserSession userSession;
    private final InternalProperties internalProperties;
    private final ObjectMapper objectMapper;

    public InfoWsAction(UserSession userSession, InternalProperties internalProperties) {
        this.userSession = userSession;
        this.internalProperties = internalProperties;
        this.objectMapper = new ObjectMapper()
                .setSerializationInclusion(JsonInclude.Include.NON_NULL)
                .findAndRegisterModules();
    }

    @Override
    public void define(NewController newController) {
        newController.createAction("info").setHandler(this);
    }

    @Override
    public void handle(Request request, Response response) throws IOException {
        userSession.checkLoggedIn().checkIsSystemAdministrator();
        Stream stream = response.stream();
        try (OutputStream outputStream = stream.output()) {
            stream.setMediaType("application/json");
            stream.setStatus(200);
            objectMapper.writeValue(outputStream, new InfoResponse(
                    new InfoResponse.Statistics(
                            internalProperties.read(InternalProperties.INSTALLATION_DATE)
                                    .map(date -> DateUtils.formatDateTime(Long.parseLong(date))).orElse(null))
                    )
            );
        }
    }

    private record InfoResponse(Statistics statistics) {
        private record Statistics(String installationDate) {
        }
    }
}
