/*
 * Copyright (C) 2020-2023 Michael Clarke
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
package com.github.mc1arke.sonarqube.plugin.almclient.github.v3;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.mc1arke.sonarqube.plugin.InvalidConfigurationException;
import com.github.mc1arke.sonarqube.plugin.almclient.LinkHeaderReader;
import com.github.mc1arke.sonarqube.plugin.almclient.github.RepositoryAuthenticationToken;
import com.github.mc1arke.sonarqube.plugin.almclient.github.v3.model.AppInstallation;
import com.github.mc1arke.sonarqube.plugin.almclient.github.v3.model.AppToken;
import com.github.mc1arke.sonarqube.plugin.almclient.github.v3.model.InstallationRepositories;
import com.github.mc1arke.sonarqube.plugin.almclient.github.v3.model.Owner;
import com.github.mc1arke.sonarqube.plugin.almclient.github.v3.model.Repository;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RestApplicationAuthenticationProviderTest {

    @Test
    void shouldReturnTokenForPaginatedInstallationsAndTokens() throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        UrlConnectionProvider urlConnectionProvider = mock(UrlConnectionProvider.class);
        Clock clock = Clock.fixed(Instant.ofEpochMilli(123456789L), ZoneId.of("UTC"));
        LinkHeaderReader linkHeaderReader = mock(LinkHeaderReader.class);

        when(linkHeaderReader.findNextLink(any())).thenAnswer(i -> Optional.ofNullable(i.getArgument(0)));

        String apiUrl = "https://api.url/api/v3";
        String appId = "appID";
        String apiPrivateKey;
        try (InputStream inputStream = Optional.ofNullable(getClass().getResourceAsStream("/rsa-private-key.pem")).orElseThrow()) {
            apiPrivateKey = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
        }
        String projectPath = "path/repo-49.3";

        List<HttpURLConnection> appPageUrlConnections = new ArrayList<>();
        List<HttpURLConnection> appTokenConnections = new ArrayList<>();
        List<HttpURLConnection> appRepositoryConnections = new ArrayList<>();
        for (int appPage = 0; appPage < 5; appPage++) {
            List<AppInstallation> appPageContents = new ArrayList<>();
            for (int i = 0; i < 10; i++) {
                int itemNumber = (appPage * 10) + i;
                appPageContents.add(new AppInstallation("http://repository.url/item-" + itemNumber, "http://acccess-token.url/item-" + itemNumber));

                HttpURLConnection appTokenConnection = mock(HttpURLConnection.class);
                when(appTokenConnection.getInputStream()).thenReturn(new ByteArrayInputStream(objectMapper.writeValueAsBytes(new AppToken("token-" + itemNumber))));
                when(urlConnectionProvider.createUrlConnection("http://acccess-token.url/item-" + itemNumber)).thenReturn(appTokenConnection);
                appTokenConnections.add(appTokenConnection);

                List<Repository> repositories = new ArrayList<>();
                for (int x = 0; x < 5; x++) {
                    repositories.add(new Repository("nodeId", "path/repo-" + itemNumber + "." + x, "url", "repo-" + itemNumber + "." + x, new Owner("login")));
                }

                HttpURLConnection repositoryConnection = mock(HttpURLConnection.class);
                when(repositoryConnection.getInputStream()).thenAnswer(invocation -> new ByteArrayInputStream(objectMapper.writeValueAsBytes(new InstallationRepositories(repositories.toArray(new Repository[0])))));
                when(urlConnectionProvider.createUrlConnection("http://repository.url/item-" + itemNumber)).thenReturn(repositoryConnection);
                if (i == 1 && appPage == 1) {
                    when(repositoryConnection.getHeaderField("Link")).thenReturn("http://repository.url/item-" + (itemNumber + 1));
                }
                appRepositoryConnections.add(repositoryConnection);
            }
            HttpURLConnection appPageUrlConnection = mock(HttpURLConnection.class);
            when(appPageUrlConnection.getInputStream()).thenReturn(new ByteArrayInputStream(objectMapper.writeValueAsBytes(appPageContents)));
            if (appPage < 4) {
                when(appPageUrlConnection.getHeaderField("Link")).thenReturn(apiUrl + "/app/installations?page=" + (appPage + 1));
            }
            appPageUrlConnections.add(appPageUrlConnection);
            if (appPage == 0) {
                when(urlConnectionProvider.createUrlConnection(apiUrl + "/app/installations")).thenReturn(appPageUrlConnection);
            } else {
                when(urlConnectionProvider.createUrlConnection(apiUrl + "/app/installations?page=" + appPage)).thenReturn(appPageUrlConnection);
            }
        }

        RepositoryAuthenticationToken expected = new RepositoryAuthenticationToken("nodeId", "token-49", "url", "repo-49.3", "login");

        RestApplicationAuthenticationProvider restApplicationAuthenticationProvider = new RestApplicationAuthenticationProvider(clock, linkHeaderReader, urlConnectionProvider);

        RepositoryAuthenticationToken repositoryAuthenticationToken = restApplicationAuthenticationProvider.getInstallationToken("https://api.url/api/", appId, apiPrivateKey, projectPath);
        assertThat(repositoryAuthenticationToken).usingRecursiveComparison().isEqualTo(expected);


        for (URLConnection installationsUrlConnection : appPageUrlConnections) {
            ArgumentCaptor<String> requestPropertyArgumentCaptor = ArgumentCaptor.forClass(String.class);
            verify(installationsUrlConnection, times(2))
                    .setRequestProperty(requestPropertyArgumentCaptor.capture(), requestPropertyArgumentCaptor.capture());
            assertThat(requestPropertyArgumentCaptor.getAllValues()).isEqualTo(Arrays.asList("Accept", "application/vnd.github.machine-man-preview+json", "Authorization",
                            "Bearer eyJhbGciOiJSUzI1NiJ9.eyJpYXQiOjEyMzQ0NiwiZXhwIjoxMjM1NjYsImlzcyI6ImFwcElEIn0.yMvAoUmmAHli-Mc-RidLbqlX2Cvc2RwPBwkgY6n1R2ZkV-IaY8uBO4s7pp0-3hcJvY4F7-UGnAi1dteGOODY8cOmx86DsSASJIHJ3wxaRxyLGOq2Z8A1KSWZj-F8O6wFf5pm2xzumm0gSSwdd3gQR0FiSn2TIHemjyoieNJfzvG2kgtHPBNIVaJcS8LqkVYBlvAujnAt1nQ1hIAbeQJyEmyVyb_NRMPQZZioBraobTlWdPWdnTQoNTWjmjcopIbUFw8s21uhMcDpA_6lS1iAZcoZKcpzMqsItEvQaiwYQWRccfZT69M_zWaVRjw2-eKsTuFXzumVyq3MnAoxy6R2Xw"));
        }

        for (HttpURLConnection accessTokensUrlConnection : appTokenConnections) {
            ArgumentCaptor<String> requestPropertyArgumentCaptor = ArgumentCaptor.forClass(String.class);
            verify(accessTokensUrlConnection, times(2))
                    .setRequestProperty(requestPropertyArgumentCaptor.capture(), requestPropertyArgumentCaptor.capture());
            verify(accessTokensUrlConnection).setRequestMethod("POST");
            assertThat(requestPropertyArgumentCaptor.getAllValues()).isEqualTo(Arrays.asList("Accept", "application/vnd.github.machine-man-preview+json",
                    "Authorization", "Bearer eyJhbGciOiJSUzI1NiJ9.eyJpYXQiOjEyMzQ0NiwiZXhwIjoxMjM1NjYsImlzcyI6ImFwcElEIn0.yMvAoUmmAHli-Mc-RidLbqlX2Cvc2RwPBwkgY6n1R2ZkV-IaY8uBO4s7pp0-3hcJvY4F7-UGnAi1dteGOODY8cOmx86DsSASJIHJ3wxaRxyLGOq2Z8A1KSWZj-F8O6wFf5pm2xzumm0gSSwdd3gQR0FiSn2TIHemjyoieNJfzvG2kgtHPBNIVaJcS8LqkVYBlvAujnAt1nQ1hIAbeQJyEmyVyb_NRMPQZZioBraobTlWdPWdnTQoNTWjmjcopIbUFw8s21uhMcDpA_6lS1iAZcoZKcpzMqsItEvQaiwYQWRccfZT69M_zWaVRjw2-eKsTuFXzumVyq3MnAoxy6R2Xw"));

        }

        for (int i = 0; i < appRepositoryConnections.size(); i++) {
            HttpURLConnection appRepositoryConnection = appRepositoryConnections.get(i);
            ArgumentCaptor<String> requestPropertyArgumentCaptor = ArgumentCaptor.forClass(String.class);
            if (i == 12) {
                verify(appRepositoryConnection, times(4))
                        .setRequestProperty(requestPropertyArgumentCaptor.capture(), requestPropertyArgumentCaptor.capture());
                assertThat(requestPropertyArgumentCaptor.getAllValues()).isEqualTo(Arrays.asList("Accept", "application/vnd.github.machine-man-preview+json",
                        "Authorization", "Bearer token-11",
                        "Accept", "application/vnd.github.machine-man-preview+json",
                        "Authorization", "Bearer token-12"));
            } else {
                verify(appRepositoryConnection, times(2))
                        .setRequestProperty(requestPropertyArgumentCaptor.capture(), requestPropertyArgumentCaptor.capture());
                assertThat(requestPropertyArgumentCaptor.getAllValues()).isEqualTo(Arrays.asList("Accept", "application/vnd.github.machine-man-preview+json",
                        "Authorization", "Bearer token-" + i));
            }

        }
    }

    @Test
    void shouldThrowExceptionIfNotTokensMatch() throws IOException {
        UrlConnectionProvider urlProvider = mock(UrlConnectionProvider.class);
        Clock clock = Clock.fixed(Instant.ofEpochMilli(123456789L), ZoneId.of("UTC"));

        String expectedAuthenticationToken = "expected authentication token";
        String projectPath = "project path";
        String expectedRepositoryId = "expected repository Id";

        URLConnection installationsUrlConnection = mock(URLConnection.class);
        doReturn(new ByteArrayInputStream(
                "[{\"repositories_url\": \"repositories_url\", \"access_tokens_url\": \"tokens_url\"}]"
                        .getBytes(StandardCharsets.UTF_8))).when(installationsUrlConnection).getInputStream();

        HttpURLConnection accessTokensUrlConnection = mock(HttpURLConnection.class);
        doReturn(new ByteArrayInputStream(
                ("{\"token\": \"" + expectedAuthenticationToken + "\"}").getBytes(StandardCharsets.UTF_8)))
                .when(accessTokensUrlConnection).getInputStream();
        doReturn(accessTokensUrlConnection).when(urlProvider).createUrlConnection("tokens_url");


        HttpURLConnection repositoriesUrlConnection = mock(HttpURLConnection.class);
        doReturn(new ByteArrayInputStream(("{\"repositories\": [{\"node_id\": \"" + expectedRepositoryId +
                                           "\", \"full_name\": \"different_path\"}]}")
                                                  .getBytes(StandardCharsets.UTF_8))).when(repositoriesUrlConnection)
                .getInputStream();
        doReturn(repositoriesUrlConnection).when(urlProvider).createUrlConnection("repositories_url");

        String apiUrl = "apiUrl";
        doReturn(installationsUrlConnection).when(urlProvider).createUrlConnection(apiUrl + "/app/installations");

        String appId = "appID";

        String apiPrivateKey;
        try (InputStream inputStream = Optional.ofNullable(getClass().getResourceAsStream("/rsa-private-key.pem")).orElseThrow()) {
            apiPrivateKey = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
        }

        RestApplicationAuthenticationProvider testCase = new RestApplicationAuthenticationProvider(clock, h -> Optional.empty(), urlProvider);
        assertThatThrownBy(() -> testCase.getInstallationToken(apiUrl, appId, apiPrivateKey, projectPath)).hasMessage(
                "No token could be found with access to the requested repository using the given application ID and key")
                .isExactlyInstanceOf(InvalidConfigurationException.class);

        ArgumentCaptor<String> requestPropertyArgumentCaptor = ArgumentCaptor.forClass(String.class);
        verify(installationsUrlConnection, times(2))
                .setRequestProperty(requestPropertyArgumentCaptor.capture(), requestPropertyArgumentCaptor.capture());
        assertThat(requestPropertyArgumentCaptor.getAllValues()).isEqualTo(Arrays.asList("Accept", "application/vnd.github.machine-man-preview+json", "Authorization",
                                   "Bearer eyJhbGciOiJSUzI1NiJ9.eyJpYXQiOjEyMzQ0NiwiZXhwIjoxMjM1NjYsImlzcyI6ImFwcElEIn0.yMvAoUmmAHli-Mc-RidLbqlX2Cvc2RwPBwkgY6n1R2ZkV-IaY8uBO4s7pp0-3hcJvY4F7-UGnAi1dteGOODY8cOmx86DsSASJIHJ3wxaRxyLGOq2Z8A1KSWZj-F8O6wFf5pm2xzumm0gSSwdd3gQR0FiSn2TIHemjyoieNJfzvG2kgtHPBNIVaJcS8LqkVYBlvAujnAt1nQ1hIAbeQJyEmyVyb_NRMPQZZioBraobTlWdPWdnTQoNTWjmjcopIbUFw8s21uhMcDpA_6lS1iAZcoZKcpzMqsItEvQaiwYQWRccfZT69M_zWaVRjw2-eKsTuFXzumVyq3MnAoxy6R2Xw"));

        requestPropertyArgumentCaptor = ArgumentCaptor.forClass(String.class);
        verify(accessTokensUrlConnection, times(2))
                .setRequestProperty(requestPropertyArgumentCaptor.capture(), requestPropertyArgumentCaptor.capture());
        verify(accessTokensUrlConnection).setRequestMethod("POST");
        assertThat(requestPropertyArgumentCaptor.getAllValues()).isEqualTo(Arrays.asList("Accept", "application/vnd.github.machine-man-preview+json", "Authorization",
                                   "Bearer eyJhbGciOiJSUzI1NiJ9.eyJpYXQiOjEyMzQ0NiwiZXhwIjoxMjM1NjYsImlzcyI6ImFwcElEIn0.yMvAoUmmAHli-Mc-RidLbqlX2Cvc2RwPBwkgY6n1R2ZkV-IaY8uBO4s7pp0-3hcJvY4F7-UGnAi1dteGOODY8cOmx86DsSASJIHJ3wxaRxyLGOq2Z8A1KSWZj-F8O6wFf5pm2xzumm0gSSwdd3gQR0FiSn2TIHemjyoieNJfzvG2kgtHPBNIVaJcS8LqkVYBlvAujnAt1nQ1hIAbeQJyEmyVyb_NRMPQZZioBraobTlWdPWdnTQoNTWjmjcopIbUFw8s21uhMcDpA_6lS1iAZcoZKcpzMqsItEvQaiwYQWRccfZT69M_zWaVRjw2-eKsTuFXzumVyq3MnAoxy6R2Xw"));

        requestPropertyArgumentCaptor = ArgumentCaptor.forClass(String.class);
        verify(repositoriesUrlConnection, times(2))
                .setRequestProperty(requestPropertyArgumentCaptor.capture(), requestPropertyArgumentCaptor.capture());
        verify(repositoriesUrlConnection).setRequestMethod("GET");
        assertThat(requestPropertyArgumentCaptor.getAllValues()).isEqualTo(Arrays.asList("Accept", "application/vnd.github.machine-man-preview+json", "Authorization",
                                   "Bearer " + expectedAuthenticationToken));

    }
}
