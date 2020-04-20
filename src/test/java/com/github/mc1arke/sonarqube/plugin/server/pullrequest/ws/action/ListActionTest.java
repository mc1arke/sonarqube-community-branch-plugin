package com.github.mc1arke.sonarqube.plugin.server.pullrequest.ws.action;

import com.google.protobuf.Message;
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
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.user.UserSession;
import org.sonarqube.ws.AlmSettings;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ListActionTest {

    @Test
    public void testDefine() {
        DbClient dbClient = mock(DbClient.class);
        UserSession userSession = mock(UserSession.class);
        ComponentFinder componentFinder = mock(ComponentFinder.class);
        ListAction testCase = new ListAction(dbClient, userSession, componentFinder);

        WebService.NewParam param = mock(WebService.NewParam.class);
        WebService.NewController newController = mock(WebService.NewController.class);
        WebService.NewAction newAction = mock(WebService.NewAction.class);
        when(newController.createAction(eq("list"))).thenReturn(newAction);
        when(newAction.setHandler(eq(testCase))).thenReturn(newAction);
        when(newAction.createParam(eq("project"))).thenReturn(param);

        testCase.define(newController);

        verify(newAction).setHandler(eq(testCase));
        verify(param).setRequired(false);
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

        AlmSettingDto gitlabAlmSettingDto = mock(AlmSettingDto.class);
        when(gitlabAlmSettingDto.getAlm()).thenReturn(ALM.GITLAB);
        when(gitlabAlmSettingDto.getKey()).thenReturn("gitlabKey");
        when(gitlabAlmSettingDto.getPersonalAccessToken()).thenReturn("gitlabPersonalAccessToken");

        AlmSettingDto bitbucketAlmSettingDto = mock(AlmSettingDto.class);
        when(bitbucketAlmSettingDto.getAlm()).thenReturn(ALM.BITBUCKET);
        when(bitbucketAlmSettingDto.getKey()).thenReturn("bitbucketKey");
        when(bitbucketAlmSettingDto.getUrl()).thenReturn("bitbucketUrl");
        when(bitbucketAlmSettingDto.getPersonalAccessToken()).thenReturn("bitbucketPersonalAccessToken");

        AlmSettingDto azureDevOpsAlmSettingDto = mock(AlmSettingDto.class);
        when(azureDevOpsAlmSettingDto.getAlm()).thenReturn(ALM.AZURE_DEVOPS);
        when(azureDevOpsAlmSettingDto.getKey()).thenReturn("azureDevopsKey");
        when(azureDevOpsAlmSettingDto.getPersonalAccessToken()).thenReturn("azureDevOpsPersonalAccessToken");

        when(almSettingDao.selectAll(eq(dbSession))).thenReturn(Arrays.asList(
            githubAlmSettingDto, gitlabAlmSettingDto, bitbucketAlmSettingDto, azureDevOpsAlmSettingDto
        ));
        when(dbClient.almSettingDao()).thenReturn(almSettingDao);

        UserSession userSession = mock(UserSession.class);

        ProtoBufWriter protoBufWriter = mock(ProtoBufWriter.class);
        ComponentFinder componentFinder = mock(ComponentFinder.class);

        ListAction testCase = new ListAction(dbClient, userSession, componentFinder, protoBufWriter);

        Request request = mock(Request.class, Mockito.RETURNS_DEEP_STUBS);
        Response response = mock(Response.class, Mockito.RETURNS_DEEP_STUBS);

        testCase.handle(request, response);

        ArgumentCaptor<Message> messageArgumentCaptor = ArgumentCaptor.forClass(Message.class);
        verify(protoBufWriter).write(messageArgumentCaptor.capture(), eq(request), eq(response));
        Message message = messageArgumentCaptor.getValue();

        AlmSettings.ListWsResponse expectedResponse = AlmSettings.ListWsResponse.newBuilder().addAlmSettings(AlmSettings.AlmSetting.newBuilder()
                .setAlm(AlmSettings.Alm.github)
                .setKey("githubKey")
                .setUrl("githubUrl")
                .build())
            .addAlmSettings(AlmSettings.AlmSetting.newBuilder()
                .setAlm(AlmSettings.Alm.gitlab)
                .setKey("gitlabKey")
                .build())
            .addAlmSettings(AlmSettings.AlmSetting.newBuilder()
                .setAlm(AlmSettings.Alm.bitbucket)
                .setKey("bitbucketKey")
                .setUrl("bitbucketUrl")
                .build())
            .addAlmSettings(AlmSettings.AlmSetting.newBuilder()
                .setAlm(AlmSettings.Alm.azure)
                .setKey("azureDevopsKey")
                .build())
            .build();

        assertThat(message).isInstanceOf(AlmSettings.ListWsResponse.class).isEqualTo(expectedResponse);
    }

}