/*
 * Copyright (C) 2020-2024 Michael Clarke
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
package com.github.mc1arke.sonarqube.plugin.scanner;

import org.junit.jupiter.api.Test;
import org.sonar.api.utils.MessageException;
import org.sonar.scanner.bootstrap.RawScannerProperties;
import org.sonar.scanner.http.transport.HttpClient;
import org.sonar.scanner.http.transport.HttpResponse;
import org.sonar.scanner.protocol.GsonHelper;
import org.sonar.scanner.scan.branch.BranchInfo;
import org.sonar.scanner.scan.branch.BranchType;
import org.sonar.scanner.scan.branch.ProjectBranches;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CommunityRepositoryBranchesLoaderTest {

    private final HttpClient httpClient = mock();

    private static RawScannerProperties propertiesWithToken() {
        Map<String, String> properties = new HashMap<>();
        properties.put("sonar.host.url", "https://sonar.example.com");
        properties.put("sonar.token", "sometoken");
        return new RawScannerProperties(properties);
    }

    @Test
    void shouldReturnEmptyBranchesOnEmptyServerResponse() {
        HttpResponse mockResponse = mock();
        when(httpClient.execute(any())).thenReturn(mockResponse);
        when(mockResponse.code()).thenReturn(200);
        when(mockResponse.isSuccessful()).thenReturn(true);

        StringReader stringReader = new StringReader(
                GsonHelper.create().toJson(new CommunityProjectBranchesLoader.BranchesResponse(new ArrayList<>())));
        when(mockResponse.bodyReader()).thenReturn(stringReader);

        CommunityProjectBranchesLoader testCase = new CommunityProjectBranchesLoader(httpClient, propertiesWithToken());
        ProjectBranches response = testCase.load("projectKey");
        assertThat(response.isEmpty()).isTrue();
    }

    @Test
    void shouldReturnAllBranchesFromNonEmptyServerResponse() {
        HttpResponse mockResponse = mock();
        when(httpClient.execute(any())).thenReturn(mockResponse);
        when(mockResponse.code()).thenReturn(200);
        when(mockResponse.isSuccessful()).thenReturn(true);

        List<BranchInfo> infos = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            infos.add(new BranchInfo("key" + i, BranchType.BRANCH, i == 1, "target" + i));
        }

        StringReader stringReader = new StringReader(
                GsonHelper.create().toJson(new CommunityProjectBranchesLoader.BranchesResponse(infos)));
        when(mockResponse.bodyReader()).thenReturn(stringReader);

        CommunityProjectBranchesLoader testCase = new CommunityProjectBranchesLoader(httpClient, propertiesWithToken());
        ProjectBranches response = testCase.load("key");
        assertThat(response.isEmpty()).isFalse();
        for (BranchInfo info : infos) {
            BranchInfo responseInfo = response.get(info.name());
            assertThat(responseInfo).isNotNull();
            assertThat(responseInfo.branchTargetName()).isEqualTo(info.branchTargetName());
            assertThat(responseInfo.isMain()).isEqualTo(info.isMain());
            assertThat(responseInfo.name()).isEqualTo(info.name());
            assertThat(responseInfo.type()).isEqualTo(info.type());
        }
    }

    @Test
    void shouldThrowMessageExceptionOnIOException() {
        HttpResponse mockResponse = mock();
        when(httpClient.execute(any())).thenReturn(mockResponse);
        when(mockResponse.code()).thenReturn(200);
        when(mockResponse.isSuccessful()).thenReturn(true);

        Reader mockReader = new BufferedReader(new StringReader(
                GsonHelper.create().toJson(new CommunityProjectBranchesLoader.BranchesResponse(new ArrayList<>())))) {
            @Override
            public void close() throws IOException {
                throw new IOException("Dummy IO Exception");
            }
        };
        when(mockResponse.bodyReader()).thenReturn(mockReader);

        CommunityProjectBranchesLoader testCase = new CommunityProjectBranchesLoader(httpClient, propertiesWithToken());
        assertThatThrownBy(() -> testCase.load("project"))
            .isInstanceOf(MessageException.class)
            .hasMessage("Could not load branches from server")
            .hasCause(new IOException("Dummy IO Exception"));
    }

    @Test
    void shouldThrowExceptionOnNon404HttpResponse() {
        HttpResponse mockResponse = mock();
        when(httpClient.execute(any())).thenReturn(mockResponse);
        when(mockResponse.code()).thenReturn(500);
        when(mockResponse.isSuccessful()).thenReturn(false);

        CommunityProjectBranchesLoader testCase = new CommunityProjectBranchesLoader(httpClient, propertiesWithToken());
        assertThatThrownBy(() -> testCase.load("project"))
            .isInstanceOf(MessageException.class)
            .hasMessage("Could not load branches from server: HTTP 500");
    }

    @Test
    void shouldReturnEmptyListOn404HttpResponse() {
        HttpResponse mockResponse = mock();
        when(httpClient.execute(any())).thenReturn(mockResponse);
        when(mockResponse.code()).thenReturn(404);

        CommunityProjectBranchesLoader testCase = new CommunityProjectBranchesLoader(httpClient, propertiesWithToken());
        assertThat(testCase.load("project").isEmpty()).isTrue();
    }
}
