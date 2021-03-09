/*
 * Copyright (C) 2020 Michael Clarke
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
package com.github.mc1arke.sonarqube.plugin.scanner;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.sonar.api.utils.MessageException;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.scanner.bootstrap.ScannerWsClient;
import org.sonar.scanner.scan.branch.ProjectPullRequests;
import org.sonar.scanner.scan.branch.ProjectPullRequestsLoader;
import org.sonar.scanner.scan.branch.PullRequestInfo;
import org.sonarqube.ws.client.GetRequest;
import org.sonarqube.ws.client.HttpException;
import org.sonarqube.ws.client.WsResponse;

import java.io.IOException;
import java.io.Reader;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Loads the Pull Requests currently known by SonarQube from the server component for client applications.
 *
 * @author Michael Clarke
 */
public class CommunityProjectPullRequestsLoader implements ProjectPullRequestsLoader {

    private static final Logger LOGGER = Loggers.get(CommunityProjectPullRequestsLoader.class);
    private static final String PROJECT_PULL_REQUESTS_URL = "/api/project_pull_requests/list?project=";

    private final ScannerWsClient scannerWsClient;
    private final Gson gson;

    public CommunityProjectPullRequestsLoader(ScannerWsClient scannerWsClient) {
        super();
        this.scannerWsClient = scannerWsClient;
        this.gson =
                new GsonBuilder().registerTypeAdapter(PullRequestInfo.class, createPullRequestInfoJsonDeserialiser())
                        .create();
    }

    private static JsonDeserializer<PullRequestInfo> createPullRequestInfoJsonDeserialiser() {
        return (jsonElement, type, jsonDeserializationContext) -> {
            JsonObject jsonObject = jsonElement.getAsJsonObject();
            long parsedDate = 0;
            try {
                String analysisDate = Optional.ofNullable(jsonObject.get("analysisDate")).map(JsonElement::getAsString).orElse(null);
                if(analysisDate == null) {
                    LOGGER.warn("Analysis Date not provided in Pull Requests API response. Will use '0' date");
                } else {
                    parsedDate = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ")
                            .parse(analysisDate).getTime();
                }
            } catch (ParseException e) {
                LOGGER.warn("Could not parse date from Pull Requests API response. Will use '0' date", e);
            }
            final String base = Optional.ofNullable(jsonObject.get("base")).map(JsonElement::getAsString).orElse(null);
            return new PullRequestInfo(jsonObject.get("key").getAsString(), jsonObject.get("branch").getAsString(),
                    base, parsedDate);
        };
    }

    @Override
    public ProjectPullRequests load(String projectKey) {
        try {
            GetRequest branchesGetRequest = new GetRequest(
                    PROJECT_PULL_REQUESTS_URL + URLEncoder.encode(projectKey, StandardCharsets.UTF_8.name()));

            try (WsResponse branchesResponse = scannerWsClient
                    .call(branchesGetRequest); Reader reader = branchesResponse
                .contentReader()) {
                PullRequestsResponse parsedResponse = gson.fromJson(reader, PullRequestsResponse.class);
                return new ProjectPullRequests(parsedResponse.getPullRequests());
            }
        } catch (IOException e) {
            throw MessageException.of("Could not load pull requests from server", e);
        } catch (HttpException e) {
            if (404 == e.code()) {
                return new ProjectPullRequests(new ArrayList<>());
            } else {
                throw MessageException.of("Could not load pull requests from server", e);
            }
        }
    }

    /*package*/ static class PullRequestsResponse {

        private final List<PullRequestInfo> pullRequests;

        /*package*/ PullRequestsResponse(List<PullRequestInfo> pullRequests) {
            super();
            this.pullRequests = pullRequests;
        }

        /*package*/ List<PullRequestInfo> getPullRequests() {
            return pullRequests;
        }
    }

}
