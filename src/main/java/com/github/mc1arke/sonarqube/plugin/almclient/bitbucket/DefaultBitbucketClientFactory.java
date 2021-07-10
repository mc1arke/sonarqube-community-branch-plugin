/*
 * Copyright (C) 2020-2021 Marvin Wichmann, Michael Clarke
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
package com.github.mc1arke.sonarqube.plugin.almclient.bitbucket;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.mc1arke.sonarqube.plugin.InvalidConfigurationException;
import com.github.mc1arke.sonarqube.plugin.almclient.bitbucket.model.cloud.BitbucketCloudConfiguration;
import com.github.mc1arke.sonarqube.plugin.almclient.bitbucket.model.server.BitbucketServerConfiguration;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import org.apache.commons.lang3.StringUtils;
import org.sonar.api.ce.ComputeEngineSide;
import org.sonar.api.server.ServerSide;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.db.alm.setting.ALM;
import org.sonar.db.alm.setting.AlmSettingDto;
import org.sonar.db.alm.setting.ProjectAlmSettingDto;

import java.util.Optional;
import java.util.function.Supplier;

@ServerSide
@ComputeEngineSide
public class DefaultBitbucketClientFactory implements BitbucketClientFactory {

    private static final Logger LOGGER = Loggers.get(DefaultBitbucketClientFactory.class);

    private final Supplier<OkHttpClient.Builder> okHttpClientBuilderSupplier;

    public DefaultBitbucketClientFactory() {
        this(OkHttpClient.Builder::new);
    }

    DefaultBitbucketClientFactory(Supplier<OkHttpClient.Builder> okHttpClientBuilderSupplier) {
        this.okHttpClientBuilderSupplier = okHttpClientBuilderSupplier;
    }

    @Override
    public BitbucketClient createClient(ProjectAlmSettingDto projectAlmSettingDto, AlmSettingDto almSettingDto) {
        String almRepo = Optional.ofNullable(StringUtils.trimToNull(projectAlmSettingDto.getAlmRepo()))
                .orElseThrow(() -> new InvalidConfigurationException(InvalidConfigurationException.Scope.PROJECT, "ALM Repo must be set in configuration"));
        if (almSettingDto.getAlm() == ALM.BITBUCKET_CLOUD) {
            String appId = Optional.ofNullable(StringUtils.trimToNull(almSettingDto.getAppId()))
                    .orElseThrow(() -> new InvalidConfigurationException(InvalidConfigurationException.Scope.GLOBAL, "App ID must be set in configuration"));
            String clientId = Optional.ofNullable(StringUtils.trimToNull(almSettingDto.getClientId()))
                    .orElseThrow(() -> new InvalidConfigurationException(InvalidConfigurationException.Scope.GLOBAL, "Client ID must be set in configuration"));
            String clientSecret = Optional.ofNullable(StringUtils.trimToNull(almSettingDto.getClientSecret()))
                    .orElseThrow(() -> new InvalidConfigurationException(InvalidConfigurationException.Scope.GLOBAL, "Client Secret must be set in configuration"));
            return new BitbucketCloudClient(new BitbucketCloudConfiguration(appId, almRepo, clientId, clientSecret), createObjectMapper(), createBaseClientBuilder(okHttpClientBuilderSupplier));
        } else {
            String almSlug = Optional.ofNullable(StringUtils.trimToNull(projectAlmSettingDto.getAlmSlug()))
                    .orElseThrow(() -> new InvalidConfigurationException(InvalidConfigurationException.Scope.PROJECT, "ALM slug must be set in configuration"));
            String url = Optional.ofNullable(StringUtils.trimToNull(almSettingDto.getUrl()))
                    .orElseThrow(() -> new InvalidConfigurationException(InvalidConfigurationException.Scope.GLOBAL, "URL must be set in configuration"));
            String personalAccessToken = Optional.ofNullable(StringUtils.trimToNull(almSettingDto.getPersonalAccessToken()))
                    .orElseThrow(() -> new InvalidConfigurationException(InvalidConfigurationException.Scope.PROJECT, "Personal access token must be set in configuration"));
            return new BitbucketServerClient(new BitbucketServerConfiguration(almRepo, almSlug, url, personalAccessToken), createObjectMapper(), createBaseClientBuilder(okHttpClientBuilderSupplier));
        }
    }

    private static ObjectMapper createObjectMapper() {
        return new ObjectMapper()
                .setSerializationInclusion(JsonInclude.Include.NON_NULL)
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    }

    private static OkHttpClient.Builder createBaseClientBuilder(Supplier<OkHttpClient.Builder> builderSupplier) {
        HttpLoggingInterceptor httpLoggingInterceptor = new HttpLoggingInterceptor(LOGGER::debug);
        httpLoggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
        return builderSupplier.get().addInterceptor(httpLoggingInterceptor);
    }
}
