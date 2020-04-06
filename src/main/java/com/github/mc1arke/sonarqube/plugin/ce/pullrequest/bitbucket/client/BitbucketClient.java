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

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.bitbucket.client.model.CreateAnnotationsRequest;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.bitbucket.client.model.CreateReportRequest;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.bitbucket.client.model.ErrorResponse;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.bitbucket.client.model.ServerProperties;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.bitbucket.BitbucketServerPullRequestDecorator;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.sonar.api.ce.ComputeEngineSide;
import org.sonar.api.config.Configuration;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;

import java.io.IOException;
import java.util.Optional;

import static java.lang.String.format;

@ComputeEngineSide
public class BitbucketClient {
    private static final Logger LOGGER = Loggers.get(BitbucketClient.class);
    private static final String REPORT_KEY = "com.github.mc1arke.sonarqube";
    private final Configuration configuration;

    private OkHttpClient client;
    private ObjectMapper objectMapper;

    public BitbucketClient(Configuration configuration) {
        this.configuration = configuration;
    }

    public boolean isConfigured() {
        return configuration.hasKey(BitbucketServerPullRequestDecorator.PULL_REQUEST_BITBUCKET_URL) &&
                configuration.hasKey(BitbucketServerPullRequestDecorator.PULL_REQUEST_BITBUCKET_TOKEN);
    }

    public ServerProperties getServerProperties() throws IOException {
        Request req = new Request.Builder()
                .get()
                .url(format("%s/rest/api/1.0/application-properties", baseUrl()))
                .build();
        try (Response response = getClient().newCall(req).execute()) {
            validate(response);

            return getObjectMapper().reader().forType(ServerProperties.class)
                    .readValue(response.body().string());
        }
    }

    public void createReport(String project, String repository, String commit, CreateReportRequest request) throws IOException {
        String body = getObjectMapper().writeValueAsString(request);
        Request req = new Request.Builder()
                .put(RequestBody.create(MediaType.parse("application/json"), body))
                .url(format("%s/rest/insights/1.0/projects/%s/repos/%s/commits/%s/reports/%s", baseUrl(), project, repository, commit, REPORT_KEY))
                .build();

        try (Response response = getClient().newCall(req).execute()) {
            validate(response);
        }
    }

    public void createAnnotations(String project, String repository, String commit, CreateAnnotationsRequest request) throws IOException {
        if (request.getAnnotations().isEmpty()) {
            return;
        }
        Request req = new Request.Builder()
                .post(RequestBody.create(MediaType.parse("application/json"), getObjectMapper().writeValueAsString(request)))
                .url(format("%s/rest/insights/1.0/projects/%s/repos/%s/commits/%s/reports/%s/annotations", baseUrl(), project, repository, commit, REPORT_KEY))
                .build();
        try (Response response = getClient().newCall(req).execute()) {
            validate(response);
        }
    }

    public void deleteAnnotations(String project, String repository, String commit) throws IOException {
        Request req = new Request.Builder()
                .delete()
                .url(format("%s/rest/insights/1.0/projects/%s/repos/%s/commits/%s/reports/%s/annotations", baseUrl(), project, repository, commit, REPORT_KEY))
                .build();
        try (Response response = getClient().newCall(req).execute()) {
            validate(response);
        }
    }

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

    private void validate(Response response) throws IOException {
        if (!response.isSuccessful()) {
            ErrorResponse errors = null;
            if (response.body() != null) {
                errors = getObjectMapper().reader().forType(ErrorResponse.class)
                        .readValue(response.body().string());
            }
            throw new BitbucketException(response.code(), errors);
        }
    }

    private OkHttpClient getClient() {
        client = Optional.ofNullable(client).orElseGet(() ->
                new OkHttpClient.Builder()
                        .authenticator(((route, response) ->
                                response.request()
                                        .newBuilder()
                                        .header("Authorization", format("Bearer %s", getToken()))
                                        .header("Accept", "application/json")
                                        .build()
                        ))
                        .build()
        );
        return client;
    }

    private ObjectMapper getObjectMapper() {
        objectMapper = Optional.ofNullable(objectMapper).orElseGet(() -> new ObjectMapper()
                .setSerializationInclusion(Include.NON_NULL)
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        );
        return objectMapper;
    }

    private String baseUrl() {
        return configuration.get(BitbucketServerPullRequestDecorator.PULL_REQUEST_BITBUCKET_URL)
                .orElseThrow(() ->
                        new IllegalArgumentException(format("Missing required property %s", BitbucketServerPullRequestDecorator.PULL_REQUEST_BITBUCKET_URL))
                );
    }

    private String getToken() {
        return configuration.get(BitbucketServerPullRequestDecorator.PULL_REQUEST_BITBUCKET_TOKEN)
                .orElseThrow(() -> new IllegalArgumentException("Personal Access Token for Bitbucket Server is missing"));
    }
}
