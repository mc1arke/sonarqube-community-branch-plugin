/*
 * Copyright (C) 2020-2021 Mathias Åhsberg, Michael Clarke
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
import com.github.mc1arke.sonarqube.plugin.almclient.bitbucket.model.CodeInsightsAnnotation;
import com.github.mc1arke.sonarqube.plugin.almclient.bitbucket.model.CodeInsightsReport;
import com.github.mc1arke.sonarqube.plugin.almclient.bitbucket.model.DataValue;
import com.github.mc1arke.sonarqube.plugin.almclient.bitbucket.model.ReportData;
import com.github.mc1arke.sonarqube.plugin.almclient.bitbucket.model.Repository;
import com.github.mc1arke.sonarqube.plugin.almclient.bitbucket.model.server.Annotation;
import com.github.mc1arke.sonarqube.plugin.almclient.bitbucket.model.server.BitbucketServerConfiguration;
import com.github.mc1arke.sonarqube.plugin.almclient.bitbucket.model.server.CreateAnnotationsRequest;
import com.github.mc1arke.sonarqube.plugin.almclient.bitbucket.model.server.CreateReportRequest;
import com.github.mc1arke.sonarqube.plugin.almclient.bitbucket.model.server.ErrorResponse;
import com.github.mc1arke.sonarqube.plugin.almclient.bitbucket.model.server.ServerProperties;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.sonar.api.ce.posttask.QualityGate;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.db.alm.setting.AlmSettingDto;
import org.sonar.db.alm.setting.ProjectAlmSettingDto;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static java.lang.String.format;

class BitbucketServerClient implements BitbucketClient {
    private static final Logger LOGGER = Loggers.get(BitbucketServerClient.class);
    private static final String REPORT_KEY = "com.github.mc1arke.sonarqube";
    private static final MediaType APPLICATION_JSON_MEDIA_TYPE = MediaType.get("application/json");
    private static final String TITLE = "SonarQube";
    private static final String REPORTER = "SonarQube";
    private static final String LINK_TEXT = "Go to SonarQube";

    private final BitbucketServerConfiguration config;
    private final ObjectMapper objectMapper;
    private final OkHttpClient okHttpClient;

    BitbucketServerClient(BitbucketServerConfiguration config, ObjectMapper objectMapper, OkHttpClient.Builder baseClientBuilder) {
        this(config, objectMapper, createAuthorisingClient(baseClientBuilder, config));
    }

    BitbucketServerClient(BitbucketServerConfiguration config, ObjectMapper objectMapper, OkHttpClient okHttpClient) {
        this.config = config;
        this.objectMapper = objectMapper;
        this.okHttpClient = okHttpClient;
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
        try (Response response = okHttpClient.newCall(req).execute()) {
            validate(response);
        }
    }

    @Override
    public void uploadAnnotations(String project, String repository, String commit, Set<CodeInsightsAnnotation> annotations) throws IOException {
        if (annotations.isEmpty()) {
            return;
        }
        Set<Annotation> annotationSet = annotations.stream().map(Annotation.class::cast).collect(Collectors.toSet());
        CreateAnnotationsRequest request = new CreateAnnotationsRequest(annotationSet);
        Request req = new Request.Builder()
                .post(RequestBody.create(objectMapper.writeValueAsString(request), APPLICATION_JSON_MEDIA_TYPE))
                .url(format("%s/rest/insights/1.0/projects/%s/repos/%s/commits/%s/reports/%s/annotations", config.getUrl(), project, repository, commit, REPORT_KEY))
                .build();
        try (Response response = okHttpClient.newCall(req).execute()) {
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
                .put(RequestBody.create(body, APPLICATION_JSON_MEDIA_TYPE))
                .url(format("%s/rest/insights/1.0/projects/%s/repos/%s/commits/%s/reports/%s", config.getUrl(), project, repository, commit, REPORT_KEY))
                .build();

        try (Response response = okHttpClient.newCall(req).execute()) {
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

    @Override
    public AnnotationUploadLimit getAnnotationUploadLimit() {
        return new AnnotationUploadLimit(1000, 1000);
    }

    @Override
    public String resolveProject(AlmSettingDto almSettingDto, ProjectAlmSettingDto projectAlmSettingDto) {
        return projectAlmSettingDto.getAlmRepo();
    }

    @Override
    public String resolveRepository(AlmSettingDto almSettingDto, ProjectAlmSettingDto projectAlmSettingDto) {
        return projectAlmSettingDto.getAlmSlug();
    }

    @Override
    public Repository retrieveRepository(String project, String repo) throws IOException {
        Request req = new Request.Builder()
                .get()
                .url(format("%s/rest/api/1.0/projects/%s/repos/%s", config.getUrl(), project, repo))
                .build();
        try (Response response = okHttpClient.newCall(req).execute()) {
            validate(response);

            return objectMapper.reader().forType(Repository.class)
                    .readValue(Optional.ofNullable(response.body())
                            .orElseThrow(() -> new IllegalStateException("No response body from BitBucket"))
                            .string());
        }
    }

    public ServerProperties getServerProperties() throws IOException {
        Request req = new Request.Builder()
                .get()
                .url(format("%s/rest/api/1.0/application-properties", config.getUrl()))
                .build();
        try (Response response = okHttpClient.newCall(req).execute()) {
            validate(response);

            return objectMapper.reader().forType(ServerProperties.class)
                    .readValue(Optional.ofNullable(response.body())
                            .orElseThrow(() -> new IllegalStateException("No response body from BitBucket"))
                            .string());
        }
    }

    private static OkHttpClient createAuthorisingClient(OkHttpClient.Builder clientBuilder, BitbucketServerConfiguration config) {
        return clientBuilder.addInterceptor(chain -> {
                    Request newRequest = chain.request().newBuilder()
                            .addHeader("Authorization", format("Bearer %s", config.getPersonalAccessToken()))
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

    @Override
    public void setApproval(boolean approve, String project, String repository, String prId) throws IOException {
        if (prId == null) {
            return;
        }

        String reqURL = format("%s/rest/api/1.0/projects/%s/repos/%s/pull-requests/%s/approve", config.getUrl(), project, repository, prId);
        // relying on approve response error code we can understand if already
        // approved without extra calls on activity resource
        if (!approve) {
            Request req = new Request.Builder()
                    .delete()
                    .url(reqURL)
                    .build();
            try (Response response = okHttpClient.newCall(req).execute()) {
                if (response.code() == 404) {
                    // it is already not approved
                    LOGGER.info("Pull request {} already disapproved", prId);
                } else {
                    validate(response);
                    LOGGER.debug("Pull request {} disapproved", prId);
                }
            }
        } else {
            Request req = new Request.Builder()
                    .post(RequestBody.create(new byte[0]))
                    .url(reqURL)
                    .build();
            try (Response response = okHttpClient.newCall(req).execute()) {
                if (response.code() == 409) {
                    // it is already approved nothing to do
                    LOGGER.info("Pull request {} already approved", prId);
                } else {
                    validate(response);
                    LOGGER.debug("Pull request {} approved", prId);
                }
            }
        }
    }

}