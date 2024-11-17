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
package com.github.mc1arke.sonarqube.plugin.almclient.bitbucket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.mc1arke.sonarqube.plugin.almclient.bitbucket.model.AnnotationUploadLimit;
import com.github.mc1arke.sonarqube.plugin.almclient.bitbucket.model.BitbucketConfiguration;
import com.github.mc1arke.sonarqube.plugin.almclient.bitbucket.model.BuildStatus;
import com.github.mc1arke.sonarqube.plugin.almclient.bitbucket.model.CodeInsightsAnnotation;
import com.github.mc1arke.sonarqube.plugin.almclient.bitbucket.model.CodeInsightsReport;
import com.github.mc1arke.sonarqube.plugin.almclient.bitbucket.model.DataValue;
import com.github.mc1arke.sonarqube.plugin.almclient.bitbucket.model.ReportStatus;
import com.github.mc1arke.sonarqube.plugin.almclient.bitbucket.model.cloud.CloudAnnotation;
import com.github.mc1arke.sonarqube.plugin.almclient.bitbucket.model.cloud.CloudCreateReportRequest;
import com.google.common.collect.Sets;
import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.times;

class BitbucketCloudClientUnitTest {

    private final ObjectMapper mapper = mock();
    private final OkHttpClient client = mock();
    private final BitbucketCloudClient underTest = new BitbucketCloudClient(mapper, client, new BitbucketConfiguration("project", "repository"));

    @BeforeEach
    void setup() {
        Call call = mock();
        when(client.newCall(any())).thenReturn(call);
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
        verify(client, times(2)).newCall(captor.capture());
        Request request = captor.getValue();
        assertEquals("PUT", request.method());
        assertEquals("https://api.bitbucket.org/2.0/repositories/project/repository/commit/commit/reports/reportKey", request.url().toString());
    }

    @Test
    void testDeleteReport() throws IOException {
        // given
        Call call = mock();
        Response response = mock();
        ArgumentCaptor<Request> captor = ArgumentCaptor.captor();

        when(client.newCall(any())).thenReturn(call);
        when(call.execute()).thenReturn(response);

        // when
        underTest.deleteExistingReport("commit", "reportKey");

        // then
        verify(client).newCall(captor.capture());
        Request request = captor.getValue();
        assertEquals("DELETE", request.method());
        assertEquals("https://api.bitbucket.org/2.0/repositories/project/repository/commit/commit/reports/reportKey", request.url().toString());
    }

    @Test
    void testUploadAnnotations() throws IOException {
        // given
        CloudAnnotation annotation = mock();
        Set<CodeInsightsAnnotation> annotations = Sets.newHashSet(annotation);
        Call call = mock();
        Response response = mock();
        ArgumentCaptor<Request> captor = ArgumentCaptor.captor();

        when(client.newCall(any())).thenReturn(call);
        when(call.execute()).thenReturn(response);
        when(response.isSuccessful()).thenReturn(true);

        when(mapper.writeValueAsString(any())).thenReturn("{payload}");

        // when
        underTest.uploadAnnotations("commit", annotations, "reportKey");

        // then
        verify(client).newCall(captor.capture());
        Request request = captor.getValue();
        assertEquals("POST", request.method());
        assertEquals("https://api.bitbucket.org/2.0/repositories/project/repository/commit/commit/reports/reportKey/annotations", request.url().toString());
    }

    @Test
    void testUploadLimit() {
        // given
        // when
        AnnotationUploadLimit annotationUploadLimit = underTest.getAnnotationUploadLimit();

        // then
        assertEquals(100, annotationUploadLimit.getAnnotationBatchSize());
        assertEquals(1000, annotationUploadLimit.getTotalAllowedAnnotations());
    }

    @Test
    void testUploadReportFailsWithMessage() throws IOException {
        // given
        CodeInsightsReport report = mock();
        Call call = mock();
        Response response = mock();
        ResponseBody responseBody = mock();

        when(client.newCall(any())).thenReturn(call);
        when(call.execute()).thenReturn(response);
        when(response.isSuccessful()).thenReturn(false);
        when(response.body()).thenReturn(responseBody);
        when(responseBody.string()).thenReturn("error!");
        when(response.code()).thenReturn(400);

        when(mapper.writeValueAsString(report)).thenReturn("{payload}");

        // when,then
        assertThatThrownBy(() -> underTest.uploadReport("commit", report, "reportKey"))
                .isInstanceOf(BitbucketCloudException.class)
                .hasMessage("HTTP Status Code: 400; Message:error!")
                .extracting(e -> ((BitbucketCloudException) e).isError(400))
                .isEqualTo(true);
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
    void testCreateAnnotationForCloud() {
        // given

        // when
        CodeInsightsAnnotation annotation = underTest.createCodeInsightsAnnotation("issueKey", 12, "http://localhost:9000/dashboard", "Failed", "/path/to/file", "MAJOR", "BUG");

        // then
        assertInstanceOf(CloudAnnotation.class, annotation);
        assertEquals("issueKey", ((CloudAnnotation) annotation).getExternalId());
        assertEquals(12, annotation.getLine());
        assertEquals("http://localhost:9000/dashboard", ((CloudAnnotation) annotation).getLink());
        assertEquals("/path/to/file", annotation.getPath());
        assertEquals("MAJOR", annotation.getSeverity());
        assertEquals("BUG", ((CloudAnnotation) annotation).getAnnotationType());
    }

    @Test
    void testCreateDataLinkForCloud() {
        // given

        // when
        DataValue data = underTest.createLinkDataValue("https://localhost:9000/any/project");

        // then
        assertInstanceOf(DataValue.CloudLink.class, data);
        assertEquals("https://localhost:9000/any/project", ((DataValue.CloudLink) data).getHref());
    }

    @Test
    void testCloudAlwaysSupportsCodeInsights() {
        // given

        // when
        boolean result = underTest.supportsCodeInsights();

        // then
        assertTrue(result);
    }

    @Test
    void testCreateCloudReport() {
        // given

        // when
        CodeInsightsReport result = underTest.createCodeInsightsReport(new ArrayList<>(), "reportDescription", Instant.now(), "dashboardUrl", "logoUrl", ReportStatus.FAILED);

        // then
        assertInstanceOf(CloudCreateReportRequest.class, result);
        assertEquals(0, result.getData().size());
        assertEquals("reportDescription", result.getDetails());
        assertEquals("dashboardUrl", result.getLink());
        assertEquals("logoUrl", ((CloudCreateReportRequest) result).getLogoUrl());
        assertEquals("FAILED", result.getResult());

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
        assertThat(request.url()).hasToString("https://api.bitbucket.org/2.0/repositories/project/repository/commit/commit/statuses/build");

        verify(mapper).writeValueAsString(buildStatus);
    }
}
