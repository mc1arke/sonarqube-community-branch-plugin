/*
 * Copyright (C) 2020-2022 Marvin Wichmann, Michael Clarke
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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.mc1arke.sonarqube.plugin.almclient.bitbucket.model.AnnotationUploadLimit;
import com.github.mc1arke.sonarqube.plugin.almclient.bitbucket.model.BitbucketConfiguration;
import com.github.mc1arke.sonarqube.plugin.almclient.bitbucket.model.CodeInsightsAnnotation;
import com.github.mc1arke.sonarqube.plugin.almclient.bitbucket.model.CodeInsightsReport;
import com.github.mc1arke.sonarqube.plugin.almclient.bitbucket.model.DataValue;
import com.github.mc1arke.sonarqube.plugin.almclient.bitbucket.model.ReportData;
import com.github.mc1arke.sonarqube.plugin.almclient.bitbucket.model.ReportStatus;
import com.github.mc1arke.sonarqube.plugin.almclient.bitbucket.model.Repository;
import com.github.mc1arke.sonarqube.plugin.almclient.bitbucket.model.cloud.CloudAnnotation;
import com.github.mc1arke.sonarqube.plugin.almclient.bitbucket.model.cloud.CloudCreateReportRequest;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static java.lang.String.format;

class BitbucketCloudClient implements BitbucketClient {

    private static final Logger LOGGER = Loggers.get(BitbucketCloudClient.class);
    private static final MediaType APPLICATION_JSON_MEDIA_TYPE = MediaType.get("application/json");
    private static final String TITLE = "SonarQube";
    private static final String REPORTER = "SonarQube";
    private static final String LINK_TEXT = "Go to SonarQube";

    private final ObjectMapper objectMapper;
    private final OkHttpClient okHttpClient;
    private final BitbucketConfiguration bitbucketConfiguration;


    BitbucketCloudClient(ObjectMapper objectMapper, OkHttpClient okHttpClient, BitbucketConfiguration bitbucketConfiguration) {
        this.objectMapper = objectMapper;
        this.okHttpClient = okHttpClient;
        this.bitbucketConfiguration = bitbucketConfiguration;
    }

    static String negotiateBearerToken(String clientId, String clientSecret, ObjectMapper objectMapper, OkHttpClient okHttpClient) {
        Request request = new Request.Builder()
                .header("Authorization", "Basic " + Base64.getEncoder().encodeToString((clientId + ":" + clientSecret).getBytes(StandardCharsets.UTF_8)))
                .url("https://bitbucket.org/site/oauth2/access_token")
                .post(RequestBody.create("grant_type=client_credentials", MediaType.parse("application/x-www-form-urlencoded")))
                .build();

        try (Response response = okHttpClient.newCall(request).execute()) {
            BitbucketCloudClient.AuthToken authToken = objectMapper.readValue(
                    Optional.ofNullable(response.body()).orElseThrow(() -> new IllegalStateException("No response returned by Bitbucket Oauth")).string(), BitbucketCloudClient.AuthToken.class);
            return authToken.getAccessToken();
        } catch (IOException ex) {
            throw new IllegalStateException("Could not retrieve bearer token", ex);
        }
    }

    @Override
    public CodeInsightsAnnotation createCodeInsightsAnnotation(String issueKey, int line, String issueUrl, String message,
                                                               String path, String severity, String type) {
        return new CloudAnnotation(issueKey,
                line,
                issueUrl,
                message,
                path,
                severity,
                type);
    }

    @Override
    public CodeInsightsReport createCodeInsightsReport(List<ReportData> reportData, String reportDescription,
                                                       Instant creationDate, String dashboardUrl, String logoUrl,
                                                       ReportStatus status) {
        return new CloudCreateReportRequest(
                reportData,
                reportDescription,
                TITLE,
                REPORTER,
                Date.from(creationDate),
                dashboardUrl, // you need to change this to a real https URL for local debugging since localhost will get declined by the API
                logoUrl,
                "COVERAGE",
                ReportStatus.FAILED == status ? "FAILED" : "PASSED"
        );
    }

    @Override
    public void deleteAnnotations(String commitSha, String reportKey) {
        // not needed here.
    }

    @Override
    public void uploadAnnotations(String commit, Set<CodeInsightsAnnotation> baseAnnotations, String reportKey) throws IOException {
        Set<CloudAnnotation> annotations = baseAnnotations.stream().map(CloudAnnotation.class::cast).collect(Collectors.toSet());

        if (annotations.isEmpty()) {
            return;
        }

        Request req = new Request.Builder()
                .post(RequestBody.create(objectMapper.writeValueAsString(annotations), APPLICATION_JSON_MEDIA_TYPE))
                .url(format("https://api.bitbucket.org/2.0/repositories/%s/%s/commit/%s/reports/%s/annotations", bitbucketConfiguration.getProject(), bitbucketConfiguration.getRepository(), commit, reportKey))
                .build();

        LOGGER.info("Creating annotations on bitbucket cloud");
        LOGGER.debug("Create annotations: " + objectMapper.writeValueAsString(annotations));

        try (Response response = okHttpClient.newCall(req).execute()) {
            validate(response);
        }
    }

    @Override
    public DataValue createLinkDataValue(String dashboardUrl) {
        return new DataValue.CloudLink(LINK_TEXT, dashboardUrl);
    }

    @Override
    public void uploadReport(String commit, CodeInsightsReport codeInsightReport, String reportKey) throws IOException {
        deleteExistingReport(commit, reportKey);

        String targetUrl = format("https://api.bitbucket.org/2.0/repositories/%s/%s/commit/%s/reports/%s", bitbucketConfiguration.getProject(), bitbucketConfiguration.getRepository(), commit, reportKey);
        String body = objectMapper.writeValueAsString(codeInsightReport);
        Request req = new Request.Builder()
                .put(RequestBody.create(body, APPLICATION_JSON_MEDIA_TYPE))
                .url(targetUrl)
                .build();

        LOGGER.info("Create report on bitbucket cloud: " + targetUrl);
        LOGGER.debug("Create report: " + body);

        try (Response response = okHttpClient.newCall(req).execute()) {
            validate(response);
        }
    }

    @Override
    public boolean supportsCodeInsights() {
        return true;
    }

    @Override
    public AnnotationUploadLimit getAnnotationUploadLimit() {
        return new AnnotationUploadLimit(100, 1000);
    }

    @Override
    public Repository retrieveRepository() throws IOException {
        Request req = new Request.Builder()
                .get()
                .url(format("https://api.bitbucket.org/2.0/repositories/%s/%s", bitbucketConfiguration.getProject(), bitbucketConfiguration.getRepository()))
                .build();
        try (Response response = okHttpClient.newCall(req).execute()) {
            validate(response);

            return objectMapper.reader().forType(Repository.class)
                    .readValue(Optional.ofNullable(response.body())
                            .orElseThrow(() -> new IllegalStateException("No response body from BitBucket"))
                            .string());
        }
    }

    void deleteExistingReport(String commit, String reportKey) throws IOException {
        Request req = new Request.Builder()
                .delete()
                .url(format("https://api.bitbucket.org/2.0/repositories/%s/%s/commit/%s/reports/%s", bitbucketConfiguration.getProject(), bitbucketConfiguration.getRepository(), commit, reportKey))
                .build();

        LOGGER.info("Deleting existing reports on bitbucket cloud");

        try (Response response = okHttpClient.newCall(req).execute()) {
            // we dont need to validate the output here since most of the time this call will just return a 404
        }
    }

    void validate(Response response) {
        if (!response.isSuccessful()) {
            String error = Optional.ofNullable(response.body()).map(b -> {
                try {
                    return b.string();
                } catch (IOException e) {
                    throw new IllegalStateException("Could not retrieve response content", e);
                }
            }).orElse("Request failed but Bitbucket didn't respond with a proper error message");
            throw new BitbucketCloudException(response.code(), error);
        }
    }

    private static class AuthToken {

        private final String accessToken;

        AuthToken(@JsonProperty("access_token") String accessToken) {
            this.accessToken = accessToken;
        }

        String getAccessToken() {
            return accessToken;
        }
    }
}
