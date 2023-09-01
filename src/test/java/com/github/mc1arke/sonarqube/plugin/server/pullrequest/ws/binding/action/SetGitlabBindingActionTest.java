/*
 * Copyright (C) 2020-2022 Michael Clarke
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
package com.github.mc1arke.sonarqube.plugin.server.pullrequest.ws.binding.action;

import org.junit.jupiter.api.Test;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.WebService;
import org.sonar.db.DbClient;
import org.sonar.db.alm.setting.ProjectAlmSettingDto;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.user.UserSession;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class SetGitlabBindingActionTest {

    @Test
    void shouldDefineActionWithRequiredParameters() {
        DbClient dbClient = mock(DbClient.class);
        UserSession userSession = mock(UserSession.class);
        ComponentFinder componentFinder = mock(ComponentFinder.class);

        WebService.NewAction newAction = mock(WebService.NewAction.class);
        when(newAction.setPost(anyBoolean())).thenReturn(newAction);

        WebService.NewParam repositoryParameter = mock(WebService.NewParam.class);
        when(newAction.createParam("repository")).thenReturn(repositoryParameter);

        WebService.NewParam almSettingParameter = mock(WebService.NewParam.class);
        when(almSettingParameter.setRequired(anyBoolean())).thenReturn(almSettingParameter);
        when(newAction.createParam("almSetting")).thenReturn(almSettingParameter);

        WebService.NewParam monoRepoParameter = mock(WebService.NewParam.class);
        when(monoRepoParameter.setRequired(anyBoolean())).thenReturn(monoRepoParameter);
        when(newAction.createParam("monorepo")).thenReturn(monoRepoParameter);

        SetGitlabBindingAction testCase = new SetGitlabBindingAction(dbClient, componentFinder, userSession);
        testCase.configureAction(newAction);

        verify(newAction).createParam("repository");
        verify(newAction).setPost(true);

        verify(almSettingParameter).setRequired(true);
        verify(monoRepoParameter).setRequired(true);
        verify(monoRepoParameter).setBooleanPossibleValues();
    }

    @Test
    void shouldHandleRequestWithValidParameters() {
        DbClient dbClient = mock(DbClient.class);
        UserSession userSession = mock(UserSession.class);
        ComponentFinder componentFinder = mock(ComponentFinder.class);

        Request request = mock(Request.class);
        when(request.param("repository")).thenReturn("repositoryId");

        SetGitlabBindingAction testCase = new SetGitlabBindingAction(dbClient, componentFinder, userSession);
        ProjectAlmSettingDto result = testCase.createProjectAlmSettingDto("projectUuid", "settingsUuid", true, request);

        assertThat(result).usingRecursiveComparison().isEqualTo(new ProjectAlmSettingDto().setProjectUuid("projectUuid").setAlmSettingUuid("settingsUuid").setAlmRepo("repositoryId").setMonorepo(true));
        verify(request).param("repository");
        verifyNoMoreInteractions(request);
    }
}
