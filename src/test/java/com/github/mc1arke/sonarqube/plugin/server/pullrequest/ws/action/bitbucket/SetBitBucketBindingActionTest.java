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
import org.sonar.db.alm.setting.ProjectAlmSettingDto;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.user.UserSession;

public class SetBitBucketBindingActionTest {

    @Test
    public void testConfigureAction() {
        DbClient dbClient = mock(DbClient.class);
        UserSession userSession = mock(UserSession.class);
        ComponentFinder componentFinder = mock(ComponentFinder.class);

        WebService.NewAction newAction = mock(WebService.NewAction.class);

        WebService.NewParam slugParameter = mock(WebService.NewParam.class);
        when(slugParameter.setMaximumLength(any(Integer.class))).thenReturn(slugParameter);
        when(slugParameter.setRequired(anyBoolean())).thenReturn(slugParameter);
        when(newAction.createParam(eq("slug"))).thenReturn(slugParameter);

        WebService.NewParam repositoryParameter = mock(WebService.NewParam.class);
        when(repositoryParameter.setMaximumLength(any(Integer.class))).thenReturn(repositoryParameter);
        when(repositoryParameter.setRequired(anyBoolean())).thenReturn(repositoryParameter);
        when(newAction.createParam(eq("repository"))).thenReturn(repositoryParameter);

        WebService.NewParam almSettingParameter = mock(WebService.NewParam.class);
        when(almSettingParameter.setMaximumLength(any(Integer.class))).thenReturn(almSettingParameter);
        when(almSettingParameter.setRequired(anyBoolean())).thenReturn(almSettingParameter);
        when(newAction.createParam(eq("almSetting"))).thenReturn(almSettingParameter);

        SetBitbucketBindingAction testCase = new SetBitbucketBindingAction(dbClient, componentFinder, userSession);
        testCase.configureAction(newAction);

        verify(slugParameter).setRequired(eq(true));

        verify(repositoryParameter).setRequired(eq(true));

        verify(almSettingParameter).setRequired(eq(true));
    }

    @Test
    public void testCreateProjectAlmSettingDto() {
        DbClient dbClient = mock(DbClient.class);
        UserSession userSession = mock(UserSession.class);
        ComponentFinder componentFinder = mock(ComponentFinder.class);

        Request request = mock(Request.class);
        when(request.mandatoryParam(eq("slug"))).thenReturn("slug");
        when(request.mandatoryParam(eq("repository"))).thenReturn("repository");

        SetBitbucketBindingAction testCase = new SetBitbucketBindingAction(dbClient, componentFinder, userSession);
        ProjectAlmSettingDto result = testCase.createProjectAlmSettingDto("projectUuid", "settingsUuid", request);

        assertThat(result).isEqualToComparingFieldByField(new ProjectAlmSettingDto().setProjectUuid("projectUuid").setAlmSettingUuid("settingsUuid").setAlmRepo("repository").setAlmSlug("slug"));

    }
}
