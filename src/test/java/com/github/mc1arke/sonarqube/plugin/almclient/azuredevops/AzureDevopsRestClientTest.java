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
package com.github.mc1arke.sonarqube.plugin.almclient.azuredevops;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.mc1arke.sonarqube.plugin.almclient.azuredevops.model.CreateCommentRequest;
import com.github.mc1arke.sonarqube.plugin.almclient.azuredevops.model.GitPullRequestStatus;
import com.github.mc1arke.sonarqube.plugin.almclient.azuredevops.model.GitStatusContext;
import com.github.mc1arke.sonarqube.plugin.almclient.azuredevops.model.PullRequest;
import com.github.mc1arke.sonarqube.plugin.almclient.azuredevops.model.enums.GitStatusState;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AzureDevopsRestClientTest {

    private final ObjectMapper objectMapper = mock();
    private final CloseableHttpClient closeableHttpClient = mock();

    @Test
    void checkErrorThrownOnNonSuccessResponseStatus() throws IOException {
        AzureDevopsRestClient underTest = new AzureDevopsRestClient("http://url.test/api", "token", objectMapper, () -> closeableHttpClient);

        CloseableHttpResponse closeableHttpResponse = mock();
        StatusLine statusLine = mock();
        when(statusLine.getStatusCode()).thenReturn(500);
        when(closeableHttpResponse.getStatusLine()).thenReturn(statusLine);
        when(closeableHttpClient.execute(any())).thenReturn(closeableHttpResponse);
        when(objectMapper.writeValueAsString(any())).thenReturn("json");

        GitPullRequestStatus gitPullRequestStatus = mock();
        assertThatThrownBy(() -> underTest.submitPullRequestStatus("project", "repo", 101, gitPullRequestStatus))
                .isExactlyInstanceOf(IllegalStateException.class)
                .hasMessage("An unexpected response code was returned from the Azure Devops API - Expected: 200, Got: 500")
                .hasNoCause();

        ArgumentCaptor<HttpUriRequest> requestArgumentCaptor = ArgumentCaptor.captor();
        verify(closeableHttpClient).execute(requestArgumentCaptor.capture());

        RequestBuilder request = RequestBuilder.copy(requestArgumentCaptor.getValue());

        assertThat(request.getMethod()).isEqualTo("post");
        assertThat(request.getUri()).isEqualTo(URI.create("http://url.test/api/project/_apis/git/repositories/repo/pullRequests/101/statuses?api-version=4.1-preview"));
        assertThat(request.getEntity().getContent()).hasContent("json");
    }

    @Test
    void checkSubmitPullRequestStatusSubmitsCorrectContent() throws IOException {
        AzureDevopsRestClient underTest = new AzureDevopsRestClient("http://url.test/api", "token", objectMapper, () -> closeableHttpClient);

        CloseableHttpResponse closeableHttpResponse = mock();
        StatusLine statusLine = mock();
        when(statusLine.getStatusCode()).thenReturn(200);
        when(closeableHttpResponse.getStatusLine()).thenReturn(statusLine);
        when(closeableHttpClient.execute(any())).thenReturn(closeableHttpResponse);
        when(objectMapper.writeValueAsString(any())).thenReturn("json");

        underTest.submitPullRequestStatus("project Id With Spaces", "repository Name With Spaces", 123, new GitPullRequestStatus(GitStatusState.SUCCEEDED, "description", new GitStatusContext("name", "genre"), "url"));

        ArgumentCaptor<HttpUriRequest> requestArgumentCaptor = ArgumentCaptor.captor();
        verify(closeableHttpClient).execute(requestArgumentCaptor.capture());

        RequestBuilder request = RequestBuilder.copy(requestArgumentCaptor.getValue());

        assertThat(request.getMethod()).isEqualTo("post");
        assertThat(request.getUri()).isEqualTo(URI.create("http://url.test/api/project%20Id%20With%20Spaces/_apis/git/repositories/repository%20Name%20With%20Spaces/pullRequests/123/statuses?api-version=4.1-preview"));
        assertThat(request.getEntity().getContent()).hasContent("json");
    }

    @Test
    void checkAddCommentToThreadSubmitsCorrectContent() throws IOException {
        AzureDevopsRestClient underTest = new AzureDevopsRestClient("http://test.url", "authToken", objectMapper, () -> closeableHttpClient);

        CloseableHttpResponse closeableHttpResponse = mock();
        StatusLine statusLine = mock();
        when(statusLine.getStatusCode()).thenReturn(200);
        when(closeableHttpResponse.getStatusLine()).thenReturn(statusLine);
        when(closeableHttpClient.execute(any())).thenReturn(closeableHttpResponse);
        when(objectMapper.writeValueAsString(any())).thenReturn("json");

        underTest.addCommentToThread("projectId", "repository Name", 123, 321, new CreateCommentRequest("comment"));

        ArgumentCaptor<HttpUriRequest> requestArgumentCaptor = ArgumentCaptor.captor();
        verify(closeableHttpClient).execute(requestArgumentCaptor.capture());

        RequestBuilder request = RequestBuilder.copy(requestArgumentCaptor.getValue());

        assertThat(request.getMethod()).isEqualTo("post");
        assertThat(request.getUri()).isEqualTo(URI.create("http://test.url/projectId/_apis/git/repositories/repository%20Name/pullRequests/123/threads/321/comments?api-version=4.1"));
        assertThat(request.getEntity().getContent()).hasContent("json");
    }

    @Test
    void checkRetrievePullRequestReturnsCorrectContent() throws IOException {
        AzureDevopsRestClient underTest = new AzureDevopsRestClient("http://test.url", "authToken", objectMapper, () -> closeableHttpClient);

        CloseableHttpResponse closeableHttpResponse = mock();
        StatusLine statusLine = mock();
        when(statusLine.getStatusCode()).thenReturn(200);
        when(closeableHttpResponse.getStatusLine()).thenReturn(statusLine);
        when(closeableHttpResponse.getEntity()).thenReturn(new StringEntity("content", StandardCharsets.UTF_8));
        when(closeableHttpClient.execute(any())).thenReturn(closeableHttpResponse);
        PullRequest pullRequest = mock();
        when(objectMapper.readValue(any(String.class), eq(PullRequest.class))).thenReturn(pullRequest);

        PullRequest result = underTest.retrievePullRequest("projectId", "repository Name", 123);

        ArgumentCaptor<HttpUriRequest> requestArgumentCaptor = ArgumentCaptor.captor();
        verify(closeableHttpClient).execute(requestArgumentCaptor.capture());

        RequestBuilder request = RequestBuilder.copy(requestArgumentCaptor.getValue());

        assertThat(request.getMethod()).isEqualTo("get");
        assertThat(request.getUri()).isEqualTo(URI.create("http://test.url/projectId/_apis/git/repositories/repository%20Name/pullRequests/123?api-version=4.1"));
        assertThat(request.getEntity()).isNull();
        assertThat(result).isSameAs(pullRequest);
    }
}