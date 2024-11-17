/*
 * Copyright (C) 2021-2024 Michael Clarke
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
import org.assertj.core.api.Condition;
import org.junit.jupiter.api.Test;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.web.UserRole;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.alm.setting.ALM;
import org.sonar.db.alm.setting.AlmSettingDao;
import org.sonar.db.alm.setting.AlmSettingDto;
import org.sonar.db.alm.setting.ProjectAlmSettingDao;
import org.sonar.db.alm.setting.ProjectAlmSettingDto;
import org.sonar.db.project.ProjectDto;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.exceptions.BadConfigurationException;
import org.sonar.server.user.UserSession;

import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ValidateBindingActionTest {

    private final DbClient dbClient = mock();
    private final ComponentFinder componentFinder = mock();
    private final UserSession userSession = mock();

    @Test
    void testConfigureActionNoOperation() {
        ValidateBindingAction underTest = new ValidateBindingAction(dbClient, componentFinder, userSession, Collections.emptyList());
        WebService.NewAction newAction = mock();
        underTest.configureAction(newAction);
        verifyNoInteractions(newAction);
    }

    @Test
    void testHandleProjectRequestSuccessWithNoFurtherOperationsWhenNoAlmConfigurationExistsForProject() {
        ValidateBindingAction underTest = new ValidateBindingAction(dbClient, componentFinder, userSession, Collections.emptyList());

        ProjectDto projectDto = mock();
        Request request = mock();
        Response response = mock();
        DbSession dbSession = mock();

        ProjectAlmSettingDao projectAlmSettingDao = mock();
        when(projectAlmSettingDao.selectByProject(dbSession, projectDto)).thenReturn(Optional.empty());
        when(dbClient.projectAlmSettingDao()).thenReturn(projectAlmSettingDao);

        when(request.param("project")).thenReturn("project");

        underTest.handle(request, response);

        verify(dbClient, never()).almSettingDao();
    }

    @Test
    void testHandleProjectRequestThrowsExceptionWhenAlmForProjectDoesNotExist() {
        ValidateBindingAction underTest = new ValidateBindingAction(dbClient, componentFinder, userSession, Collections.emptyList());

        ProjectDto projectDto = mock();
        Request request = mock();
        Response response = mock();
        DbSession dbSession = mock();

        String almUuid = "almUuid";
        ProjectAlmSettingDto projectAlmSettingDto = mock();
        when(projectAlmSettingDto.getAlmSettingUuid()).thenReturn(almUuid);
        ProjectAlmSettingDao projectAlmSettingDao = mock();
        when(projectAlmSettingDao.selectByProject(dbSession, projectDto)).thenReturn(Optional.of(projectAlmSettingDto));
        when(dbClient.projectAlmSettingDao()).thenReturn(projectAlmSettingDao);

        AlmSettingDao almSettingDao = mock();
        when(almSettingDao.selectByUuid(dbSession, almUuid)).thenReturn(Optional.empty());
        when(dbClient.almSettingDao()).thenReturn(almSettingDao);

        assertThatThrownBy(() -> underTest.handleProjectRequest(projectDto, request, response, dbSession))
                .isInstanceOf(BadConfigurationException.class)
                .hasMessage("The ALM setting bound to the project no longer exists")
                .has(new Condition<>(t -> ((BadConfigurationException) t).scope().equals("PROJECT"), "PROJECT Scope for exception"));
    }

    @Test
    void testHandleProjectRequestThrowsExceptionWhenNoValidatorExistsForAlm() {
        ValidateBindingAction underTest = new ValidateBindingAction(dbClient, componentFinder, userSession, Collections.emptyList());

        ProjectDto projectDto = mock();
        Request request = mock();
        Response response = mock();
        DbSession dbSession = mock();

        String almUuid = "almUuid";
        ProjectAlmSettingDto projectAlmSettingDto = mock();
        when(projectAlmSettingDto.getAlmSettingUuid()).thenReturn(almUuid);
        ProjectAlmSettingDao projectAlmSettingDao = mock();
        when(projectAlmSettingDao.selectByProject(dbSession, projectDto)).thenReturn(Optional.of(projectAlmSettingDto));
        when(dbClient.projectAlmSettingDao()).thenReturn(projectAlmSettingDao);

        AlmSettingDto almSettingDto = mock();
        when(almSettingDto.getAlm()).thenReturn(ALM.AZURE_DEVOPS);
        AlmSettingDao almSettingDao = mock();
        when(almSettingDao.selectByUuid(dbSession, almUuid)).thenReturn(Optional.of(almSettingDto));
        when(dbClient.almSettingDao()).thenReturn(almSettingDao);

        assertThatThrownBy(() -> underTest.handleProjectRequest(projectDto, request, response, dbSession))
                .isInstanceOf(BadConfigurationException.class)
                .hasMessage("The AZURE_DEVOPS ALM is not supported by any validators")
                .has(new Condition<>(t -> ((BadConfigurationException) t).scope().equals("PROJECT"), "PROJECT Scope for exception"));
    }

    @Test
    void testHandleProjectRequestThrowsInvalidConfigurationExceptionWhenRuntimeExceptionThrownByValidator() {
        Validator validator = mock();
        when(validator.alm()).thenReturn(Collections.singletonList(ALM.AZURE_DEVOPS));
        doThrow(new InvalidConfigurationException(InvalidConfigurationException.Scope.PROJECT, "dummy")).when(validator).validate(any(), any());
        ValidateBindingAction underTest = new ValidateBindingAction(dbClient, componentFinder, userSession, Collections.singletonList(validator));

        ProjectDto projectDto = mock();
        Request request = mock();
        Response response = mock();
        DbSession dbSession = mock();

        String almUuid = "almUuid";
        ProjectAlmSettingDto projectAlmSettingDto = mock();
        when(projectAlmSettingDto.getAlmSettingUuid()).thenReturn(almUuid);
        ProjectAlmSettingDao projectAlmSettingDao = mock();
        when(projectAlmSettingDao.selectByProject(dbSession, projectDto)).thenReturn(Optional.of(projectAlmSettingDto));
        when(dbClient.projectAlmSettingDao()).thenReturn(projectAlmSettingDao);

        AlmSettingDto almSettingDto = mock();
        when(almSettingDto.getAlm()).thenReturn(ALM.AZURE_DEVOPS);
        AlmSettingDao almSettingDao = mock();
        when(almSettingDao.selectByUuid(dbSession, almUuid)).thenReturn(Optional.of(almSettingDto));
        when(dbClient.almSettingDao()).thenReturn(almSettingDao);

        assertThatThrownBy(() -> underTest.handleProjectRequest(projectDto, request, response, dbSession))
                .isInstanceOf(BadConfigurationException.class)
                .hasMessage("dummy")
                .has(new Condition<>(t -> ((BadConfigurationException) t).scope().equals("PROJECT"), "PROJECT Scope for exception"));
    }

    @Test
    void testHandleProjectRequestHappyPath() {
        Validator validator = mock();
        when(validator.alm()).thenReturn(Collections.singletonList(ALM.AZURE_DEVOPS));
        ValidateBindingAction underTest = new ValidateBindingAction(dbClient, componentFinder, userSession, Collections.singletonList(validator));

        ProjectDto projectDto = mock();
        Request request = mock();
        Response response = mock();
        DbSession dbSession = mock();

        when(dbClient.openSession(anyBoolean())).thenReturn(dbSession);

        when(componentFinder.getProjectByKey(dbSession, "project")).thenReturn(projectDto);

        String almUuid = "almUuid";
        ProjectAlmSettingDto projectAlmSettingDto = mock();
        when(projectAlmSettingDto.getAlmSettingUuid()).thenReturn(almUuid);
        ProjectAlmSettingDao projectAlmSettingDao = mock();
        when(projectAlmSettingDao.selectByProject(dbSession, projectDto)).thenReturn(Optional.of(projectAlmSettingDto));
        when(dbClient.projectAlmSettingDao()).thenReturn(projectAlmSettingDao);

        AlmSettingDto almSettingDto = mock();
        when(almSettingDto.getAlm()).thenReturn(ALM.AZURE_DEVOPS);
        AlmSettingDao almSettingDao = mock();
        when(almSettingDao.selectByUuid(dbSession, almUuid)).thenReturn(Optional.of(almSettingDto));
        when(dbClient.almSettingDao()).thenReturn(almSettingDao);

        when(request.mandatoryParam("project")).thenReturn("project");

        underTest.handle(request, response);

        verify(validator).validate(projectAlmSettingDto, almSettingDto);
        verify(userSession).hasEntityPermission(UserRole.USER, projectDto);
    }
}
