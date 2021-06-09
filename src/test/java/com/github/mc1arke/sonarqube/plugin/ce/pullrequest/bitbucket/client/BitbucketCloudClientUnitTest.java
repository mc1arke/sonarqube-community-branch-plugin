package com.github.mc1arke.sonarqube.plugin.ce.pullrequest.bitbucket.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.bitbucket.client.model.AnnotationUploadLimit;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.bitbucket.client.model.CodeInsightsAnnotation;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.bitbucket.client.model.CodeInsightsReport;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.bitbucket.client.model.DataValue;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.bitbucket.client.model.cloud.CloudAnnotation;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.bitbucket.client.model.cloud.CloudCreateReportRequest;
import com.google.common.collect.Sets;
import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.sonar.api.ce.posttask.QualityGate;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.times;

@RunWith(MockitoJUnitRunner.class)
public class BitbucketCloudClientUnitTest {

    private BitbucketCloudClient underTest;

    @Mock
    private ObjectMapper mapper;

    @Mock
    private OkHttpClient client;

    @Before
    public void before() {
        Call call = mock(Call.class);
        when(client.newCall(any())).thenReturn(call);
        underTest = new BitbucketCloudClient(mapper, client);
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
        underTest.uploadReport("project", "repository", "commit", report);

        // then
        verify(client, times(2)).newCall(captor.capture());
        Request request = captor.getValue();
        assertEquals("PUT", request.method());
        assertEquals("https://api.bitbucket.org/2.0/repositories/project/repository/commit/commit/reports/com.github.mc1arke.sonarqube", request.url().toString());
    }

    @Test
    public void testDeleteReport() throws IOException {
        // given
        Call call = mock(Call.class);
        Response response = mock(Response.class);
        ArgumentCaptor<Request> captor = ArgumentCaptor.forClass(Request.class);

        when(client.newCall(any())).thenReturn(call);
        when(call.execute()).thenReturn(response);

        // when
        underTest.deleteExistingReport("project", "repository", "commit");

        // then
        verify(client, times(1)).newCall(captor.capture());
        Request request = captor.getValue();
        assertEquals("DELETE", request.method());
        assertEquals("https://api.bitbucket.org/2.0/repositories/project/repository/commit/commit/reports/com.github.mc1arke.sonarqube", request.url().toString());
    }

    @Test
    public void testUploadAnnotations() throws IOException {
        // given
        CodeInsightsAnnotation annotation = mock(CloudAnnotation.class);
        Set<CodeInsightsAnnotation> annotations = Sets.newHashSet(annotation);
        Call call = mock(Call.class);
        Response response = mock(Response.class);
        ArgumentCaptor<Request> captor = ArgumentCaptor.forClass(Request.class);

        when(client.newCall(any())).thenReturn(call);
        when(call.execute()).thenReturn(response);
        when(response.isSuccessful()).thenReturn(true);

        when(mapper.writeValueAsString(any())).thenReturn("{payload}");

        // when
        underTest.uploadAnnotations("project", "repository", "commit", annotations);

        // then
        verify(client, times(1)).newCall(captor.capture());
        Request request = captor.getValue();
        assertEquals("POST", request.method());
        assertEquals("https://api.bitbucket.org/2.0/repositories/project/repository/commit/commit/reports/com.github.mc1arke.sonarqube/annotations", request.url().toString());
    }

    @Test
    public void testUploadLimit() {
        // given
        // when
        AnnotationUploadLimit annotationUploadLimit = underTest.getAnnotationUploadLimit();

        // then
        assertEquals(100, annotationUploadLimit.getAnnotationBatchSize());
        assertEquals(1000, annotationUploadLimit.getTotalAllowedAnnotations());
    }

    @Test
    public void testUploadReportFailsWithMessage() throws IOException {
        // given
        CodeInsightsReport report = mock(CodeInsightsReport.class);
        Call call = mock(Call.class);
        Response response = mock(Response.class);
        ResponseBody responseBody = mock(ResponseBody.class);

        when(client.newCall(any())).thenReturn(call);
        when(call.execute()).thenReturn(response);
        when(response.isSuccessful()).thenReturn(false);
        when(response.body()).thenReturn(responseBody);
        when(responseBody.string()).thenReturn("error!");
        when(response.code()).thenReturn(400);

        when(mapper.writeValueAsString(report)).thenReturn("{payload}");

        // when,then
        assertThatThrownBy(() -> underTest.uploadReport("project", "repository", "commit", report))
                .isInstanceOf(BitbucketCloudException.class)
                .hasMessage("HTTP Status Code: 400; Message:error!")
                .extracting(e -> ((BitbucketCloudException) e).isError(400))
                .isEqualTo(true);
    }

    @Test
    public void testUploadAnnotationsWithEmptyAnnotations() throws IOException {
        // given
        Set<CodeInsightsAnnotation> annotations = Sets.newHashSet();

        // when
        underTest.uploadAnnotations("project", "repository", "commit", annotations);

        // then
        verify(client, times(0)).newCall(any());
    }

    @Test
    public void testCreateAnnotationForCloud() {
        // given

        // when
        CodeInsightsAnnotation annotation = underTest.createCodeInsightsAnnotation("issueKey", 12, "http://localhost:9000/dashboard", "Failed", "/path/to/file", "MAJOR", "BUG");

        // then
        assertTrue(annotation instanceof CloudAnnotation);
        assertEquals("issueKey", ((CloudAnnotation) annotation).getExternalId());
        assertEquals(12, ((CloudAnnotation) annotation).getLine());
        assertEquals("http://localhost:9000/dashboard", ((CloudAnnotation) annotation).getLink());
        assertEquals("/path/to/file", ((CloudAnnotation) annotation).getPath());
        assertEquals("MAJOR", ((CloudAnnotation) annotation).getSeverity());
        assertEquals("BUG", ((CloudAnnotation) annotation).getAnnotationType());
    }

    @Test
    public void testCreateDataLinkForCloud() {
        // given

        // when
        DataValue data = underTest.createLinkDataValue("https://localhost:9000/any/project");

        // then
        assertTrue(data instanceof DataValue.CloudLink);
        assertEquals("https://localhost:9000/any/project", ((DataValue.CloudLink) data).getHref());
    }

    @Test
    public void testCloudAlwaysSupportsCodeInsights() {
        // given

        // when
        boolean result = underTest.supportsCodeInsights();

        // then
        assertTrue(result);
    }

    @Test
    public void testCreateCloudReport() {
        // given

        // when
        CodeInsightsReport result = underTest.createCodeInsightsReport(new ArrayList<>(), "reportDescription", Instant.now(), "dashboardUrl", "logoUrl", QualityGate.Status.ERROR);

        // then
        assertTrue(result instanceof CloudCreateReportRequest);
        assertEquals(0, ((CloudCreateReportRequest) result).getData().size());
        assertEquals("reportDescription", ((CloudCreateReportRequest) result).getDetails());
        assertEquals("dashboardUrl", ((CloudCreateReportRequest) result).getLink());
        assertEquals("logoUrl", ((CloudCreateReportRequest) result).getLogoUrl());
        assertEquals("FAILED", ((CloudCreateReportRequest) result).getResult());

    }
}
