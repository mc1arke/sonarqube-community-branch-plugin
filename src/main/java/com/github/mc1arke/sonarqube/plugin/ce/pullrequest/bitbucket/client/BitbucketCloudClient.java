/*
 * Copyright (C) 2020 Marvin Wichmann
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

import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.bitbucket.client.model.cloud.CloudAnnotation;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.bitbucket.client.model.cloud.CloudCreateReportRequest;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.sonar.api.ce.ComputeEngineSide;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;

import java.io.IOException;
import java.util.Set;

import static java.lang.String.format;

@ComputeEngineSide
public class BitbucketCloudClient extends AbstractBitbucketClient {
    private static final Logger LOGGER = Loggers.get(BitbucketCloudClient.class);
    private static final String REPORT_KEY = "com.github.mc1arke.sonarqube";
    private static final MediaType APPLICATION_JSON_MEDIA_TYPE = MediaType.get("application/json");

    void createReport(String project, String repository, String commit, CloudCreateReportRequest request) throws IOException {
        String body = getObjectMapper().writeValueAsString(request);
        Request req = new Request.Builder()
                .put(RequestBody.create(APPLICATION_JSON_MEDIA_TYPE, body))
                .url(format("%s/2.0/repositories/%s/%s/commit/%s/reports/%s", baseUrl(), project, repository, commit, REPORT_KEY))
                .build();

        LOGGER.info("Create report on bitbucket cloud");
        LOGGER.debug("Create report: " + body);

        try (Response response = getClient().newCall(req).execute()) {
            validate(response);
        }
    }

    void createAnnotations(String project, String repository, String commit, Set<CloudAnnotation> annotations) throws IOException {
        if (annotations.isEmpty()) {
            return;
        }

        Request req = new Request.Builder()
                .post(RequestBody.create(APPLICATION_JSON_MEDIA_TYPE, getObjectMapper().writeValueAsString(annotations)))
                .url(format("%s/2.0/repositories/%s/%s/commit/%s/reports/%s/annotations", baseUrl(), project, repository, commit, REPORT_KEY))
                .build();

        LOGGER.info("Creating annotations on bitbucket cloud");
        LOGGER.debug("Create annotations: " + getObjectMapper().writeValueAsString(annotations));

        try (Response response = getClient().newCall(req).execute()) {
            validate(response);
        }
    }

    void deleteReport(String project, String repository, String commit) throws IOException {
        Request req = new Request.Builder()
                .delete()
                .url(format("%s/2.0/repositories/%s/%s/commit/%s/reports/%s", baseUrl(), project, repository, commit, REPORT_KEY))
                .build();

        LOGGER.info("Deleting existing reports on bitbucket cloud");

        try (Response response = getClient().newCall(req).execute()) {
            // we dont need to validate the output here since most of the time this call will just return a 404
        }
    }

    private OkHttpClient getClient() {
        return new OkHttpClient.Builder()
                .addInterceptor(chain -> {
                    Request newRequest = chain.request().newBuilder()
                            .addHeader("Authorization", format("Basic %s", getToken()))
                            .addHeader("Accept", APPLICATION_JSON_MEDIA_TYPE.toString())
                            .build();
                    return chain.proceed(newRequest);
                })
                .build();
    }

    void validate(Response response) throws IOException {
        if (!response.isSuccessful()) {
            String error = null;
            if (response.body() != null) {
                error = response.body().string();
            }
            throw new RuntimeException("Bitbucket returned the following error: " + error);
        }
    }
}
