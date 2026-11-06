/*
 * Copyright (C) 2021-2026 Michael Clarke, Sebastiaan Speck
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

import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.HttpClientBuilder;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.sonar.api.config.internal.Encryption;
import org.sonar.api.config.internal.Settings;
import org.sonar.db.alm.setting.AlmSettingDto;
import org.sonar.db.alm.setting.ProjectAlmSettingDto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DefaultAzureDevopsClientFactoryTest {

    private static final int TIMEOUT_MILLISECONDS = 30_000;

    @Test
    void shouldUseDefaultTimeoutsForAllHttpActivity() {
        Settings settings = mock();
        Encryption encryption = mock();
        when(settings.getEncryption()).thenReturn(encryption);
        AlmSettingDto almSettingDto = mock();
        when(almSettingDto.getUrl()).thenReturn("http://url.test/api");
        when(almSettingDto.getDecryptedPersonalAccessToken(encryption)).thenReturn("token");
        HttpClientBuilder builder = mock();
        when(builder.useSystemProperties()).thenReturn(builder);
        when(builder.setDefaultRequestConfig(any())).thenReturn(builder);

        try (MockedStatic<HttpClientBuilder> mockedBuilder = mockStatic(HttpClientBuilder.class)) {
            mockedBuilder.when(HttpClientBuilder::create).thenReturn(builder);

            new DefaultAzureDevopsClientFactory(settings).createClient(mock(ProjectAlmSettingDto.class), almSettingDto);

            verify(builder).useSystemProperties();
            ArgumentCaptor<RequestConfig> configCaptor = ArgumentCaptor.captor();
            verify(builder).setDefaultRequestConfig(configCaptor.capture());
            assertThat(configCaptor.getValue().getConnectionRequestTimeout()).isEqualTo(TIMEOUT_MILLISECONDS);
            assertThat(configCaptor.getValue().getConnectTimeout()).isEqualTo(TIMEOUT_MILLISECONDS);
            assertThat(configCaptor.getValue().getSocketTimeout()).isEqualTo(TIMEOUT_MILLISECONDS);
        }
    }
}
