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
package com.github.mc1arke.sonarqube.plugin.almclient.gitlab;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.mc1arke.sonarqube.plugin.InvalidConfigurationException;

import org.apache.commons.lang.StringUtils;
import org.apache.http.impl.client.HttpClients;
import org.sonar.api.ce.ComputeEngineSide;
import org.sonar.api.config.internal.Settings;
import org.sonar.api.server.ServerSide;
import org.sonar.db.alm.setting.AlmSettingDto;
import org.sonar.db.alm.setting.ProjectAlmSettingDto;

import java.util.Optional;

@ServerSide
@ComputeEngineSide
public class DefaultGitlabClientFactory implements GitlabClientFactory {

    private final ObjectMapper objectMapper;
    private final LinkHeaderReader linkHeaderReader;
    private final Settings settings;

    public DefaultGitlabClientFactory(LinkHeaderReader linkHeaderReader, Settings settings) {
        super();
        this.linkHeaderReader = linkHeaderReader;
        this.settings = settings;
        this.objectMapper = new ObjectMapper()
                .configure(DeserializationFeature.ACCEPT_EMPTY_ARRAY_AS_NULL_OBJECT, true)
                .configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true)
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    @Override
    public GitlabClient createClient(ProjectAlmSettingDto projectAlmSettingDto, AlmSettingDto almSettingDto) {
        String apiURL = Optional.ofNullable(StringUtils.stripToNull(almSettingDto.getUrl()))
                .orElseThrow(() -> new InvalidConfigurationException(InvalidConfigurationException.Scope.GLOBAL, "ALM URL must be specified"));
        String apiToken = almSettingDto.getDecryptedPersonalAccessToken(settings.getEncryption());

        return new GitlabRestClient(apiURL, apiToken, linkHeaderReader, objectMapper, HttpClients::createSystem);
    }
}
