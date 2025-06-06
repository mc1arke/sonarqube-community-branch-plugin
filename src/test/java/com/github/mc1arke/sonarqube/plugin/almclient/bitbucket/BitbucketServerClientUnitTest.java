/*
 * Copyright (C) 2021-2025 Michael Clarke
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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.github.mc1arke.sonarqube.plugin.almclient.bitbucket.model.AnnotationUploadLimit;
import com.github.mc1arke.sonarqube.plugin.almclient.bitbucket.model.BuildStatus;
import com.github.mc1arke.sonarqube.plugin.almclient.bitbucket.model.CodeInsightsAnnotation;
import com.github.mc1arke.sonarqube.plugin.almclient.bitbucket.model.CodeInsightsReport;
import com.github.mc1arke.sonarqube.plugin.almclient.bitbucket.model.DataValue;
import com.github.mc1arke.sonarqube.plugin.almclient.bitbucket.model.ReportStatus;
import com.github.mc1arke.sonarqube.plugin.almclient.bitbucket.model.server.Annotation;
import com.github.mc1arke.sonarqube.plugin.almclient.bitbucket.model.server.BitbucketServerConfiguration;
import com.github.mc1arke.sonarqube.plugin.almclient.bitbucket.model.server.CreateReportRequest;
import com.github.mc1arke.sonarqube.plugin.almclient.bitbucket.model.server.ErrorResponse;
import com.github.mc1arke.sonarqube.plugin.almclient.bitbucket.model.server.ServerProperties;
import com.google.common.collect.Sets;
import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.Buffer;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BitbucketServerClientUnitTest {

    private final ObjectMapper mapper = spy();
    private final OkHttpClient client = mock();
    private final BitbucketServerClient underTest = new BitbucketServerClient(new BitbucketServerConfiguration("project", "repository", "https://my-server.org"), mapper, client);

    @Test
    void testSupportsCodeInsightsIsFalse() throws IOException {
        // given
        ServerProperties serverProperties = new ServerProperties("5.0");

        Call call = mock();
        Response response = mock();
        ObjectReader reader = mock();
        ResponseBody responseBody = mock();

        when(client.newCall(any())).thenReturn(call);
        when(call.execute()).thenReturn(response);
        when(response.isSuccessful()).thenReturn(true);
        when(response.body()).thenReturn(responseBody);
        when(responseBody.string()).thenReturn("test");

        when(mapper.reader()).thenReturn(reader);
        when(reader.forType(ServerProperties.class)).thenReturn(reader);
        when(reader.readValue(any(String.class))).thenReturn(serverProperties);

        // when
        boolean result = underTest.supportsCodeInsights();

        // then
        assertFalse(result);
    }

    @Test
    void testSupportsCodeInsightsIsTrueWhenVersionEqual() throws IOException {
        // given
        ServerProperties serverProperties = new ServerProperties("5.15");

        Call call = mock();
        Response response = mock();
        ObjectReader reader = mock();
        ResponseBody responseBody = mock();

        when(client.newCall(any())).thenReturn(call);
        when(call.execute()).thenReturn(response);
        when(response.isSuccessful()).thenReturn(true);
        when(response.body()).thenReturn(responseBody);
        when(responseBody.string()).thenReturn("test");

        when(mapper.reader()).thenReturn(reader);
        when(reader.forType(ServerProperties.class)).thenReturn(reader);
        when(reader.readValue(any(String.class))).thenReturn(serverProperties);

        // when
        boolean result = underTest.supportsCodeInsights();

        // then
        assertTrue(result);
    }

    @Test
    void testSupportsCodeInsightsIsTrueIfVersionIsHigher() throws IOException {
        // given
        ServerProperties serverProperties = new ServerProperties("6.0");

        Call call = mock();
        Response response = mock();
        ObjectReader reader = mock();
        ResponseBody responseBody = mock();

        when(client.newCall(any())).thenReturn(call);
        when(call.execute()).thenReturn(response);
        when(response.isSuccessful()).thenReturn(true);
        when(response.body()).thenReturn(responseBody);
        when(responseBody.string()).thenReturn("test");

        when(mapper.reader()).thenReturn(reader);
        when(reader.forType(ServerProperties.class)).thenReturn(reader);
        when(reader.readValue(any(String.class))).thenReturn(serverProperties);

        // when
        boolean result = underTest.supportsCodeInsights();

        // then
        assertTrue(result);
    }

    @Test
    void testSupportsCodeInsightsIsFalseWhenException() throws IOException {
        // given
        Call call = mock();
        when(client.newCall(any())).thenReturn(call);
        when(call.execute()).thenThrow(new IOException());

        // when
        boolean result = underTest.supportsCodeInsights();

        // then
        assertFalse(result);
    }

    @Test
    void testGetServerProperties() throws IOException {
        // given
        ServerProperties serverProperties = new ServerProperties("5.0");

        Call call = mock();
        Response response = mock();
        ObjectReader reader = mock();
        ResponseBody responseBody = mock();
        ArgumentCaptor<Request> captor = ArgumentCaptor.captor();

        when(client.newCall(any())).thenReturn(call);
        when(call.execute()).thenReturn(response);
        when(response.isSuccessful()).thenReturn(true);
        when(response.body()).thenReturn(responseBody);
        when(responseBody.string()).thenReturn("{version: '5.0'}");

        when(mapper.reader()).thenReturn(reader);
        when(reader.forType(ServerProperties.class)).thenReturn(reader);
        when(reader.readValue(any(String.class))).thenReturn(serverProperties);

        // when
        ServerProperties result = underTest.getServerProperties();

        // then
        verify(client).newCall(captor.capture());
        Request request = captor.getValue();
        assertEquals("GET", request.method());
        assertEquals("https://my-server.org/rest/api/1.0/application-properties", request.url().toString());
        assertEquals("5.0", result.getVersion());
    }

    @Test
    void testGetServerPropertiesError() throws IOException {
        // given
        Call call = mock();
        Response response = mock();
        ObjectReader reader = mock();

        when(client.newCall(any())).thenReturn(call);
        when(call.execute()).thenReturn(response);
        when(response.isSuccessful()).thenReturn(true);
        when(response.body()).thenReturn(null);

        when(mapper.reader()).thenReturn(reader);
        when(reader.forType(ServerProperties.class)).thenReturn(reader);

        // when, then
        assertThatThrownBy(underTest::getServerProperties)
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void testUploadReport() throws IOException {
        // given
        CodeInsightsReport report = mock();
        Call call = mock();
        Response response = mock();
        ArgumentCaptor<Request> captor = ArgumentCaptor.captor();

        when(client.newCall(any())).thenReturn(call);
        when(call.execute()).thenReturn(response);
        when(response.isSuccessful()).thenReturn(true);

        when(mapper.writeValueAsString(report)).thenReturn("{payload}");

        // when
        underTest.uploadReport("commit", report, "reportKey");

        // then
        verify(client).newCall(captor.capture());
        Request request = captor.getValue();
        assertEquals("PUT", request.method());
        assertEquals("https://my-server.org/rest/insights/1.0/projects/project/repos/repository/commits/commit/reports/reportKey", request.url().toString());
    }

    @Test
    void testUploadReportFails() throws IOException {
        // given
        CodeInsightsReport report = mock();
        Call call = mock();
        Response response = mock();

        when(client.newCall(any())).thenReturn(call);
        when(call.execute()).thenReturn(response);
        when(response.isSuccessful()).thenReturn(false);
        when(response.body()).thenReturn(null);

        when(mapper.writeValueAsString(report)).thenReturn("{payload}");

        // when,then
        assertThatThrownBy(() -> underTest.uploadReport("commit", report, "reportKey"))
                .isInstanceOf(BitbucketException.class);
    }

    @Test
    void testUploadReportFailsWithMessage() throws IOException {
        // given
        ErrorResponse.Error error = new ErrorResponse.Error("error!");
        ErrorResponse errorResponse = new ErrorResponse(Sets.newHashSet(error));

        CodeInsightsReport report = mock();
        Call call = mock();
        Response response = mock();
        ResponseBody responseBody = mock();
        ObjectReader reader = mock();

        when(client.newCall(any())).thenReturn(call);
        when(call.execute()).thenReturn(response);
        when(response.isSuccessful()).thenReturn(false);
        when(response.body()).thenReturn(responseBody);
        when(responseBody.string()).thenReturn("error!");
        when(response.code()).thenReturn(400);

        when(mapper.writeValueAsString(report)).thenReturn("{payload}");

        when(mapper.reader()).thenReturn(reader);
        when(reader.forType(ErrorResponse.class)).thenReturn(reader);
        when(reader.readValue(any(String.class))).thenReturn(errorResponse);


        // when,then
        assertThatThrownBy(() -> underTest.uploadReport("commit", report, "reportKey"))
                .isInstanceOf(BitbucketException.class)
                .hasMessage("error!")
                .extracting(e -> ((BitbucketException) e).isError(400))
                .isEqualTo(true);
    }

    @Test
    void testUploadAnnotations() throws IOException {
        // given
        Annotation annotation = mock();
        when(annotation.getLine()).thenReturn(12);
        when(annotation.getMessage()).thenReturn("hello");
        when(annotation.getSeverity()).thenReturn("severe");
        when(annotation.getPath()).thenReturn("path");
        when(annotation.getExternalId()).thenReturn("external ID");
        when(annotation.getLink()).thenReturn("link");
        when(annotation.getType()).thenReturn("type");
        Set<CodeInsightsAnnotation> annotations = Sets.newHashSet(annotation);
        Call call = mock();
        Response response = mock();
        ArgumentCaptor<Request> captor = ArgumentCaptor.captor();

        when(client.newCall(any())).thenReturn(call);
        when(call.execute()).thenReturn(response);
        when(response.isSuccessful()).thenReturn(true);

        // when
        underTest.uploadAnnotations("commit", annotations, "reportKey");

        // then
        verify(client).newCall(captor.capture());
        Request request = captor.getValue();
        assertEquals("POST", request.method());
        assertEquals("https://my-server.org/rest/insights/1.0/projects/project/repos/repository/commits/commit/reports/reportKey/annotations", request.url().toString());

        try (Buffer bodyContent = new Buffer()) {
            request.body().writeTo(bodyContent);
            assertEquals("{\"annotations\":[{\"externalId\":\"external ID\",\"line\":12,\"type\":\"type\",\"link\":\"link\",\"message\":\"hello\",\"path\":\"path\",\"severity\":\"severe\"}]}", bodyContent.readUtf8());
        }
    }

    @Test
    void testUploadAnnotationsWithEmptyAnnotations() throws IOException {
        // given
        Set<CodeInsightsAnnotation> annotations = Sets.newHashSet();

        // when
        underTest.uploadAnnotations("commit", annotations, "reportKey");

        // then
        verify(client, never()).newCall(any());
    }

    @Test
    void testDeleteAnnotations() throws IOException {
        // given
        Call call = mock();
        Response response = mock();
        ArgumentCaptor<Request> captor = ArgumentCaptor.captor();

        when(client.newCall(any())).thenReturn(call);
        when(call.execute()).thenReturn(response);
        when(response.isSuccessful()).thenReturn(true);

        // when
        underTest.deleteAnnotations("commit", "reportKey");

        // then
        verify(client).newCall(captor.capture());
        Request request = captor.getValue();
        assertEquals("DELETE", request.method());
        assertEquals("https://my-server.org/rest/insights/1.0/projects/project/repos/repository/commits/commit/reports/reportKey/annotations", request.url().toString());
    }

    @Test
    void testCreateAnnotationForServer() {
        // given
        // when
        CodeInsightsAnnotation annotation = underTest.createCodeInsightsAnnotation("issueKey", 12, "http://localhost:9000/dashboard", "Failed", "/path/to/file", "MAJOR", "BUG");

        // then
        assertInstanceOf(Annotation.class, annotation);
        assertEquals("issueKey", ((Annotation) annotation).getExternalId());
        assertEquals(12, annotation.getLine());
        assertEquals("http://localhost:9000/dashboard", ((Annotation) annotation).getLink());
        assertEquals("/path/to/file", annotation.getPath());
        assertEquals("MAJOR", annotation.getSeverity());
        assertEquals("BUG", ((Annotation) annotation).getType());
    }

    @Test
    void testCreateDataLinkForServer() {
        // given
        // when
        DataValue data = underTest.createLinkDataValue("https://localhost:9000/any/project");

        // then
        assertInstanceOf(DataValue.Link.class, data);
        assertEquals("https://localhost:9000/any/project", ((DataValue.Link) data).getHref());
    }

    @Test
    void testUploadLimit() {
        // given
        // when
        AnnotationUploadLimit annotationUploadLimit = underTest.getAnnotationUploadLimit();

        // then
        assertEquals(1000, annotationUploadLimit.getAnnotationBatchSize());
        assertEquals(1000, annotationUploadLimit.getTotalAllowedAnnotations());
    }

    @Test
    void testCreateCloudReport() {
        // given

        // when
        CodeInsightsReport result = underTest.createCodeInsightsReport(new ArrayList<>(), "reportDescription", Instant.now(), "dashboardUrl", "logoUrl", ReportStatus.FAILED);

        // then
        assertInstanceOf(CreateReportRequest.class, result);
        assertEquals(0, result.getData().size());
        assertEquals("reportDescription", result.getDetails());
        assertEquals("dashboardUrl", result.getLink());
        assertEquals("logoUrl", ((CreateReportRequest) result).getLogoUrl());
        assertEquals("FAIL", result.getResult());
    }

    @Test
    void shouldSubmitBuildStatusToServer() throws IOException {
        // given
        Call call = mock();
        Response response = mock();
        ArgumentCaptor<Request> captor = ArgumentCaptor.captor();

        when(client.newCall(any())).thenReturn(call);
        when(call.execute()).thenReturn(response);
        when(response.isSuccessful()).thenReturn(true);

        when(mapper.writeValueAsString(any())).thenReturn("{payload}");

        BuildStatus buildStatus = new BuildStatus(BuildStatus.State.INPROGRESS, "key", "name", "url");

        // when
        underTest.submitBuildStatus("commit", buildStatus);

        // then
        verify(client).newCall(captor.capture());
        Request request = captor.getValue();
        assertThat(request.method()).isEqualTo("POST");
        assertThat(request.url()).hasToString("https://my-server.org/rest/api/1.0/projects/project/repos/repository/commits/commit/builds");

        verify(mapper).writeValueAsString(buildStatus);
    }

    @CsvSource({"shortReportKey, shortReportKey", "fiftyCharactersLongReportKey123456789012, fiftyCharactersLongReportKey123456789012",
            "moreThanFiftyCharactersLongReportKey12345678901234567890, t-FiftyCharactersLongReportKey12345678901234567890"})
    @ParameterizedTest
   void shouldNormaliseReportKeyToFiftyCharacters(String input, String expected) {
        String result = underTest.normaliseReportKey(input);

        assertThat(result).hasSizeLessThanOrEqualTo(50)
                .isEqualTo(expected);
    }
}
