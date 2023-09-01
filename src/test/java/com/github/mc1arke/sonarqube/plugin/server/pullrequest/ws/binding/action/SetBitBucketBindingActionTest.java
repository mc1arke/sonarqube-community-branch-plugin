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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SetBitBucketBindingActionTest {

    @Test
    void shouldDefineActionWithRequiredParameters() {
        DbClient dbClient = mock(DbClient.class);
        UserSession userSession = mock(UserSession.class);
        ComponentFinder componentFinder = mock(ComponentFinder.class);

        WebService.NewAction newAction = mock(WebService.NewAction.class);
        when(newAction.setPost(anyBoolean())).thenReturn(newAction);

        WebService.NewParam slugParameter = mock(WebService.NewParam.class);
        when(slugParameter.setMaximumLength(any(Integer.class))).thenReturn(slugParameter);
        when(slugParameter.setRequired(anyBoolean())).thenReturn(slugParameter);
        when(newAction.createParam("slug")).thenReturn(slugParameter);

        WebService.NewParam repositoryParameter = mock(WebService.NewParam.class);
        when(repositoryParameter.setMaximumLength(any(Integer.class))).thenReturn(repositoryParameter);
        when(repositoryParameter.setRequired(anyBoolean())).thenReturn(repositoryParameter);
        when(newAction.createParam("repository")).thenReturn(repositoryParameter);

        WebService.NewParam almSettingParameter = mock(WebService.NewParam.class);
        when(almSettingParameter.setMaximumLength(any(Integer.class))).thenReturn(almSettingParameter);
        when(almSettingParameter.setRequired(anyBoolean())).thenReturn(almSettingParameter);
        when(newAction.createParam("almSetting")).thenReturn(almSettingParameter);

        WebService.NewParam monoRepoParameter = mock(WebService.NewParam.class);
        when(monoRepoParameter.setRequired(anyBoolean())).thenReturn(monoRepoParameter);
        when(newAction.createParam("monorepo")).thenReturn(monoRepoParameter);

        SetBitbucketBindingAction testCase = new SetBitbucketBindingAction(dbClient, componentFinder, userSession);
        testCase.configureAction(newAction);

        verify(newAction).setPost(true);
        verify(slugParameter).setRequired(true);

        verify(repositoryParameter).setRequired(true);

        verify(almSettingParameter).setRequired(true);

        verify(monoRepoParameter).setRequired(true);
        verify(monoRepoParameter).setBooleanPossibleValues();
    }

    @Test
    void shouldHandleRequestWithRequiredParameters() {
        DbClient dbClient = mock(DbClient.class);
        UserSession userSession = mock(UserSession.class);
        ComponentFinder componentFinder = mock(ComponentFinder.class);

        Request request = mock(Request.class);
        when(request.mandatoryParam("slug")).thenReturn("slug");
        when(request.mandatoryParam("repository")).thenReturn("repository");

        SetBitbucketBindingAction testCase = new SetBitbucketBindingAction(dbClient, componentFinder, userSession);
        ProjectAlmSettingDto result = testCase.createProjectAlmSettingDto("projectUuid", "settingsUuid", true, request);

        assertThat(result).usingRecursiveComparison().isEqualTo(new ProjectAlmSettingDto().setProjectUuid("projectUuid").setAlmSettingUuid("settingsUuid").setAlmRepo("repository").setAlmSlug("slug").setMonorepo(true));

    }
}
