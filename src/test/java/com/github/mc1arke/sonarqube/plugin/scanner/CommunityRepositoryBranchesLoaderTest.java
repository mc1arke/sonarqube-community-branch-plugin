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
import org.sonar.scanner.http.ScannerWsClient;
import org.sonar.scanner.protocol.GsonHelper;
import org.sonar.scanner.scan.branch.BranchInfo;
import org.sonar.scanner.scan.branch.BranchType;
import org.sonar.scanner.scan.branch.ProjectBranches;
import org.sonarqube.ws.client.HttpException;
import org.sonarqube.ws.client.WsResponse;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CommunityRepositoryBranchesLoaderTest {

    private final ScannerWsClient scannerWsClient = mock();

    @Test
    void shouldReturnEmptyBranchesOnEmptyServerResponse() {
        WsResponse mockResponse = mock();
        when(scannerWsClient.call(any())).thenReturn(mockResponse);

        StringReader stringReader = new StringReader(
                GsonHelper.create().toJson(new CommunityProjectBranchesLoader.BranchesResponse(new ArrayList<>())));
        when(mockResponse.contentReader()).thenReturn(stringReader);

        CommunityProjectBranchesLoader testCase = new CommunityProjectBranchesLoader(scannerWsClient);
        ProjectBranches response = testCase.load("projectKey");
        assertThat(response.isEmpty()).isTrue();
    }

    @Test
    void shouldReturnAllBranchesFromNonEmptyServerResponse() {
        WsResponse mockResponse = mock();
        when(scannerWsClient.call(any())).thenReturn(mockResponse);

        List<BranchInfo> infos = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            infos.add(new BranchInfo("key" + i, BranchType.BRANCH, i == 1, "target" + i));
        }

        StringReader stringReader = new StringReader(
                GsonHelper.create().toJson(new CommunityProjectBranchesLoader.BranchesResponse(infos)));
        when(mockResponse.contentReader()).thenReturn(stringReader);

        CommunityProjectBranchesLoader testCase = new CommunityProjectBranchesLoader(scannerWsClient);
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
        WsResponse mockResponse = mock();
        when(scannerWsClient.call(any())).thenReturn(mockResponse);

        Reader mockReader = new BufferedReader(new StringReader(
                GsonHelper.create().toJson(new CommunityProjectBranchesLoader.BranchesResponse(new ArrayList<>())))) {
            @Override
            public void close() throws IOException {
                throw new IOException("Dummy IO Exception");
            }
        };
        when(mockResponse.contentReader()).thenReturn(mockReader);

        CommunityProjectBranchesLoader testCase = new CommunityProjectBranchesLoader(scannerWsClient);
        assertThatThrownBy(() -> testCase.load("project"))
            .isInstanceOf(MessageException.class)
            .hasMessage("Could not load branches from server")
            .hasCause(new IOException("Dummy IO Exception"));
    }


    @Test
    void shouldThrowExceptionOnNon404HttpResponse() {
        WsResponse mockResponse = mock();
        when(scannerWsClient.call(any())).thenReturn(mockResponse);

        Reader mockReader = new BufferedReader(new StringReader(
                GsonHelper.create().toJson(new CommunityProjectBranchesLoader.BranchesResponse(new ArrayList<>())))) {
            @Override
            public void close() {
                throw new HttpException("url", 12, "content");
            }
        };
        when(mockResponse.contentReader()).thenReturn(mockReader);

        CommunityProjectBranchesLoader testCase = new CommunityProjectBranchesLoader(scannerWsClient);
        assertThatThrownBy(() ->testCase.load("project"))
            .isInstanceOf(MessageException.class)
            .hasMessage("Could not load branches from server")
            .hasCause(new HttpException("url", 12, "content"));
    }


    @Test
    void shouldReturnEmptyListOn404HttpResponse() {
        when(scannerWsClient.call(any())).thenThrow(new HttpException("url", 404, "content"));

        CommunityProjectBranchesLoader testCase = new CommunityProjectBranchesLoader(scannerWsClient);
        assertThat(testCase.load("project").isEmpty()).isTrue();
    }
}
