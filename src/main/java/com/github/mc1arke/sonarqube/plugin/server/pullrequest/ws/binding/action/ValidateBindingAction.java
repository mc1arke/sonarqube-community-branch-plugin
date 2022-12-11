/*
 * Copyright (C) 2021-2022 Michael Clarke
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

import com.github.mc1arke.sonarqube.plugin.InvalidConfigurationException;
import com.github.mc1arke.sonarqube.plugin.server.pullrequest.validator.Validator;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.web.UserRole;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.alm.setting.ALM;
import org.sonar.db.alm.setting.AlmSettingDto;
import org.sonar.db.alm.setting.ProjectAlmSettingDto;
import org.sonar.db.project.ProjectDto;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.exceptions.BadConfigurationException;
import org.sonar.server.user.UserSession;

import java.util.List;

public class ValidateBindingAction extends ProjectWsAction {

    private final List<Validator> validators;

    public ValidateBindingAction(DbClient dbClient, ComponentFinder componentFinder, UserSession userSession, List<Validator> validators) {
        super("validate_binding", dbClient, componentFinder, userSession, UserRole.USER);
        this.validators = validators;
    }

    @Override
    protected void configureAction(WebService.NewAction action) {
        //no-op
    }

    @Override
    protected void handleProjectRequest(ProjectDto project, Request request, Response response, DbSession dbSession) {
        DbClient dbClient = getDbClient();
        dbClient.projectAlmSettingDao()
                .selectByProject(dbSession, project)
                .ifPresent(projectAlmSettingDto -> validateProject(projectAlmSettingDto, dbSession, dbClient, validators));
    }

    private static void validateProject(ProjectAlmSettingDto projectAlmSettingDto, DbSession dbSession, DbClient dbClient, List<Validator> validators) {
        try {
            AlmSettingDto almSettingDto = dbClient.almSettingDao().selectByUuid(dbSession, projectAlmSettingDto.getAlmSettingUuid())
                .orElseThrow(() -> new InvalidConfigurationException(InvalidConfigurationException.Scope.PROJECT, "The ALM setting bound to the project no longer exists"));

            ALM targetAlm = almSettingDto.getAlm();

            Validator validator = validators.stream()
                    .filter(decorator -> decorator.alm().contains(targetAlm))
                    .findFirst()
                    .orElseThrow(() -> new InvalidConfigurationException(InvalidConfigurationException.Scope.PROJECT, String.format("The %s ALM is not supported by any validators", targetAlm)));

            validator.validate(projectAlmSettingDto, almSettingDto);
        } catch (InvalidConfigurationException ex) {
            throw new BadConfigurationException(ex.getScope().name(), ex.getMessage());
        }
    }
}
