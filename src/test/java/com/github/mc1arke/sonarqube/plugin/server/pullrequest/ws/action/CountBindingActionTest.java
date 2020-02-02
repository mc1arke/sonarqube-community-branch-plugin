package com.github.mc1arke.sonarqube.plugin.server.pullrequest.ws.action;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.alm.setting.ALM;
import org.sonar.db.alm.setting.AlmSettingDao;
import org.sonar.db.alm.setting.AlmSettingDto;
import org.sonar.db.alm.setting.ProjectAlmSettingDao;
import org.sonar.server.user.UserSession;
import org.sonarqube.ws.AlmSettings;

import com.google.protobuf.Message;

public class CountBindingActionTest {

    @Test
    public void testDefine() {
        DbClient dbClient = mock(DbClient.class);
        UserSession userSession = mock(UserSession.class);
        CountBindingAction testCase = new CountBindingAction(dbClient, userSession);

        WebService.NewParam param = mock(WebService.NewParam.class);
        WebService.NewController newController = mock(WebService.NewController.class);
        WebService.NewAction newAction = mock(WebService.NewAction.class);
        when(newController.createAction(eq("count_binding"))).thenReturn(newAction);
        when(newAction.setHandler(eq(testCase))).thenReturn(newAction);
        when(newAction.createParam(eq("almSetting"))).thenReturn(param);

        testCase.define(newController);

        verify(newAction).setHandler(eq(testCase));
        verify(param).setRequired(true);
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

        when(almSettingDao.selectByKey(eq(dbSession), eq("almSetting"))).thenReturn(Optional.of(githubAlmSettingDto));
        when(dbClient.almSettingDao()).thenReturn(almSettingDao);

        ProjectAlmSettingDao projectAlmSettingDao = mock(ProjectAlmSettingDao.class);
        when(projectAlmSettingDao.countByAlmSetting(eq(dbSession), eq(githubAlmSettingDto))).thenReturn(1234);
        UserSession userSession = mock(UserSession.class);
        when(dbClient.projectAlmSettingDao()).thenReturn(projectAlmSettingDao);

        ProtoBufWriter protoBufWriter = mock(ProtoBufWriter.class);

        CountBindingAction testCase = new CountBindingAction(dbClient, userSession, protoBufWriter);

        Request request = mock(Request.class, Mockito.RETURNS_DEEP_STUBS);
        Response response = mock(Response.class, Mockito.RETURNS_DEEP_STUBS);

        when(request.mandatoryParam("almSetting")).thenReturn("almSetting");

        testCase.handle(request, response);

        ArgumentCaptor<Message> messageArgumentCaptor = ArgumentCaptor.forClass(Message.class);
        verify(protoBufWriter).write(messageArgumentCaptor.capture(), eq(request), eq(response));
        Message message = messageArgumentCaptor.getValue();

        AlmSettings.CountBindingWsResponse expectedResponse = AlmSettings.CountBindingWsResponse.newBuilder()
            .setKey("githubKey")
            .setProjects(1234)
            .build();

        assertThat(message).isInstanceOf(AlmSettings.CountBindingWsResponse.class).isEqualTo(expectedResponse);
    }

}