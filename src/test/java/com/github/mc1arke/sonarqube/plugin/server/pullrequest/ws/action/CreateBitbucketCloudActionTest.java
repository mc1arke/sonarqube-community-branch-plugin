/*
 * Copyright (C) 2021 Michael Clarke
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

import org.junit.Test;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.WebService;
import org.sonar.db.DbClient;
import org.sonar.db.alm.setting.ALM;
import org.sonar.db.alm.setting.AlmSettingDto;
import org.sonar.server.user.UserSession;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class CreateBitbucketCloudActionTest {

    @Test
    public void testConfigureAction() {
        DbClient dbClient = mock(DbClient.class);
        UserSession userSession = mock(UserSession.class);

        WebService.NewAction newAction = mock(WebService.NewAction.class);

        WebService.NewParam clientIdParameter = mock(WebService.NewParam.class);
        when(clientIdParameter.setMaximumLength(any(Integer.class))).thenReturn(clientIdParameter);
        when(clientIdParameter.setRequired(anyBoolean())).thenReturn(clientIdParameter);
        when(newAction.createParam("clientId")).thenReturn(clientIdParameter);

        WebService.NewParam clientSecretParameter = mock(WebService.NewParam.class);
        when(clientSecretParameter.setMaximumLength(any(Integer.class))).thenReturn(clientSecretParameter);
        when(clientSecretParameter.setRequired(anyBoolean())).thenReturn(clientSecretParameter);
        when(newAction.createParam("clientSecret")).thenReturn(clientSecretParameter);

        WebService.NewParam workspaceParameter = mock(WebService.NewParam.class);
        when(workspaceParameter.setRequired(anyBoolean())).thenReturn(workspaceParameter);
        when(newAction.createParam("workspace")).thenReturn(workspaceParameter);

        CreateBitbucketCloudAction testCase = new CreateBitbucketCloudAction(dbClient, userSession);
        testCase.configureAction(newAction);

        verify(workspaceParameter).setRequired(true);

        verify(clientIdParameter).setRequired(true);
        verify(clientIdParameter).setMaximumLength(2000);
        verify(clientSecretParameter).setRequired(true);
        verify(clientSecretParameter).setMaximumLength(2000);
    }

    @Test
    public void testCreateAlmSettingDto() {
        DbClient dbClient = mock(DbClient.class);
        UserSession userSession = mock(UserSession.class);

        Request request = mock(Request.class);
        when(request.mandatoryParam("clientId")).thenReturn("clientId");
        when(request.mandatoryParam("clientSecret")).thenReturn("clientSecret");
        when(request.mandatoryParam("workspace")).thenReturn("workspace");

        CreateBitbucketCloudAction testCase = new CreateBitbucketCloudAction(dbClient, userSession);
        AlmSettingDto result = testCase.createAlmSettingDto("key", request);

        assertThat(result).isEqualToComparingFieldByField(new AlmSettingDto().setAlm(ALM.BITBUCKET_CLOUD).setKey("key").setClientId("clientId").setClientSecret("clientSecret").setAppId("workspace"));
    }
}
