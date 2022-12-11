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
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.project.ProjectDto;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.user.UserSession;

public class DeleteBindingAction extends ProjectWsAction {

    private final DbClient dbClient;

    public DeleteBindingAction(DbClient dbClient, UserSession userSession, ComponentFinder componentFinder) {
        super("delete_binding", dbClient, componentFinder, userSession);
        this.dbClient = dbClient;
    }


    @Override
    protected void configureAction(WebService.NewAction action) {
        action.setPost(true);
    }

    @Override
    protected void handleProjectRequest(ProjectDto project, Request request, Response response, DbSession dbSession) {
        dbClient.projectAlmSettingDao().deleteByProject(dbSession, project);
        dbSession.commit();

        response.noContent();
    }

}
