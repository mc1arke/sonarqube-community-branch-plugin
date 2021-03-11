/*
 * Copyright (C) 2020-2021 Marvin Wichmann, Michael Clarke
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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.bitbucket.client.model.AnnotationUploadLimit;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.bitbucket.client.model.CodeInsightsAnnotation;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.bitbucket.client.model.CodeInsightsReport;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.bitbucket.client.model.DataValue;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.bitbucket.client.model.ReportData;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.bitbucket.client.model.cloud.BitbucketCloudConfiguration;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.bitbucket.client.model.cloud.CloudAnnotation;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.bitbucket.client.model.cloud.CloudCreateReportRequest;
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
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static java.lang.String.format;


public class BitbucketCloudClient implements BitbucketClient {

    private static final Logger LOGGER = Loggers.get(BitbucketCloudClient.class);
    private static final String REPORT_KEY = "com.github.mc1arke.sonarqube";
    private static final MediaType APPLICATION_JSON_MEDIA_TYPE = MediaType.get("application/json");
    private static final String TITLE = "SonarQube";
    private static final String REPORTER = "SonarQube";
    private static final String LINK_TEXT = "Go to SonarQube";

    private final ObjectMapper objectMapper;
    private final String bearerToken;
    private final Supplier<OkHttpClient.Builder> okHttpClientBuilderSupplier;

    public BitbucketCloudClient(BitbucketCloudConfiguration config, ObjectMapper objectMapper) {
        this(config, objectMapper, OkHttpClient.Builder::new);
    }

    BitbucketCloudClient(BitbucketCloudConfiguration config, ObjectMapper objectMapper, Supplier<OkHttpClient.Builder> okHttpClientBuilderSupplier) {
        this.objectMapper = objectMapper;
        this.okHttpClientBuilderSupplier = okHttpClientBuilderSupplier;
        this.bearerToken = negotiateBearerToken(config, objectMapper, okHttpClientBuilderSupplier.get().build());
    }

    private static String negotiateBearerToken(BitbucketCloudConfiguration bitbucketCloudConfiguration, ObjectMapper objectMapper, OkHttpClient okHttpClient) {
        Request request = new Request.Builder()
                .header("Authorization", "Basic " + Base64.getEncoder().encodeToString((bitbucketCloudConfiguration.getClientId() + ":" + bitbucketCloudConfiguration.getSecret()).getBytes(
                        StandardCharsets.UTF_8)))
                .url("https://bitbucket.org/site/oauth2/access_token")
                .post(RequestBody.create("grant_type=client_credentials", MediaType.parse("application/x-www-form-urlencoded")))
                .build();

        try (Response response = okHttpClient.newCall(request).execute()) {
            AuthToken authToken = objectMapper.readValue(
                    Optional.ofNullable(response.body()).orElseThrow(() -> new IllegalStateException("No response returned by Bitbucket Oauth")).string(), AuthToken.class);
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
                                                       QualityGate.Status status) {
        return new CloudCreateReportRequest(
                reportData,
                reportDescription,
                TITLE,
                REPORTER,
                Date.from(creationDate),
                dashboardUrl, // you need to change this to a real https URL for local debugging since localhost will get declined by the API
                logoUrl,
                "COVERAGE",
                QualityGate.Status.ERROR.equals(status) ? "FAILED" : "PASSED"
        );
    }

    @Override
    public void deleteAnnotations(String project, String repo, String commitSha) {
        // not needed here.
    }

    public void uploadAnnotations(String project, String repository, String commit, Set<CodeInsightsAnnotation> baseAnnotations) throws IOException {
        Set<CloudAnnotation> annotations = baseAnnotations.stream().map(annotation -> (CloudAnnotation) annotation).collect(Collectors.toSet());

        if (annotations.isEmpty()) {
            return;
        }

        Request req = new Request.Builder()
                .post(RequestBody.create(objectMapper.writeValueAsString(annotations), APPLICATION_JSON_MEDIA_TYPE))
                .url(format("https://api.bitbucket.org/2.0/repositories/%s/%s/commit/%s/reports/%s/annotations", project, repository, commit, REPORT_KEY))
                .build();

        LOGGER.info("Creating annotations on bitbucket cloud");
        LOGGER.debug("Create annotations: " + objectMapper.writeValueAsString(annotations));

        try (Response response = getClient().newCall(req).execute()) {
            validate(response);
        }
    }

    @Override
    public DataValue createLinkDataValue(String dashboardUrl) {
        return new DataValue.CloudLink(LINK_TEXT, dashboardUrl);
    }

    @Override
    public void uploadReport(String project, String repository, String commit, CodeInsightsReport codeInsightReport) throws IOException {
        deleteExistingReport(project, repository, commit);

        String targetUrl = format("https://api.bitbucket.org/2.0/repositories/%s/%s/commit/%s/reports/%s", project, repository, commit, REPORT_KEY);
        String body = objectMapper.writeValueAsString(codeInsightReport);
        Request req = new Request.Builder()
                .put(RequestBody.create(body, APPLICATION_JSON_MEDIA_TYPE))
                .url(targetUrl)
                .build();

        LOGGER.info("Create report on bitbucket cloud: " + targetUrl);
        LOGGER.debug("Create report: " + body);

        try (Response response = getClient().newCall(req).execute()) {
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
    public String resolveProject(AlmSettingDto almSettingDto, ProjectAlmSettingDto projectAlmSettingDto) {
        return almSettingDto.getAppId();
    }

    @Override
    public String resolveRepository(AlmSettingDto almSettingDto, ProjectAlmSettingDto projectAlmSettingDto) {
        return projectAlmSettingDto.getAlmRepo();
    }

    void deleteExistingReport(String project, String repository, String commit) throws IOException {
        Request req = new Request.Builder()
                .delete()
                .url(format("https://api.bitbucket.org/2.0/repositories/%s/%s/commit/%s/reports/%s", project, repository, commit, REPORT_KEY))
                .build();

        LOGGER.info("Deleting existing reports on bitbucket cloud");

        try (Response response = getClient().newCall(req).execute()) {
            // we dont need to validate the output here since most of the time this call will just return a 404
        }
    }

    private OkHttpClient getClient() {
        return okHttpClientBuilderSupplier.get()
                .addInterceptor(chain -> {
                    Request newRequest = chain.request().newBuilder()
                            .addHeader("Authorization", format("Bearer %s", bearerToken))
                            .addHeader("Accept", APPLICATION_JSON_MEDIA_TYPE.toString())
                            .build();
                    return chain.proceed(newRequest);
                })
                .build();
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

    static class AuthToken {

        private final String accessToken;

        AuthToken(@JsonProperty("access_token") String accessToken) {
            this.accessToken = accessToken;
        }

        String getAccessToken() {
            return accessToken;
        }
    }
}
