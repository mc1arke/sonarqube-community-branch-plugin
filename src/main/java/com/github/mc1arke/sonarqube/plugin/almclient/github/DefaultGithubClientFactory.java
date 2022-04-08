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
package com.github.mc1arke.sonarqube.plugin.almclient.github;

import com.github.mc1arke.sonarqube.plugin.InvalidConfigurationException;
import com.github.mc1arke.sonarqube.plugin.almclient.github.v4.GraphqlGithubClient;
import com.github.mc1arke.sonarqube.plugin.almclient.github.v4.GraphqlProvider;
import org.sonar.api.ce.ComputeEngineSide;
import org.sonar.api.config.internal.Settings;
import org.sonar.api.server.ServerSide;
import org.sonar.db.alm.setting.AlmSettingDto;
import org.sonar.db.alm.setting.ProjectAlmSettingDto;

import java.io.IOException;
import java.util.Optional;

@ServerSide
@ComputeEngineSide
public class DefaultGithubClientFactory implements GithubClientFactory {

    private final GithubApplicationAuthenticationProvider githubApplicationAuthenticationProvider;
    private final Settings settings;
    private final GraphqlProvider graphqlProvider;

    public DefaultGithubClientFactory(GithubApplicationAuthenticationProvider githubApplicationAuthenticationProvider, Settings settings, GraphqlProvider graphqlProvider) {
        this.githubApplicationAuthenticationProvider = githubApplicationAuthenticationProvider;
        this.settings = settings;
        this.graphqlProvider = graphqlProvider;
    }

    @Override
    public GithubClient createClient(ProjectAlmSettingDto projectAlmSettingDto, AlmSettingDto almSettingDto) {
        String apiUrl = Optional.ofNullable(almSettingDto.getUrl()).orElseThrow(() -> new InvalidConfigurationException(InvalidConfigurationException.Scope.GLOBAL, "No URL has been set for Github connections"));
        String apiPrivateKey = Optional.ofNullable(almSettingDto.getDecryptedPrivateKey(settings.getEncryption())).orElseThrow(() -> new InvalidConfigurationException(InvalidConfigurationException.Scope.GLOBAL, "No private key has been set for Github connections"));
        String projectPath = Optional.ofNullable(projectAlmSettingDto.getAlmRepo()).orElseThrow(() -> new InvalidConfigurationException(InvalidConfigurationException.Scope.PROJECT, "No repository name has been set for Github connections"));
        String appId = Optional.ofNullable(almSettingDto.getAppId()).orElseThrow(() -> new InvalidConfigurationException(InvalidConfigurationException.Scope.GLOBAL, "No App ID has been set for Github connections"));

        try {
            RepositoryAuthenticationToken repositoryAuthenticationToken =
                    githubApplicationAuthenticationProvider.getInstallationToken(apiUrl, appId, apiPrivateKey, projectPath);

            return new GraphqlGithubClient(graphqlProvider, apiUrl, repositoryAuthenticationToken);
        } catch (IOException ex) {
            throw new InvalidConfigurationException(InvalidConfigurationException.Scope.PROJECT, "Could not create Github client - " + ex.getMessage(), ex);
        }

    }
}
