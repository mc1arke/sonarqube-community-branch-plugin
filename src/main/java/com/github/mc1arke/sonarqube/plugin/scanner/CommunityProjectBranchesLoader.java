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
import org.sonar.api.utils.MessageException;
import org.sonar.scanner.http.ScannerWsClient;
import org.sonar.scanner.protocol.GsonHelper;
import org.sonar.scanner.scan.branch.BranchInfo;
import org.sonar.scanner.scan.branch.ProjectBranches;
import org.sonar.scanner.scan.branch.ProjectBranchesLoader;
import org.sonar.server.branch.ws.ProjectBranchesParameters;
import org.sonarqube.ws.client.GetRequest;
import org.sonarqube.ws.client.HttpException;
import org.sonarqube.ws.client.WsResponse;

import java.io.IOException;
import java.io.Reader;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Loads the branches currently known by SonarQube from the server component for client applications.
 *
 * @author Michael Clarke
 */
public class CommunityProjectBranchesLoader implements ProjectBranchesLoader {

    private static final String PROJECT_BRANCHES_URL =
            String.format("/%s/%s?%s=", ProjectBranchesParameters.CONTROLLER, ProjectBranchesParameters.ACTION_LIST,
                          ProjectBranchesParameters.PARAM_PROJECT);

    private final ScannerWsClient scannerWsClient;
    private final Gson gson;

    public CommunityProjectBranchesLoader(ScannerWsClient scannerWsClient) {
        super();
        this.scannerWsClient = scannerWsClient;
        this.gson = GsonHelper.create();
    }

    @Override
    public ProjectBranches load(String projectKey) {
        try {
            GetRequest branchesGetRequest =
                    new GetRequest(PROJECT_BRANCHES_URL + URLEncoder.encode(projectKey, StandardCharsets.UTF_8.name()));
            try (WsResponse branchesResponse = scannerWsClient
                    .call(branchesGetRequest); Reader reader = branchesResponse
                .contentReader()) {
                BranchesResponse parsedResponse = gson.fromJson(reader, BranchesResponse.class);
                return new ProjectBranches(parsedResponse.getBranches());
            }
        } catch (IOException e) {
            throw MessageException.of("Could not load branches from server", e);
        } catch (HttpException e) {
            if (404 == e.code()) {
                return new ProjectBranches(new ArrayList<>());
            } else {
                throw MessageException.of("Could not load branches from server", e);
            }
        }
    }

    /*package*/ static class BranchesResponse {

        private final List<BranchInfo> branches;

        /*package*/ BranchesResponse(List<BranchInfo> branches) {
            super();
            this.branches = branches;
        }

        /*package*/ List<BranchInfo> getBranches() {
            return branches;
        }
    }

}
