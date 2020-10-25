package com.github.mc1arke.sonarqube.plugin.server.pullrequest.ws.action.github;

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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class CreateGithubActionTest {

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

        WebService.NewParam clientIdParameter = mock(WebService.NewParam.class);
        when(clientIdParameter.setMaximumLength(any(Integer.class))).thenReturn(clientIdParameter);
        when(clientIdParameter.setRequired(anyBoolean())).thenReturn(clientIdParameter);
        when(newAction.createParam(eq("clientId"))).thenReturn(clientIdParameter);

        WebService.NewParam clientSecretParameter = mock(WebService.NewParam.class);
        when(clientSecretParameter.setMaximumLength(any(Integer.class))).thenReturn(clientSecretParameter);
        when(clientSecretParameter.setRequired(anyBoolean())).thenReturn(clientSecretParameter);
        when(newAction.createParam(eq("clientSecret"))).thenReturn(clientSecretParameter);

        CreateGithubAction testCase = new CreateGithubAction(dbClient, userSession);
        testCase.configureAction(newAction);

        verify(urlParameter).setRequired(eq(true));
        verify(urlParameter).setMaximumLength(2000);

        verify(appIdParameter).setRequired(eq(true));
        verify(appIdParameter).setMaximumLength(80);

        verify(privateKeyParameter).setRequired(eq(true));
        verify(privateKeyParameter).setMaximumLength(2000);

        verify(clientIdParameter).setRequired(eq(true));
        verify(clientIdParameter).setMaximumLength(80);

        verify(clientSecretParameter).setRequired(eq(true));
        verify(clientSecretParameter).setMaximumLength(80);
    }

    @Test
    public void testCreateAlmSettingDto() {
        DbClient dbClient = mock(DbClient.class);
        UserSession userSession = mock(UserSession.class);

        Request request = mock(Request.class);
        when(request.mandatoryParam(eq("appId"))).thenReturn("appId");
        when(request.mandatoryParam(eq("privateKey"))).thenReturn("privateKey");
        when(request.mandatoryParam(eq("url"))).thenReturn("url");

        CreateGithubAction testCase = new CreateGithubAction(dbClient, userSession);
        AlmSettingDto result = testCase.createAlmSettingDto("key", request);

        assertThat(result).isEqualToComparingFieldByField(new AlmSettingDto().setAlm(ALM.GITHUB).setKey("key").setUrl("url").setPrivateKey("privateKey").setAppId("appId"));
    }
}
