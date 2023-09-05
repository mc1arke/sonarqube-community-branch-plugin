/*
 * Copyright (C) 2022 Michael Clarke
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package com.github.mc1arke.sonarqube.plugin.server.pullrequest.ws.pullrequest.action;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.db.DbClient;
import org.sonar.db.component.BranchDao;
import org.sonar.db.component.BranchDto;
import org.sonar.db.component.BranchType;
import org.sonar.db.entity.EntityDto;
import org.sonar.db.project.ProjectDto;
import org.sonar.server.component.ComponentCleanerService;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.exceptions.UnauthorizedException;
import org.sonar.server.user.UserSession;

class DeleteActionTest {

    private final DbClient dbClient = mock(DbClient.class);
    private final UserSession userSession = mock(UserSession.class);
    private final ComponentFinder componentFinder = mock(ComponentFinder.class);
    private final ComponentCleanerService componentCleanerService = mock(ComponentCleanerService.class);
    private final DeleteAction underTest = new DeleteAction(dbClient, componentFinder, userSession, componentCleanerService);

    @Test
    void shouldDefineEndpointWithAllParameters() {
        WebService.NewController newController = mock(WebService.NewController.class);
        WebService.NewAction newAction = mock(WebService.NewAction.class);
        when(newAction.setHandler(any())).thenReturn(newAction);
        when(newController.createAction(any())).thenReturn(newAction);
        WebService.NewParam projectParam = mock(WebService.NewParam.class);
        WebService.NewParam pullRequestParam = mock(WebService.NewParam.class);
        when(newAction.createParam(any())).thenReturn(projectParam, pullRequestParam);

        when(newAction.setPost(anyBoolean())).thenReturn(newAction);

        underTest.define(newController);

        verify(newController).createAction("delete");
        verify(newAction).setHandler(underTest);
        verify(newAction).createParam("project");
        verify(newAction).createParam("pullRequest");
        verify(newAction).setPost(true);
        verifyNoMoreInteractions(newAction);
        verify(projectParam).setRequired(true);
        verify(pullRequestParam).setRequired(true);
        verifyNoMoreInteractions(projectParam);
        verifyNoMoreInteractions(pullRequestParam);

        verifyNoMoreInteractions(newController);
    }

    @Test
    void shouldExecuteRequestWithValidParameters() {
        Request request = mock(Request.class);
        when(request.mandatoryParam("project")).thenReturn("project");
        when(request.mandatoryParam("pullRequest")).thenReturn("pullRequestId");

        when(componentFinder.getProjectByKey(any(), any())).thenReturn(new ProjectDto().setKey("projectKey").setUuid("uuid0"));

        when(userSession.checkLoggedIn()).thenReturn(userSession);

        BranchDto pullRequest = new BranchDto().setBranchType(BranchType.PULL_REQUEST);
        BranchDao branchDao = mock(BranchDao.class);
        when(dbClient.branchDao()).thenReturn(branchDao);
        when(branchDao.selectByPullRequestKey(any(), any(), any())).thenReturn(Optional.of(pullRequest));

        Response response = mock(Response.class);

        underTest.handle(request, response);

        verify(componentCleanerService).deleteBranch(any(), eq(pullRequest));
        verify(response).noContent();
    }

    @Test
    void shouldNotPerformDeleteIfUserNotLoggedIn() {
        Request request = mock(Request.class);
        when(request.mandatoryParam("project")).thenReturn("project");
        when(request.mandatoryParam("pullRequest")).thenReturn("pullRequestId");

        when(componentFinder.getProjectByKey(any(), any())).thenReturn(new ProjectDto().setKey("projectKey").setUuid("uuid0"));

        when(userSession.checkLoggedIn()).thenThrow(new UnauthorizedException("Dummy"));

        Response response = mock(Response.class);

        assertThatThrownBy(() -> underTest.handle(request, response)).isInstanceOf(UnauthorizedException.class).hasMessage("Dummy");

        verify(componentCleanerService, never()).deleteBranch(any(), any());
        verify(response, never()).noContent();
    }

    @Test
    void shouldNotPerformDeleteIfUserNotProjectAdmin() {
        Request request = mock(Request.class);
        when(request.mandatoryParam("project")).thenReturn("project");
        when(request.mandatoryParam("pullRequest")).thenReturn("pullRequestId");

        when(componentFinder.getProjectByKey(any(), any())).thenReturn(new ProjectDto().setKey("projectKey").setUuid("uuid0"));

        when(userSession.checkLoggedIn()).thenReturn(userSession);
        when(userSession.hasEntityPermission(any(), any(EntityDto.class))).thenThrow(new UnauthorizedException("Dummy"));

        Response response = mock(Response.class);

        assertThatThrownBy(() -> underTest.handle(request, response)).isInstanceOf(UnauthorizedException.class).hasMessage("Dummy");

        verify(componentCleanerService, never()).deleteBranch(any(), any());
        verify(response, never()).noContent();
    }

}