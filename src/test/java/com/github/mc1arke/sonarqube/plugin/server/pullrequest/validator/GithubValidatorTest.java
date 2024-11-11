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
package com.github.mc1arke.sonarqube.plugin.server.pullrequest.validator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;

import org.assertj.core.api.Condition;
import org.junit.jupiter.api.Test;
import org.kohsuke.github.GitHub;
import org.sonar.db.alm.setting.ALM;
import org.sonar.db.alm.setting.AlmSettingDto;
import org.sonar.db.alm.setting.ProjectAlmSettingDto;

import com.github.mc1arke.sonarqube.plugin.InvalidConfigurationException;
import com.github.mc1arke.sonarqube.plugin.almclient.github.GithubClientFactory;

class GithubValidatorTest {

    private final GithubClientFactory githubClientFactory = mock();
    private final ProjectAlmSettingDto projectAlmSettingDto = mock();
    private final AlmSettingDto almSettingDto = mock();

    @Test
    void shouldReturnCorrectAlm() {
        GithubValidator underTest = new GithubValidator(githubClientFactory);
        assertThat(underTest.alm()).containsOnly(ALM.GITHUB);
    }

    @Test
    void shouldThrowInvalidConfigurationExceptionIfCreateClientFails() throws IOException {
        GithubValidator underTest = new GithubValidator(githubClientFactory);
        when(githubClientFactory.createClient(any(), any())).thenThrow(new IllegalStateException("dummy"));
        assertThatThrownBy(() -> underTest.validate(projectAlmSettingDto, almSettingDto))
                .isInstanceOf(InvalidConfigurationException.class)
                .hasMessage("Could not create Github client - dummy")
                .has(new Condition<>(t -> ((InvalidConfigurationException) t).getScope() == InvalidConfigurationException.Scope.PROJECT, "PROJECT Scope for exception"));
    }

    @Test
    void shouldRethrownConfigrationExceptionIfThrownFromCreateClient() throws IOException {
        GithubValidator underTest = new GithubValidator(githubClientFactory);
        when(githubClientFactory.createClient(any(), any())).thenThrow(new InvalidConfigurationException(InvalidConfigurationException.Scope.PROJECT, "dummy"));
        assertThatThrownBy(() -> underTest.validate(projectAlmSettingDto, almSettingDto))
                .isInstanceOf(InvalidConfigurationException.class)
                .hasMessage("dummy")
                .has(new Condition<>(t -> ((InvalidConfigurationException) t).getScope() == InvalidConfigurationException.Scope.PROJECT, "PROJECT Scope for exception"));
    }

    @Test
    void shouldNotThrowExceptionIfRepoRetrieveSuccessfully() throws IOException {
        GithubValidator underTest = new GithubValidator(githubClientFactory);
        GitHub gitHub = mock();
        when(githubClientFactory.createClient(any(), any())).thenReturn(gitHub);
        when(projectAlmSettingDto.getAlmRepo()).thenReturn("reponame");

        underTest.validate(projectAlmSettingDto, almSettingDto);

        verify(githubClientFactory).createClient(almSettingDto, projectAlmSettingDto);
        verify(gitHub).getRepository("reponame");
    }
}
