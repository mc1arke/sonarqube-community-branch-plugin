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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.alm.setting.AlmSettingDao;
import org.sonar.db.alm.setting.AlmSettingDto;
import org.sonar.db.alm.setting.ProjectAlmSettingDao;
import org.sonar.db.alm.setting.ProjectAlmSettingDto;
import org.sonar.db.project.ProjectDto;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.user.UserSession;

class SetBindingActionTest {

    @Test
    void shouldDefineActionWithRequiredParameters() {
        DbClient dbClient = mock();
        ComponentFinder componentFinder = mock();
        UserSession userSession = mock();
        ProjectAlmSettingDto projectAlmSettingDto = mock();
        SetBindingAction testCase = new SetBindingAction(dbClient, componentFinder, userSession, "dummy") {

            @Override
            protected ProjectAlmSettingDto createProjectAlmSettingDto(String projectUuid, String settingsUuid, boolean monoRepo, Request request) {
                return projectAlmSettingDto;
            }
        };

        Map<String, WebService.NewParam> paramMap = new HashMap<>();

        WebService.NewController newController = mock();
        WebService.NewAction newAction = mock();
        when(newController.createAction(any())).thenReturn(newAction);
        when(newAction.setPost(true)).thenReturn(newAction);
        when(newAction.setHandler(testCase)).thenReturn(newAction);
        when(newAction.createParam(any())).then(i -> {
            WebService.NewParam newParam = mock();
            paramMap.put(i.getArgument(0), newParam);
            when(newParam.setRequired(anyBoolean())).thenReturn(newParam);
            return newParam;
        });
        testCase.define(newController);

        verify(newAction).createParam("project");
        verify(newAction).createParam("almSetting");
        verify(paramMap.get("project")).setRequired(true);
        verify(paramMap.get("almSetting")).setRequired(true);
        verify(paramMap.get("monorepo")).setRequired(true);
        verify(paramMap.get("monorepo")).setBooleanPossibleValues();
    }

    @Test
    void shouldHandleRequestWithRequiredParameters() {
        DbClient dbClient = mock();
        DbSession dbSession = mock();
        when(dbClient.openSession(false)).thenReturn(dbSession);
        AlmSettingDao almSettingDao = mock();
        AlmSettingDto almSettingDto = mock();
        when(almSettingDto.getUuid()).thenReturn("almSettingsUuid");
        when(almSettingDao.selectByKey(dbSession, "almSetting")).thenReturn(Optional.of(almSettingDto));
        when(dbClient.almSettingDao()).thenReturn(almSettingDao);
        ProjectAlmSettingDao projectAlmSettingDao = mock();
        when(dbClient.projectAlmSettingDao()).thenReturn(projectAlmSettingDao);
        ComponentFinder componentFinder = mock();
        ProjectDto componentDto = mock();
        when(componentDto.getUuid()).thenReturn("projectUuid");
        when(componentFinder.getProjectByKey(dbSession, "project")).thenReturn(componentDto);
        UserSession userSession = mock();
        ThreadLocal<WebService.NewAction> capturedAction = new ThreadLocal<>();
        ProjectAlmSettingDto projectAlmSettingDto = mock();
        SetBindingAction testCase = new SetBindingAction(dbClient, componentFinder, userSession, "dummy") {

            @Override
            protected void configureAction(WebService.NewAction action) {
                capturedAction.set(action);
            }

            @Override
            protected ProjectAlmSettingDto createProjectAlmSettingDto(String projectUuid, String settingsUuid, boolean monoRepo, Request request) {
                assertThat(projectUuid).isEqualTo("projectUuid");
                assertThat(settingsUuid).isEqualTo("almSettingsUuid");
                return projectAlmSettingDto;
            }
        };

        Request request = mock();
        Response response = mock();

        when(request.mandatoryParam("almSetting")).thenReturn("almSetting");
        when(request.mandatoryParam("project")).thenReturn("project");

        testCase.handle(request, response);

        verify(projectAlmSettingDao).insertOrUpdate(eq(dbSession), eq(projectAlmSettingDto), any(), any(), any());
        verify(dbSession).commit();
        verify(response).noContent();
    }
}