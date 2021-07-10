/*
 * Copyright (C) 2021 Michael Clarke
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
package com.github.mc1arke.sonarqube.plugin.server.pullrequest.validator;

import com.github.mc1arke.sonarqube.plugin.InvalidConfigurationException;
import com.github.mc1arke.sonarqube.plugin.almclient.gitlab.GitlabClient;
import com.github.mc1arke.sonarqube.plugin.almclient.gitlab.GitlabClientFactory;
import com.github.mc1arke.sonarqube.plugin.almclient.gitlab.model.Project;
import org.assertj.core.api.Condition;
import org.junit.jupiter.api.Test;
import org.sonar.db.alm.setting.ALM;
import org.sonar.db.alm.setting.AlmSettingDto;
import org.sonar.db.alm.setting.ProjectAlmSettingDto;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GitlabValidatorTest {

    private final GitlabClientFactory gitlabClientFactory = mock(GitlabClientFactory.class);
    private final ProjectAlmSettingDto projectAlmSettingDto = mock(ProjectAlmSettingDto.class);
    private final AlmSettingDto almSettingDto = mock(AlmSettingDto.class);

    @Test
    void testCorrectAlmReturnedForValidator() {
        GitlabValidator underTest = new GitlabValidator(gitlabClientFactory);
        assertThat(underTest.alm()).containsOnly(ALM.GITLAB);
    }

    @Test
    void testInvalidConfigurationExceptionThrownIfCreateClientFails() {
        GitlabValidator underTest = new GitlabValidator(gitlabClientFactory);
        when(gitlabClientFactory.createClient(any(), any())).thenThrow(new IllegalStateException("dummy"));
        assertThatThrownBy(() -> underTest.validate(projectAlmSettingDto, almSettingDto))
                .isInstanceOf(InvalidConfigurationException.class)
                .hasMessage("Could not create Gitlab client - dummy")
                .has(new Condition<>(t -> ((InvalidConfigurationException) t).getScope() == InvalidConfigurationException.Scope.PROJECT, "PROJECT Scope for exception"));
    }

    @Test
    void testInvalidConfigurationExceptionThrownIfRetrieveProjectFails() throws IOException {
        GitlabValidator underTest = new GitlabValidator(gitlabClientFactory);
        GitlabClient gitlabClient = mock(GitlabClient.class);
        when(gitlabClient.getProject(any())).thenThrow(new IllegalStateException("dummy"));
        when(gitlabClientFactory.createClient(any(), any())).thenReturn(gitlabClient);
        assertThatThrownBy(() -> underTest.validate(projectAlmSettingDto, almSettingDto))
                .isInstanceOf(InvalidConfigurationException.class)
                .hasMessage("Project could not be retrieved from Gitlab - dummy")
                .has(new Condition<>(t -> ((InvalidConfigurationException) t).getScope() == InvalidConfigurationException.Scope.PROJECT, "PROJECT Scope for exception"));
    }

    @Test
    void testInvalidConfigurationExceptionRethrownIfCreateClientThrowsInvalidConfigurationException() {
        GitlabValidator underTest = new GitlabValidator(gitlabClientFactory);
        when(gitlabClientFactory.createClient(any(), any())).thenThrow(new InvalidConfigurationException(InvalidConfigurationException.Scope.PROJECT, "dummy"));
        assertThatThrownBy(() -> underTest.validate(projectAlmSettingDto, almSettingDto))
                .isInstanceOf(InvalidConfigurationException.class)
                .hasMessage("dummy")
                .has(new Condition<>(t -> ((InvalidConfigurationException) t).getScope() == InvalidConfigurationException.Scope.PROJECT, "PROJECT Scope for exception"));
    }

    @Test
    void testHappyPath() throws IOException {
        GitlabValidator underTest = new GitlabValidator(gitlabClientFactory);
        GitlabClient gitlabClient = mock(GitlabClient.class);
        when(gitlabClient.getProject(any())).thenReturn(mock(Project.class));
        when(gitlabClientFactory.createClient(any(), any())).thenReturn(gitlabClient);

        underTest.validate(projectAlmSettingDto, almSettingDto);

        verify(gitlabClient).getProject(any());
    }
}
