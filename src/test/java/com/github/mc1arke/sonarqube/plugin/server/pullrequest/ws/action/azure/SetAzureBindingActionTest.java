package com.github.mc1arke.sonarqube.plugin.server.pullrequest.ws.action.azure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import org.junit.Test;
import org.sonar.api.server.ws.Request;
import org.sonar.db.DbClient;
import org.sonar.db.alm.setting.ProjectAlmSettingDto;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.user.UserSession;

public class SetAzureBindingActionTest {


    @Test
    public void testCreateProjectAlmSettingDto() {
        DbClient dbClient = mock(DbClient.class);
        UserSession userSession = mock(UserSession.class);
        ComponentFinder componentFinder = mock(ComponentFinder.class);

        Request request = mock(Request.class);

        SetAzureBindingAction testCase = new SetAzureBindingAction(dbClient, componentFinder, userSession);
        ProjectAlmSettingDto result = testCase.createProjectAlmSettingDto("projectUuid", "settingsUuid", request);

        assertThat(result).isEqualToComparingFieldByField(new ProjectAlmSettingDto().setProjectUuid("projectUuid").setAlmSettingUuid("settingsUuid"));
        verifyNoMoreInteractions(request);

    }
}
