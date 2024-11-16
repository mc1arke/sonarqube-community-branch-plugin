/*
 * Copyright (C) 2021-2024 Michael Clarke
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
package com.github.mc1arke.sonarqube.plugin.almclient.azuredevops;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.mc1arke.sonarqube.plugin.almclient.azuredevops.model.CommentThread;
import com.github.mc1arke.sonarqube.plugin.almclient.azuredevops.model.CommentThreadResponse;
import com.github.mc1arke.sonarqube.plugin.almclient.azuredevops.model.Commit;
import com.github.mc1arke.sonarqube.plugin.almclient.azuredevops.model.Commits;
import com.github.mc1arke.sonarqube.plugin.almclient.azuredevops.model.ConnectionData;
import com.github.mc1arke.sonarqube.plugin.almclient.azuredevops.model.CreateCommentRequest;
import com.github.mc1arke.sonarqube.plugin.almclient.azuredevops.model.CreateCommentThreadRequest;
import com.github.mc1arke.sonarqube.plugin.almclient.azuredevops.model.GitPullRequestStatus;
import com.github.mc1arke.sonarqube.plugin.almclient.azuredevops.model.PullRequest;
import com.github.mc1arke.sonarqube.plugin.almclient.azuredevops.model.Repository;
import com.github.mc1arke.sonarqube.plugin.almclient.azuredevops.model.UpdateCommentThreadStatusRequest;
import com.github.mc1arke.sonarqube.plugin.almclient.azuredevops.model.enums.CommentThreadStatus;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

public class AzureDevopsRestClient implements AzureDevopsClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(AzureDevopsRestClient.class);
    private static final String API_VERSION = "4.1";
    private static final String API_VERSION_PREVIEW = API_VERSION + "-preview";

    private final String authToken;
    private final String apiUrl;
    private final ObjectMapper objectMapper;
    private final Supplier<CloseableHttpClient> httpClientFactory;

    AzureDevopsRestClient(String apiUrl, String authToken, ObjectMapper objectMapper, Supplier<CloseableHttpClient> httpClientFactory) {
        super();
        this.apiUrl = apiUrl;
        this.authToken = authToken;
        this.objectMapper = objectMapper;
        this.httpClientFactory = httpClientFactory;
    }

    @Override
    public void submitPullRequestStatus(String projectId, String repositoryName, int pullRequestId, GitPullRequestStatus status) throws IOException {
        String url = String.format("%s/%s/_apis/git/repositories/%s/pullRequests/%s/statuses?api-version=%s", apiUrl, encode(projectId), encode(repositoryName), pullRequestId, API_VERSION_PREVIEW);
        execute(url, "post", objectMapper.writeValueAsString(status), null);
    }

    @Override
    public Repository getRepository(String projectId, String repositoryName) throws IOException {
        String url = String.format("%s/%s/_apis/git/repositories/%s?api-version=%s", apiUrl, encode(projectId), encode(repositoryName), API_VERSION);
        return execute(url, "get", null, Repository.class);
    }

    @Override
    public List<CommentThread> retrieveThreads(String projectId, String repositoryName, int pullRequestId) throws IOException {
        String url = String.format("%s/%s/_apis/git/repositories/%s/pullRequests/%s/threads?api-version=%s", apiUrl, encode(projectId), encode(repositoryName), pullRequestId, API_VERSION);
        return Objects.requireNonNull(execute(url, "get", null, CommentThreadResponse.class)).getValue();
    }

    @Override
    public CommentThread createThread(String projectId, String repositoryName, int pullRequestId, CreateCommentThreadRequest thread) throws IOException {
        String url = String.format("%s/%s/_apis/git/repositories/%s/pullRequests/%s/threads?api-version=%s", apiUrl, encode(projectId), encode(repositoryName), pullRequestId, API_VERSION);
        return execute(url, "post", objectMapper.writeValueAsString(thread), CommentThread.class);
    }

    @Override
    public void addCommentToThread(String projectId, String repositoryName, int pullRequestId, int threadId, CreateCommentRequest comment) throws IOException {
        String url = String.format("%s/%s/_apis/git/repositories/%s/pullRequests/%s/threads/%s/comments?api-version=%s", apiUrl, encode(projectId), encode(repositoryName), pullRequestId, threadId, API_VERSION);
        execute(url, "post", objectMapper.writeValueAsString(comment), null);
    }

    @Override
    public void resolvePullRequestThread(String projectId, String repositoryName, int pullRequestId, int threadId) throws IOException {
        String url = String.format("%s/%s/_apis/git/repositories/%s/pullRequests/%s/threads/%s?api-version=%s", apiUrl, encode(projectId), encode(repositoryName), pullRequestId, threadId, API_VERSION);

        UpdateCommentThreadStatusRequest commentThread = new UpdateCommentThreadStatusRequest(CommentThreadStatus.CLOSED);
        execute(url, "patch", objectMapper.writeValueAsString(commentThread), null);
    }

    @Override
    public void deletePullRequestThreadComment(String projectId, String repositoryName, int pullRequestId, int threadId, int commentId) throws IOException {
        String url = String.format("%s/%s/_apis/git/repositories/%s/pullRequests/%s/threads/%s/comments/%s?api-version=%s", apiUrl, encode(projectId), encode(repositoryName), pullRequestId, threadId, commentId, API_VERSION);

        execute(url, "delete", null, null);
    }

    @Override
    public PullRequest retrievePullRequest(String projectId, String repositoryName, int pullRequestId) throws IOException {
        String url = String.format("%s/%s/_apis/git/repositories/%s/pullRequests/%s?api-version=%s", apiUrl, encode(projectId), encode(repositoryName), pullRequestId, API_VERSION);
        return execute(url, "get", null, PullRequest.class);
    }

    @Override
    public List<Commit> getPullRequestCommits(String projectId, String repositoryName, int pullRequestId) throws IOException {
        String url = String.format("%s/%s/_apis/git/repositories/%s/pullRequests/%s/commits?api-version=%s", apiUrl, encode(projectId), encode(repositoryName), pullRequestId, API_VERSION);
        return Objects.requireNonNull(execute(url, "get", null, Commits.class)).getValue();
    }

    @Override
    public ConnectionData getConnectionData() throws IOException {
        String url = String.format("%s/_apis/ConnectionData?api-version=%s", apiUrl, API_VERSION_PREVIEW);
        return Objects.requireNonNull(execute(url, "get", null, ConnectionData.class));
    }


    private <T> T execute(String url, String method, String content, Class<T> type) throws IOException {
        RequestBuilder requestBuilder = RequestBuilder.create(method)
                .setUri(url)
                .addHeader("Authorization", "Basic " + authToken)
                .addHeader("Content-type", ContentType.APPLICATION_JSON.withCharset(StandardCharsets.UTF_8).toString());

        Optional.ofNullable(content).ifPresent(body -> requestBuilder.setEntity(new StringEntity(body, StandardCharsets.UTF_8)));
        Optional.ofNullable(type).ifPresent(responseType -> requestBuilder.addHeader("Accept", ContentType.APPLICATION_JSON.getMimeType()));

        try (CloseableHttpClient httpClient = httpClientFactory.get()) {
            HttpResponse httpResponse = httpClient.execute(requestBuilder.build());

            validateResponse(httpResponse);

            if (null == type) {
                return null;
            }
            return objectMapper.readValue(EntityUtils.toString(httpResponse.getEntity(), StandardCharsets.UTF_8), type);
        }
    }

    private static void validateResponse(HttpResponse httpResponse) {
        if (httpResponse.getStatusLine().getStatusCode() == 200) {
            return;
        }

        LOGGER.atError().setMessage("Azure Devops response status did not match expected value. Expected: 200 {}{}{}{}")
                .addArgument(System::lineSeparator)
                .addArgument(httpResponse::getStatusLine)
                .addArgument(System::lineSeparator)
                .addArgument(() -> Optional.ofNullable(httpResponse.getEntity()).map(entity -> {
                    try {
                        return EntityUtils.toString(entity, StandardCharsets.UTF_8);
                    } catch (IOException ex) {
                        LOGGER.warn("Could not decode response entity", ex);
                        return "";
                    }
                }).orElse(""))
                .log();


        throw new IllegalStateException("An unexpected response code was returned from the Azure Devops API - Expected: 200, Got: " + httpResponse.getStatusLine().getStatusCode());
    }

    private static String encode(String input) {
        return URLEncoder.encode(input, StandardCharsets.UTF_8).replace("+", "%20");
    }
}
