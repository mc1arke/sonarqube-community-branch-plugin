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
package com.github.mc1arke.sonarqube.plugin.ce.pullrequest.bitbucket.server;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.AnalysisDetails;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.PostAnalysisIssueVisitor;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.PullRequestBuildStatusDecorator;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.bitbucket.response.activity.Activity;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.bitbucket.response.activity.ActivityPage;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.bitbucket.Anchor;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.bitbucket.FileComment;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.bitbucket.SummaryComment;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.bitbucket.response.activity.Comment;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.bitbucket.response.diff.*;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.markup.MarkdownFormatterFactory;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.sonar.api.ce.posttask.Analysis;
import org.sonar.api.ce.posttask.Branch;
import org.sonar.api.ce.posttask.PostProjectAnalysisTask;
import org.sonar.api.ce.posttask.QualityGate;
import org.sonar.api.config.Configuration;
import org.sonar.api.issue.Issue;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.platform.Server;
import org.sonar.api.rules.RuleType;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.ce.task.projectanalysis.component.ConfigurationRepository;
import org.sonar.ce.task.projectanalysis.component.TreeRootHolder;
import org.sonar.ce.task.projectanalysis.measure.Measure;
import org.sonar.ce.task.projectanalysis.measure.MeasureRepository;
import org.sonar.ce.task.projectanalysis.metric.MetricRepository;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.server.measure.Rating;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

import static org.sonar.api.rule.Severity.*;

public class BitbucketServerPullRequestDecorator implements PullRequestBuildStatusDecorator {

    private static final Logger LOGGER = Loggers.get(BitbucketServerPullRequestDecorator.class);
    private static final List<String> OPEN_ISSUE_STATUSES =
            Issue.STATUSES.stream().filter(s -> !Issue.STATUS_CLOSED.equals(s) && !Issue.STATUS_RESOLVED.equals(s))
                    .collect(Collectors.toList());

    private static final String NEW_LINE = "\n";
    private static final String REST_API = "/rest/api/1.0/";
    private static final String USER_PR_API = "users/%s/repos/%s/pull-requests/%s/";
    private static final String PROJECT_PR_API = "projects/%s/repos/%s/pull-requests/%s/";
    private static final String COMMENTS_API = "comments";
    private static final String DIFF_API = "diff";
    private static final String ACTIVITIES = "activities?limit=%s";

    private static final String FULL_PR_COMMENT_API = "%s" + REST_API + PROJECT_PR_API + COMMENTS_API;
    private static final String FULL_PR_COMMENT_USER_API = "%s" + REST_API + USER_PR_API + COMMENTS_API;

    private static final String FULL_PR_ACTIVITIES_API = "%s" + REST_API + PROJECT_PR_API + ACTIVITIES;
    private static final String FULL_PR_ACTIVITIES_USER_API = "%s" + REST_API + USER_PR_API + ACTIVITIES;

    private static final String FULL_PR_DIFF_API = "%s" + REST_API + PROJECT_PR_API + DIFF_API;
    private static final String FULL_PR_DIFF_USER_API = "%s" + REST_API + USER_PR_API + DIFF_API;

    private final ConfigurationRepository configurationRepository;
    private final Server server;
    private final MetricRepository metricRepository;
    private final MeasureRepository measureRepository;
    private final TreeRootHolder treeRootHolder;
    private final PostAnalysisIssueVisitor postAnalysisIssueVisitor;

    public BitbucketServerPullRequestDecorator(Server server, ConfigurationRepository configurationRepository,
                                               MeasureRepository measureRepository, MetricRepository metricRepository,
                                               TreeRootHolder treeRootHolder,
                                               PostAnalysisIssueVisitor postAnalysisIssueVisitor) {
        super();
        this.configurationRepository = configurationRepository;
        this.server = server;
        this.measureRepository = measureRepository;
        this.metricRepository = metricRepository;
        this.treeRootHolder = treeRootHolder;
        this.postAnalysisIssueVisitor = postAnalysisIssueVisitor;
    }

    @Override
    public void decorateQualityGateStatus(AnalysisDetails analysisDetails) {
        LOGGER.info("starting to analyze with " + analysisDetails.toString());

        try {
            Configuration configuration = configurationRepository.getConfiguration();
            final String hostURL = getMandatoryProperty("sonar.pullrequest.bitbucket.url", configuration);
            final String apiToken = getMandatoryProperty("sonar.pullrequest.bitbucket.token", configuration);
            final String repositorySlug = getMandatoryProperty("sonar.pullrequest.bitbucket.repositorySlug", configuration);
            final String pullRequestId = analysisDetails.getBranchName();
            final String userSlug = configuration.get("sonar.pullrequest.bitbucket.userSlug").orElse(StringUtils.EMPTY);
            final String projectKey = configuration.get("sonar.pullrequest.bitbucket.projectKey").orElse(StringUtils.EMPTY);
            final String commentUserSlug = configuration.get("sonar.pullrequest.bitbucket.comment.userSlug").orElse(StringUtils.EMPTY);

            final boolean summaryCommentEnabled = Boolean.parseBoolean(getMandatoryProperty("sonar.pullrequest.summary.comment.enabled", configuration));
            final boolean fileCommentEnabled = Boolean.parseBoolean(getMandatoryProperty("sonar.pullrequest.file.comment.enabled", configuration));
            final boolean deleteCommentsEnabled = Boolean.parseBoolean(getMandatoryProperty("sonar.pullrequest.delete.comments.enabled", configuration));

            final String commentUrl;
            final String activityUrl;
            final String diffUrl;
            if (StringUtils.isNotBlank(userSlug)) {
                commentUrl = String.format(FULL_PR_COMMENT_USER_API, hostURL, userSlug, repositorySlug, pullRequestId);
                diffUrl = String.format(FULL_PR_DIFF_USER_API, hostURL, userSlug, repositorySlug, pullRequestId);
                activityUrl = String.format(FULL_PR_ACTIVITIES_USER_API, hostURL, userSlug, repositorySlug, pullRequestId, 250);
            }
            else if (StringUtils.isNotBlank(projectKey)) {
                commentUrl = String.format(FULL_PR_COMMENT_API, hostURL, projectKey, repositorySlug, pullRequestId);
                diffUrl = String.format(FULL_PR_DIFF_API, hostURL, projectKey, repositorySlug, pullRequestId);
                activityUrl = String.format(FULL_PR_ACTIVITIES_API, hostURL, projectKey, repositorySlug, pullRequestId, 250);
            }
            else
            {
                throw new IllegalStateException("Property userSlug or projectKey needs to be set.");
            }
            LOGGER.info(String.format("Comment URL is: %s ", commentUrl));
            LOGGER.info(String.format("Activity URL is: %s ", activityUrl));
            LOGGER.info(String.format("Diff URL is: %s ", diffUrl));

            Map<String, String> headers = new HashMap<>();
            headers.put("Authorization", String.format("Bearer %s", apiToken));
            headers.put("Accept", "application/json");

            deleteComments(activityUrl, commentUrl, commentUserSlug, headers, deleteCommentsEnabled);
            String analysisSummary = analysisDetails.createAnalysisSummary(new MarkdownFormatterFactory());
            StringEntity summaryCommentEntity = new StringEntity(new ObjectMapper().writeValueAsString(new SummaryComment(analysisSummary)), ContentType.APPLICATION_JSON);
            postComment(commentUrl, headers, summaryCommentEntity, summaryCommentEnabled);

            DiffPage diffPage = getPage(diffUrl, headers, DiffPage.class);
            List<PostAnalysisIssueVisitor.ComponentIssue> componentIssues = analysisDetails.getPostAnalysisIssueVisitor().getIssues().stream().filter(i -> OPEN_ISSUE_STATUSES.contains(i.getIssue().status())).collect(Collectors.toList());
            for (PostAnalysisIssueVisitor.ComponentIssue componentIssue : componentIssues) {
                final DefaultIssue issue = componentIssue.getIssue();
                LOGGER.info(issue.toString());

                String analysisIssueSummary = analysisDetails.createAnalysisIssueSummary(componentIssue, new MarkdownFormatterFactory());
                String issuePath = analysisDetails.getSCMPathForIssue(componentIssue);
                int issueLine = issue.getLine() != null ? issue.getLine() : 0;
                String issueType = getIssueType(diffPage, issuePath, issueLine);
                String fileType = "TO";
                if (issueType.equals("CONTEXT")) {
                    fileType = "FROM";
                }
                StringEntity fileCommentEntity = new StringEntity(
                        new ObjectMapper().writeValueAsString(new FileComment(analysisIssueSummary, new Anchor(issueLine, issueType, issuePath, fileType))), ContentType.APPLICATION_JSON
                );
                postComment(commentUrl, headers, fileCommentEntity, fileCommentEnabled);
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

        if (!diffs.isEmpty())
        {
            for (Diff diff : diffs) {
                List<Hunk> hunks = diff.getHunks();
                if (!hunks.isEmpty())
                {
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
                Optional<Line> optionalLine = segment.getLines().stream().filter(line -> line.getDestination() == issueLine).findFirst();
                if (optionalLine.isPresent())
                {
                    issueType = segment.getType();
                    break;
                }
            }
        }
        return issueType;
    }

    protected boolean deleteComments(String activityUrl, String commentUrl, String userSlug, Map<String, String> headers, boolean deleteCommentsEnabled) {
        if (!deleteCommentsEnabled) {
            return false;
        }
        if (StringUtils.isEmpty(userSlug)) {
            LOGGER.info("No comments deleted cause property comment.userSlug is not set.");
            return false;
        }
        boolean commentsRemoved = false;
        final ActivityPage activityPage = getPage(activityUrl, headers, ActivityPage.class);
        if (activityPage != null) {
            final List<Comment> commentsToDelete = getCommentsToDelete(userSlug, activityPage);
            LOGGER.info(String.format("Deleting %s comments", commentsToDelete));
            for (Comment comment : commentsToDelete) {
                try {
                    boolean commentDeleted = deleteComment(commentUrl, headers, comment);
                    if (commentDeleted) {
                        commentsRemoved = true;
                    }
                } catch (IOException ex) {
                    LOGGER.error("Could not delete comment from Bitbucket Server", ex);
                }
            }

        }
        return commentsRemoved;
    }

    private boolean deleteComment(String commentUrl, Map<String, String> headers, Comment comment) throws IOException {
        boolean commentDeleted = false;
        HttpDelete httpDelete = new HttpDelete(String.format(commentUrl + "/%s?version=%s", comment.getId(), comment.getVersion()));
        LOGGER.info("delete " + comment.getId() + " " + comment.getVersion());
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            httpDelete.addHeader(entry.getKey(), entry.getValue());
        }
        try (CloseableHttpClient closeableHttpClient = HttpClients.createDefault()) {
            HttpResponse deleteResponse = closeableHttpClient.execute(httpDelete);
            if (null != deleteResponse && deleteResponse.getStatusLine().getStatusCode() != 204) {
                LOGGER.error(IOUtils.toString(deleteResponse.getEntity().getContent(), StandardCharsets.UTF_8.name()));
                LOGGER.error("An error was returned in the response from the Bitbucket API. See the previous log messages for details");
            } else if (null != deleteResponse) {
                LOGGER.info(String.format("Comment %s version %s deleted", comment.getId(), comment.getVersion()));
                commentDeleted = true;
            }
        }
        return commentDeleted;
    }

    protected List<Comment> getCommentsToDelete(String userSlug, ActivityPage activityPage) {
        return Arrays.stream(activityPage.getValues())
                        .filter(a -> a.getComment() != null)
                        .filter(a -> a.getComment().getAuthor() != null)
                        .filter(a -> userSlug.equals(a.getComment().getAuthor().getSlug()))
                        .map(Activity::getComment)
                        .collect(Collectors.toList());
    }

    protected <T> T getPage(String diffUrl, Map<String, String> headers, Class<T> type) {
        T page = null;
        try (CloseableHttpClient closeableHttpClient = HttpClients.createDefault())
        {
            LOGGER.info(String.format("Getting page %s", type));
            HttpGet httpGet = new HttpGet(diffUrl);
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                httpGet.addHeader(entry.getKey(), entry.getValue());
            }
            HttpResponse httpResponse = closeableHttpClient.execute(httpGet);
            if (null != httpResponse && httpResponse.getStatusLine().getStatusCode() != 200) {
                HttpEntity entity = httpResponse.getEntity();
                LOGGER.error(IOUtils.toString(entity.getContent(), StandardCharsets.UTF_8.name()));
            } else if (null != httpResponse) {
                HttpEntity entity = httpResponse.getEntity();
                page = new ObjectMapper()
                        .configure(DeserializationFeature.ACCEPT_EMPTY_ARRAY_AS_NULL_OBJECT, true)
                        .configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true)
                        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                        .readValue(IOUtils.toString(entity.getContent(), StandardCharsets.UTF_8.name()), type);
                LOGGER.info(new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(page));
            }
        } catch (IOException ex) {
            LOGGER.error(String.format("Could not get %s from Bitbucket Server", type.getName()), ex);
        }
        return type.cast(page);
    }

    protected boolean postComment(String commentUrl, Map<String, String> headers, StringEntity requestEntity, boolean sendRequest) throws IOException {
        boolean commentPosted = false;
        HttpPost httpPost = new HttpPost(commentUrl);
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            httpPost.addHeader(entry.getKey(), entry.getValue());
        }
        httpPost.setEntity(requestEntity);
        LOGGER.info(EntityUtils.toString(requestEntity));
        if (sendRequest) {
            try (CloseableHttpClient closeableHttpClient = HttpClients.createDefault())
            {
                HttpResponse httpResponse = closeableHttpClient.execute(httpPost);
                if (null != httpResponse && httpResponse.getStatusLine().getStatusCode() != 201) {
                    HttpEntity entity = httpResponse.getEntity();
                    LOGGER.error(IOUtils.toString(entity.getContent(), StandardCharsets.UTF_8.name()));
                } else if (null != httpResponse) {
                    HttpEntity entity = httpResponse.getEntity();
                    LOGGER.info(IOUtils.toString(entity.getContent(), StandardCharsets.UTF_8.name()));
                    LOGGER.info("Comment posted");
                    commentPosted = true;
                }
            }
        }
        return commentPosted;
    }

    private static String pluralOf(long value, String singleLabel, String multiLabel) {
        return value + " " + (1 == value ? singleLabel : multiLabel);
    }


    private static String getMandatoryProperty(String propertyName, Configuration configuration) {
        return configuration.get(propertyName).orElseThrow(() -> new IllegalStateException(
                String.format("%s must be specified in the project configuration", propertyName)));
    }

    private static String format(QualityGate.Condition condition) {
        org.sonar.api.measures.Metric<?> metric = CoreMetrics.getMetric(condition.getMetricKey());
        if (metric.getType() == org.sonar.api.measures.Metric.ValueType.RATING) {
            return String
                    .format("%s %s (%s %s)", Rating.valueOf(Integer.parseInt(condition.getValue())), metric.getName(),
                            condition.getOperator() == QualityGate.Operator.GREATER_THAN ? "is worse than" :
                            "is better than", Rating.valueOf(Integer.parseInt(condition.getErrorThreshold())));
        } else {
            return String.format("%s %s (%s %s)", condition.getStatus().equals(QualityGate.EvaluationStatus.NO_VALUE) ? "0" : condition.getValue(), metric.getName(),
                                 condition.getOperator() == QualityGate.Operator.GREATER_THAN ? "is greater than" :
                                 "is less than", condition.getErrorThreshold());
        }
    }

    @Override
    public String name() {
        return "BitbucketServer";
    }
}
