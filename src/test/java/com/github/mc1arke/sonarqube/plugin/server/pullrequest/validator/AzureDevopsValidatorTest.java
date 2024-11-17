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

import com.github.mc1arke.sonarqube.plugin.InvalidConfigurationException;
import com.github.mc1arke.sonarqube.plugin.almclient.azuredevops.AzureDevopsClient;
import com.github.mc1arke.sonarqube.plugin.almclient.azuredevops.AzureDevopsClientFactory;
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

class AzureDevopsValidatorTest {

    private final AzureDevopsClientFactory azureDevopsClientFactory = mock();
    private final ProjectAlmSettingDto projectAlmSettingDto = mock();
    private final AlmSettingDto almSettingDto = mock();

    @Test
    void testCorrectAlmReturnedForValidator() {
        AzureDevopsValidator underTest = new AzureDevopsValidator(azureDevopsClientFactory);
        assertThat(underTest.alm()).containsOnly(ALM.AZURE_DEVOPS);
    }

    @Test
    void testInvalidConfigurationExceptionThrownIfCreateClientFails() {
        AzureDevopsValidator underTest = new AzureDevopsValidator(azureDevopsClientFactory);
        when(azureDevopsClientFactory.createClient(any(), any())).thenThrow(new IllegalStateException("dummy"));
        assertThatThrownBy(() -> underTest.validate(projectAlmSettingDto, almSettingDto))
                .isInstanceOf(InvalidConfigurationException.class)
                .hasMessage("Could not create Azure Devops client - dummy")
                .has(new Condition<>(t -> ((InvalidConfigurationException) t).getScope() == InvalidConfigurationException.Scope.PROJECT, "PROJECT Scope for exception"));
    }

    @Test
    void testInvalidConfigurationExceptionThrownIfRetrieveProjectFails() throws IOException {
        AzureDevopsValidator underTest = new AzureDevopsValidator(azureDevopsClientFactory);
        when(projectAlmSettingDto.getAlmSlug()).thenReturn("slug");
        when(projectAlmSettingDto.getAlmRepo()).thenReturn("repo");
        AzureDevopsClient azureDevopsClient = mock();
        when(azureDevopsClient.getRepository(any(), any())).thenThrow(new IllegalStateException("dummy"));
        when(azureDevopsClientFactory.createClient(any(), any())).thenReturn(azureDevopsClient);
        assertThatThrownBy(() -> underTest.validate(projectAlmSettingDto, almSettingDto))
                .isInstanceOf(InvalidConfigurationException.class)
                .hasMessage("Project could not be retrieved from Azure Devops - dummy")
                .has(new Condition<>(t -> ((InvalidConfigurationException) t).getScope() == InvalidConfigurationException.Scope.PROJECT, "PROJECT Scope for exception"));
    }

    @Test
    void testInvalidConfigurationExceptionRethrownIfCreateClientThrowsInvalidConfigurationException() {
        AzureDevopsValidator underTest = new AzureDevopsValidator(azureDevopsClientFactory);
        when(azureDevopsClientFactory.createClient(any(), any())).thenThrow(new InvalidConfigurationException(InvalidConfigurationException.Scope.PROJECT, "dummy"));
        assertThatThrownBy(() -> underTest.validate(projectAlmSettingDto, almSettingDto))
                .isInstanceOf(InvalidConfigurationException.class)
                .hasMessage("dummy")
                .has(new Condition<>(t -> ((InvalidConfigurationException) t).getScope() == InvalidConfigurationException.Scope.PROJECT, "PROJECT Scope for exception"));
    }

    @Test
    void testExceptionOnMissingSlug() throws IOException {
        AzureDevopsValidator underTest = new AzureDevopsValidator(azureDevopsClientFactory);
        AzureDevopsClient azureDevopsClient = mock();
        when(azureDevopsClient.getRepository(any(), any())).thenReturn(mock());
        when(azureDevopsClientFactory.createClient(any(), any())).thenReturn(azureDevopsClient);

        assertThatThrownBy(() -> underTest.validate(projectAlmSettingDto, almSettingDto))
                .isInstanceOf(InvalidConfigurationException.class)
                .hasMessage("Repository slug must be provided")
                .has(new Condition<>(t -> ((InvalidConfigurationException) t).getScope() == InvalidConfigurationException.Scope.PROJECT, "PROJECT Scope for exception"));
    }

    @Test
    void testExceptionOnMissingRepo() throws IOException {
        AzureDevopsValidator underTest = new AzureDevopsValidator(azureDevopsClientFactory);
        AzureDevopsClient azureDevopsClient = mock();
        when(azureDevopsClient.getRepository(any(), any())).thenReturn(mock());
        when(azureDevopsClientFactory.createClient(any(), any())).thenReturn(azureDevopsClient);
        when(projectAlmSettingDto.getAlmSlug()).thenReturn("slug");

        assertThatThrownBy(() -> underTest.validate(projectAlmSettingDto, almSettingDto))
                .isInstanceOf(InvalidConfigurationException.class)
                .hasMessage("Repository name must be provided")
                .has(new Condition<>(t -> ((InvalidConfigurationException) t).getScope() == InvalidConfigurationException.Scope.PROJECT, "PROJECT Scope for exception"));
    }


    @Test
    void testHappyPath() throws IOException {
        AzureDevopsValidator underTest = new AzureDevopsValidator(azureDevopsClientFactory);
        AzureDevopsClient azureDevopsClient = mock();
        when(azureDevopsClient.getRepository(any(), any())).thenReturn(mock());
        when(azureDevopsClientFactory.createClient(any(), any())).thenReturn(azureDevopsClient);
        when(projectAlmSettingDto.getAlmSlug()).thenReturn("slug");
        when(projectAlmSettingDto.getAlmRepo()).thenReturn("repo");

        underTest.validate(projectAlmSettingDto, almSettingDto);

        verify(azureDevopsClient).getRepository(any(), any());
    }
}
