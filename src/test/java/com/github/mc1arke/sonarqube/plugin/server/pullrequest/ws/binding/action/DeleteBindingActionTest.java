/*
 * Copyright (C) 2020-2024 Michael Clarke
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
 */
package com.github.mc1arke.sonarqube.plugin.server.pullrequest.ws.binding.action;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.alm.setting.AlmSettingDao;
import org.sonar.db.alm.setting.ProjectAlmSettingDao;
import org.sonar.db.project.ProjectDto;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.user.UserSession;

class DeleteBindingActionTest {

    @Test
    void shouldDefineEndpointWithParameters() {
        DbClient dbClient = mock();
        UserSession userSession = mock();
        ComponentFinder componentFinder = mock();
        DeleteBindingAction testCase = new DeleteBindingAction(dbClient, userSession, componentFinder);

        WebService.NewParam keyParam = mock();
        when(keyParam.setMaximumLength(200)).thenReturn(keyParam);
        WebService.NewParam newKeyParam = mock();
        when(newKeyParam.setMaximumLength(200)).thenReturn(newKeyParam);
        WebService.NewController newController = mock();
        WebService.NewAction newAction = mock();
        when(newController.createAction("delete_binding")).thenReturn(newAction);
        when(newAction.setPost(true)).thenReturn(newAction);
        when(newAction.setHandler(testCase)).thenReturn(newAction);
        when(newAction.createParam("project")).thenReturn(keyParam);

        testCase.define(newController);

        verify(newAction).setHandler(testCase);
        verify(keyParam).setRequired(true);

    }

    @Test
    void shouldHandleEndpointWithValidRequest() {
        DbClient dbClient = mock();
        DbSession dbSession = mock();
        when(dbClient.openSession(false)).thenReturn(dbSession);
        AlmSettingDao almSettingDao = mock();
        when(dbClient.almSettingDao()).thenReturn(almSettingDao);

        ProjectAlmSettingDao projectAlmSettingDao = mock();
        when(dbClient.projectAlmSettingDao()).thenReturn(projectAlmSettingDao);

        UserSession userSession = mock();

        ProjectDto componentDto = mock();
        ComponentFinder componentFinder = mock();
        when(componentFinder.getProjectByKey(dbSession, "projectKey")).thenReturn(componentDto);

        DeleteBindingAction testCase = new DeleteBindingAction(dbClient, userSession, componentFinder);

        Request request = mock(Request.class, Mockito.RETURNS_DEEP_STUBS);
        when(request.mandatoryParam("project")).thenReturn("projectKey");
        Response response = mock(Response.class, Mockito.RETURNS_DEEP_STUBS);

        testCase.handle(request, response);

        verify(dbSession).commit();
        verify(projectAlmSettingDao).deleteByProject(dbSession, componentDto);
        verify(response).noContent();
        verify(userSession).hasEntityPermission("admin", componentDto);

    }


}