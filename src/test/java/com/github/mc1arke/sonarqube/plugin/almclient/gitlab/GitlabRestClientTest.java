package com.github.mc1arke.sonarqube.plugin.almclient.gitlab;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.mc1arke.sonarqube.plugin.almclient.LinkHeaderReader;
import com.github.mc1arke.sonarqube.plugin.almclient.gitlab.model.MergeRequestNote;
import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.StatusLine;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GitlabRestClientTest {

    private final CloseableHttpClient closeableHttpClient = mock(CloseableHttpClient.class);
    private final LinkHeaderReader linkHeaderReader = mock(LinkHeaderReader.class);
    private final ObjectMapper objectMapper = mock(ObjectMapper.class);

    @Test
    void checkErrorThrownOnNonSuccessResponseStatus() throws IOException {
        GitlabRestClient underTest = new GitlabRestClient("http://url.test/api", "token", linkHeaderReader, objectMapper, () -> closeableHttpClient);

        CloseableHttpResponse closeableHttpResponse = mock(CloseableHttpResponse.class);
        StatusLine statusLine = mock(StatusLine.class);
        when(statusLine.getStatusCode()).thenReturn(500);
        when(closeableHttpResponse.getStatusLine()).thenReturn(statusLine);
        when(closeableHttpClient.execute(any())).thenReturn(closeableHttpResponse);

        MergeRequestNote mergeRequestNote = mock(MergeRequestNote.class);
        when(mergeRequestNote.getContent()).thenReturn("note");

        assertThatThrownBy(() -> underTest.addMergeRequestDiscussion(101, 99, mergeRequestNote))
                .isExactlyInstanceOf(IllegalStateException.class)
                .hasMessage("An unexpected response code was returned from the Gitlab API - Expected: 201, Got: 500")
                .hasNoCause();

        ArgumentCaptor<HttpUriRequest> requestArgumentCaptor = ArgumentCaptor.forClass(HttpUriRequest.class);
        verify(closeableHttpClient).execute(requestArgumentCaptor.capture());

        HttpEntityEnclosingRequest request = (HttpEntityEnclosingRequest) requestArgumentCaptor.getValue();

        assertThat(request.getRequestLine().getMethod()).isEqualTo("POST");
        assertThat(request.getRequestLine().getUri()).isEqualTo("http://url.test/api/projects/101/merge_requests/99/discussions");
        assertThat(request.getEntity().getContent()).hasContent("body=note");
    }

    @Test
    void checkCorrectEncodingUsedOnMergeRequestDiscussion() throws IOException {
        CloseableHttpResponse closeableHttpResponse = mock(CloseableHttpResponse.class);
        StatusLine statusLine = mock(StatusLine.class);
        when(statusLine.getStatusCode()).thenReturn(201);
        when(closeableHttpResponse.getStatusLine()).thenReturn(statusLine);
        HttpEntity httpEntity = mock(HttpEntity.class);
        when(closeableHttpResponse.getEntity()).thenReturn(httpEntity);
        when(closeableHttpClient.execute(any())).thenReturn(closeableHttpResponse);

        MergeRequestNote mergeRequestNote = new MergeRequestNote("Merge request note");

        GitlabRestClient underTest = new GitlabRestClient("http://api.url", "token", linkHeaderReader, objectMapper, () -> closeableHttpClient);
        underTest.addMergeRequestDiscussion(123, 321, mergeRequestNote);

        ArgumentCaptor<HttpUriRequest> requestArgumentCaptor = ArgumentCaptor.forClass(HttpUriRequest.class);
        verify(closeableHttpClient).execute(requestArgumentCaptor.capture());

        HttpEntityEnclosingRequest request = (HttpEntityEnclosingRequest) requestArgumentCaptor.getValue();

        assertThat(request.getRequestLine().getMethod()).isEqualTo("POST");
        assertThat(request.getRequestLine().getUri()).isEqualTo("http://api.url/projects/123/merge_requests/321/discussions");
        assertThat(request.getEntity().getContent()).hasContent("body=Merge+request+note");
    }

}