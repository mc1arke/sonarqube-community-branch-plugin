package com.github.mc1arke.sonarqube.plugin.server.pullrequest.ws.pr;

import org.junit.Test;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.web.UserRole;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.BranchDao;
import org.sonar.db.component.BranchDto;
import org.sonar.db.project.ProjectDto;
import org.sonar.server.component.ComponentCleanerService;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.user.UserSession;

import java.util.Optional;

import static com.github.mc1arke.sonarqube.plugin.server.pullrequest.ws.pr.PullRequestsWsParameters.PARAM_PULL_REQUEST;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.when;
import static org.sonar.db.component.BranchType.PULL_REQUEST;
import static org.sonar.server.branch.ws.ProjectBranchesParameters.ACTION_DELETE;
import static org.sonar.server.branch.ws.ProjectBranchesParameters.PARAM_PROJECT;

public class DeleteActionTest {

    @Test
    public void testDefine() {
        DbClient dbClient = mock(DbClient.class);
        ComponentFinder componentFinder = mock(ComponentFinder.class);
        UserSession userSession = mock(UserSession.class);
        ComponentCleanerService componentCleanerService = mock(ComponentCleanerService.class);

        DeleteAction testCase = new DeleteAction(dbClient, componentFinder, userSession, componentCleanerService);

        WebService.NewParam keyParam = mock(WebService.NewParam.class);
        when(keyParam.setDescription(anyString())).thenReturn(keyParam);
        when(keyParam.setExampleValue(any())).thenReturn(keyParam);
        when(keyParam.setRequired(eq(true))).thenReturn(keyParam);

        WebService.NewController newController = mock(WebService.NewController.class);
        WebService.NewAction newAction = mock(WebService.NewAction.class);

        when(newController.createAction(eq(ACTION_DELETE))).thenReturn(newAction);

        when(newAction.setSince(anyString())).thenReturn(newAction);
        when(newAction.setDescription(anyString())).thenReturn(newAction);
        when(newAction.setPost(eq(true))).thenReturn(newAction);
        when(newAction.setHandler(eq(testCase))).thenReturn(newAction);
        when(newAction.createParam(anyString())).thenReturn(keyParam);

        testCase.define(newController);

        verify(newAction).setHandler(eq(testCase));
        verify(keyParam, times(2)).setRequired(true);
    }

    @Test
    public void testHandle() throws Exception {
        DbClient dbClient = mock(DbClient.class);
        ComponentFinder componentFinder = mock(ComponentFinder.class);
        UserSession userSession = mock(UserSession.class);
        ComponentCleanerService componentCleanerService = mock(ComponentCleanerService.class);

        DeleteAction testCase = new DeleteAction(dbClient, componentFinder, userSession, componentCleanerService);

        Request request = mock(Request.class);
        Response response = mock(Response.class);
        DbSession dbSession = mock(DbSession.class);
        ProjectDto projectDto = mock(ProjectDto.class);
        BranchDao branchDao = mock(BranchDao.class);
        BranchDto branchDto = mock(BranchDto.class);

        when(userSession.checkLoggedIn()).thenReturn(null);
        when(request.mandatoryParam(eq(PARAM_PROJECT))).thenReturn("fake");
        when(request.mandatoryParam(eq(PARAM_PULL_REQUEST))).thenReturn("fake");

        when(dbClient.openSession(eq(false))).thenReturn(dbSession);

        when(componentFinder.getProjectOrApplicationByKey(eq(dbSession), eq("fake"))).thenReturn(projectDto);
        when(userSession.checkProjectPermission(eq(UserRole.ADMIN), eq(projectDto))).thenReturn(null);
        when(projectDto.getUuid()).thenReturn("fake");
        when(branchDto.getBranchType()).thenReturn(PULL_REQUEST);
        when(branchDao.selectByPullRequestKey(eq(dbSession), eq("fake"), eq("fake"))).thenReturn(Optional.of(branchDto));
        when(dbClient.branchDao()).thenReturn(branchDao);

        // verify no exception is thrown
        testCase.handle(request, response);
    }

}
