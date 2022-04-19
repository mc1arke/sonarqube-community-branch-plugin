/*
 * Copyright (C) 2021-2022 Michael Clarke
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
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class BitbucketServerClientUnitTest {

    private BitbucketServerClient underTest;

    @Spy
    private ObjectMapper mapper;

    @Mock
    private OkHttpClient client;

    @Before
    public void before() {
        BitbucketServerConfiguration
                config = new BitbucketServerConfiguration("project", "repository", "https://my-server.org");
        underTest = new BitbucketServerClient(config, mapper, client);
    }

    @Test
    public void testSupportsCodeInsightsIsFalse() throws IOException {
        // given
        ServerProperties serverProperties = new ServerProperties("5.0");

        Call call = mock(Call.class);
        Response response = mock(Response.class);
        ObjectReader reader = mock(ObjectReader.class);
        ResponseBody responseBody = mock(ResponseBody.class);

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
    public void testSupportsCodeInsightsIsTrueWhenVersionEqual() throws IOException {
        // given
        ServerProperties serverProperties = new ServerProperties("5.15");

        Call call = mock(Call.class);
        Response response = mock(Response.class);
        ObjectReader reader = mock(ObjectReader.class);
        ResponseBody responseBody = mock(ResponseBody.class);

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
    public void testSupportsCodeInsightsIsTrueIfVersionIsHigher() throws IOException {
        // given
        ServerProperties serverProperties = new ServerProperties("6.0");

        Call call = mock(Call.class);
        Response response = mock(Response.class);
        ObjectReader reader = mock(ObjectReader.class);
        ResponseBody responseBody = mock(ResponseBody.class);

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
    public void testSupportsCodeInsightsIsFalseWhenException() throws IOException {
        // given
        Call call = mock(Call.class);
        when(client.newCall(any())).thenReturn(call);
        when(call.execute()).thenThrow(new IOException());

        // when
        boolean result = underTest.supportsCodeInsights();

        // then
        assertFalse(result);
    }

    @Test
    public void testGetServerProperties() throws IOException {
        // given
        ServerProperties serverProperties = new ServerProperties("5.0");

        Call call = mock(Call.class);
        Response response = mock(Response.class);
        ObjectReader reader = mock(ObjectReader.class);
        ResponseBody responseBody = mock(ResponseBody.class);
        ArgumentCaptor<Request> captor = ArgumentCaptor.forClass(Request.class);

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
    public void testGetServerPropertiesError() throws IOException {
        // given
        Call call = mock(Call.class);
        Response response = mock(Response.class);
        ObjectReader reader = mock(ObjectReader.class);

        when(client.newCall(any())).thenReturn(call);
        when(call.execute()).thenReturn(response);
        when(response.isSuccessful()).thenReturn(true);
        when(response.body()).thenReturn(null);

        when(mapper.reader()).thenReturn(reader);
        when(reader.forType(ServerProperties.class)).thenReturn(reader);

        // when, then
        assertThatThrownBy(() -> underTest.getServerProperties())
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    public void testUploadReport() throws IOException {
        // given
        CodeInsightsReport report = mock(CodeInsightsReport.class);
        Call call = mock(Call.class);
        Response response = mock(Response.class);
        ArgumentCaptor<Request> captor = ArgumentCaptor.forClass(Request.class);

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
    public void testUploadReportFails() throws IOException {
        // given
        CodeInsightsReport report = mock(CodeInsightsReport.class);
        Call call = mock(Call.class);
        Response response = mock(Response.class);

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
    public void testUploadReportFailsWithMessage() throws IOException {
        // given
        ErrorResponse.Error error = new ErrorResponse.Error("error!");
        ErrorResponse errorResponse = new ErrorResponse(Sets.newHashSet(error));

        CodeInsightsReport report = mock(CodeInsightsReport.class);
        Call call = mock(Call.class);
        Response response = mock(Response.class);
        ResponseBody responseBody = mock(ResponseBody.class);
        ObjectReader reader = mock(ObjectReader.class);

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
    public void testUploadAnnotations() throws IOException {
        // given
        Annotation annotation = mock(Annotation.class);
        when(annotation.getLine()).thenReturn(12);
        when(annotation.getMessage()).thenReturn("hello");
        when(annotation.getSeverity()).thenReturn("severe");
        when(annotation.getPath()).thenReturn("path");
        when(annotation.getExternalId()).thenReturn("external ID");
        when(annotation.getLink()).thenReturn("link");
        when(annotation.getType()).thenReturn("type");
        Set<CodeInsightsAnnotation> annotations = Sets.newHashSet(annotation);
        Call call = mock(Call.class);
        Response response = mock(Response.class);
        ArgumentCaptor<Request> captor = ArgumentCaptor.forClass(Request.class);

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
    public void testUploadAnnotationsWithEmptyAnnotations() throws IOException {
        // given
        Set<CodeInsightsAnnotation> annotations = Sets.newHashSet();

        // when
        underTest.uploadAnnotations("commit", annotations, "reportKey");

        // then
        verify(client, never()).newCall(any());
    }

    @Test
    public void testDeleteAnnotations() throws IOException {
        // given
        Call call = mock(Call.class);
        Response response = mock(Response.class);
        ArgumentCaptor<Request> captor = ArgumentCaptor.forClass(Request.class);

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
    public void testCreateAnnotationForServer() {
        // given
        // when
        CodeInsightsAnnotation annotation = underTest.createCodeInsightsAnnotation("issueKey", 12, "http://localhost:9000/dashboard", "Failed", "/path/to/file", "MAJOR", "BUG");

        // then
        assertTrue(annotation instanceof Annotation);
        assertEquals("issueKey", ((Annotation) annotation).getExternalId());
        assertEquals(12, annotation.getLine());
        assertEquals("http://localhost:9000/dashboard", ((Annotation) annotation).getLink());
        assertEquals("/path/to/file", annotation.getPath());
        assertEquals("MAJOR", annotation.getSeverity());
        assertEquals("BUG", ((Annotation) annotation).getType());
    }

    @Test
    public void testCreateDataLinkForServer() {
        // given
        // when
        DataValue data = underTest.createLinkDataValue("https://localhost:9000/any/project");

        // then
        assertTrue(data instanceof DataValue.Link);
        assertEquals("https://localhost:9000/any/project", ((DataValue.Link) data).getHref());
    }

    @Test
    public void testUploadLimit() {
        // given
        // when
        AnnotationUploadLimit annotationUploadLimit = underTest.getAnnotationUploadLimit();

        // then
        assertEquals(1000, annotationUploadLimit.getAnnotationBatchSize());
        assertEquals(1000, annotationUploadLimit.getTotalAllowedAnnotations());
    }

    @Test
    public void testCreateCloudReport() {
        // given

        // when
        CodeInsightsReport result = underTest.createCodeInsightsReport(new ArrayList<>(), "reportDescription", Instant.now(), "dashboardUrl", "logoUrl", ReportStatus.FAILED);

        // then
        assertTrue(result instanceof CreateReportRequest);
        assertEquals(0, result.getData().size());
        assertEquals("reportDescription", result.getDetails());
        assertEquals("dashboardUrl", result.getLink());
        assertEquals("logoUrl", ((CreateReportRequest) result).getLogoUrl());
        assertEquals("FAIL", result.getResult());
    }
}
