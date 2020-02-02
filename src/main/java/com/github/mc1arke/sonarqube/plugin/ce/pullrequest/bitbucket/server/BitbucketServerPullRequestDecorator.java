/*
 * Copyright (C) 2020 Oliver Jedinger, Michael Clarke
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
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.AnalysisDetails;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.PostAnalysisIssueVisitor;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.PullRequestBuildStatusDecorator;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.bitbucket.Anchor;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.bitbucket.FileComment;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.bitbucket.SummaryComment;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.bitbucket.response.diff.Diff;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.bitbucket.response.diff.DiffLine;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.bitbucket.response.diff.DiffPage;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.bitbucket.response.diff.Hunk;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.bitbucket.response.diff.Segment;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.markup.MarkdownFormatterFactory;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.sonar.api.issue.Issue;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.db.alm.setting.ALM;
import org.sonar.db.alm.setting.AlmSettingDto;
import org.sonar.db.alm.setting.ProjectAlmSettingDto;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class BitbucketServerPullRequestDecorator implements PullRequestBuildStatusDecorator {

    private static final Logger LOGGER = Loggers.get(BitbucketServerPullRequestDecorator.class);
    private static final List<String> OPEN_ISSUE_STATUSES =
            Issue.STATUSES.stream().filter(s -> !Issue.STATUS_CLOSED.equals(s) && !Issue.STATUS_RESOLVED.equals(s))
                    .collect(Collectors.toList());

    private static final String REST_API = "/rest/api/1.0/";
    private static final String PROJECT_PR_API = "projects/%s/repos/%s/pull-requests/%s/";
    private static final String COMMENTS_API = "comments";
    private static final String DIFF_API = "diff";

    private static final String FULL_PR_COMMENT_API = "%s" + REST_API + PROJECT_PR_API + COMMENTS_API;
    private static final String FULL_PR_DIFF_API = "%s" + REST_API + PROJECT_PR_API + DIFF_API;

    @Override
    public void decorateQualityGateStatus(AnalysisDetails analysisDetails, AlmSettingDto almSettingDto,
                                          ProjectAlmSettingDto projectAlmSettingDto) {
        LOGGER.info("starting to analyze with " + analysisDetails.toString());

        try {
            final String hostURL = almSettingDto.getUrl();
            final String apiToken = almSettingDto.getPersonalAccessToken();
            final String repositorySlug = projectAlmSettingDto.getAlmSlug();
            final String pullRequestId = analysisDetails.getBranchName();
            final String projectKey = projectAlmSettingDto.getAlmRepo();

            String commentUrl = String.format(FULL_PR_COMMENT_API, hostURL, projectKey, repositorySlug, pullRequestId);
            String diffUrl = String.format(FULL_PR_DIFF_API, hostURL, projectKey, repositorySlug, pullRequestId);

            LOGGER.debug(String.format("Comment URL is: %s ", commentUrl));
            LOGGER.debug(String.format("Diff URL is: %s ", diffUrl));

            Map<String, String> headers = new HashMap<>();
            headers.put("Authorization", String.format("Bearer %s", apiToken));
            headers.put("Accept", "application/json");

            String analysisSummary = analysisDetails.createAnalysisSummary(new MarkdownFormatterFactory());
            StringEntity summaryCommentEntity = new StringEntity(new ObjectMapper().writeValueAsString(new SummaryComment(analysisSummary)), ContentType.APPLICATION_JSON);
            postComment(commentUrl, headers, summaryCommentEntity);

            DiffPage diffPage = getPage(diffUrl, headers, DiffPage.class);
            List<PostAnalysisIssueVisitor.ComponentIssue> componentIssues = analysisDetails.getPostAnalysisIssueVisitor().getIssues().stream().filter(i -> OPEN_ISSUE_STATUSES.contains(i.getIssue().status())).collect(Collectors.toList());
            for (PostAnalysisIssueVisitor.ComponentIssue componentIssue : componentIssues) {
                final DefaultIssue issue = componentIssue.getIssue();
                String analysisIssueSummary = analysisDetails.createAnalysisIssueSummary(componentIssue, new MarkdownFormatterFactory());
                String issuePath = analysisDetails.getSCMPathForIssue(componentIssue).orElse(StringUtils.EMPTY);
                int issueLine = issue.getLine() != null ? issue.getLine() : 0;
                String issueType = getIssueType(diffPage, issuePath, issueLine);
                String fileType = "TO";
                if (issueType.equals("CONTEXT")) {
                    fileType = "FROM";
                }
                StringEntity fileCommentEntity = new StringEntity(
                        new ObjectMapper().writeValueAsString(new FileComment(analysisIssueSummary, new Anchor(issueLine, issueType, issuePath, fileType))), ContentType.APPLICATION_JSON
                );
                postComment(commentUrl, headers, fileCommentEntity);
            }
        } catch (IOException ex) {
            throw new IllegalStateException("Could not decorate Pull Request on Bitbucket Server", ex);
        }

    }

    protected String getIssueType(DiffPage diffPage, String issuePath, int issueLine) {
        String issueType = "CONTEXT";
        List<Diff> diffs = diffPage.getDiffs().stream()
                .filter(diff -> diff.getDestination() != null)
                .filter(diff -> issuePath.equals(diff.getDestination().getToString()))
                .collect(Collectors.toList());

        if (!diffs.isEmpty()) {
            for (Diff diff : diffs) {
                List<Hunk> hunks = diff.getHunks();
                if (!hunks.isEmpty()) {
                    issueType = getExtractIssueType(issueLine, issueType, hunks);
                }
            }
        }
        return issueType;
    }

    private String getExtractIssueType(int issueLine, String issueType, List<Hunk> hunks) {
        for (Hunk hunk : hunks) {
            List<Segment> segments = hunk.getSegments();
            for (Segment segment : segments) {
                Optional<DiffLine> optionalLine = segment.getLines().stream().filter(diffLine -> diffLine.getDestination() == issueLine).findFirst();
                if (optionalLine.isPresent()) {
                    issueType = segment.getType();
                    break;
                }
            }
        }
        return issueType;
    }

    protected <T> T getPage(String diffUrl, Map<String, String> headers, Class<T> type) {
        T page = null;
        try (CloseableHttpClient closeableHttpClient = HttpClients.createDefault()) {
            LOGGER.debug(String.format("Getting page %s", type));
            HttpGet httpGet = new HttpGet(diffUrl);
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                httpGet.addHeader(entry.getKey(), entry.getValue());
            }
            HttpResponse httpResponse = closeableHttpClient.execute(httpGet);
            if (null == httpResponse) {
                LOGGER.error(String.format("HttpResponse for getting page %s was null", type));
            } else if (httpResponse.getStatusLine().getStatusCode() != 200) {
                HttpEntity entity = httpResponse.getEntity();
                LOGGER.error("Error response from Bitbucket: " + IOUtils.toString(entity.getContent(), StandardCharsets.UTF_8.name()));
                throw new IllegalStateException(String.format("Error response returned from Bitbucket Server. Expected HTTP Status 200 but got %s", httpResponse.getStatusLine().getStatusCode()) );
            } else {
                HttpEntity entity = httpResponse.getEntity();
                page = new ObjectMapper()
                        .configure(DeserializationFeature.ACCEPT_EMPTY_ARRAY_AS_NULL_OBJECT, true)
                        .configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true)
                        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                        .readValue(IOUtils.toString(entity.getContent(), StandardCharsets.UTF_8.name()), type);
                LOGGER.debug(new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(page));
            }
        } catch (IOException ex) {
            LOGGER.error(String.format("Could not get %s from Bitbucket Server", type.getName()), ex);
        }
        return type.cast(page);
    }

    protected boolean postComment(String commentUrl, Map<String, String> headers, StringEntity requestEntity)
            throws IOException {
        boolean commentPosted = false;
        HttpPost httpPost = new HttpPost(commentUrl);
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            httpPost.addHeader(entry.getKey(), entry.getValue());
        }
        httpPost.setEntity(requestEntity);
        LOGGER.debug(EntityUtils.toString(requestEntity));
        try (CloseableHttpClient closeableHttpClient = HttpClients.createDefault()) {
            HttpResponse httpResponse = closeableHttpClient.execute(httpPost);
            if (null == httpResponse) {
                LOGGER.error("HttpResponse for posting comment was null");
            } else if (httpResponse.getStatusLine().getStatusCode() != 201) {
                HttpEntity entity = httpResponse.getEntity();
                LOGGER.error(IOUtils.toString(entity.getContent(), StandardCharsets.UTF_8.name()));
            } else {
                HttpEntity entity = httpResponse.getEntity();
                LOGGER.debug(IOUtils.toString(entity.getContent(), StandardCharsets.UTF_8.name()));
                commentPosted = true;
            }
        }
        return commentPosted;
    }

    @Override
    public String name() {
        return "BitbucketServer";
    }

    @Override
    public ALM alm() {
        return ALM.BITBUCKET;
    }
}
