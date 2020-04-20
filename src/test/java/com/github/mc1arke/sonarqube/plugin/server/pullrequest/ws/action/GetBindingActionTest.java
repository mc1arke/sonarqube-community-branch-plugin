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
import org.sonar.db.alm.setting.ProjectAlmSettingDao;
import org.sonar.db.alm.setting.ProjectAlmSettingDto;
import org.sonar.db.project.ProjectDto;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.user.UserSession;
import org.sonarqube.ws.AlmSettings;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class GetBindingActionTest {

    @Test
    public void testDefine() {
        DbClient dbClient = mock(DbClient.class);
        ComponentFinder componentFinder = mock(ComponentFinder.class);
        UserSession userSession = mock(UserSession.class);
        GetBindingAction testCase = new GetBindingAction(dbClient, componentFinder, userSession);
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

        verify(newAction).setHandler(eq(testCase));
        verify(newAction).createParam(eq("project"));
        verify(paramMap.get("project")).setRequired(true);
    }

    @Test
    public void testHandle() {
        DbClient dbClient = mock(DbClient.class);
        DbSession dbSession = mock(DbSession.class);
        when(dbClient.openSession(eq(false))).thenReturn(dbSession);
        AlmSettingDao almSettingDao = mock(AlmSettingDao.class);
        AlmSettingDto almSettingDto = mock(AlmSettingDto.class);
        when(almSettingDto.getUuid()).thenReturn("almSettingsUuid");
        when(almSettingDao.selectByUuid(eq(dbSession), eq("almSettingUuid"))).thenReturn(Optional.of(almSettingDto));
        when(almSettingDto.getAlm()).thenReturn(ALM.GITHUB);
        when(almSettingDto.getKey()).thenReturn("key");
        when(almSettingDto.getUrl()).thenReturn("url");
        when(dbClient.almSettingDao()).thenReturn(almSettingDao);
        ProjectDto projectDto = mock(ProjectDto.class);
        ProjectAlmSettingDao projectAlmSettingDao = mock(ProjectAlmSettingDao.class);
        when(dbClient.projectAlmSettingDao()).thenReturn(projectAlmSettingDao);
        ComponentFinder componentFinder = mock(ComponentFinder.class);
        when(projectDto.getKey()).thenReturn("projectUuid");
        when(componentFinder.getProjectByKey(eq(dbSession), eq("project"))).thenReturn(projectDto);
        UserSession userSession = mock(UserSession.class);
        ProjectAlmSettingDto projectAlmSettingDto = mock(ProjectAlmSettingDto.class);
        when(projectAlmSettingDto.getAlmSettingUuid()).thenReturn("almSettingUuid");
        when(projectAlmSettingDto.getAlmRepo()).thenReturn("repository");
        when(projectAlmSettingDto.getAlmSlug()).thenReturn("slug");
        when(projectAlmSettingDao.selectByProject(eq(dbSession), eq(projectDto))).thenReturn(Optional.of(projectAlmSettingDto));
        ProtoBufWriter protoBufWriter = mock(ProtoBufWriter.class);

        GetBindingAction testCase = new GetBindingAction(dbClient, componentFinder, userSession, protoBufWriter);

        Request request = mock(Request.class, Mockito.RETURNS_DEEP_STUBS);
        Response response = mock(Response.class, Mockito.RETURNS_DEEP_STUBS);

        when(request.mandatoryParam("almSetting")).thenReturn("almSetting");
        when(request.param("project")).thenReturn("project");
        when(request.getMediaType()).thenReturn("dummy");

        testCase.handle(request, response);

        ArgumentCaptor<Message> messageArgumentCaptor = ArgumentCaptor.forClass(Message.class);
        verify(protoBufWriter).write(messageArgumentCaptor.capture(), eq(request), eq(response));
        Message message = messageArgumentCaptor.getValue();

        AlmSettings.GetBindingWsResponse expectedResponse = AlmSettings.GetBindingWsResponse.newBuilder()
            .setAlm(AlmSettings.Alm.github)
            .setRepository("repository")
            .setKey("key")
            .setUrl("url")
            .setSlug("slug")
            .build();
        assertThat(message).isInstanceOf(AlmSettings.GetBindingWsResponse.class).isEqualTo(expectedResponse);
    }

    @Test
    public void testHandleMissingProjectParameter() {
        DbClient dbClient = mock(DbClient.class);
        DbSession dbSession = mock(DbSession.class);
        when(dbClient.openSession(eq(false))).thenReturn(dbSession);
        AlmSettingDao almSettingDao = mock(AlmSettingDao.class);
        AlmSettingDto almSettingDto = mock(AlmSettingDto.class);
        when(almSettingDto.getUuid()).thenReturn("almSettingsUuid");
        when(almSettingDao.selectByUuid(eq(dbSession), eq("almSettingUuid"))).thenReturn(Optional.of(almSettingDto));
        when(almSettingDto.getAlm()).thenReturn(ALM.GITHUB);
        when(almSettingDto.getKey()).thenReturn("key");
        when(almSettingDto.getUrl()).thenReturn("url");
        when(dbClient.almSettingDao()).thenReturn(almSettingDao);
        ProjectDto projectDto = mock(ProjectDto.class);
        ProjectAlmSettingDao projectAlmSettingDao = mock(ProjectAlmSettingDao.class);
        when(dbClient.projectAlmSettingDao()).thenReturn(projectAlmSettingDao);
        ComponentFinder componentFinder = mock(ComponentFinder.class);
        when(projectDto.getKey()).thenReturn("projectUuid");
        when(componentFinder.getProjectByKey(eq(dbSession), eq("project"))).thenReturn(projectDto);
        UserSession userSession = mock(UserSession.class);
        ProjectAlmSettingDto projectAlmSettingDto = mock(ProjectAlmSettingDto.class);
        when(projectAlmSettingDto.getAlmSettingUuid()).thenReturn("almSettingUuid");
        when(projectAlmSettingDto.getAlmRepo()).thenReturn("repository");
        when(projectAlmSettingDto.getAlmSlug()).thenReturn("slug");
        when(projectAlmSettingDao.selectByProject(eq(dbSession), eq(projectDto))).thenReturn(Optional.of(projectAlmSettingDto));
        ProtoBufWriter protoBufWriter = mock(ProtoBufWriter.class);

        GetBindingAction testCase = new GetBindingAction(dbClient, componentFinder, userSession, protoBufWriter);

        Request request = mock(Request.class, Mockito.RETURNS_DEEP_STUBS);
        Response response = mock(Response.class, Mockito.RETURNS_DEEP_STUBS);

        when(request.mandatoryParam("almSetting")).thenReturn("almSetting");
        when(request.getMediaType()).thenReturn("dummy");

        assertThatThrownBy(() -> testCase.handle(request, response))
                .isInstanceOf(IllegalArgumentException.class).hasMessage("The 'project' parameter is missing");
    }

    @Test
    public void testHandleUnboundProject() {
        DbClient dbClient = mock(DbClient.class);
        DbSession dbSession = mock(DbSession.class);
        when(dbClient.openSession(eq(false))).thenReturn(dbSession);
        ProjectAlmSettingDao projectAlmSettingDao = mock(ProjectAlmSettingDao.class);

        when(projectAlmSettingDao.selectByProject(eq(dbSession), eq("projectUuid"))).thenReturn(Optional.empty());
        when(dbClient.projectAlmSettingDao()).thenReturn(projectAlmSettingDao);

        ProjectDto projectDto = mock(ProjectDto.class);
        when(projectDto.getKey()).thenReturn("project");

        ComponentFinder componentFinder = mock(ComponentFinder.class);
        when(projectDto.getKey()).thenReturn("project");
        when(componentFinder.getProjectByKey(eq(dbSession), eq("project"))).thenReturn(projectDto);
        UserSession userSession = mock(UserSession.class);

        GetBindingAction testCase = new GetBindingAction(dbClient, componentFinder, userSession);

        Request request = mock(Request.class, Mockito.RETURNS_DEEP_STUBS);
        Response response = mock(Response.class, Mockito.RETURNS_DEEP_STUBS);

        when(request.mandatoryParam("almSetting")).thenReturn("almSetting");
        when(request.param("project")).thenReturn("project");
        when(request.getMediaType()).thenReturn("dummy");

        assertThatThrownBy(() -> testCase.handle(request, response)).isInstanceOf(NotFoundException.class).hasMessage("Project 'project' is not bound to any ALM");
    }

    @Test
    public void testHandleUnknownAlmSetting() {
        DbClient dbClient = mock(DbClient.class);
        DbSession dbSession = mock(DbSession.class);
        when(dbClient.openSession(eq(false))).thenReturn(dbSession);
        ProjectAlmSettingDao projectAlmSettingDao = mock(ProjectAlmSettingDao.class);
        ProjectAlmSettingDto projectAlmSettingDto = mock(ProjectAlmSettingDto.class);
        when(projectAlmSettingDto.getAlmSettingUuid()).thenReturn("settingUuid");

        ProjectDto projectDto = mock(ProjectDto.class);
        when(projectDto.getKey()).thenReturn("project");

        when(projectAlmSettingDao.selectByProject(eq(dbSession), eq(projectDto))).thenReturn(Optional.of(projectAlmSettingDto));
        when(dbClient.projectAlmSettingDao()).thenReturn(projectAlmSettingDao);

        AlmSettingDao almSettingDao = mock(AlmSettingDao.class);
        when(dbClient.almSettingDao()).thenReturn(almSettingDao);
        when(almSettingDao.selectByKey(eq(dbSession), eq("settingUuid"))).thenReturn(Optional.empty());

        ComponentFinder componentFinder = mock(ComponentFinder.class);
        when(projectDto.getKey()).thenReturn("projectUuid");
        when(componentFinder.getProjectByKey(eq(dbSession), eq("project"))).thenReturn(projectDto);
        UserSession userSession = mock(UserSession.class);

        GetBindingAction testCase = new GetBindingAction(dbClient, componentFinder, userSession);

        Request request = mock(Request.class, Mockito.RETURNS_DEEP_STUBS);
        Response response = mock(Response.class, Mockito.RETURNS_DEEP_STUBS);

        when(request.mandatoryParam("almSetting")).thenReturn("almSetting");
        when(request.param("project")).thenReturn("project");
        when(request.getMediaType()).thenReturn("dummy");

        assertThatThrownBy(() -> testCase.handle(request, response)).isInstanceOf(IllegalStateException.class).hasMessage("ALM setting 'settingUuid' cannot be found");

    }
}