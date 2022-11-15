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
package com.github.mc1arke.sonarqube.plugin.almclient.github;

import com.github.mc1arke.sonarqube.plugin.InvalidConfigurationException;
import com.github.mc1arke.sonarqube.plugin.almclient.github.v3.RestApplicationAuthenticationProvider;
import com.github.mc1arke.sonarqube.plugin.almclient.github.v4.GraphqlGithubClient;
import com.github.mc1arke.sonarqube.plugin.almclient.github.v4.GraphqlProvider;
import org.assertj.core.api.Condition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sonar.api.config.internal.Encryption;
import org.sonar.api.config.internal.Settings;
import org.sonar.db.alm.setting.AlmSettingDto;
import org.sonar.db.alm.setting.ProjectAlmSettingDto;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DefaultGithubClientFactoryTest {

    private final AlmSettingDto almSettingDto = mock(AlmSettingDto.class);
    private final ProjectAlmSettingDto projectAlmSettingDto = mock(ProjectAlmSettingDto.class);
    private final RestApplicationAuthenticationProvider restApplicationAuthenticationProvider = mock(RestApplicationAuthenticationProvider.class);
    private final Settings settings = mock(Settings.class);
    private final GraphqlProvider graphqlProvider = mock(GraphqlProvider.class);

    @BeforeEach
    public void setUp() {
        when(almSettingDto.getUrl()).thenReturn("url");
        when(almSettingDto.getDecryptedPrivateKey(any())).thenReturn("privateKey");
        when(projectAlmSettingDto.getAlmRepo()).thenReturn("repo");
        when(almSettingDto.getAppId()).thenReturn("appId");
        when(settings.getEncryption()).thenReturn(mock(Encryption.class));
    }

    @Test
    void testExceptionThrownIfUrlMissing() {
        when(almSettingDto.getUrl()).thenReturn(null);
        DefaultGithubClientFactory underTest = new DefaultGithubClientFactory(restApplicationAuthenticationProvider, settings, graphqlProvider);
        assertThatThrownBy(() -> underTest.createClient(projectAlmSettingDto, almSettingDto))
                .isInstanceOf(InvalidConfigurationException.class)
                .hasMessage("No URL has been set for Github connections")
                .has(new Condition<>(t -> ((InvalidConfigurationException) t).getScope() == InvalidConfigurationException.Scope.GLOBAL, "GLOBAL Scope for exception"));
    }

    @Test
    void testExceptionThrownIfPrivateKeyMissing() {
        when(almSettingDto.getDecryptedPrivateKey(any())).thenReturn(null);
        DefaultGithubClientFactory underTest = new DefaultGithubClientFactory(restApplicationAuthenticationProvider, settings, graphqlProvider);
        assertThatThrownBy(() -> underTest.createClient(projectAlmSettingDto, almSettingDto))
                .isInstanceOf(InvalidConfigurationException.class)
                .hasMessage("No private key has been set for Github connections")
                .has(new Condition<>(t -> ((InvalidConfigurationException) t).getScope() == InvalidConfigurationException.Scope.GLOBAL, "GLOBAL Scope for exception"));
    }

    @Test
    void testExceptionThrownIfAlmRepoMissing() {
        when(projectAlmSettingDto.getAlmRepo()).thenReturn(null);
        DefaultGithubClientFactory underTest = new DefaultGithubClientFactory(restApplicationAuthenticationProvider, settings, graphqlProvider);
        assertThatThrownBy(() -> underTest.createClient(projectAlmSettingDto, almSettingDto))
                .isInstanceOf(InvalidConfigurationException.class)
                .hasMessage("No repository name has been set for Github connections")
                .has(new Condition<>(t -> ((InvalidConfigurationException) t).getScope() == InvalidConfigurationException.Scope.PROJECT, "PROJECT Scope for exception"));
    }

    @Test
    void testExceptionThrownIfAppIdMissing() {
        when(almSettingDto.getAppId()).thenReturn(null);
        DefaultGithubClientFactory underTest = new DefaultGithubClientFactory(restApplicationAuthenticationProvider, settings, graphqlProvider);
        assertThatThrownBy(() -> underTest.createClient(projectAlmSettingDto, almSettingDto))
                .isInstanceOf(InvalidConfigurationException.class)
                .hasMessage("No App ID has been set for Github connections")
                .has(new Condition<>(t -> ((InvalidConfigurationException) t).getScope() == InvalidConfigurationException.Scope.GLOBAL, "GLOBAL Scope for exception"));
    }

    @Test
    void testExceptionThrownIfAuthenticationProviderThrowsException() throws IOException {
        DefaultGithubClientFactory underTest = new DefaultGithubClientFactory(restApplicationAuthenticationProvider, settings, graphqlProvider);
        when(restApplicationAuthenticationProvider.getInstallationToken(any(), any(), any(), any())).thenThrow(new IOException("dummy"));
        assertThatThrownBy(() -> underTest.createClient(projectAlmSettingDto, almSettingDto))
                .isInstanceOf(InvalidConfigurationException.class)
                .hasMessage("Could not create Github client - dummy")
                .has(new Condition<>(t -> ((InvalidConfigurationException) t).getScope() == InvalidConfigurationException.Scope.PROJECT, "PROJECT Scope for exception"));
    }

    @Test
    void testHappyPath() throws IOException {
        DefaultGithubClientFactory underTest = new DefaultGithubClientFactory(restApplicationAuthenticationProvider, settings, graphqlProvider);
        when(projectAlmSettingDto.getAlmRepo()).thenReturn("alm/slug");

        RepositoryAuthenticationToken repositoryAuthenticationToken = mock(RepositoryAuthenticationToken.class);
        when(restApplicationAuthenticationProvider.getInstallationToken(any(), any(), any(), any())).thenReturn(repositoryAuthenticationToken);
        assertThat(underTest.createClient(projectAlmSettingDto, almSettingDto)).usingRecursiveComparison().isEqualTo(new GraphqlGithubClient(graphqlProvider, "url", repositoryAuthenticationToken));
    }
}
