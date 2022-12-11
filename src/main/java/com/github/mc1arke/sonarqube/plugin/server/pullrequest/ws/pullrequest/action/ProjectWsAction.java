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
 *
 */
package com.github.mc1arke.sonarqube.plugin.server.pullrequest.ws.pullrequest.action;

import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.project.ProjectDto;
import org.sonar.server.component.ComponentFinder;

public abstract class ProjectWsAction implements PullRequestWsAction {

    private static final String PROJECT_PARAMETER = "project";

    private final String actionName;
    private final DbClient dbClient;
    private final ComponentFinder componentFinder;

    protected ProjectWsAction(String actionName, DbClient dbClient, ComponentFinder componentFinder) {
        super();
        this.actionName = actionName;
        this.dbClient = dbClient;
        this.componentFinder = componentFinder;
    }

    @Override
    public void define(WebService.NewController context) {
        WebService.NewAction action = context.createAction(actionName).setHandler(this);
        action.createParam(PROJECT_PARAMETER).setRequired(true);

        configureAction(action);
    }

    protected abstract void configureAction(WebService.NewAction action);


    @Override
    public void handle(Request request, Response response) {
        String projectKey = request.mandatoryParam(PROJECT_PARAMETER);

        try (DbSession dbSession = dbClient.openSession(false)) {
            ProjectDto project = componentFinder.getProjectByKey(dbSession, projectKey);
            handleProjectRequest(project, request, response, dbSession);
        }
    }

    protected abstract void handleProjectRequest(ProjectDto project, Request request, Response response, DbSession dbSession);

    protected DbClient getDbClient() {
        return dbClient;
    }
}