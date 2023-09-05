/*
 * Copyright (C) 2009-2022 SonarSource SA (mailto:info AT sonarsource DOT com), Michael Clarke
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

import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.web.UserRole;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.BranchDto;
import org.sonar.db.component.BranchType;
import org.sonar.db.project.ProjectDto;
import org.sonar.server.component.ComponentCleanerService;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.user.UserSession;

public class DeleteAction extends ProjectWsAction {

    private static final String PULL_REQUEST_PARAMETER = "pullRequest";

    private final UserSession userSession;
    private final ComponentCleanerService componentCleanerService;

    public DeleteAction(DbClient dbClient, ComponentFinder componentFinder, UserSession userSession, ComponentCleanerService componentCleanerService) {
        super("delete", dbClient, componentFinder);
        this.userSession = userSession;
        this.componentCleanerService = componentCleanerService;
    }

    @Override
    protected void configureAction(WebService.NewAction action) {
        action.setPost(true)
            .createParam(PULL_REQUEST_PARAMETER)
            .setRequired(true);
    }

    @Override
    public void handleProjectRequest(ProjectDto project, Request request, Response response, DbSession dbSession) {
        userSession.checkLoggedIn().hasEntityPermission(UserRole.ADMIN, project);

        String pullRequestId = request.mandatoryParam(PULL_REQUEST_PARAMETER);

        BranchDto pullRequest = getDbClient().branchDao().selectByPullRequestKey(dbSession, project.getUuid(), pullRequestId)
            .filter(branch -> branch.getBranchType() == BranchType.PULL_REQUEST)
            .orElseThrow(() -> new NotFoundException(String.format("Pull request '%s' is not found for project '%s'", pullRequestId, project.getKey())));

        componentCleanerService.deleteBranch(dbSession, pullRequest);
        response.noContent();
    }
}
