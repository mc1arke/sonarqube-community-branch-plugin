/*
 * Copyright (C) 2019 Michael Clarke
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

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.utils.MessageException;
import org.sonar.scanner.bootstrap.ScannerWsClient;
import org.sonar.scanner.protocol.GsonHelper;
import org.sonar.scanner.scan.branch.ProjectPullRequests;
import org.sonar.scanner.scan.branch.PullRequestInfo;
import org.sonarqube.ws.client.HttpException;
import org.sonarqube.ws.client.WsResponse;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Michael Clarke
 */
public class CommunityProjectPullRequestsLoaderTest {

    private final ScannerWsClient scannerWsClient = mock(ScannerWsClient.class);
    private final ExpectedException expectedException = ExpectedException.none();

    @Rule
    public ExpectedException expectedException() {
        return expectedException;
    }

    @Test
    public void testEmptyPullRequestsOnEmptyServerResponse() {
        WsResponse mockResponse = mock(WsResponse.class);
        when(scannerWsClient.call(any())).thenReturn(mockResponse);

        StringReader stringReader = new StringReader(GsonHelper.create()
                                                             .toJson(new CommunityProjectPullRequestsLoader.PullRequestsResponse(
                                                                     new ArrayList<>())));
        when(mockResponse.contentReader()).thenReturn(stringReader);

        CommunityProjectPullRequestsLoader testCase = new CommunityProjectPullRequestsLoader(scannerWsClient);
        ProjectPullRequests response = testCase.load("projectKey");
        assertTrue(response.isEmpty());
    }

    @Test
    public void testAllPullRequestsFromNonEmptyServerResponse() throws ParseException {
        WsResponse mockResponse = mock(WsResponse.class);
        when(scannerWsClient.call(any())).thenReturn(mockResponse);

        StringReader stringReader = new StringReader(
                "{\"pullRequests\":[{\"key\":\"101\",\"title\":\"dummybranch\",\"branch\":\"dummybranch\",\"base\":\"master\",\"status\":{\"qualityGateStatus\":\"OK\",\"bugs\":0,\"vulnerabilities\":0,\"codeSmells\":0},\"analysisDate\":\"2019-04-04T19:44:27+0100\"}]}");
        when(mockResponse.contentReader()).thenReturn(stringReader);

        CommunityProjectPullRequestsLoader testCase = new CommunityProjectPullRequestsLoader(scannerWsClient);
        ProjectPullRequests response = testCase.load("key");
        assertFalse(response.isEmpty());

        PullRequestInfo responseInfo = response.get("dummybranch");
        assertNotNull(responseInfo);
        assertEquals(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ").parse("2019-04-04T19:44:27+0100").getTime(),
                     responseInfo.getAnalysisDate());
        assertEquals("master", responseInfo.getBase());
        assertEquals("dummybranch", responseInfo.getBranch());
        assertEquals("101", responseInfo.getKey());
    }

    @Test
    public void testAllPullRequestsFromNonEmptyServerResponseWithInvalidDate() {
        WsResponse mockResponse = mock(WsResponse.class);
        when(scannerWsClient.call(any())).thenReturn(mockResponse);

        StringReader stringReader = new StringReader(
                "{\"pullRequests\":[{\"key\":\"101\",\"title\":\"dummybranch\",\"branch\":\"dummybranch\",\"base\":\"master\",\"status\":{\"qualityGateStatus\":\"OK\",\"bugs\":0,\"vulnerabilities\":0,\"codeSmells\":0},\"analysisDate\":\"\"}]}");
        when(mockResponse.contentReader()).thenReturn(stringReader);

        CommunityProjectPullRequestsLoader testCase = new CommunityProjectPullRequestsLoader(scannerWsClient);
        ProjectPullRequests response = testCase.load("key");
        assertFalse(response.isEmpty());

        PullRequestInfo responseInfo = response.get("dummybranch");
        assertNotNull(responseInfo);
        assertEquals(0, responseInfo.getAnalysisDate());
        assertEquals("master", responseInfo.getBase());
        assertEquals("dummybranch", responseInfo.getBranch());
        assertEquals("101", responseInfo.getKey());
    }

    @Test
    public void testAllPullRequestsFromNonEmptyServerResponseWithoutBase() {
        WsResponse mockResponse = mock(WsResponse.class);
        when(scannerWsClient.call(any())).thenReturn(mockResponse);

        StringReader stringReader = new StringReader(
                "{\"pullRequests\":[{\"key\":\"101\",\"title\":\"dummybranch\",\"branch\":\"dummybranch\",\"status\":{\"qualityGateStatus\":\"OK\",\"bugs\":0,\"vulnerabilities\":0,\"codeSmells\":0},\"analysisDate\":\"\"}]}");
        when(mockResponse.contentReader()).thenReturn(stringReader);

        CommunityProjectPullRequestsLoader testCase = new CommunityProjectPullRequestsLoader(scannerWsClient);
        ProjectPullRequests response = testCase.load("key");
        assertFalse(response.isEmpty());

        PullRequestInfo responseInfo = response.get("dummybranch");
        assertNotNull(responseInfo);
        assertEquals(0, responseInfo.getAnalysisDate());
        assertNull(responseInfo.getBase());
        assertEquals("dummybranch", responseInfo.getBranch());
        assertEquals("101", responseInfo.getKey());
    }

    @Test
    public void testMessageExceptionOnIOException() {
        WsResponse mockResponse = mock(WsResponse.class);
        when(scannerWsClient.call(any())).thenReturn(mockResponse);

        Reader mockReader = new BufferedReader(new StringReader(GsonHelper.create()
                                                                        .toJson(new CommunityProjectPullRequestsLoader.PullRequestsResponse(
                                                                                new ArrayList<>())))) {
            public void close() throws IOException {
                throw new IOException("Dummy IO Exception");
            }
        };
        when(mockResponse.contentReader()).thenReturn(mockReader);

        expectedException.expectMessage("Could not load pull requests from server");
        expectedException.expect(MessageException.class);

        CommunityProjectPullRequestsLoader testCase = new CommunityProjectPullRequestsLoader(scannerWsClient);
        testCase.load("project");


    }


    @Test
    public void testErrorOnNon404HttpResponse() {
        WsResponse mockResponse = mock(WsResponse.class);
        when(scannerWsClient.call(any())).thenReturn(mockResponse);

        Reader mockReader = new BufferedReader(new StringReader(GsonHelper.create()
                                                                        .toJson(new CommunityProjectPullRequestsLoader.PullRequestsResponse(
                                                                                new ArrayList<>())))) {
            public void close() {
                throw new HttpException("url", 12, "content");
            }
        };
        when(mockResponse.contentReader()).thenReturn(mockReader);

        expectedException.expectMessage("Could not load pull requests from server");
        expectedException.expect(MessageException.class);

        CommunityProjectPullRequestsLoader testCase = new CommunityProjectPullRequestsLoader(scannerWsClient);
        testCase.load("project");
    }


    @Test
    public void testEmptyListOn404HttpResponse() {
        when(scannerWsClient.call(any())).thenThrow(new HttpException("url", 404, "content"));

        CommunityProjectPullRequestsLoader testCase = new CommunityProjectPullRequestsLoader(scannerWsClient);
        assertTrue(testCase.load("project").isEmpty());
    }
}
