/*
 * Copyright (C) 2021-2026 Michael Clarke
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

import com.github.mc1arke.sonarqube.plugin.almclient.bitbucket.model.AnnotationUploadLimit;
import com.github.mc1arke.sonarqube.plugin.almclient.bitbucket.model.BitbucketConfiguration;
import com.github.mc1arke.sonarqube.plugin.almclient.bitbucket.model.BuildStatus;
import com.github.mc1arke.sonarqube.plugin.almclient.bitbucket.model.CloudLink;
import com.github.mc1arke.sonarqube.plugin.almclient.bitbucket.model.CodeInsightsAnnotation;
import com.github.mc1arke.sonarqube.plugin.almclient.bitbucket.model.CodeInsightsReport;
import com.github.mc1arke.sonarqube.plugin.almclient.bitbucket.model.DataValue;
import com.github.mc1arke.sonarqube.plugin.almclient.bitbucket.model.ReportStatus;
import com.github.mc1arke.sonarqube.plugin.almclient.bitbucket.model.Repository;
import com.github.mc1arke.sonarqube.plugin.almclient.bitbucket.model.cloud.CloudAnnotation;
import com.github.mc1arke.sonarqube.plugin.almclient.bitbucket.model.cloud.CloudCreateReportRequest;
import com.google.common.collect.Sets;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import okhttp3.Call;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.slf4j.LoggerFactory;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

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

    private final JsonMapper mapper = mock();
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
        assertInstanceOf(CloudLink.class, data);
        assertEquals("https://localhost:9000/any/project", ((CloudLink) data).getHref());
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

    @Test
    void shouldReturnOriginalKeyFromNormalisation() {
        assertThat(underTest.normaliseReportKey("MyVeryLongReportKeyThatWon'tBeModifiedByTheNormalisation"))
                .isEqualTo("MyVeryLongReportKeyThatWon'tBeModifiedByTheNormalisation");
    }

    private static Response successfulResponse(String body) {
        Response response = mock();
        when(response.isSuccessful()).thenReturn(true);
        when(response.body()).thenReturn(ResponseBody.create(body, MediaType.parse("application/json")));
        return response;
    }

    @Test
    void shouldThrowIllegalStateExceptionWhenBearerTokenResponseIsNotValidJson() throws IOException {
        Call call = mock();
        Response response = successfulResponse("this is not json");
        when(client.newCall(any())).thenReturn(call);
        when(call.execute()).thenReturn(response);
        JsonMapper jsonMapper = JsonMapper.builder().build();

        assertThatThrownBy(() -> BitbucketCloudClient.negotiateBearerToken("clientId", "clientSecret", jsonMapper, client))
                .isExactlyInstanceOf(IllegalStateException.class)
                .hasMessage("Could not retrieve bearer token")
                .hasCauseInstanceOf(JacksonException.class);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void shouldUploadAnnotationsWithDebugLoggingEnabled(boolean debugSerialisationFails) throws IOException {
        Logger logger = (Logger) LoggerFactory.getLogger(BitbucketCloudClient.class);
        Level originalLevel = logger.getLevel();
        logger.setLevel(Level.DEBUG);
        try {
            CloudAnnotation annotation = mock();
            Set<CodeInsightsAnnotation> annotations = Sets.newHashSet(annotation);
            Call call = mock();
            Response response = mock();
            when(client.newCall(any())).thenReturn(call);
            when(call.execute()).thenReturn(response);
            when(response.isSuccessful()).thenReturn(true);
            if (debugSerialisationFails) {
                when(mapper.writeValueAsString(any()))
                        .thenReturn("{payload}")
                        .thenThrow(new JacksonException("cannot serialise") {});
            } else {
                when(mapper.writeValueAsString(any())).thenReturn("{payload}");
            }

            underTest.uploadAnnotations("commit", annotations, "reportKey");

            verify(mapper, times(2)).writeValueAsString(any());
            verify(client).newCall(any());
        } finally {
            logger.setLevel(originalLevel);
        }
    }

    @Test
    void shouldRetrieveRepository() throws IOException {
        Call call = mock();
        Response response = successfulResponse("{\"slug\": \"repository\"}");
        when(client.newCall(any())).thenReturn(call);
        when(call.execute()).thenReturn(response);
        BitbucketCloudClient underTest = new BitbucketCloudClient(JsonMapper.builder().build(), client, new BitbucketConfiguration("project", "repository"));

        Repository repository = underTest.retrieveRepository();

        assertThat(repository.getSlug()).isEqualTo("repository");
        ArgumentCaptor<Request> captor = ArgumentCaptor.captor();
        verify(client).newCall(captor.capture());
        assertEquals("GET", captor.getValue().method());
        assertEquals("https://api.bitbucket.org/2.0/repositories/project/repository", captor.getValue().url().toString());
    }
}
