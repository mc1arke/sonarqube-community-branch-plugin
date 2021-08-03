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
package com.github.mc1arke.sonarqube.plugin.ce.pullrequest.bitbucket.client;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.bitbucket.client.model.cloud.BitbucketCloudConfiguration;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.bitbucket.client.model.server.BitbucketServerConfiguration;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.db.alm.setting.ALM;
import org.sonar.db.alm.setting.AlmSettingDto;
import org.sonar.db.alm.setting.ProjectAlmSettingDto;

import java.util.function.Supplier;

public class BitbucketClientFactory {

    private static final Logger LOGGER = Loggers.get(BitbucketClientFactory.class);

    private final Supplier<OkHttpClient.Builder> okHttpClientBuilderSupplier;

    public BitbucketClientFactory(Supplier<OkHttpClient.Builder> okHttpClientBuilderSupplier) {
        this.okHttpClientBuilderSupplier = okHttpClientBuilderSupplier;
    }

    public BitbucketClient createClient(AlmSettingDto almSettingDto, ProjectAlmSettingDto projectAlmSettingDto) {
        if (almSettingDto.getAlm() == ALM.BITBUCKET_CLOUD) {
            return new BitbucketCloudClient(new BitbucketCloudConfiguration(almSettingDto.getAppId(), projectAlmSettingDto.getAlmRepo(), almSettingDto.getClientId(), almSettingDto.getClientSecret()), createObjectMapper(), createBaseClientBuilder(okHttpClientBuilderSupplier));
        } else {
            return new BitbucketServerClient(new BitbucketServerConfiguration(projectAlmSettingDto.getAlmRepo(), projectAlmSettingDto.getAlmSlug(), almSettingDto.getUrl(), almSettingDto.getPersonalAccessToken()), createObjectMapper(), createBaseClientBuilder(okHttpClientBuilderSupplier));
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
