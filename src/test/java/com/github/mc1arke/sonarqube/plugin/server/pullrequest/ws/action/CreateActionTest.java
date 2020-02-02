package com.github.mc1arke.sonarqube.plugin.server.pullrequest.ws.action;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Test;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.alm.setting.AlmSettingDao;
import org.sonar.db.alm.setting.AlmSettingDto;
import org.sonar.server.user.UserSession;

public class CreateActionTest {

    @Test
    public void testDefine() {
        DbClient dbClient = mock(DbClient.class);
        UserSession userSession = mock(UserSession.class);
        AtomicReference<WebService.NewAction> capturedAction = new AtomicReference<>();
        AlmSettingDto almSettingDto = mock(AlmSettingDto.class);
        CreateAction testCase = new CreateAction(dbClient, userSession, "dummy") {

            @Override
            protected void configureAction(WebService.NewAction action) {
                capturedAction.set(action);
            }

            @Override
            protected AlmSettingDto createAlmSettingDto(String key, Request request) {
                return almSettingDto;
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
            when(newParam.setMaximumLength(any(Integer.class))).thenReturn(newParam);
            paramMap.put(i.getArgument(0), newParam);
            return newParam;
        });
        testCase.define(newController);

        assertThat(capturedAction.get()).isEqualTo(newAction);
        verify(newAction).createParam(eq("key"));
        verify(paramMap.get("key")).setRequired(true);
        verify(paramMap.get("key")).setMaximumLength(200);
    }

    @Test
    public void testHandle() {
        DbClient dbClient = mock(DbClient.class);
        DbSession dbSession = mock(DbSession.class);
        when(dbClient.openSession(eq(false))).thenReturn(dbSession);

        AlmSettingDao almSettingDao = mock(AlmSettingDao.class);
        when(dbClient.almSettingDao()).thenReturn(almSettingDao);

        AlmSettingDto almSettingDto = mock(AlmSettingDto.class);

        UserSession userSession = mock(UserSession.class);
        ThreadLocal<WebService.NewAction> capturedAction = new ThreadLocal<>();
        CreateAction testCase = new CreateAction(dbClient, userSession, "dummy") {

            @Override
            protected void configureAction(WebService.NewAction action) {
                capturedAction.set(action);
            }

            @Override
            protected AlmSettingDto createAlmSettingDto(String key, Request request) {
                assertThat(key).isEqualTo("key");
                return almSettingDto;
            }
        };

        Request request = mock(Request.class);
        Response response = mock(Response.class);

        when(request.mandatoryParam("key")).thenReturn("key");

        testCase.handle(request, response);

        verify(almSettingDao).selectByKey(eq(dbSession), eq("key"));
        verify(almSettingDao).insert(eq(dbSession), eq(almSettingDto));
        verify(dbSession).commit();
        verify(response).noContent();
    }
}