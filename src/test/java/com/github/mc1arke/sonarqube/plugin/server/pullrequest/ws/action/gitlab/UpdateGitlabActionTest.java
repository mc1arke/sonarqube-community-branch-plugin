package com.github.mc1arke.sonarqube.plugin.server.pullrequest.ws.action.gitlab;

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

public class UpdateGitlabActionTest {

    @Test
    public void testConfigureAction() {
        DbClient dbClient = mock(DbClient.class);
        UserSession userSession = mock(UserSession.class);

        WebService.NewAction newAction = mock(WebService.NewAction.class);

        WebService.NewParam personalAccessTokenParameter = mock(WebService.NewParam.class);
        when(personalAccessTokenParameter.setMaximumLength(any(Integer.class))).thenReturn(personalAccessTokenParameter);
        when(personalAccessTokenParameter.setRequired(anyBoolean())).thenReturn(personalAccessTokenParameter);
        when(newAction.createParam(eq("personalAccessToken"))).thenReturn(personalAccessTokenParameter);

        UpdateGitlabAction testCase = new UpdateGitlabAction(dbClient, userSession);
        testCase.configureAction(newAction);

        verify(personalAccessTokenParameter).setRequired(eq(true));
        verify(personalAccessTokenParameter).setMaximumLength(2000);
    }

    @Test
    public void testUpdateAlmSettingsDto() {
        DbClient dbClient = mock(DbClient.class);
        UserSession userSession = mock(UserSession.class);

        Request request = mock(Request.class);
        when(request.mandatoryParam(eq("personalAccessToken"))).thenReturn("personalAccessToken");

        UpdateGitlabAction testCase = new UpdateGitlabAction(dbClient, userSession);
        AlmSettingDto result = testCase.updateAlmSettingsDto(new AlmSettingDto().setKey("originalKey"), request);

        assertThat(result).isEqualToComparingFieldByField(new AlmSettingDto().setKey("originalKey").setPersonalAccessToken("personalAccessToken"));
    }
}
