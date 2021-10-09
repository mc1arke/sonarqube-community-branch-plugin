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
package com.github.mc1arke.sonarqube.plugin.almclient.azuredevops;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.impl.client.HttpClients;
import org.sonar.api.ce.ComputeEngineSide;
import org.sonar.api.config.internal.Settings;
import org.sonar.api.server.ServerSide;
import org.sonar.db.alm.setting.AlmSettingDto;
import org.sonar.db.alm.setting.ProjectAlmSettingDto;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Optional;

@ServerSide
@ComputeEngineSide
public class DefaultAzureDevopsClientFactory implements AzureDevopsClientFactory {

    private final ObjectMapper objectMapper;
    private final Settings settings;

    public DefaultAzureDevopsClientFactory(Settings settings) {
        this.settings = settings;
        objectMapper = new ObjectMapper()
                .configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true)
                .configure(DeserializationFeature.ACCEPT_EMPTY_ARRAY_AS_NULL_OBJECT, true)
                .configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true)
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .setSerializationInclusion(JsonInclude.Include.NON_NULL);
    }

    @Override
    public AzureDevopsClient createClient(ProjectAlmSettingDto projectAlmSettingDto, AlmSettingDto almSettingDto) {
        String apiUrl = Optional.ofNullable(almSettingDto.getUrl()).map(StringUtils::trimToNull).orElseThrow(() -> new IllegalStateException("ALM URL must be provided"));
        String accessToken = Optional.ofNullable(almSettingDto.getDecryptedPersonalAccessToken(settings.getEncryption())).map(StringUtils::trimToNull).orElseThrow(() -> new IllegalStateException("Personal Access Token must be provided"));
        return new AzureDevopsRestClient(apiUrl, Base64.getEncoder().encodeToString((":" + accessToken).getBytes(StandardCharsets.UTF_8)), objectMapper, HttpClients::createSystem);
    }
}
