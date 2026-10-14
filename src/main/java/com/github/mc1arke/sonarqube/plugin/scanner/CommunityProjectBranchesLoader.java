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
import org.sonar.scanner.bootstrap.RawScannerProperties;
import org.sonar.scanner.http.transport.HttpClient;
import org.sonar.scanner.http.transport.HttpRequest;
import org.sonar.scanner.http.transport.HttpResponse;
import org.sonar.scanner.protocol.GsonHelper;
import org.sonar.scanner.scan.branch.BranchInfo;
import org.sonar.scanner.scan.branch.ProjectBranches;
import org.sonar.scanner.scan.branch.ProjectBranchesLoader;
import org.sonar.server.branch.ws.ProjectBranchesParameters;

import java.io.IOException;
import java.io.Reader;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Loads the branches currently known by SonarQube from the server component for client applications.
 *
 * <p>SonarQube 26.10 removed {@code org.sonar.scanner.http.ScannerWsClient} together with the whole
 * {@code org.sonarqube.ws.client} package from the scanner engine: server communication moved to the
 * protobuf based domain API ({@code org.sonar.scanner.http.domain.DomainApiClientFactory}). This loader
 * therefore issues the request through the engine's transport layer
 * ({@link HttpClient}/{@link HttpRequest}) instead of the removed web-service client. The HTTP client is
 * the one the scanner already uses, so proxy, TLS and timeout settings keep applying; only the
 * {@code Authorization} header has to be provided explicitly because the token is no longer applied
 * by the removed client.</p>
 *
 * @author Michael Clarke
 */
public class CommunityProjectBranchesLoader implements ProjectBranchesLoader {

    private static final String PROJECT_BRANCHES_URL =
            String.format("/%s/%s", ProjectBranchesParameters.CONTROLLER, ProjectBranchesParameters.ACTION_LIST);

    private static final String HOST_URL_PROPERTY = "sonar.host.url";
    private static final String TOKEN_PROPERTY = "sonar.token";
    private static final String LOGIN_PROPERTY = "sonar.login";

    private final HttpClient httpClient;
    private final Map<String, String> scannerProperties;
    private final Gson gson;

    public CommunityProjectBranchesLoader(HttpClient httpClient, RawScannerProperties properties) {
        super();
        this.httpClient = httpClient;
        this.scannerProperties = properties.properties();
        this.gson = GsonHelper.create();
    }

    @Override
    public ProjectBranches load(String projectKey) {
        try {
            HttpRequest request =
                    HttpRequest.get(URI.create(hostUrl() + PROJECT_BRANCHES_URL))
                               .queryParameter(ProjectBranchesParameters.PARAM_PROJECT, projectKey)
                               .header("Authorization", "Bearer " + token())
                               .build();

            try (HttpResponse response = httpClient.execute(request)) {
                if (404 == response.code()) {
                    return new ProjectBranches(new ArrayList<>());
                }
                if (!response.isSuccessful()) {
                    throw MessageException.of("Could not load branches from server: HTTP " + response.code());
                }
                try (Reader reader = response.bodyReader()) {
                    BranchesResponse parsedResponse = gson.fromJson(reader, BranchesResponse.class);
                    return new ProjectBranches(parsedResponse.getBranches());
                }
            }
        } catch (IOException e) {
            throw MessageException.of("Could not load branches from server", e);
        }
    }

    private String hostUrl() {
        String hostUrl = scannerProperties.get(HOST_URL_PROPERTY);
        if (null == hostUrl || hostUrl.isEmpty()) {
            throw MessageException.of("Property '" + HOST_URL_PROPERTY + "' is missing");
        }
        return hostUrl.endsWith("/") ? hostUrl.substring(0, hostUrl.length() - 1) : hostUrl;
    }

    private String token() {
        String token = scannerProperties.get(TOKEN_PROPERTY);
        if (null == token || token.isEmpty()) {
            token = scannerProperties.get(LOGIN_PROPERTY);
        }
        if (null == token || token.isEmpty()) {
            throw MessageException.of("Property '" + TOKEN_PROPERTY + "' is missing");
        }
        return token;
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
