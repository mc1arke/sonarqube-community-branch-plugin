/*
 * Copyright (C) 2020 Oliver Jedinger, Artemy Osipov
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
package com.github.mc1arke.sonarqube.plugin.ce.pullrequest.bitbucket.server;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.bitbucket.PullRequestParticipant;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.bitbucket.response.activity.ActivityPage;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.bitbucket.response.activity.Comment;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.bitbucket.response.diff.DiffPage;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

class BitbucketServerClient {

    private static final Logger LOGGER = Loggers.get(BitbucketServerClient.class);

    private static final String REST_API = "/rest/api/1.0";
    private static final String USER_PR_API = "/users/%s/repos/%s/pull-requests/%s";
    private static final String PROJECT_PR_API = "/projects/%s/repos/%s/pull-requests/%s";
    private static final String COMMENTS_API = "/comments";
    private static final String ACTIVITIES_API = "/activities?limit=250";
    private static final String DIFF_API = "/diff";
    private static final String PARTICIPANTS_API = "/participants";

    private final String apiUrl;
    private final String apiToken;
    private final ObjectMapper jsonMapper;

    public BitbucketServerClient(String hostUrl, String apiToken) {
        this.apiUrl = normalizeHostUrl(hostUrl) + REST_API;
        this.apiToken = apiToken;
        this.jsonMapper = new ObjectMapper()
                .configure(DeserializationFeature.ACCEPT_EMPTY_ARRAY_AS_NULL_OBJECT, true)
                .configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true)
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    private String normalizeHostUrl(String hostUrl) {
        return hostUrl.replaceAll("/$", "");
    }

    private Map<String, String> buildHeaders() {
        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", String.format("Bearer %s", apiToken));
        headers.put("Accept", "application/json");

        return headers;
    }

    private String makePullRequestCommentUrl(BitbucketServerRepository repository, String pullRequestId, Comment comment) {
        return makePullRequestCommentsUrl(repository, pullRequestId) + String.format("/%s?version=%s", comment.getId(), comment.getVersion());
    }

    private String makePullRequestParticipantsUrl(BitbucketServerRepository repository, String pullRequestId, String userSlug) {
        return makePullRequestUrl(repository, pullRequestId) + PARTICIPANTS_API + "/" + userSlug;
    }

    private String makePullRequestCommentsUrl(BitbucketServerRepository repository, String pullRequestId) {
        return makePullRequestUrl(repository, pullRequestId) + COMMENTS_API;
    }

    private String makePullRequestActivitiesUrl(BitbucketServerRepository repository, String pullRequestId) {
        return makePullRequestUrl(repository, pullRequestId) + ACTIVITIES_API;
    }

    private String makePullRequestDiffUrl(BitbucketServerRepository repository, String pullRequestId) {
        return makePullRequestUrl(repository, pullRequestId) + DIFF_API;
    }

    private String makePullRequestUrl(BitbucketServerRepository repository, String pullRequestId) {
        if (repository.getProjectType() == BitbucketServerRepository.ProjectType.USER) {
            return apiUrl + String.format(USER_PR_API, repository.getProjectKey(), repository.getRepositorySlug(), pullRequestId);
        }

        return apiUrl + String.format(PROJECT_PR_API, repository.getProjectKey(), repository.getRepositorySlug(), pullRequestId);
    }

    public ActivityPage getActivityPage(BitbucketServerRepository repository, String pullRequestId) throws IOException {
        String activitiesApiUrl = makePullRequestActivitiesUrl(repository, pullRequestId);

        LOGGER.debug("Getting activity page");
        return getPage(activitiesApiUrl, ActivityPage.class);
    }

    public DiffPage getDiffPage(BitbucketServerRepository repository, String pullRequestId) throws IOException {
        String diffApiUrl = makePullRequestDiffUrl(repository, pullRequestId);

        LOGGER.debug("Getting diff page");
        return getPage(diffApiUrl, DiffPage.class);
    }

    private <T> T getPage(String pageUrl, Class<T> type) throws IOException {
        HttpGet request = new HttpGet(pageUrl);
        buildHeaders().forEach(request::addHeader);

        String response = doRequest(request, 200);

        return jsonMapper.readValue(response, type);
    }

    public void deleteCommentFromPullRequest(BitbucketServerRepository repository, String pullRequestId, Comment comment) throws IOException {
        String commentUrl = makePullRequestCommentUrl(repository, pullRequestId, comment);
        HttpDelete request = new HttpDelete(commentUrl);
        buildHeaders().forEach(request::addHeader);

        LOGGER.debug("Delete comment {} with version {}", comment.getId(), comment.getVersion());
        doRequest(request, 204);
    }

    public void postCommentToPullRequest(BitbucketServerRepository repository, String pullRequestId, Object content) throws IOException {
        String commentsUrl = makePullRequestCommentsUrl(repository, pullRequestId);
        HttpPost request = new HttpPost(commentsUrl);
        buildHeaders().forEach(request::addHeader);

        StringEntity contentEntity = new StringEntity(jsonMapper.writeValueAsString(content), ContentType.APPLICATION_JSON);
        request.setEntity(contentEntity);

        LOGGER.debug("Post comment {}", contentEntity);
        doRequest(request, 201);
    }

    public void addPullRequestStatus(BitbucketServerRepository repository, String pullRequestId, String userSlug, PullRequestParticipant.Status status) throws IOException {
        String participantsUrl = makePullRequestParticipantsUrl(repository, pullRequestId, userSlug);
        HttpPut request = new HttpPut(participantsUrl);
        buildHeaders().forEach(request::addHeader);

        StringEntity contentEntity = new StringEntity(jsonMapper.writeValueAsString(new PullRequestParticipant(status)), ContentType.APPLICATION_JSON);
        request.setEntity(contentEntity);

        LOGGER.debug("Put participant {}", contentEntity);
        doRequest(request, 200);
    }

    private String doRequest(HttpUriRequest request, int expectedStatus) throws IOException {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            LOGGER.debug("Execute {} at {}", request.getMethod(), request.getURI());
            HttpResponse httpResponse = httpClient.execute(request);
            if (httpResponse == null) {
                throw new IOException("No response returned from Bitbucket Server");
            } else if (httpResponse.getStatusLine().getStatusCode() != expectedStatus) {
                HttpEntity entity = httpResponse.getEntity();
                LOGGER.error("Error response from Bitbucket: {}", IOUtils.toString(entity.getContent(), StandardCharsets.UTF_8));
                throw new IOException(
                        String.format("Error response returned from Bitbucket Server. Expected HTTP Status %s but got %s",
                                expectedStatus, httpResponse.getStatusLine().getStatusCode()));
            } else {
                HttpEntity entity = httpResponse.getEntity();

                if (entity == null) {
                    return null;
                }

                String response = IOUtils.toString(entity.getContent(), StandardCharsets.UTF_8);
                LOGGER.debug("Response from Bitbucket Server: {}", response);

                return response;
            }
        }
    }
}
