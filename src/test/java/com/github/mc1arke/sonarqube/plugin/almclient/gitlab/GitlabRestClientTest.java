/*
 * Copyright (C) 2021-2024 Michael Clarke
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
package com.github.mc1arke.sonarqube.plugin.almclient.gitlab;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.mc1arke.sonarqube.plugin.almclient.gitlab.model.MergeRequestNote;
import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GitlabRestClientTest {

    private final CloseableHttpClient closeableHttpClient = mock();
    private final LinkHeaderReader linkHeaderReader = mock();
    private final ObjectMapper objectMapper = mock();

    @Test
    void checkErrorThrownOnNonSuccessResponseStatus() throws IOException {
        GitlabRestClient underTest = new GitlabRestClient("http://url.test/api", "token", linkHeaderReader, objectMapper, () -> closeableHttpClient);

        CloseableHttpResponse closeableHttpResponse = mock();
        StatusLine statusLine = mock();
        when(statusLine.getStatusCode()).thenReturn(500);
        when(closeableHttpResponse.getStatusLine()).thenReturn(statusLine);
        when(closeableHttpClient.execute(any())).thenReturn(closeableHttpResponse);

        MergeRequestNote mergeRequestNote = mock();
        when(mergeRequestNote.getContent()).thenReturn("note");

        assertThatThrownBy(() -> underTest.addMergeRequestDiscussion(101, 99, mergeRequestNote))
                .isExactlyInstanceOf(IllegalStateException.class)
                .hasMessage("An unexpected response code was returned from the Gitlab API - Expected: 201, Got: 500")
                .hasNoCause();

        ArgumentCaptor<HttpUriRequest> requestArgumentCaptor = ArgumentCaptor.captor();
        verify(closeableHttpClient).execute(requestArgumentCaptor.capture());

        HttpEntityEnclosingRequest request = (HttpEntityEnclosingRequest) requestArgumentCaptor.getValue();

        assertThat(request.getRequestLine().getMethod()).isEqualTo("POST");
        assertThat(request.getRequestLine().getUri()).isEqualTo("http://url.test/api/projects/101/merge_requests/99/discussions");
        assertThat(request.getEntity().getContent()).hasContent("body=note");
    }

    @Test
    void checkCorrectEncodingUsedOnMergeRequestDiscussion() throws IOException {
        CloseableHttpResponse closeableHttpResponse = mock();
        StatusLine statusLine = mock();
        when(statusLine.getStatusCode()).thenReturn(201);
        when(closeableHttpResponse.getStatusLine()).thenReturn(statusLine);
        HttpEntity httpEntity = mock();
        when(closeableHttpResponse.getEntity()).thenReturn(httpEntity);
        when(closeableHttpClient.execute(any())).thenReturn(closeableHttpResponse);

        MergeRequestNote mergeRequestNote = new MergeRequestNote("Merge request note");

        GitlabRestClient underTest = new GitlabRestClient("http://api.url", "token", linkHeaderReader, objectMapper, () -> closeableHttpClient);
        underTest.addMergeRequestDiscussion(123, 321, mergeRequestNote);

        ArgumentCaptor<HttpUriRequest> requestArgumentCaptor = ArgumentCaptor.captor();
        verify(closeableHttpClient).execute(requestArgumentCaptor.capture());

        HttpEntityEnclosingRequest request = (HttpEntityEnclosingRequest) requestArgumentCaptor.getValue();

        assertThat(request.getRequestLine().getMethod()).isEqualTo("POST");
        assertThat(request.getRequestLine().getUri()).isEqualTo("http://api.url/projects/123/merge_requests/321/discussions");
        assertThat(request.getEntity().getContent()).hasContent("body=Merge+request+note");
    }

}