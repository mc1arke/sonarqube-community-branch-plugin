package com.github.mc1arke.sonarqube.plugin.server.pullrequest.ws.action.github;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.WebService;
import org.sonar.db.DbClient;
import org.sonar.db.alm.setting.AlmSettingDto;
import org.sonar.server.user.UserSession;

public class UpdateGithubActionTest {

    @Test
    public void testConfigureAction() {
        DbClient dbClient = mock(DbClient.class);
        UserSession userSession = mock(UserSession.class);

        WebService.NewAction newAction = mock(WebService.NewAction.class);

        WebService.NewParam urlParameter = mock(WebService.NewParam.class);
        when(urlParameter.setMaximumLength(any(Integer.class))).thenReturn(urlParameter);
        when(urlParameter.setRequired(anyBoolean())).thenReturn(urlParameter);
        when(newAction.createParam(eq("url"))).thenReturn(urlParameter);

        WebService.NewParam appIdParameter = mock(WebService.NewParam.class);
        when(appIdParameter.setMaximumLength(any(Integer.class))).thenReturn(appIdParameter);
        when(appIdParameter.setRequired(anyBoolean())).thenReturn(appIdParameter);
        when(newAction.createParam(eq("appId"))).thenReturn(appIdParameter);

        WebService.NewParam privateKeyParameter = mock(WebService.NewParam.class);
        when(privateKeyParameter.setMaximumLength(any(Integer.class))).thenReturn(privateKeyParameter);
        when(privateKeyParameter.setRequired(anyBoolean())).thenReturn(privateKeyParameter);
        when(newAction.createParam(eq("privateKey"))).thenReturn(privateKeyParameter);

        UpdateGithubAction testCase = new UpdateGithubAction(dbClient, userSession);
        testCase.configureAction(newAction);

        verify(urlParameter).setRequired(eq(true));
        verify(urlParameter).setMaximumLength(2000);

        verify(appIdParameter).setRequired(eq(true));
        verify(appIdParameter).setMaximumLength(80);

        verify(privateKeyParameter).setRequired(eq(true));
        verify(privateKeyParameter).setMaximumLength(2000);
    }

    @Test
    public void testUpdateAlmSettingsDto() {
        DbClient dbClient = mock(DbClient.class);
        UserSession userSession = mock(UserSession.class);

        Request request = mock(Request.class);
        when(request.mandatoryParam(eq("appId"))).thenReturn("appId");
        when(request.mandatoryParam(eq("privateKey"))).thenReturn("privateKey");
        when(request.mandatoryParam(eq("url"))).thenReturn("url");

        UpdateGithubAction testCase = new UpdateGithubAction(dbClient, userSession);
        AlmSettingDto result = testCase.updateAlmSettingsDto(new AlmSettingDto().setKey("originalKey"), request);

        assertThat(result).isEqualToComparingFieldByField(new AlmSettingDto().setKey("originalKey").setUrl("url").setPrivateKey("privateKey").setAppId("appId"));
    }
}
