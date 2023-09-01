/*
 * Copyright (C) 2020-2022 Michael Clarke
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

import com.github.mc1arke.sonarqube.plugin.InvalidConfigurationException;
import com.github.mc1arke.sonarqube.plugin.almclient.LinkHeaderReader;
import com.github.mc1arke.sonarqube.plugin.almclient.github.RepositoryAuthenticationToken;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.github.mc1arke.sonarqube.plugin.almclient.github.v3.model.AppInstallation;
import java.util.List;
import org.apache.commons.io.IOUtils;
import org.junit.Test;
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
import java.util.Arrays;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class RestApplicationAuthenticationProviderTest {

    @Test
    public void testTokenRetrievedHappyPath() throws IOException {
        testTokenForUrl("apiUrl", "apiUrl/app/installations");
    }

    @Test
    public void testTokenRetrievedHappyPathApiPath() throws IOException {
        testTokenForUrl("apiUrl/api", "apiUrl/api/v3/app/installations");
    }

    @Test
    public void testTokenRetrievedHappyPathApiPathTrailingSlash() throws IOException {
        testTokenForUrl("apiUrl/api/", "apiUrl/api/v3/app/installations");
    }

    @Test
    public void testTokenRetrievedHappyPathV3Path() throws IOException {
        testTokenForUrl("apiUrl/api/v3", "apiUrl/api/v3/app/installations");
    }

    @Test
    public void testTokenRetrievedHappyPathV3PathTrailingSlash() throws IOException {
        testTokenForUrl("apiUrl/api/v3/", "apiUrl/api/v3/app/installations");
    }

    private void testTokenForUrl(String apiUrl, String fullUrl) throws IOException {
        UrlConnectionProvider urlProvider = mock(UrlConnectionProvider.class);
        Clock clock = Clock.fixed(Instant.ofEpochMilli(123456789L), ZoneId.of("UTC"));

        String expectedAuthenticationToken = "expected authentication token";
        String projectPath = "project path";
        String expectedRepositoryId = "expected repository Id";
        String expectedHtmlUrl = "http://url.for/users/repo";

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
        doReturn(new ByteArrayInputStream(
                ("{\"repositories\": [{\"node_id\": \"" + expectedRepositoryId + "\", \"full_name\": \"" + projectPath +
                 "\", \"html_url\": \"" + expectedHtmlUrl + "\", \"name\": \"project\", \"owner\": {\"login\": \"owner_name\"}}]}").getBytes(StandardCharsets.UTF_8))).when(repositoriesUrlConnection).getInputStream();
        doReturn(repositoriesUrlConnection).when(urlProvider).createUrlConnection("repositories_url");

        doReturn(installationsUrlConnection).when(urlProvider).createUrlConnection(fullUrl);

        String appId = "appID";

        String apiPrivateKey;
        try (InputStream inputStream = getClass().getResourceAsStream("/rsa-private-key.pem")) {
            apiPrivateKey = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
        }

        RestApplicationAuthenticationProvider testCase = new RestApplicationAuthenticationProvider(clock, h -> Optional.empty(), urlProvider);
        RepositoryAuthenticationToken result = testCase.getInstallationToken(apiUrl, appId, apiPrivateKey, projectPath);

        assertEquals(expectedAuthenticationToken, result.getAuthenticationToken());
        assertEquals(expectedRepositoryId, result.getRepositoryId());
        assertEquals(expectedHtmlUrl, result.getRepositoryUrl());

        ArgumentCaptor<String> requestPropertyArgumentCaptor = ArgumentCaptor.forClass(String.class);
        verify(installationsUrlConnection, times(2))
                .setRequestProperty(requestPropertyArgumentCaptor.capture(), requestPropertyArgumentCaptor.capture());
        assertEquals(Arrays.asList("Accept", "application/vnd.github.machine-man-preview+json", "Authorization",
                                   "Bearer eyJhbGciOiJSUzI1NiJ9.eyJpYXQiOjEyMzQ0NiwiZXhwIjoxMjM1NjYsImlzcyI6ImFwcElEIn0.yMvAoUmmAHli-Mc-RidLbqlX2Cvc2RwPBwkgY6n1R2ZkV-IaY8uBO4s7pp0-3hcJvY4F7-UGnAi1dteGOODY8cOmx86DsSASJIHJ3wxaRxyLGOq2Z8A1KSWZj-F8O6wFf5pm2xzumm0gSSwdd3gQR0FiSn2TIHemjyoieNJfzvG2kgtHPBNIVaJcS8LqkVYBlvAujnAt1nQ1hIAbeQJyEmyVyb_NRMPQZZioBraobTlWdPWdnTQoNTWjmjcopIbUFw8s21uhMcDpA_6lS1iAZcoZKcpzMqsItEvQaiwYQWRccfZT69M_zWaVRjw2-eKsTuFXzumVyq3MnAoxy6R2Xw"),
                     requestPropertyArgumentCaptor.getAllValues());

        requestPropertyArgumentCaptor = ArgumentCaptor.forClass(String.class);
        verify(accessTokensUrlConnection, times(2))
                .setRequestProperty(requestPropertyArgumentCaptor.capture(), requestPropertyArgumentCaptor.capture());
        verify(accessTokensUrlConnection).setRequestMethod("POST");
        assertEquals(Arrays.asList("Accept", "application/vnd.github.machine-man-preview+json", "Authorization",
                                   "Bearer eyJhbGciOiJSUzI1NiJ9.eyJpYXQiOjEyMzQ0NiwiZXhwIjoxMjM1NjYsImlzcyI6ImFwcElEIn0.yMvAoUmmAHli-Mc-RidLbqlX2Cvc2RwPBwkgY6n1R2ZkV-IaY8uBO4s7pp0-3hcJvY4F7-UGnAi1dteGOODY8cOmx86DsSASJIHJ3wxaRxyLGOq2Z8A1KSWZj-F8O6wFf5pm2xzumm0gSSwdd3gQR0FiSn2TIHemjyoieNJfzvG2kgtHPBNIVaJcS8LqkVYBlvAujnAt1nQ1hIAbeQJyEmyVyb_NRMPQZZioBraobTlWdPWdnTQoNTWjmjcopIbUFw8s21uhMcDpA_6lS1iAZcoZKcpzMqsItEvQaiwYQWRccfZT69M_zWaVRjw2-eKsTuFXzumVyq3MnAoxy6R2Xw"),
                     requestPropertyArgumentCaptor.getAllValues());

        requestPropertyArgumentCaptor = ArgumentCaptor.forClass(String.class);
        verify(repositoriesUrlConnection, times(2))
                .setRequestProperty(requestPropertyArgumentCaptor.capture(), requestPropertyArgumentCaptor.capture());
        verify(repositoriesUrlConnection).setRequestMethod("GET");
        assertEquals(Arrays.asList("Accept", "application/vnd.github.machine-man-preview+json", "Authorization",
                                   "Bearer " + expectedAuthenticationToken),
                     requestPropertyArgumentCaptor.getAllValues());
    }

    @Test
    public void testAppInstallationsPagination() throws IOException {

        UrlConnectionProvider urlProvider = mock(UrlConnectionProvider.class);
        Clock clock = Clock.fixed(Instant.ofEpochMilli(123456789L), ZoneId.of("UTC"));

        String apiUrl = "apiUrl";

        int pages=4;

        for(int i=1; i<=pages;i++) {
            HttpURLConnection installationsUrlConnection = mock(HttpURLConnection.class);
            doReturn(installationsUrlConnection).when(urlProvider).createUrlConnection(eq(apiUrl + "/app/installations?page=" + i));
            when(installationsUrlConnection.getInputStream()).thenReturn(new ByteArrayInputStream(
                "[{\"repositories_url\": \"repositories_url\", \"access_tokens_url\": \"tokens_url\"}]"
                    .getBytes(StandardCharsets.UTF_8)));
            when(installationsUrlConnection.getHeaderField("Link")).thenReturn(i == pages ? null:  apiUrl + "/app/installations?page=" + (i+1) );
        }

        RestApplicationAuthenticationProvider testCase = new RestApplicationAuthenticationProvider(clock, Optional::ofNullable, urlProvider);
        ObjectMapper objectMapper = new ObjectMapper().disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        List<AppInstallation> token = testCase.getAppInstallations(objectMapper, apiUrl + "/app/installations?page=1", "token");
        assertThat(token).hasSize(pages);

    }

    @Test
    public void testTokenRetrievedPaginatedHappyPath() throws IOException {
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


        for (int i = 0; i < 2; i ++) {
            HttpURLConnection repositoriesUrlConnection = mock(HttpURLConnection.class);
            doReturn(new ByteArrayInputStream(
                    ("{\"repositories\": [{\"node_id\": \"" + expectedRepositoryId + (i == 0 ? "a" : "") + "\", \"full_name\": \"" +
                     projectPath + (i == 0 ? "a" : "") + "\", \"name\": \"name\", \"owner\": {\"login\": \"login\"}}]}").getBytes(StandardCharsets.UTF_8))).when(repositoriesUrlConnection).getInputStream();

            doReturn(i == 0 ? "a" : null).when(repositoriesUrlConnection).getHeaderField("Link");
            doReturn(repositoriesUrlConnection).when(urlProvider).createUrlConnection(i == 0 ? "repositories_url" : "https://dummy.url/path?param=dummy&page=" + (i + 1));
        }


        String apiUrl = "apiUrl";
        doReturn(installationsUrlConnection).when(urlProvider).createUrlConnection(apiUrl + "/app/installations");

        String appId = "appID";

        String apiPrivateKey;
        try (InputStream inputStream = getClass().getResourceAsStream("/rsa-private-key.pem")) {
            apiPrivateKey = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
        }

        LinkHeaderReader linkHeaderReader = mock(LinkHeaderReader.class);
        doReturn(Optional.of("https://dummy.url/path?param=dummy&page=2")).when(linkHeaderReader).findNextLink("a");
        doReturn(Optional.empty()).when(linkHeaderReader).findNextLink(isNull());

        RestApplicationAuthenticationProvider testCase = new RestApplicationAuthenticationProvider(clock, linkHeaderReader, urlProvider);
        RepositoryAuthenticationToken result = testCase.getInstallationToken(apiUrl, appId, apiPrivateKey, projectPath);

        assertEquals(expectedAuthenticationToken, result.getAuthenticationToken());
        assertEquals(expectedRepositoryId, result.getRepositoryId());

        ArgumentCaptor<String> requestPropertyArgumentCaptor = ArgumentCaptor.forClass(String.class);
        verify(installationsUrlConnection, times(2))
                .setRequestProperty(requestPropertyArgumentCaptor.capture(), requestPropertyArgumentCaptor.capture());
        assertEquals(Arrays.asList("Accept", "application/vnd.github.machine-man-preview+json", "Authorization",
                                   "Bearer eyJhbGciOiJSUzI1NiJ9.eyJpYXQiOjEyMzQ0NiwiZXhwIjoxMjM1NjYsImlzcyI6ImFwcElEIn0.yMvAoUmmAHli-Mc-RidLbqlX2Cvc2RwPBwkgY6n1R2ZkV-IaY8uBO4s7pp0-3hcJvY4F7-UGnAi1dteGOODY8cOmx86DsSASJIHJ3wxaRxyLGOq2Z8A1KSWZj-F8O6wFf5pm2xzumm0gSSwdd3gQR0FiSn2TIHemjyoieNJfzvG2kgtHPBNIVaJcS8LqkVYBlvAujnAt1nQ1hIAbeQJyEmyVyb_NRMPQZZioBraobTlWdPWdnTQoNTWjmjcopIbUFw8s21uhMcDpA_6lS1iAZcoZKcpzMqsItEvQaiwYQWRccfZT69M_zWaVRjw2-eKsTuFXzumVyq3MnAoxy6R2Xw"),
                     requestPropertyArgumentCaptor.getAllValues());

        requestPropertyArgumentCaptor = ArgumentCaptor.forClass(String.class);
        verify(accessTokensUrlConnection, times(2))
                .setRequestProperty(requestPropertyArgumentCaptor.capture(), requestPropertyArgumentCaptor.capture());
        verify(accessTokensUrlConnection).setRequestMethod("POST");
        assertEquals(Arrays.asList("Accept", "application/vnd.github.machine-man-preview+json", "Authorization",
                                   "Bearer eyJhbGciOiJSUzI1NiJ9.eyJpYXQiOjEyMzQ0NiwiZXhwIjoxMjM1NjYsImlzcyI6ImFwcElEIn0.yMvAoUmmAHli-Mc-RidLbqlX2Cvc2RwPBwkgY6n1R2ZkV-IaY8uBO4s7pp0-3hcJvY4F7-UGnAi1dteGOODY8cOmx86DsSASJIHJ3wxaRxyLGOq2Z8A1KSWZj-F8O6wFf5pm2xzumm0gSSwdd3gQR0FiSn2TIHemjyoieNJfzvG2kgtHPBNIVaJcS8LqkVYBlvAujnAt1nQ1hIAbeQJyEmyVyb_NRMPQZZioBraobTlWdPWdnTQoNTWjmjcopIbUFw8s21uhMcDpA_6lS1iAZcoZKcpzMqsItEvQaiwYQWRccfZT69M_zWaVRjw2-eKsTuFXzumVyq3MnAoxy6R2Xw"),
                     requestPropertyArgumentCaptor.getAllValues());
    }

    @Test
    public void testExceptionOnNoMatchingToken() throws IOException {
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
        try (InputStream inputStream = getClass().getResourceAsStream("/rsa-private-key.pem")) {
            apiPrivateKey = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
        }

        RestApplicationAuthenticationProvider testCase = new RestApplicationAuthenticationProvider(clock, h -> Optional.empty(), urlProvider);
        assertThatThrownBy(() -> testCase.getInstallationToken(apiUrl, appId, apiPrivateKey, projectPath)).hasMessage(
                "No token could be found with access to the requested repository using the given application ID and key")
                .isExactlyInstanceOf(InvalidConfigurationException.class);

        ArgumentCaptor<String> requestPropertyArgumentCaptor = ArgumentCaptor.forClass(String.class);
        verify(installationsUrlConnection, times(2))
                .setRequestProperty(requestPropertyArgumentCaptor.capture(), requestPropertyArgumentCaptor.capture());
        assertEquals(Arrays.asList("Accept", "application/vnd.github.machine-man-preview+json", "Authorization",
                                   "Bearer eyJhbGciOiJSUzI1NiJ9.eyJpYXQiOjEyMzQ0NiwiZXhwIjoxMjM1NjYsImlzcyI6ImFwcElEIn0.yMvAoUmmAHli-Mc-RidLbqlX2Cvc2RwPBwkgY6n1R2ZkV-IaY8uBO4s7pp0-3hcJvY4F7-UGnAi1dteGOODY8cOmx86DsSASJIHJ3wxaRxyLGOq2Z8A1KSWZj-F8O6wFf5pm2xzumm0gSSwdd3gQR0FiSn2TIHemjyoieNJfzvG2kgtHPBNIVaJcS8LqkVYBlvAujnAt1nQ1hIAbeQJyEmyVyb_NRMPQZZioBraobTlWdPWdnTQoNTWjmjcopIbUFw8s21uhMcDpA_6lS1iAZcoZKcpzMqsItEvQaiwYQWRccfZT69M_zWaVRjw2-eKsTuFXzumVyq3MnAoxy6R2Xw"),
                     requestPropertyArgumentCaptor.getAllValues());

        requestPropertyArgumentCaptor = ArgumentCaptor.forClass(String.class);
        verify(accessTokensUrlConnection, times(2))
                .setRequestProperty(requestPropertyArgumentCaptor.capture(), requestPropertyArgumentCaptor.capture());
        verify(accessTokensUrlConnection).setRequestMethod("POST");
        assertEquals(Arrays.asList("Accept", "application/vnd.github.machine-man-preview+json", "Authorization",
                                   "Bearer eyJhbGciOiJSUzI1NiJ9.eyJpYXQiOjEyMzQ0NiwiZXhwIjoxMjM1NjYsImlzcyI6ImFwcElEIn0.yMvAoUmmAHli-Mc-RidLbqlX2Cvc2RwPBwkgY6n1R2ZkV-IaY8uBO4s7pp0-3hcJvY4F7-UGnAi1dteGOODY8cOmx86DsSASJIHJ3wxaRxyLGOq2Z8A1KSWZj-F8O6wFf5pm2xzumm0gSSwdd3gQR0FiSn2TIHemjyoieNJfzvG2kgtHPBNIVaJcS8LqkVYBlvAujnAt1nQ1hIAbeQJyEmyVyb_NRMPQZZioBraobTlWdPWdnTQoNTWjmjcopIbUFw8s21uhMcDpA_6lS1iAZcoZKcpzMqsItEvQaiwYQWRccfZT69M_zWaVRjw2-eKsTuFXzumVyq3MnAoxy6R2Xw"),
                     requestPropertyArgumentCaptor.getAllValues());

        requestPropertyArgumentCaptor = ArgumentCaptor.forClass(String.class);
        verify(repositoriesUrlConnection, times(2))
                .setRequestProperty(requestPropertyArgumentCaptor.capture(), requestPropertyArgumentCaptor.capture());
        verify(repositoriesUrlConnection).setRequestMethod("GET");
        assertEquals(Arrays.asList("Accept", "application/vnd.github.machine-man-preview+json", "Authorization",
                                   "Bearer " + expectedAuthenticationToken),
                     requestPropertyArgumentCaptor.getAllValues());

    }
}
