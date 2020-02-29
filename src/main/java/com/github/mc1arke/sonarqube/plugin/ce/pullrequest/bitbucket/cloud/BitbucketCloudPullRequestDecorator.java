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
package com.github.mc1arke.sonarqube.plugin.ce.pullrequest.bitbucket.cloud;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.AnalysisDetails;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.PostAnalysisIssueVisitor;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.PullRequestBuildStatusDecorator;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.UnifyConfiguration;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.bitbucket.HttpUtils;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.bitbucket.cloud.dto.CommentDTO;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.bitbucket.cloud.dto.CommentPageDTO;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.bitbucket.cloud.dto.ContentDTO;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.bitbucket.cloud.dto.InlineDTO;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.bitbucket.server.BitbucketServerPullRequestDecorator;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.markup.MarkdownFormatterFactory;
import com.google.common.annotations.VisibleForTesting;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.sonar.api.config.Configuration;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.ce.task.projectanalysis.component.ConfigurationRepository;
import org.sonar.core.issue.DefaultIssue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BitbucketCloudPullRequestDecorator extends BitbucketServerPullRequestDecorator implements PullRequestBuildStatusDecorator {

    public static final String PULL_REQUEST_BITBUCKET_APP_PASSWORD = "com.github.mc1arke.sonarqube.plugin.branch.pullrequest.bitbucket.cloud.appPassword";

    public static final String PULL_REQUEST_BITBUCKET_APP_USERNAME = "com.github.mc1arke.sonarqube.plugin.branch.pullrequest.bitbucket.cloud.appUsername";

    public static final String PULL_REQUEST_BITBUCKET_USER_UUID = "com.github.mc1arke.sonarqube.plugin.branch.pullrequest.bitbucket.cloud.userUuid";

    public static final String PULL_REQUEST_BITBUCKET_WORKSPACE = "com.github.mc1arke.sonarqube.plugin.branch.pullrequest.bitbucket.cloud.workspace";

    public static final String PULL_REQUEST_BITBUCKET_REPOSITORY_SLUG = "com.github.mc1arke.sonarqube.plugin.branch.pullrequest.bitbucket.cloud.repositorySlug";

    private static final Logger LOGGER = Loggers.get(BitbucketCloudPullRequestDecorator.class);

    private static final String API_ENDPOINT = "https://api.bitbucket.org/2.0/";
    private static final String PR_COMMENT_API = API_ENDPOINT + "repositories/%s/%s/pullrequests/%s/comments";


    private final ConfigurationRepository configurationRepository;
    private String commentApiEndpoint;
    private Map<String, String> headers = new HashMap<>();

    public BitbucketCloudPullRequestDecorator(ConfigurationRepository configurationRepository) {
        super();
        this.configurationRepository = configurationRepository;
    }

    @VisibleForTesting
    protected static String getMandatoryProperty(String propertyName, Configuration configuration) {
        return configuration.get(propertyName).orElseThrow(() -> new IllegalStateException(
                String.format("%s must be specified in the project configuration", propertyName)));
    }

    @Override
    public void decorateQualityGateStatus(AnalysisDetails analysisDetails, UnifyConfiguration unifyConfiguration) {
        LOGGER.info("starting to analyze with " + analysisDetails.toString());

        final Configuration configuration = configurationRepository.getConfiguration();
        final boolean summaryCommentEnabled = Boolean.parseBoolean(getMandatoryProperty(PULL_REQUEST_COMMENT_SUMMARY_ENABLED, configuration));
        final boolean fileCommentEnabled = Boolean.parseBoolean(getMandatoryProperty(PULL_REQUEST_FILE_COMMENT_ENABLED, configuration));
        final boolean deleteCommentsEnabled = Boolean.parseBoolean(getMandatoryProperty(PULL_REQUEST_DELETE_COMMENTS_ENABLED, configuration));

        this.commentApiEndpoint = getCommentApiUrl(analysisDetails.getBranchName());

        final String appPassword = getMandatoryProperty(PULL_REQUEST_BITBUCKET_APP_PASSWORD, configuration);
        final String appUsername = getMandatoryProperty(PULL_REQUEST_BITBUCKET_APP_USERNAME, configuration);
        headers.put(HttpHeaders.AUTHORIZATION, "Basic " + Base64.getEncoder().encodeToString((appUsername + ":" + appPassword).getBytes()));

        List<CommentDTO> comments = getComments();
        if (deleteCommentsEnabled) {
            final String serviceUserUuid = getMandatoryProperty(PULL_REQUEST_BITBUCKET_USER_UUID, configuration);
            deleteComments(comments, serviceUserUuid);
        }

        if (summaryCommentEnabled) {
            postSummaryComment(analysisDetails);
        }

        if (fileCommentEnabled) {
            postFileBasedComments(analysisDetails);
        }
    }

    @VisibleForTesting
    protected void postFileBasedComments(AnalysisDetails analysisDetails) {
        List<PostAnalysisIssueVisitor.ComponentIssue> componentIssues = getOpenComponentIssues(analysisDetails);
        componentIssues.forEach(componentIssue -> {
            final DefaultIssue issue = componentIssue.getIssue();
            String analysisIssueSummary = analysisDetails.createAnalysisIssueSummary(componentIssue, new MarkdownFormatterFactory());
            String issuePath = analysisDetails.getSCMPathForIssue(componentIssue).orElse(StringUtils.EMPTY);
            int issueLine = issue.getLine() != null ? issue.getLine() : 0;

            ContentDTO content = new ContentDTO(null, null, analysisIssueSummary, null);
            InlineDTO inline = new InlineDTO(null, issueLine, issuePath);
            CommentDTO commentDTO = new CommentDTO(null, content, inline, null);
            postComment(commentDTO);
        });
    }

    @VisibleForTesting
    protected void postSummaryComment(AnalysisDetails analysisDetails) {
        String summary = analysisDetails.createAnalysisSummary(new MarkdownFormatterFactory());
        ContentDTO content = new ContentDTO(null, null, summary, null);
        CommentDTO commentDTO = new CommentDTO(null, content, null, null);
        postComment(commentDTO);
    }

    /**
     * Returns a comments from the API endpoint.
     * <p>
     * Please note that this call might produce multiple requests because the API is paginated.
     * Each response returns 10 comments, if there are more need to go over them in a follow up
     * request.
     * </p>
     *
     * @return List of all comments
     */
    @VisibleForTesting
    protected List<CommentDTO> getComments() {
        List<CommentDTO> comments = new ArrayList<>();
        String next = commentApiEndpoint;
        do {
            LOGGER.info("Deleting comments from sonar service user for url: " + next);
            CommentPageDTO page = HttpUtils.getPage(next, headers, CommentPageDTO.class);
            next = page.getNext();
            if (page.getComments() != null) {
                comments.addAll(page.getComments());
            }
        } while (StringUtils.isNotEmpty(next));
        return comments;
    }

    @VisibleForTesting
    protected void deleteComments(List<CommentDTO> comments, String serviceUserUuid) {
        comments.stream()
                .filter(comment -> serviceUserUuid.equals(comment.getUser().getUuid()))
                .forEach(this::deleteComment);
    }

    @VisibleForTesting
    protected void deleteComment(CommentDTO comment) {
        String deleteCommentUrl = this.commentApiEndpoint + "/%s";
        HttpDelete httpDelete = new HttpDelete(String.format(deleteCommentUrl, comment.getId()));
        LOGGER.debug("delete " + comment.getId());
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            httpDelete.addHeader(entry.getKey(), entry.getValue());
        }
        try (CloseableHttpClient closeableHttpClient = HttpClients.createDefault()) {
            HttpResponse deleteResponse = closeableHttpClient.execute(httpDelete);
            if (null == deleteResponse) {
                LOGGER.error("HttpResponse for deleting comment was null");
            } else if (deleteResponse.getStatusLine().getStatusCode() != 204) {
                LOGGER.error(IOUtils.toString(deleteResponse.getEntity().getContent(), StandardCharsets.UTF_8.name()));
                LOGGER.error("An error was returned in the response from the Bitbucket Cloud API. See the previous log messages for details");
            } else {
                LOGGER.debug(String.format("Comment %s deleted", comment.getId()));
            }
        } catch (IOException e) {
            LOGGER.error("An error occured while trying to delete a comment in the Bitbucket Cloud API.", e);
        }
    }

    @VisibleForTesting
    protected void postComment(CommentDTO comment) {
        try {
            StringEntity commentEntity = new StringEntity(new ObjectMapper().writeValueAsString(comment), ContentType.APPLICATION_JSON);
            LOGGER.debug(new ObjectMapper().writeValueAsString(comment));
            super.postComment(this.commentApiEndpoint, this.headers, commentEntity, true);
        } catch (IOException e) {
            LOGGER.error("An error occured while trying to add summary comment to Bitbucket Cloud.", e);
        }
    }

    @VisibleForTesting
    protected String getCommentApiUrl(String pullRequestId) {
        final Configuration configuration = configurationRepository.getConfiguration();
        final String workspace = getMandatoryProperty(PULL_REQUEST_BITBUCKET_WORKSPACE, configuration);
        final String repositorySlug = getMandatoryProperty(PULL_REQUEST_BITBUCKET_REPOSITORY_SLUG, configuration);

        return String.format(PR_COMMENT_API, workspace, repositorySlug, pullRequestId);
    }

    @Override
    public String name() {
        return "BitbucketCloud";
    }

    public void setCommentApiEndpoint(String commentApiEndpoint) {
        this.commentApiEndpoint = commentApiEndpoint;
    }

    public void setHeaders(Map<String, String> headers) {
        this.headers = headers;
    }
}
