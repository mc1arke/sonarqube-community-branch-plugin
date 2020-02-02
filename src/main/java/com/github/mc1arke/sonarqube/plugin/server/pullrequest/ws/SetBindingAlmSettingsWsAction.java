/*
 * Copyright (C) 2020 Michael Clarke
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
package com.github.mc1arke.sonarqube.plugin.server.pullrequest.ws;

import com.github.mc1arke.sonarqube.plugin.server.pullrequest.ws.action.AlmSettingsWsAction;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.ComponentDto;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.user.UserSession;

import static org.sonar.api.web.UserRole.ADMIN;

public abstract class SetBindingAlmSettingsWsAction extends AlmSettingsWsAction {

    private final ComponentFinder componentFinder;
    private final UserSession userSession;

    public SetBindingAlmSettingsWsAction(DbClient dbClient, ComponentFinder componentFinder, UserSession userSession) {
        super(dbClient);
        this.componentFinder = componentFinder;
        this.userSession = userSession;
    }

    protected ComponentDto getProject(DbSession dbSession, String projectKey) {
        ComponentDto project = componentFinder.getByKey(dbSession, projectKey);
        userSession.checkComponentPermission(ADMIN, project);
        return project;
    }
}
