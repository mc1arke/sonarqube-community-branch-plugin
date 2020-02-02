package com.github.mc1arke.sonarqube.plugin.server.pullrequest.ws.action.bitbucket;

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
import org.sonar.db.alm.setting.ALM;
import org.sonar.db.alm.setting.AlmSettingDto;
import org.sonar.server.user.UserSession;

public class UpdateBitbucketActionTest {

    @Test
    public void testConfigureAction() {
        DbClient dbClient = mock(DbClient.class);
        UserSession userSession = mock(UserSession.class);

        WebService.NewAction newAction = mock(WebService.NewAction.class);

        WebService.NewParam urlParameter = mock(WebService.NewParam.class);
        when(urlParameter.setMaximumLength(any(Integer.class))).thenReturn(urlParameter);
        when(urlParameter.setRequired(anyBoolean())).thenReturn(urlParameter);
        when(newAction.createParam(eq("url"))).thenReturn(urlParameter);


        WebService.NewParam personalAccessTokenParameter = mock(WebService.NewParam.class);
        when(personalAccessTokenParameter.setMaximumLength(any(Integer.class))).thenReturn(personalAccessTokenParameter);
        when(personalAccessTokenParameter.setRequired(anyBoolean())).thenReturn(personalAccessTokenParameter);
        when(newAction.createParam(eq("personalAccessToken"))).thenReturn(personalAccessTokenParameter);

        UpdateBitbucketAction testCase = new UpdateBitbucketAction(dbClient, userSession);
        testCase.configureAction(newAction);

        verify(urlParameter).setRequired(eq(true));
        verify(urlParameter).setMaximumLength(2000);

        verify(personalAccessTokenParameter).setRequired(eq(true));
        verify(personalAccessTokenParameter).setMaximumLength(2000);
    }

    @Test
    public void testUpdateAlmSettingsDto() {
        DbClient dbClient = mock(DbClient.class);
        UserSession userSession = mock(UserSession.class);

        Request request = mock(Request.class);
        when(request.mandatoryParam(eq("personalAccessToken"))).thenReturn("personalAccessToken");
        when(request.mandatoryParam(eq("url"))).thenReturn("url");

        UpdateBitbucketAction testCase = new UpdateBitbucketAction(dbClient, userSession);
        AlmSettingDto result = testCase.updateAlmSettingsDto(new AlmSettingDto().setKey("originalKey"), request);

        assertThat(result).isEqualToComparingFieldByField(new AlmSettingDto().setKey("originalKey").setPersonalAccessToken("personalAccessToken").setUrl("url"));
    }
}
