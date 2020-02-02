package com.github.mc1arke.sonarqube.plugin.server.pullrequest.ws.action;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Test;
import org.mockito.Mockito;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.alm.setting.ALM;
import org.sonar.db.alm.setting.AlmSettingDao;
import org.sonar.db.alm.setting.AlmSettingDto;
import org.sonar.server.user.UserSession;

public class UpdateActionTest {

    @Test
    public void testDefine() {
        DbClient dbClient = mock(DbClient.class);
        UserSession userSession = mock(UserSession.class);
        AtomicReference<WebService.NewAction> atomicReference = new AtomicReference<>();
        UpdateAction testCase = new UpdateAction(dbClient, userSession, "dummy") {

            @Override
            protected void configureAction(WebService.NewAction action) {
                atomicReference.set(action);
            }

            @Override
            protected AlmSettingDto updateAlmSettingsDto(AlmSettingDto almSettingDto, Request request) {
                return null;
            }
        };

        WebService.NewParam keyParam = mock(WebService.NewParam.class);
        when(keyParam.setMaximumLength(eq(200))).thenReturn(keyParam);
        WebService.NewParam newKeyParam = mock(WebService.NewParam.class);
        when(newKeyParam.setMaximumLength(eq(200))).thenReturn(newKeyParam);
        WebService.NewController newController = mock(WebService.NewController.class);
        WebService.NewAction newAction = mock(WebService.NewAction.class);
        when(newController.createAction(eq("dummy"))).thenReturn(newAction);
        when(newAction.setPost(eq(true))).thenReturn(newAction);
        when(newAction.setHandler(eq(testCase))).thenReturn(newAction);
        when(newAction.createParam(eq("key"))).thenReturn(keyParam);
        when(newAction.createParam(eq("newKey"))).thenReturn(newKeyParam);

        testCase.define(newController);

        verify(newAction).setHandler(eq(testCase));
        verify(keyParam).setRequired(true);

        assertThat(atomicReference.get()).isSameAs(newAction);
    }

    @Test
    public void testHandle() {
        DbClient dbClient = mock(DbClient.class);
        DbSession dbSession = mock(DbSession.class);
        when(dbClient.openSession(eq(false))).thenReturn(dbSession);
        AlmSettingDao almSettingDao = mock(AlmSettingDao.class);

        AlmSettingDto githubAlmSettingDto = mock(AlmSettingDto.class);
        when(githubAlmSettingDto.getAlm()).thenReturn(ALM.GITHUB);
        when(githubAlmSettingDto.getKey()).thenReturn("githubKey");
        when(githubAlmSettingDto.getUrl()).thenReturn("githubUrl");
        when(githubAlmSettingDto.getAppId()).thenReturn("githubAppId");
        when(githubAlmSettingDto.getPrivateKey()).thenReturn("githubPrivateKey");
        when(githubAlmSettingDto.setKey(eq("projectKey"))).thenReturn(githubAlmSettingDto);

        when(almSettingDao.selectByKey(eq(dbSession), eq("projectKey"))).thenReturn(Optional.of(githubAlmSettingDto));
        when(dbClient.almSettingDao()).thenReturn(almSettingDao);

        UserSession userSession = mock(UserSession.class);

        UpdateAction testCase = new UpdateAction(dbClient, userSession, "dummy") {

            @Override
            protected void configureAction(WebService.NewAction action) {

            }

            @Override
            protected AlmSettingDto updateAlmSettingsDto(AlmSettingDto almSettingDto, Request request) {
                return almSettingDto;
            }
        };

        Request request = mock(Request.class, Mockito.RETURNS_DEEP_STUBS);
        when(request.mandatoryParam("key")).thenReturn("projectKey");
        Response response = mock(Response.class, Mockito.RETURNS_DEEP_STUBS);

        testCase.handle(request, response);

        verify(dbSession).commit();
        verify(almSettingDao).update(eq(dbSession), eq(githubAlmSettingDto));
        verify(response).noContent();

    }

    @Test
    public void testHandleNewKey() {
        DbClient dbClient = mock(DbClient.class);
        DbSession dbSession = mock(DbSession.class);
        when(dbClient.openSession(eq(false))).thenReturn(dbSession);
        AlmSettingDao almSettingDao = mock(AlmSettingDao.class);

        AlmSettingDto githubAlmSettingDto = mock(AlmSettingDto.class);
        when(githubAlmSettingDto.getAlm()).thenReturn(ALM.GITHUB);
        when(githubAlmSettingDto.getKey()).thenReturn("githubKey");
        when(githubAlmSettingDto.getUrl()).thenReturn("githubUrl");
        when(githubAlmSettingDto.getAppId()).thenReturn("githubAppId");
        when(githubAlmSettingDto.getPrivateKey()).thenReturn("githubPrivateKey");
        when(githubAlmSettingDto.setKey(eq("projectKey2"))).thenReturn(githubAlmSettingDto);

        when(almSettingDao.selectByKey(eq(dbSession), eq("projectKey"))).thenReturn(Optional.of(githubAlmSettingDto));
        when(dbClient.almSettingDao()).thenReturn(almSettingDao);

        UserSession userSession = mock(UserSession.class);

        UpdateAction testCase = new UpdateAction(dbClient, userSession, "dummy") {

            @Override
            protected void configureAction(WebService.NewAction action) {

            }

            @Override
            protected AlmSettingDto updateAlmSettingsDto(AlmSettingDto almSettingDto, Request request) {
                return almSettingDto;
            }
        };

        Request request = mock(Request.class, Mockito.RETURNS_DEEP_STUBS);
        when(request.mandatoryParam("key")).thenReturn("projectKey");
        when(request.param("newKey")).thenReturn("projectKey2");
        Response response = mock(Response.class, Mockito.RETURNS_DEEP_STUBS);

        testCase.handle(request, response);

        verify(dbSession).commit();
        verify(almSettingDao).update(eq(dbSession), eq(githubAlmSettingDto));
        verify(response).noContent();

    }

    @Test
    public void testHandleNewKeyAlreadyExists() {
        DbClient dbClient = mock(DbClient.class);
        DbSession dbSession = mock(DbSession.class);
        when(dbClient.openSession(eq(false))).thenReturn(dbSession);
        AlmSettingDao almSettingDao = mock(AlmSettingDao.class);

        AlmSettingDto githubAlmSettingDto = mock(AlmSettingDto.class);
        when(githubAlmSettingDto.getAlm()).thenReturn(ALM.GITHUB);
        when(githubAlmSettingDto.getKey()).thenReturn("githubKey");
        when(githubAlmSettingDto.getUrl()).thenReturn("githubUrl");
        when(githubAlmSettingDto.getAppId()).thenReturn("githubAppId");
        when(githubAlmSettingDto.getPrivateKey()).thenReturn("githubPrivateKey");
        when(githubAlmSettingDto.setKey(eq("projectKey2"))).thenReturn(githubAlmSettingDto);

        AlmSettingDto almSettingDto2 = mock(AlmSettingDto.class);
        when(almSettingDto2.getKey()).thenReturn("projectKey2");

        when(almSettingDao.selectByKey(eq(dbSession), eq("projectKey"))).thenReturn(Optional.of(githubAlmSettingDto));
        when(almSettingDao.selectByKey(eq(dbSession), eq("projectKey2"))).thenReturn(Optional.of(almSettingDto2));
        when(dbClient.almSettingDao()).thenReturn(almSettingDao);

        UserSession userSession = mock(UserSession.class);

        UpdateAction testCase = new UpdateAction(dbClient, userSession, "dummy") {

            @Override
            protected void configureAction(WebService.NewAction action) {

            }

            @Override
            protected AlmSettingDto updateAlmSettingsDto(AlmSettingDto almSettingDto, Request request) {
                return almSettingDto;
            }
        };

        Request request = mock(Request.class, Mockito.RETURNS_DEEP_STUBS);
        when(request.mandatoryParam("key")).thenReturn("projectKey");
        when(request.param("newKey")).thenReturn("projectKey2");
        Response response = mock(Response.class, Mockito.RETURNS_DEEP_STUBS);

        assertThatThrownBy(() -> testCase.handle(request, response)).isInstanceOf(IllegalArgumentException.class).hasMessage("ALM setting 'projectKey2' already exists");

        verify(dbSession, never()).commit();
        verify(almSettingDao, never()).update(eq(dbSession), eq(githubAlmSettingDto));
        verify(response, never()).noContent();

    }

}