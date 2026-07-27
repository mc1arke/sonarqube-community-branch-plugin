/*
 * Copyright (C) 2020-2026 Marvin Wichmann, Michael Clarke
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

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.ResponseBody;
import okhttp3.logging.HttpLoggingInterceptor;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.sonar.api.config.internal.Encryption;
import org.sonar.api.config.internal.Settings;
import org.sonar.db.alm.setting.ALM;
import org.sonar.db.alm.setting.AlmSettingDto;
import org.sonar.db.alm.setting.ProjectAlmSettingDto;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DefaultBitbucketClientFactoryUnitTest {

    @Test
    void testCreateClientIsCloudIfCloudConfig() throws IOException {
        // given
        AlmSettingDto almSettingDto = new AlmSettingDto().setAlm(ALM.BITBUCKET_CLOUD)
                .setClientId("clientId")
                .setAppId("appId")
                .setClientSecret("clientSecret");
        ProjectAlmSettingDto projectAlmSettingDto = new ProjectAlmSettingDto()
                .setAlmRepo("almRepo");

        OkHttpClient.Builder mainClientBuilder = mock(Mockito.RETURNS_DEEP_STUBS);
        OkHttpClient.Builder tokenClientBuilder = mock(Mockito.RETURNS_DEEP_STUBS);

        when(mainClientBuilder.addInterceptor(any())).thenReturn(mainClientBuilder);
        when(tokenClientBuilder.addInterceptor(any())).thenReturn(tokenClientBuilder);

        ResponseBody responseBody = mock();
        when(responseBody.string()).thenReturn("{\"access_token\": \"dummy\"}");
        when(tokenClientBuilder.build().newCall(any()).execute().body()).thenReturn(responseBody);

        Settings settings = mock();
        Encryption encryption = mock();

        // when
        when(settings.getEncryption()).thenReturn(encryption);
        HttpClientBuilderFactory httpClientBuilderFactory = mock();
        when(httpClientBuilderFactory.createClientBuilder())
                .thenReturn(mainClientBuilder)
                .thenReturn(tokenClientBuilder);
        BitbucketClient client = new DefaultBitbucketClientFactory(settings, httpClientBuilderFactory).createClient(projectAlmSettingDto, almSettingDto);

        // then
        assertThat(client).isInstanceOf(BitbucketCloudClient.class);

        ArgumentCaptor<Interceptor> mainInterceptorsCaptor = ArgumentCaptor.captor();
        verify(mainClientBuilder, times(2)).addInterceptor(mainInterceptorsCaptor.capture());

        List<Interceptor> mainInterceptors = mainInterceptorsCaptor.getAllValues();
        HttpLoggingInterceptor mainLoggingInterceptor = (HttpLoggingInterceptor) mainInterceptors.stream()
                .filter(HttpLoggingInterceptor.class::isInstance)
                .findFirst()
                .orElseThrow(() -> new AssertionError("HttpLoggingInterceptor missing from main client builder"));

        assertThat(mainLoggingInterceptor.getLevel()).isEqualTo(HttpLoggingInterceptor.Level.BODY);

        ArgumentCaptor<Interceptor> tokenInterceptorsCaptor = ArgumentCaptor.captor();
        verify(tokenClientBuilder).addInterceptor(tokenInterceptorsCaptor.capture());

        HttpLoggingInterceptor tokenLoggingInterceptor = (HttpLoggingInterceptor) tokenInterceptorsCaptor.getValue();
        assertThat(tokenLoggingInterceptor.getLevel()).isEqualTo(HttpLoggingInterceptor.Level.BASIC);

        Interceptor authInterceptor = mainInterceptors.stream()
                .filter(i -> !(i instanceof HttpLoggingInterceptor))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Authentication interceptor missing from main client"));

        Interceptor.Chain chain = mock();
        Request request = mock();
        when(chain.request()).thenReturn(request);
        Request.Builder requestBuilder = mock();
        when(requestBuilder.addHeader(any(), any())).thenReturn(requestBuilder);
        when(request.newBuilder()).thenReturn(requestBuilder);

        Request boundRequest = mock();
        when(requestBuilder.build()).thenReturn(boundRequest);

        authInterceptor.intercept(chain);
        verify(requestBuilder).addHeader("Authorization", "Bearer dummy");
        verify(requestBuilder).addHeader("Accept", "application/json");
        verify(chain).proceed(boundRequest);
    }

    @Test
    void testCreateClientIfNotCloudConfig() {
        // given
        AlmSettingDto almSettingDto = new AlmSettingDto().setAlm(ALM.BITBUCKET)
                .setUrl("url")
                .setPersonalAccessToken("personalAccessToken");
        ProjectAlmSettingDto projectAlmSettingDto = new ProjectAlmSettingDto()
                .setAlmRepo("almRepo")
                .setAlmSlug("almSlug");

        Settings settings = mock();
        Encryption encryption = mock();

        // when
        when(settings.getEncryption()).thenReturn(encryption);
        HttpClientBuilderFactory httpClientBuilderFactory = mock();
        when(httpClientBuilderFactory.createClientBuilder()).then(i -> mock(OkHttpClient.Builder.class, Mockito.RETURNS_DEEP_STUBS));
        BitbucketClient client = new DefaultBitbucketClientFactory(settings, httpClientBuilderFactory).createClient(projectAlmSettingDto, almSettingDto);

        // then
        assertThat(client).isInstanceOf(BitbucketServerClient.class);
    }


}
