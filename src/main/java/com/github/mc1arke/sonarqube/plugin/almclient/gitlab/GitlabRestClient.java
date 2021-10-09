/*
 * Copyright (C) 2021 Michael Clarke
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
package com.github.mc1arke.sonarqube.plugin.almclient.gitlab;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.mc1arke.sonarqube.plugin.almclient.LinkHeaderReader;
import com.github.mc1arke.sonarqube.plugin.almclient.gitlab.model.Commit;
import com.github.mc1arke.sonarqube.plugin.almclient.gitlab.model.CommitNote;
import com.github.mc1arke.sonarqube.plugin.almclient.gitlab.model.Discussion;
import com.github.mc1arke.sonarqube.plugin.almclient.gitlab.model.MergeRequest;
import com.github.mc1arke.sonarqube.plugin.almclient.gitlab.model.MergeRequestNote;
import com.github.mc1arke.sonarqube.plugin.almclient.gitlab.model.PipelineStatus;
import com.github.mc1arke.sonarqube.plugin.almclient.gitlab.model.Project;
import com.github.mc1arke.sonarqube.plugin.almclient.gitlab.model.User;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

class GitlabRestClient implements GitlabClient {

    private static final Logger LOGGER = Loggers.get(GitlabRestClient.class);

    private final String baseGitlabApiUrl;
    private final String authToken;
    private final ObjectMapper objectMapper;
    private final LinkHeaderReader linkHeaderReader;
    private final Supplier<CloseableHttpClient> httpClientFactory;

    GitlabRestClient(String baseGitlabApiUrl, String authToken, LinkHeaderReader linkHeaderReader, ObjectMapper objectMapper, Supplier<CloseableHttpClient> httpClientFactory) {
        this.baseGitlabApiUrl = baseGitlabApiUrl;
        this.authToken = authToken;
        this.linkHeaderReader = linkHeaderReader;
        this.objectMapper = objectMapper;
        this.httpClientFactory = httpClientFactory;
    }

    @Override
    public MergeRequest getMergeRequest(String projectSlug, long mergeRequestIid) throws IOException {
        return entity(new HttpGet(String.format("%s/projects/%s/merge_requests/%s", baseGitlabApiUrl, URLEncoder.encode(projectSlug, StandardCharsets.UTF_8), mergeRequestIid)), MergeRequest.class);
    }

    @Override
    public User getCurrentUser() throws IOException {
        return entity(new HttpGet(String.format("%s/user", baseGitlabApiUrl)), User.class);
    }

    @Override
    public List<Commit> getMergeRequestCommits(long projectId, long mergeRequestIid) throws IOException {
        return entities(new HttpGet(String.format("%s/projects/%s/merge_requests/%s/commits", baseGitlabApiUrl, projectId, mergeRequestIid)), Commit.class);
    }

    @Override
    public List<Discussion> getMergeRequestDiscussions(long projectId, long mergeRequestIid) throws IOException {
        return entities(new HttpGet(String.format("%s/projects/%s/merge_requests/%s/discussions", baseGitlabApiUrl, projectId, mergeRequestIid)), Discussion.class);
    }

    @Override
    public Discussion addMergeRequestDiscussion(long projectId, long mergeRequestIid, MergeRequestNote mergeRequestNote) throws IOException {
        String targetUrl = String.format("%s/projects/%s/merge_requests/%s/discussions", baseGitlabApiUrl, projectId, mergeRequestIid);

        List<NameValuePair> requestContent = new ArrayList<>();
        requestContent.add(new BasicNameValuePair("body", mergeRequestNote.getContent()));

        if (mergeRequestNote instanceof CommitNote) {
            CommitNote commitNote = (CommitNote) mergeRequestNote;
            requestContent.addAll(Arrays.asList(
                new BasicNameValuePair("position[base_sha]", commitNote.getBaseSha()),
                new BasicNameValuePair("position[start_sha]", commitNote.getStartSha()),
                new BasicNameValuePair("position[head_sha]", commitNote.getHeadSha()),
                new BasicNameValuePair("position[old_path]", commitNote.getOldPath()),
                new BasicNameValuePair("position[new_path]", commitNote.getNewPath()),
                new BasicNameValuePair("position[new_line]", String.valueOf(commitNote.getNewLine())),
                new BasicNameValuePair("position[position_type]", "text"))
            );
        }

        HttpPost httpPost = new HttpPost(targetUrl);
        httpPost.addHeader("Content-type", ContentType.APPLICATION_FORM_URLENCODED.getMimeType());
        httpPost.setEntity(new UrlEncodedFormEntity(requestContent, StandardCharsets.UTF_8));
        return entity(httpPost, Discussion.class, httpResponse -> validateResponse(httpResponse, 201, "Discussion successfully created"));
    }

    @Override
    public void addMergeRequestDiscussionNote(long projectId, long mergeRequestIid, String discussionId, String noteContent) throws IOException {
        String targetUrl = String.format("%s/projects/%s/merge_requests/%s/discussions/%s/notes", baseGitlabApiUrl, projectId, mergeRequestIid, discussionId);

        HttpPost httpPost = new HttpPost(targetUrl);
        httpPost.setEntity(new UrlEncodedFormEntity(Collections.singletonList(new BasicNameValuePair("body", noteContent)), StandardCharsets.UTF_8));
        entity(httpPost, null, httpResponse -> validateResponse(httpResponse, 201, "Commit discussions note added"));
    }

    @Override
    public void resolveMergeRequestDiscussion(long projectId, long mergeRequestIid, String discussionId) throws IOException {
        String discussionIdUrl = String.format("%s/projects/%s/merge_requests/%s/discussions/%s?resolved=true", baseGitlabApiUrl, projectId, mergeRequestIid, discussionId);

        HttpPut httpPut = new HttpPut(discussionIdUrl);
        entity(httpPut, null);
    }

    @Override
    public void setMergeRequestPipelineStatus(long projectId, String commitRevision, PipelineStatus status) throws IOException {
        List<NameValuePair> entityFields = new ArrayList<>(Arrays.asList(
                new BasicNameValuePair("name", status.getPipelineName()),
                new BasicNameValuePair("target_url", status.getTargetUrl()),
                new BasicNameValuePair("description", status.getPipelineDescription())));

        status.getPipelineId().ifPresent(pipelineId -> entityFields.add(new BasicNameValuePair("pipeline_id", Long.toString(pipelineId))));
        status.getCoverage().ifPresent(coverage -> entityFields.add(new BasicNameValuePair("coverage", coverage.toString())));

        String statusUrl = String.format("%s/projects/%s/statuses/%s?state=%s", baseGitlabApiUrl, projectId, commitRevision, status.getState().getLabel());

        HttpPost httpPost = new HttpPost(statusUrl);
        httpPost.addHeader("Content-type", ContentType.APPLICATION_FORM_URLENCODED.getMimeType());
        httpPost.setEntity(new UrlEncodedFormEntity(entityFields, StandardCharsets.UTF_8));
        entity(httpPost, null, httpResponse -> {
            if (httpResponse.toString().contains("Cannot transition status")) {
                // Workaround for https://gitlab.com/gitlab-org/gitlab-ce/issues/25807
                LOGGER.debug("Transition status is already {}", status);
            } else {
                validateResponse(httpResponse, 201, "Comment posted");
            }
        });
    }

    @Override
    public Project getProject(String projectSlug) throws IOException {
        return entity(new HttpGet(String.format("%s/projects/%s", baseGitlabApiUrl, URLEncoder.encode(projectSlug, StandardCharsets.UTF_8))), Project.class);
    }

    private <X> X entity(HttpRequestBase httpRequest, Class<X> type) throws IOException {
        return entity(httpRequest, type, httpResponse -> validateResponse(httpResponse, 200, null));
    }

    private <X> X entity(HttpRequestBase httpRequest, Class<X> type, Consumer<HttpResponse> responseValidator) throws IOException {
        httpRequest.addHeader("PRIVATE-TOKEN", authToken);

        try (CloseableHttpClient httpClient = httpClientFactory.get()) {
            HttpResponse httpResponse = httpClient.execute(httpRequest);

            responseValidator.accept(httpResponse);

            if (null == type) {
                return null;
            }
            return objectMapper.readValue(EntityUtils.toString(httpResponse.getEntity(), StandardCharsets.UTF_8), type);
        }
    }

    private <X> List<X> entities(HttpGet httpRequest, Class<X> type) throws IOException {
        return entities(httpRequest, type, httpResponse -> validateResponse(httpResponse, 200, null));
    }

    private <X> List<X> entities(HttpGet httpRequest, Class<X> type, Consumer<HttpResponse> responseValidator) throws IOException {
        httpRequest.addHeader("PRIVATE-TOKEN", authToken);

        try (CloseableHttpClient httpClient = httpClientFactory.get()) {
            HttpResponse httpResponse = httpClient.execute(httpRequest);

            responseValidator.accept(httpResponse);

            List<X> entities = new ArrayList<>(objectMapper.readValue(EntityUtils.toString(httpResponse.getEntity(), StandardCharsets.UTF_8),
                    objectMapper.getTypeFactory().constructCollectionType(List.class, type)));

            Optional<String> nextURL = Optional.ofNullable(httpResponse.getFirstHeader("Link"))
                    .map(NameValuePair::getValue)
                    .flatMap(linkHeaderReader::findNextLink);
            if (nextURL.isPresent()) {
                entities.addAll(entities(new HttpGet(nextURL.get()), type, responseValidator));
            }

            return entities;
        }
    }

    private static void validateResponse(HttpResponse httpResponse, int expectedStatus, String successLogMessage) {
        if (httpResponse.getStatusLine().getStatusCode() == expectedStatus) {
            LOGGER.debug(Optional.ofNullable(successLogMessage).map(v -> v + System.lineSeparator()).orElse("") + httpResponse);
            return;
        }

        String responseContent = Optional.ofNullable(httpResponse.getEntity()).map(entity -> {
            try {
                return EntityUtils.toString(entity, StandardCharsets.UTF_8);
            } catch (IOException ex) {
                LOGGER.warn("Could not decode response entity", ex);
                return "";
            }
        }).orElse("");

        LOGGER.error("Gitlab response status did not match expected value. Expected: " + expectedStatus
                + System.lineSeparator()
                + httpResponse
                + System.lineSeparator()
                + responseContent);

        throw new IllegalStateException("An unexpected response code was returned from the Gitlab API - Expected: " + expectedStatus + ", Got: " + httpResponse.getStatusLine().getStatusCode());

    }

}
