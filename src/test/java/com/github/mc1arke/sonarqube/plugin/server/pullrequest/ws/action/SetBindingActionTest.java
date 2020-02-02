package com.github.mc1arke.sonarqube.plugin.server.pullrequest.ws.action;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.junit.Test;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.alm.setting.AlmSettingDao;
import org.sonar.db.alm.setting.AlmSettingDto;
import org.sonar.db.alm.setting.ProjectAlmSettingDao;
import org.sonar.db.alm.setting.ProjectAlmSettingDto;
import org.sonar.db.component.ComponentDto;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.user.UserSession;

public class SetBindingActionTest {

    @Test
    public void testDefine() {
        DbClient dbClient = mock(DbClient.class);
        ComponentFinder componentFinder = mock(ComponentFinder.class);
        UserSession userSession = mock(UserSession.class);
        ProjectAlmSettingDto projectAlmSettingDto = mock(ProjectAlmSettingDto.class);
        SetBindingAction testCase = new SetBindingAction(dbClient, componentFinder, userSession, "dummy") {

            @Override
            protected ProjectAlmSettingDto createProjectAlmSettingDto(String projectUuid, String settingsUuid, Request request) {
                return projectAlmSettingDto;
            }
        };

        Map<String, WebService.NewParam> paramMap = new HashMap<>();

        WebService.NewController newController = mock(WebService.NewController.class);
        WebService.NewAction newAction = mock(WebService.NewAction.class);
        when(newController.createAction(any())).thenReturn(newAction);
        when(newAction.setPost(eq(true))).thenReturn(newAction);
        when(newAction.setHandler(eq(testCase))).thenReturn(newAction);
        when(newAction.createParam(any())).then(i -> {
            WebService.NewParam newParam = mock(WebService.NewParam.class);
            paramMap.put(i.getArgument(0), newParam);
            return newParam;
        });
        testCase.define(newController);

        verify(newAction).createParam(eq("project"));
        verify(newAction).createParam(eq("almSetting"));
        verify(paramMap.get("project")).setRequired(true);
        verify(paramMap.get("almSetting")).setRequired(true);
    }

    @Test
    public void testHandle() {
        DbClient dbClient = mock(DbClient.class);
        DbSession dbSession = mock(DbSession.class);
        when(dbClient.openSession(eq(false))).thenReturn(dbSession);
        AlmSettingDao almSettingDao = mock(AlmSettingDao.class);
        AlmSettingDto almSettingDto = mock(AlmSettingDto.class);
        when(almSettingDto.getUuid()).thenReturn("almSettingsUuid");
        when(almSettingDao.selectByKey(eq(dbSession), eq("almSetting"))).thenReturn(Optional.of(almSettingDto));
        when(dbClient.almSettingDao()).thenReturn(almSettingDao);
        ProjectAlmSettingDao projectAlmSettingDao = mock(ProjectAlmSettingDao.class);
        when(dbClient.projectAlmSettingDao()).thenReturn(projectAlmSettingDao);
        ComponentFinder componentFinder = mock(ComponentFinder.class);
        ComponentDto componentDto = mock(ComponentDto.class);
        when(componentDto.uuid()).thenReturn("projectUuid");
        when(componentFinder.getByKey(eq(dbSession), eq("project"))).thenReturn(componentDto);
        UserSession userSession = mock(UserSession.class);
        ThreadLocal<WebService.NewAction> capturedAction = new ThreadLocal<>();
        ProjectAlmSettingDto projectAlmSettingDto = mock(ProjectAlmSettingDto.class);
        SetBindingAction testCase = new SetBindingAction(dbClient, componentFinder, userSession, "dummy") {

            @Override
            protected void configureAction(WebService.NewAction action) {
                capturedAction.set(action);
            }

            @Override
            protected ProjectAlmSettingDto createProjectAlmSettingDto(String projectUuid, String settingsUuid, Request request) {
                assertThat(projectUuid).isEqualTo("projectUuid");
                assertThat(settingsUuid).isEqualTo("almSettingsUuid");
                return projectAlmSettingDto;
            }
        };

        Request request = mock(Request.class);
        Response response = mock(Response.class);

        when(request.mandatoryParam("almSetting")).thenReturn("almSetting");
        when(request.mandatoryParam("project")).thenReturn("project");

        testCase.handle(request, response);

        verify(projectAlmSettingDao).insertOrUpdate(eq(dbSession), eq(projectAlmSettingDto));
        verify(dbSession).commit();
        verify(response).noContent();
    }
}