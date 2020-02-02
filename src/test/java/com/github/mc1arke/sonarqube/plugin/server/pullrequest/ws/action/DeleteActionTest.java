package com.github.mc1arke.sonarqube.plugin.server.pullrequest.ws.action;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.Test;
import org.mockito.Mockito;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.alm.setting.AlmSettingDao;
import org.sonar.db.alm.setting.AlmSettingDto;
import org.sonar.db.alm.setting.ProjectAlmSettingDao;
import org.sonar.server.user.UserSession;

public class DeleteActionTest {

    @Test
    public void testDefine() {
        DbClient dbClient = mock(DbClient.class);
        UserSession userSession = mock(UserSession.class);
        DeleteAction testCase = new DeleteAction(dbClient, userSession);

        WebService.NewParam keyParam = mock(WebService.NewParam.class);
        when(keyParam.setMaximumLength(eq(200))).thenReturn(keyParam);
        WebService.NewParam newKeyParam = mock(WebService.NewParam.class);
        when(newKeyParam.setMaximumLength(eq(200))).thenReturn(newKeyParam);
        WebService.NewController newController = mock(WebService.NewController.class);
        WebService.NewAction newAction = mock(WebService.NewAction.class);
        when(newController.createAction(eq("delete"))).thenReturn(newAction);
        when(newAction.setPost(eq(true))).thenReturn(newAction);
        when(newAction.setHandler(eq(testCase))).thenReturn(newAction);
        when(newAction.createParam(eq("key"))).thenReturn(keyParam);

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

        AlmSettingDto almSettingDto = mock(AlmSettingDto.class);
        when(almSettingDao.selectByKey(eq(dbSession), eq("projectKey"))).thenReturn(Optional.of(almSettingDto));

        ProjectAlmSettingDao projectAlmSettingDao = mock(ProjectAlmSettingDao.class);
        when(dbClient.projectAlmSettingDao()).thenReturn(projectAlmSettingDao);

        UserSession userSession = mock(UserSession.class);

        DeleteAction testCase = new DeleteAction(dbClient, userSession);

        Request request = mock(Request.class, Mockito.RETURNS_DEEP_STUBS);
        when(request.mandatoryParam("key")).thenReturn("projectKey");
        Response response = mock(Response.class, Mockito.RETURNS_DEEP_STUBS);

        testCase.handle(request, response);

        verify(dbSession).commit();
        verify(almSettingDao).delete(eq(dbSession), eq(almSettingDto));
        verify(projectAlmSettingDao).deleteByAlmSetting(eq(dbSession), eq(almSettingDto));
        verify(response).noContent();

    }


}