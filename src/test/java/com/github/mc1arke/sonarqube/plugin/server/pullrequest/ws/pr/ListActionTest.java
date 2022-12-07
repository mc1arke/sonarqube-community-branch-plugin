package com.github.mc1arke.sonarqube.plugin.server.pullrequest.ws.pr;

import org.junit.Test;
import org.sonar.api.server.ws.Change;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.web.UserRole;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.BranchDao;
import org.sonar.db.component.BranchDto;
import org.sonar.db.component.SnapshotDao;
import org.sonar.db.component.SnapshotDto;
import org.sonar.db.measure.LiveMeasureDao;
import org.sonar.db.measure.LiveMeasureDto;
import org.sonar.db.permission.GlobalPermission;
import org.sonar.db.project.ProjectDto;
import org.sonar.db.protobuf.DbProjectBranches;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.issue.index.IssueIndex;
import org.sonar.server.issue.index.PrStatistics;
import org.sonar.server.user.UserSession;

import java.io.OutputStream;
import java.util.Arrays;
import java.util.Date;

import static java.util.Collections.singletonList;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.sonar.api.measures.CoreMetrics.ALERT_STATUS_KEY;
import static org.sonar.db.component.BranchType.PULL_REQUEST;
import static org.sonar.server.branch.ws.ProjectBranchesParameters.PARAM_PROJECT;

public class ListActionTest {

    @Test
    public void testDefine() {
        DbClient dbClient = mock(DbClient.class);
        UserSession userSession = mock(UserSession.class);
        ComponentFinder componentFinder = mock(ComponentFinder.class);
        IssueIndex issueIndex = mock(IssueIndex.class);

        ListAction testCase = new ListAction(dbClient, userSession, componentFinder, issueIndex);

        WebService.NewParam keyParam = mock(WebService.NewParam.class);
        when(keyParam.setDescription(anyString())).thenReturn(keyParam);
        when(keyParam.setExampleValue(any())).thenReturn(keyParam);
        when(keyParam.setRequired(eq(true))).thenReturn(keyParam);

        WebService.NewController newController = mock(WebService.NewController.class);
        WebService.NewAction newAction = mock(WebService.NewAction.class);

        when(newController.createAction(eq("list"))).thenReturn(newAction);

        when(newAction.setSince(anyString())).thenReturn(newAction);
        when(newAction.setDescription(anyString())).thenReturn(newAction);
        when(newAction.setResponseExample(eq(getClass().getResource("list-example.json")))).thenReturn(newAction);
        when(newAction.setHandler(eq(testCase))).thenReturn(newAction);
        when(newAction.setChangelog(any(Change.class))).thenReturn(newAction);
        when(newAction.createParam(anyString())).thenReturn(keyParam);

        testCase.define(newController);

        verify(newAction).setHandler(eq(testCase));
        verify(keyParam).setRequired(true);
    }

    @Test
    public void testHandle() throws Exception {
        DbClient dbClient = mock(DbClient.class);
        UserSession userSession = mock(UserSession.class);
        ComponentFinder componentFinder = mock(ComponentFinder.class);
        IssueIndex issueIndex = mock(IssueIndex.class);

        ListAction testCase = new ListAction(dbClient, userSession, componentFinder, issueIndex);

        Request request = mock(Request.class);
        Response response = mock(Response.class);
        DbSession dbSession = mock(DbSession.class);
        ProjectDto projectDto = mock(ProjectDto.class);
        BranchDao branchDao = mock(BranchDao.class);
        BranchDto branchDto = mock(BranchDto.class);
        PrStatistics prStatistics = mock(PrStatistics.class);
        LiveMeasureDto liveMeasureDto = mock(LiveMeasureDto.class);
        LiveMeasureDao liveMeasureDao = mock(LiveMeasureDao.class);
        SnapshotDto snapshotDto = mock(SnapshotDto.class);
        SnapshotDao snapshotDao = mock(SnapshotDao.class);
        Response.Stream responseStream = mock(Response.Stream.class);
        OutputStream outputStream = mock(OutputStream.class);

        when(userSession.hasProjectPermission(eq("user"), eq(projectDto))).thenReturn(true);
        when(userSession.hasProjectPermission(eq("scan"), eq(projectDto))).thenReturn(true);
        when(userSession.hasPermission(eq(GlobalPermission.SCAN))).thenReturn(true);
        when(request.mandatoryParam(eq(PARAM_PROJECT))).thenReturn("fake");

        when(dbClient.openSession(eq(false))).thenReturn(dbSession);

        when(componentFinder.getProjectOrApplicationByKey(eq(dbSession), eq("fake"))).thenReturn(projectDto);
        when(userSession.checkProjectPermission(eq(UserRole.ADMIN), eq(projectDto))).thenReturn(null);
        when(projectDto.getUuid()).thenReturn("fake");
        when(branchDto.getBranchType()).thenReturn(PULL_REQUEST);
        when(branchDto.getMergeBranchUuid()).thenReturn("fake");
        when(branchDto.getUuid()).thenReturn("fake");
        when(branchDto.getKey()).thenReturn("fake");
        when(branchDto.getPullRequestData()).thenReturn(
                DbProjectBranches.PullRequestData.newBuilder()
                        .setBranch("fake")
                        .setUrl("fake")
                        .setTitle("fake")
                        .build()
        );
        when(branchDao.selectByProject(eq(dbSession), eq(projectDto))).thenReturn(Arrays.asList(branchDto));
        when(branchDao.selectByUuids(eq(dbSession), eq(Arrays.asList("fake")))).thenReturn(Arrays.asList(branchDto));
        when(dbClient.branchDao()).thenReturn(branchDao);

        when(prStatistics.getBranchUuid()).thenReturn("fake");
        when(issueIndex.searchBranchStatistics(eq("fake"), eq(Arrays.asList("fake")))).thenReturn(Arrays.asList(prStatistics));

        when(liveMeasureDto.getComponentUuid()).thenReturn("fake");
        when(liveMeasureDao.selectByComponentUuidsAndMetricKeys(eq(dbSession), eq(Arrays.asList("fake")), eq(singletonList(ALERT_STATUS_KEY))))
                .thenReturn(Arrays.asList(liveMeasureDto));
        when(dbClient.liveMeasureDao()).thenReturn(liveMeasureDao);

        when(snapshotDto.getComponentUuid()).thenReturn("fake");
        when(snapshotDto.getCreatedAt()).thenReturn(new Date().getTime());
        when(snapshotDao.selectLastAnalysesByRootComponentUuids(eq(dbSession), eq(Arrays.asList("fake"))))
                .thenReturn(Arrays.asList(snapshotDto));
        when(dbClient.snapshotDao()).thenReturn(snapshotDao);

        when(responseStream.output()).thenReturn(outputStream);
        when(response.stream()).thenReturn(responseStream);
        when(request.getMediaType()).thenReturn("application/x-protobuf");

        // verify no exception is thrown
        testCase.handle(request, response);
    }

}
