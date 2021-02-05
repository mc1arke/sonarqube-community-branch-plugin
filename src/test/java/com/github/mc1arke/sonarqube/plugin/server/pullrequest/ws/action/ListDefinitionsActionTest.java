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
import org.sonar.server.user.UserSession;
import org.sonarqube.ws.AlmSettings;

import java.util.Arrays;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ListDefinitionsActionTest {

    @Test
    public void testDefine() {
        DbClient dbClient = mock(DbClient.class);
        UserSession userSession = mock(UserSession.class);
        ListDefinitionsAction testCase = new ListDefinitionsAction(dbClient, userSession);

        WebService.NewController newController = mock(WebService.NewController.class);
        WebService.NewAction newAction = mock(WebService.NewAction.class);
        when(newController.createAction(eq("list_definitions"))).thenReturn(newAction);
        when(newAction.setHandler(eq(testCase))).thenReturn(newAction);

        testCase.define(newController);

        verify(newAction).setHandler(eq(testCase));
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
        when(githubAlmSettingDto.getClientId()).thenReturn("githubClientId");
        when(githubAlmSettingDto.getClientSecret()).thenReturn("githubClientSecret");
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
        when(azureDevOpsAlmSettingDto.getUrl()).thenReturn("azureDevopsUrl");
        when(azureDevOpsAlmSettingDto.getPersonalAccessToken()).thenReturn("azureDevOpsPersonalAccessToken");

        when(almSettingDao.selectAll(eq(dbSession))).thenReturn(Arrays.asList(
            githubAlmSettingDto, gitlabAlmSettingDto, bitbucketAlmSettingDto, azureDevOpsAlmSettingDto
        ));
        when(dbClient.almSettingDao()).thenReturn(almSettingDao);

        UserSession userSession = mock(UserSession.class);

        ProtoBufWriter protoBufWriter = mock(ProtoBufWriter.class);

        ListDefinitionsAction testCase = new ListDefinitionsAction(dbClient, userSession, protoBufWriter);

        Request request = mock(Request.class, Mockito.RETURNS_DEEP_STUBS);
        Response response = mock(Response.class, Mockito.RETURNS_DEEP_STUBS);

        testCase.handle(request, response);

        ArgumentCaptor<Message> messageArgumentCaptor = ArgumentCaptor.forClass(Message.class);
        verify(protoBufWriter).write(messageArgumentCaptor.capture(), eq(request), eq(response));
        Message message = messageArgumentCaptor.getValue();

        AlmSettings.ListDefinitionsWsResponse expectedResponse = AlmSettings.ListDefinitionsWsResponse.newBuilder()
            .addGithub(AlmSettings.AlmSettingGithub.newBuilder()
                .setKey("githubKey")
                .setUrl("githubUrl")
                .setAppId("githubAppId")
                .setPrivateKey("githubPrivateKey")
                .setClientId("githubClientId")
                .setClientSecret("githubClientSecret")
                .build())
            .addAzure(AlmSettings.AlmSettingAzure.newBuilder()
                .setKey("azureDevopsKey")
                .setPersonalAccessToken("azureDevOpsPersonalAccessToken")
                .setUrl("azureDevopsUrl")
                .build())
            .addBitbucket(AlmSettings.AlmSettingBitbucket.newBuilder()
                .setKey("bitbucketKey")
                .setPersonalAccessToken("bitbucketPersonalAccessToken")
                .setUrl("bitbucketUrl")
                .build())
            .addGitlab(AlmSettings.AlmSettingGitlab.newBuilder()
                .setKey("gitlabKey")
                .setPersonalAccessToken("gitlabPersonalAccessToken")
                .build())
            .build();

        assertThat(message).isInstanceOf(AlmSettings.ListDefinitionsWsResponse.class).isEqualTo(expectedResponse);
    }

    @Test
    public void testHandleWithGitlabUrl() {
        DbClient dbClient = mock(DbClient.class);
        DbSession dbSession = mock(DbSession.class);
        when(dbClient.openSession(eq(false))).thenReturn(dbSession);
        AlmSettingDao almSettingDao = mock(AlmSettingDao.class);

        AlmSettingDto gitlabAlmSettingDto = mock(AlmSettingDto.class);
        when(gitlabAlmSettingDto.getAlm()).thenReturn(ALM.GITLAB);
        when(gitlabAlmSettingDto.getKey()).thenReturn("gitlabKey");
        when(gitlabAlmSettingDto.getUrl()).thenReturn("url");
        when(gitlabAlmSettingDto.getPersonalAccessToken()).thenReturn("gitlabPersonalAccessToken");
        when(gitlabAlmSettingDto.getUrl()).thenReturn("url");

        when(almSettingDao.selectAll(eq(dbSession))).thenReturn(Collections.singletonList(gitlabAlmSettingDto));
        when(dbClient.almSettingDao()).thenReturn(almSettingDao);

        UserSession userSession = mock(UserSession.class);

        ProtoBufWriter protoBufWriter = mock(ProtoBufWriter.class);

        ListDefinitionsAction testCase = new ListDefinitionsAction(dbClient, userSession, protoBufWriter);

        Request request = mock(Request.class, Mockito.RETURNS_DEEP_STUBS);
        Response response = mock(Response.class, Mockito.RETURNS_DEEP_STUBS);

        testCase.handle(request, response);

        ArgumentCaptor<Message> messageArgumentCaptor = ArgumentCaptor.forClass(Message.class);
        verify(protoBufWriter).write(messageArgumentCaptor.capture(), eq(request), eq(response));
        Message message = messageArgumentCaptor.getValue();

        AlmSettings.ListDefinitionsWsResponse expectedResponse = AlmSettings.ListDefinitionsWsResponse.newBuilder()
           .addGitlab(AlmSettings.AlmSettingGitlab.newBuilder()
                .setKey("gitlabKey")
                .setUrl("url")
                .setPersonalAccessToken("gitlabPersonalAccessToken")
                .build())
            .build();

        assertThat(message).isInstanceOf(AlmSettings.ListDefinitionsWsResponse.class).isEqualTo(expectedResponse);
    }

}