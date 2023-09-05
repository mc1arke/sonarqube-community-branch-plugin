/*
 * Copyright (C) 2020-2022 Michael Clarke
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
package com.github.mc1arke.sonarqube.plugin.server.pullrequest.ws.binding.action;

import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.web.UserRole;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.project.ProjectDto;
import org.sonar.server.almsettings.ws.AlmSettingsWsAction;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.user.UserSession;

public abstract class ProjectWsAction implements AlmSettingsWsAction {

    private static final String PROJECT_PARAMETER = "project";

    private final String actionName;
    private final DbClient dbClient;
    private final ComponentFinder componentFinder;
    private final UserSession userSession;
    private final String permission;

    protected ProjectWsAction(String actionName, DbClient dbClient, ComponentFinder componentFinder, UserSession userSession) {
        this(actionName, dbClient, componentFinder, userSession, UserRole.ADMIN);
    }

    protected ProjectWsAction(String actionName, DbClient dbClient, ComponentFinder componentFinder, UserSession userSession, String permission) {
        super();
        this.actionName = actionName;
        this.dbClient = dbClient;
        this.componentFinder = componentFinder;
        this.userSession = userSession;
        this.permission = permission;
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
            userSession.hasEntityPermission(permission, project);
            handleProjectRequest(project, request, response, dbSession);
        }
    }

    protected abstract void handleProjectRequest(ProjectDto project, Request request, Response response, DbSession dbSession);

    protected DbClient getDbClient() {
        return dbClient;
    }
}
