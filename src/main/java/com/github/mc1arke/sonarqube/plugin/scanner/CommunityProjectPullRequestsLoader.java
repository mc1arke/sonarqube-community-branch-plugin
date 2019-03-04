/*
 * Copyright (C) 2019 Michael Clarke
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
import org.sonar.api.utils.MessageException;
import org.sonar.scanner.bootstrap.ScannerWsClient;
import org.sonar.scanner.protocol.GsonHelper;
import org.sonar.scanner.scan.branch.ProjectPullRequests;
import org.sonar.scanner.scan.branch.ProjectPullRequestsLoader;
import org.sonar.scanner.scan.branch.PullRequestInfo;
import org.sonar.scanner.util.ScannerUtils;
import org.sonarqube.ws.client.GetRequest;
import org.sonarqube.ws.client.HttpException;
import org.sonarqube.ws.client.WsResponse;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

/**
 * Loads the Pull Requests currently known by SonarQube from the server component for client applications.
 *
 * @author Michael Clarke
 */
public class CommunityProjectPullRequestsLoader implements ProjectPullRequestsLoader {

    private static final String PROJECT_PULL_REQUESTS_URL = "/api/project_pull_requests/list?project=";

    private final ScannerWsClient scannerWsClient;
    private final Gson gson;

    public CommunityProjectPullRequestsLoader(ScannerWsClient scannerWsClient) {
        super();
        this.scannerWsClient = scannerWsClient;
        this.gson = GsonHelper.create();
    }

    @Override
    public ProjectPullRequests load(String projectKey) {
        GetRequest branchesGetRequest =
                new GetRequest(PROJECT_PULL_REQUESTS_URL + ScannerUtils.encodeForUrl(projectKey));

        try (WsResponse branchesResponse = scannerWsClient.call(branchesGetRequest); Reader reader = branchesResponse
                .contentReader()) {
            PullRequestsResponse parsedResponse = gson.fromJson(reader, PullRequestsResponse.class);
            return new ProjectPullRequests(parsedResponse.getPullRequests());
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
