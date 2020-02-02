package com.github.mc1arke.sonarqube.plugin.server.pullrequest.ws.action;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.mockito.Mockito;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.alm.setting.AlmSettingDao;
import org.sonar.db.alm.setting.ProjectAlmSettingDao;
import org.sonar.db.component.ComponentDto;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.user.UserSession;

public class DeleteBindingActionTest {

    @Test
    public void testDefine() {
        DbClient dbClient = mock(DbClient.class);
        UserSession userSession = mock(UserSession.class);
        ComponentFinder componentFinder = mock(ComponentFinder.class);
        DeleteBindingAction testCase = new DeleteBindingAction(dbClient, userSession, componentFinder);

        WebService.NewParam keyParam = mock(WebService.NewParam.class);
        when(keyParam.setMaximumLength(eq(200))).thenReturn(keyParam);
        WebService.NewParam newKeyParam = mock(WebService.NewParam.class);
        when(newKeyParam.setMaximumLength(eq(200))).thenReturn(newKeyParam);
        WebService.NewController newController = mock(WebService.NewController.class);
        WebService.NewAction newAction = mock(WebService.NewAction.class);
        when(newController.createAction(eq("delete_binding"))).thenReturn(newAction);
        when(newAction.setPost(eq(true))).thenReturn(newAction);
        when(newAction.setHandler(eq(testCase))).thenReturn(newAction);
        when(newAction.createParam(eq("project"))).thenReturn(keyParam);

        testCase.define(newController);

        verify(newAction).setHandler(eq(testCase));
        verify(keyParam).setRequired(true);

    }

    @Test
    public void testHandle() {
        DbClient dbClient = mock(DbClient.class);
        DbSession dbSession = mock(DbSession.class);
        when(dbClient.openSession(eq(false))).thenReturn(dbSession);
        AlmSettingDao almSettingDao = mock(AlmSettingDao.class);
        when(dbClient.almSettingDao()).thenReturn(almSettingDao);

        ProjectAlmSettingDao projectAlmSettingDao = mock(ProjectAlmSettingDao.class);
        when(dbClient.projectAlmSettingDao()).thenReturn(projectAlmSettingDao);

        UserSession userSession = mock(UserSession.class);

        ComponentDto componentDto = mock(ComponentDto.class);
        ComponentFinder componentFinder = mock(ComponentFinder.class);
        when(componentFinder.getByKey(eq(dbSession), eq("projectKey"))).thenReturn(componentDto);

        DeleteBindingAction testCase = new DeleteBindingAction(dbClient, userSession, componentFinder);

        Request request = mock(Request.class, Mockito.RETURNS_DEEP_STUBS);
        when(request.mandatoryParam("project")).thenReturn("projectKey");
        Response response = mock(Response.class, Mockito.RETURNS_DEEP_STUBS);

        testCase.handle(request, response);

        verify(dbSession).commit();
        verify(projectAlmSettingDao).deleteByProject(eq(dbSession), eq(componentDto));
        verify(response).noContent();
        verify(userSession).checkComponentPermission("admin", componentDto);

    }


}