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
import com.github.mc1arke.sonarqube.plugin.almclient.bitbucket.BitbucketClient;
import com.github.mc1arke.sonarqube.plugin.almclient.bitbucket.BitbucketClientFactory;
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

class BitbucketValidatorTest {

    private final BitbucketClientFactory bitbucketClientFactory = mock();
    private final ProjectAlmSettingDto projectAlmSettingDto = mock();
    private final AlmSettingDto almSettingDto = mock();

    @Test
    void testCorrectAlmReturnedForValidator() {
        BitbucketValidator underTest = new BitbucketValidator(bitbucketClientFactory);
        assertThat(underTest.alm()).containsOnly(ALM.BITBUCKET, ALM.BITBUCKET_CLOUD);
    }

    @Test
    void testInvalidConfigurationExceptionThrownIfCreateClientFails() {
        BitbucketValidator underTest = new BitbucketValidator(bitbucketClientFactory);
        when(bitbucketClientFactory.createClient(any(), any())).thenThrow(new IllegalStateException("dummy"));
        assertThatThrownBy(() -> underTest.validate(projectAlmSettingDto, almSettingDto))
                .isInstanceOf(InvalidConfigurationException.class)
                .hasMessage("Could not create Bitbucket client - dummy")
                .has(new Condition<>(t -> ((InvalidConfigurationException) t).getScope() == InvalidConfigurationException.Scope.PROJECT, "PROJECT Scope for exception"));
    }

    @Test
    void testInvalidConfigurationExceptionRethrownIfCreateClientThrows() {
        BitbucketValidator underTest = new BitbucketValidator(bitbucketClientFactory);
        when(bitbucketClientFactory.createClient(any(), any())).thenThrow(new InvalidConfigurationException(InvalidConfigurationException.Scope.GLOBAL, "dummy"));
        assertThatThrownBy(() -> underTest.validate(projectAlmSettingDto, almSettingDto))
                .isInstanceOf(InvalidConfigurationException.class)
                .hasMessage("dummy")
                .has(new Condition<>(t -> ((InvalidConfigurationException) t).getScope() == InvalidConfigurationException.Scope.GLOBAL, "PROJECT Scope for exception"));
    }

    @Test
    void testInvalidConfigurationExceptionThrownIfRetrieveRepositoryFails() throws IOException {
        BitbucketValidator underTest = new BitbucketValidator(bitbucketClientFactory);
        BitbucketClient bitbucketClient = mock();
        when(bitbucketClient.retrieveRepository()).thenThrow(new IOException("dummy"));
        when(bitbucketClientFactory.createClient(any(), any())).thenReturn(bitbucketClient);
        assertThatThrownBy(() -> underTest.validate(projectAlmSettingDto, almSettingDto))
                .isInstanceOf(InvalidConfigurationException.class)
                .hasMessage("Could not retrieve repository details from Bitbucket - dummy")
                .has(new Condition<>(t -> ((InvalidConfigurationException) t).getScope() == InvalidConfigurationException.Scope.PROJECT, "PROJECT Scope for exception"));
    }

    @Test
    void testInvalidConfigurationExceptionThrownIfCodeInsightsCheckFails() {
        BitbucketValidator underTest = new BitbucketValidator(bitbucketClientFactory);
        BitbucketClient bitbucketClient = mock();
        when(bitbucketClient.supportsCodeInsights()).thenThrow(new IllegalStateException("dummy"));
        when(bitbucketClientFactory.createClient(any(), any())).thenReturn(bitbucketClient);
        assertThatThrownBy(() -> underTest.validate(projectAlmSettingDto, almSettingDto))
                .isInstanceOf(InvalidConfigurationException.class)
                .hasMessage("Could not check Bitbucket configuration - dummy")
                .has(new Condition<>(t -> ((InvalidConfigurationException) t).getScope() == InvalidConfigurationException.Scope.PROJECT, "PROJECT Scope for exception"));
    }

    @Test
    void testInvalidConfigurationExceptionThrownIfCodeInsightsIsFalse() {
        BitbucketValidator underTest = new BitbucketValidator(bitbucketClientFactory);
        BitbucketClient bitbucketClient = mock();
        when(bitbucketClient.supportsCodeInsights()).thenReturn(false);
        when(bitbucketClientFactory.createClient(any(), any())).thenReturn(bitbucketClient);
        assertThatThrownBy(() -> underTest.validate(projectAlmSettingDto, almSettingDto))
                .isInstanceOf(InvalidConfigurationException.class)
                .hasMessage("The configured Bitbucket instance does not support code insights")
                .has(new Condition<>(t -> ((InvalidConfigurationException) t).getScope() == InvalidConfigurationException.Scope.PROJECT, "PROJECT Scope for exception"));
    }

    @Test
    void testHappyPath() {
        BitbucketValidator underTest = new BitbucketValidator(bitbucketClientFactory);
        BitbucketClient bitbucketClient = mock();
        when(bitbucketClient.supportsCodeInsights()).thenReturn(true);
        when(bitbucketClientFactory.createClient(any(), any())).thenReturn(bitbucketClient);

        underTest.validate(projectAlmSettingDto, almSettingDto);

        verify(bitbucketClient).supportsCodeInsights();
    }
}
