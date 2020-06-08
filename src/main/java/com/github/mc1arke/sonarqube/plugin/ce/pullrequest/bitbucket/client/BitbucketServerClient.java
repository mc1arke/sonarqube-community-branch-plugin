/*
 * Copyright (C) 2020 Mathias Åhsberg
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
package com.github.mc1arke.sonarqube.plugin.ce.pullrequest.bitbucket.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.bitbucket.client.model.BitbucketConfiguration;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.bitbucket.client.model.CodeInsightsAnnotation;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.bitbucket.client.model.CodeInsightsReport;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.bitbucket.client.model.DataValue;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.bitbucket.client.model.ReportData;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.bitbucket.client.model.server.Annotation;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.bitbucket.client.model.server.CreateAnnotationsRequest;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.bitbucket.client.model.server.CreateReportRequest;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.bitbucket.client.model.server.ErrorResponse;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.bitbucket.client.model.server.ServerProperties;
import com.google.common.annotations.VisibleForTesting;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.sonar.api.ce.posttask.QualityGate;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static java.lang.String.format;

public class BitbucketServerClient implements BitbucketClient {
    private static final Logger LOGGER = Loggers.get(BitbucketServerClient.class);
    private static final String REPORT_KEY = "com.github.mc1arke.sonarqube";
    private static final MediaType APPLICATION_JSON_MEDIA_TYPE = MediaType.get("application/json");
    private static final String TITLE = "SonarQube";
    private static final String REPORTER = "SonarQube";
    private static final String LINK_TEXT = "Go to SonarQube";

    private final BitbucketConfiguration config;
    private final ObjectMapper objectMapper;

    public BitbucketServerClient(BitbucketConfiguration config, ObjectMapper objectMapper) {
        this.config = config;
        this.objectMapper = objectMapper;
    }

    @Override
    public CodeInsightsAnnotation createCodeInsightsAnnotation(String issueKey, int line, String issueUrl, String message, String path, String severity, String type) {
        return new Annotation(issueKey,
                line,
                issueUrl,
                message,
                path,
                severity,
                type);
    }

    @Override
    public CodeInsightsReport createCodeInsightsReport(List<ReportData> reportData, String reportDescription, Instant creationDate, String dashboardUrl, String logoUrl, QualityGate.Status status) {
        return new CreateReportRequest(
                reportData,
                reportDescription,
                TITLE,
                REPORTER,
                creationDate,
                dashboardUrl,
                logoUrl,
                QualityGate.Status.ERROR.equals(status) ? "FAIL" : "PASS"
        );
    }

    public void deleteAnnotations(String project, String repository, String commit) throws IOException {
        Request req = new Request.Builder()
                .delete()
                .url(format("%s/rest/insights/1.0/projects/%s/repos/%s/commits/%s/reports/%s/annotations", config.getUrl(), project, repository, commit, REPORT_KEY))
                .build();
        try (Response response = getClient().newCall(req).execute()) {
            validate(response);
        }
    }

    @Override
    public void uploadAnnotations(String project, String repository, String commit, Set<CodeInsightsAnnotation> annotations) throws IOException {
        Set<Annotation> annotationSet = annotations.stream().map(annotation -> (Annotation) annotation).collect(Collectors.toSet());
        CreateAnnotationsRequest request = new CreateAnnotationsRequest(annotationSet);
        if (request.getAnnotations().isEmpty()) {
            return;
        }
        Request req = new Request.Builder()
                .post(RequestBody.create(APPLICATION_JSON_MEDIA_TYPE, objectMapper.writeValueAsString(request)))
                .url(format("%s/rest/insights/1.0/projects/%s/repos/%s/commits/%s/reports/%s/annotations", config.getUrl(), project, repository, commit, REPORT_KEY))
                .build();
        try (Response response = getClient().newCall(req).execute()) {
            validate(response);
        }
    }

    @Override
    public DataValue createLinkDataValue(String dashboardUrl) {
        return new DataValue.Link(LINK_TEXT, dashboardUrl);
    }

    @Override
    public void uploadReport(String project, String repository, String commit, CodeInsightsReport codeInsightReport) throws IOException {
        String body = objectMapper.writeValueAsString(codeInsightReport);
        Request req = new Request.Builder()
                .put(RequestBody.create(APPLICATION_JSON_MEDIA_TYPE, body))
                .url(format("%s/rest/insights/1.0/projects/%s/repos/%s/commits/%s/reports/%s", config.getUrl(), project, repository, commit, REPORT_KEY))
                .build();

        try (Response response = getClient().newCall(req).execute()) {
            validate(response);
        }
    }

    @Override
    public boolean supportsCodeInsights() {
        try {
            ServerProperties server = getServerProperties();
            LOGGER.debug(format("Your Bitbucket Server installation is version %s", server.getVersion()));
            if (server.hasCodeInsightsApi()) {
                return true;
            } else {
                LOGGER.info("Bitbucket Server version is to old. %s is the minimum version that supports Code Insights",
                        ServerProperties.CODE_INSIGHT_VERSION);
            }
        } catch (IOException e) {
            LOGGER.error("Could not determine Bitbucket Server version", e);
            return false;
        }
        return false;
    }

    public ServerProperties getServerProperties() throws IOException {
        Request req = new Request.Builder()
                .get()
                .url(format("%s/rest/api/1.0/application-properties", config.getUrl()))
                .build();
        try (Response response = getClient().newCall(req).execute()) {
            validate(response);

            return objectMapper.reader().forType(ServerProperties.class)
                    .readValue(Optional.ofNullable(response.body())
                            .orElseThrow(() -> new IllegalStateException("No response body from BitBucket"))
                            .string());
        }
    }

    @VisibleForTesting
    OkHttpClient getClient() {
        return new OkHttpClient.Builder()
                .addInterceptor(chain -> {
                    Request newRequest = chain.request().newBuilder()
                            .addHeader("Authorization", format("Bearer %s", config.getToken()))
                            .addHeader("Accept", APPLICATION_JSON_MEDIA_TYPE.toString())
                            .build();
                    return chain.proceed(newRequest);
                })
                .build();
    }

    void validate(Response response) throws IOException {
        if (!response.isSuccessful()) {
            ErrorResponse errors = null;
            if (response.body() != null) {
                errors = objectMapper.reader().forType(ErrorResponse.class)
                        .readValue(response.body().string());
            }
            throw new BitbucketException(response.code(), errors);
        }
    }
}
