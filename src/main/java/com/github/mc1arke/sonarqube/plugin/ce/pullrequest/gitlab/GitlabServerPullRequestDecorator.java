/*
 * Copyright (C) 2019 Markus Heberling
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
package com.github.mc1arke.sonarqube.plugin.ce.pullrequest.gitlab;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.AnalysisDetails;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.PostAnalysisIssueVisitor;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.PullRequestBuildStatusDecorator;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.gitlab.response.Commit;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.gitlab.response.Discussion;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.gitlab.response.MergeRequest;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.gitlab.response.Note;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.gitlab.response.User;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.markup.MarkdownFormatterFactory;
import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.sonar.api.ce.posttask.QualityGate;
import org.sonar.api.config.Configuration;
import org.sonar.api.issue.Issue;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.platform.Server;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.ce.task.projectanalysis.component.ConfigurationRepository;
import org.sonar.ce.task.projectanalysis.scm.Changeset;
import org.sonar.ce.task.projectanalysis.scm.ScmInfoRepository;

public class GitlabServerPullRequestDecorator implements PullRequestBuildStatusDecorator {

    private static final Logger LOGGER = Loggers.get(GitlabServerPullRequestDecorator.class);
    private static final List<String> OPEN_ISSUE_STATUSES =
            Issue.STATUSES.stream().filter(s -> !Issue.STATUS_CLOSED.equals(s) && !Issue.STATUS_RESOLVED.equals(s))
                    .collect(Collectors.toList());
    public static final String PULLREQUEST_GITLAB_URL = "com.github.mc1arke.sonarqube.plugin.branch.pullrequest.gitlab.url";
    public static final String PULLREQUEST_GITLAB_TOKEN = "com.github.mc1arke.sonarqube.plugin.branch.pullrequest.gitlab.token";
    public static final String PULLREQUEST_GITLAB_REPOSITORY_SLUG = "com.github.mc1arke.sonarqube.plugin.branch.pullrequest.gitlab.repositorySlug";


    private final ConfigurationRepository configurationRepository;
    private final Server server;
    private final ScmInfoRepository scmInfoRepository;

    public GitlabServerPullRequestDecorator(Server server, ConfigurationRepository configurationRepository, ScmInfoRepository scmInfoRepository) {
        super();
        this.configurationRepository = configurationRepository;
        this.server = server;
        this.scmInfoRepository = scmInfoRepository;
    }

    @Override
    public void decorateQualityGateStatus(AnalysisDetails analysis) {
        LOGGER.info("starting to analyze with " + analysis.toString());
        String revision = analysis.getCommitSha();

        try {
            Configuration configuration = configurationRepository.getConfiguration();
            final String hostURL = getMandatoryProperty(PULLREQUEST_GITLAB_URL, configuration);
            final String apiToken = getMandatoryProperty(PULLREQUEST_GITLAB_TOKEN, configuration);
            final String repositorySlug = getMandatoryProperty(PULLREQUEST_GITLAB_REPOSITORY_SLUG, configuration);
            final String pullRequestId = analysis.getBranchName();

            final boolean summaryCommentEnabled = Boolean.parseBoolean(getMandatoryProperty(PULL_REQUEST_COMMENT_SUMMARY_ENABLED, configuration));
            final boolean fileCommentEnabled = Boolean.parseBoolean(getMandatoryProperty(PULL_REQUEST_FILE_COMMENT_ENABLED, configuration));
            final boolean deleteCommentsEnabled = Boolean.parseBoolean(getMandatoryProperty(PULL_REQUEST_DELETE_COMMENTS_ENABLED, configuration));

            final String restURL = String.format("%s/api/v4", hostURL);
            final String userURL = restURL + "/user";
            final String projectURL = restURL + String.format("/projects/%s", URLEncoder.encode(repositorySlug, StandardCharsets.UTF_8.name()));
            final String statusUrl = projectURL + String.format("/statuses/%s", revision);
            final String mergeRequestURl = projectURL + String.format("/merge_requests/%s", pullRequestId);
            final String prCommitsURL = mergeRequestURl + "/commits";
            final String mergeRequestDiscussionURL = mergeRequestURl + "/discussions";


            LOGGER.info(String.format("Status url is: %s ", statusUrl));
            LOGGER.info(String.format("PR commits url is: %s ", prCommitsURL));
            LOGGER.info(String.format("MR discussion url is: %s ", mergeRequestDiscussionURL));
            LOGGER.info(String.format("User url is: %s ", userURL));

            Map<String, String> headers = new HashMap<>();
            headers.put("PRIVATE-TOKEN", apiToken);
            headers.put("Accept", "application/json");

            User user = getSingle(userURL, headers, User.class);
            LOGGER.info(String.format("Using user: %s ", user.getUsername()));

            List<String> commits = getPagedList(prCommitsURL, headers, deleteCommentsEnabled, new TypeReference<List<Commit>>() {
            }).stream().map(Commit::getId).collect(Collectors.toList());
            MergeRequest mergeRequest = getSingle(mergeRequestURl, headers, MergeRequest.class);

            List<Discussion> discussions = getPagedList(mergeRequestDiscussionURL, headers, deleteCommentsEnabled, new TypeReference<List<Discussion>>() {
            });

            LOGGER.info(String.format("Discussions in MR: %s ", discussions
                    .stream()
                    .map(Discussion::getId)
                    .collect(Collectors.joining(", "))));

            for (Discussion discussion : discussions) {
                for (Note note : discussion.getNotes()) {
                    if (!note.isSystem() && note.getAuthor() != null && note.getAuthor().getUsername().equals(user.getUsername())) {
                        //delete only our own comments
                        deleteCommitDiscussionNote(mergeRequestDiscussionURL + String.format("/%s/notes/%s",
                                discussion.getId(),
                                note.getId()),
                                headers, deleteCommentsEnabled);
                    }
                }
            }

            QualityGate.Condition newCoverageCondition = analysis.findQualityGateCondition(CoreMetrics.NEW_COVERAGE_KEY)
                    .orElseThrow(() -> new IllegalStateException("Could not find New Coverage Condition in analysis"));
            String coverageValue = newCoverageCondition.getStatus().equals(QualityGate.EvaluationStatus.NO_VALUE) ? "0" : newCoverageCondition.getValue();


            List<PostAnalysisIssueVisitor.ComponentIssue> openIssues = analysis.getPostAnalysisIssueVisitor().getIssues().stream().filter(i -> OPEN_ISSUE_STATUSES.contains(i.getIssue().getStatus())).collect(Collectors.toList());

            String summaryComment = analysis.createAnalysisSummary(new MarkdownFormatterFactory());
            List<NameValuePair> summaryContentParams = Collections.singletonList(new BasicNameValuePair("body", summaryComment));

            postStatus(statusUrl, headers, analysis, coverageValue, true);

            postCommitComment(mergeRequestDiscussionURL, headers, summaryContentParams, summaryCommentEnabled);

            for (PostAnalysisIssueVisitor.ComponentIssue issue : openIssues) {
                String path = analysis.getSCMPathForIssue(issue).orElse(null);
                if (path != null && issue.getIssue().getLine() != null) {
                    //only if we have a path and line number
                    String fileComment = analysis.createAnalysisIssueSummary(issue, new MarkdownFormatterFactory());
                    if (scmInfoRepository.getScmInfo(issue.getComponent())
                            .filter(i -> i.hasChangesetForLine(issue.getIssue().getLine()))
                            .map(i -> i.getChangesetForLine(issue.getIssue().getLine()))
                            .map(Changeset::getRevision)
                            .filter(commits::contains)
                            .isPresent()) {
                        //only if the change is on a commit, that belongs to this MR

                        List<NameValuePair> fileContentParams = Arrays.asList(
                                new BasicNameValuePair("body", fileComment),
                                new BasicNameValuePair("position[base_sha]", mergeRequest.getDiffRefs().getBaseSha()),
                                new BasicNameValuePair("position[start_sha]", mergeRequest.getDiffRefs().getStartSha()),
                                new BasicNameValuePair("position[head_sha]", mergeRequest.getDiffRefs().getHeadSha()),
                                new BasicNameValuePair("position[new_path]", path),
                                new BasicNameValuePair("position[new_line]", String.valueOf(issue.getIssue().getLine())),
                                new BasicNameValuePair("position[position_type]", "text"));
                        postCommitComment(mergeRequestDiscussionURL, headers, fileContentParams, fileCommentEnabled);
                    } else {
                        LOGGER.info(String.format("Skipping %s:%d since the commit does not belong to the MR", path, issue.getIssue().getLine()));
                    }
                }
            }
        } catch (IOException ex) {
            throw new IllegalStateException("Could not decorate Pull Request on Gitlab Server", ex);
        }

    }

    private <X> X getSingle(String userURL, Map<String, String> headers, Class<X> type) throws IOException {
        HttpGet httpGet = new HttpGet(userURL);
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            httpGet.addHeader(entry.getKey(), entry.getValue());
        }
        HttpResponse httpResponse = HttpClients.createDefault().execute(httpGet);
        if (null != httpResponse && httpResponse.getStatusLine().getStatusCode() != 200) {
            LOGGER.error(httpResponse.toString());
            LOGGER.error(EntityUtils.toString(httpResponse.getEntity(), StandardCharsets.UTF_8));
            throw new IllegalStateException("An error was returned in the response from the Gitlab API. See the previous log messages for details");
        } else if (null != httpResponse) {
            LOGGER.debug(httpResponse.toString());
            HttpEntity entity = httpResponse.getEntity();
            X user = new ObjectMapper()
                    .configure(DeserializationFeature.ACCEPT_EMPTY_ARRAY_AS_NULL_OBJECT, true)
                    .configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true)
                    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                    .readValue(IOUtils.toString(entity.getContent(), StandardCharsets.UTF_8), type);

            LOGGER.info(type + " received");

            return user;
        } else {
            throw new IOException("No response reveived");
        }
    }

    private <X> List<X> getPagedList(String commitDiscussionURL, Map<String, String> headers, boolean sendRequest, TypeReference<List<X>> typeRef) throws IOException {
        HttpGet httpGet = new HttpGet(commitDiscussionURL);
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            httpGet.addHeader(entry.getKey(), entry.getValue());
        }

        List<X> discussions = new ArrayList<>();

        if (sendRequest) {
            HttpResponse httpResponse = HttpClients.createDefault().execute(httpGet);
            if (null != httpResponse && httpResponse.getStatusLine().getStatusCode() != 200) {
                LOGGER.error(httpResponse.toString());
                LOGGER.error(EntityUtils.toString(httpResponse.getEntity(), StandardCharsets.UTF_8));
                throw new IllegalStateException("An error was returned in the response from the Gitlab API. See the previous log messages for details");
            } else if (null != httpResponse) {
                LOGGER.debug(httpResponse.toString());
                HttpEntity entity = httpResponse.getEntity();
                List<X> pagedDiscussions = new ObjectMapper()
                        .configure(DeserializationFeature.ACCEPT_EMPTY_ARRAY_AS_NULL_OBJECT, true)
                        .configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true)
                        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                        .readValue(IOUtils.toString(entity.getContent(), StandardCharsets.UTF_8), typeRef);
                discussions.addAll(pagedDiscussions);
                LOGGER.info("MR discussions received");
                Optional<String> nextURL = getNextUrl(httpResponse);
                if (nextURL.isPresent()) {
                    LOGGER.info("Getting next page");
                    discussions.addAll(getPagedList(nextURL.get(), headers, sendRequest, typeRef));
                }
            }
        }
        return discussions;
    }

    private void deleteCommitDiscussionNote(String commitDiscussionNoteURL, Map<String, String> headers, boolean sendRequest) throws IOException {
        //https://docs.gitlab.com/ee/api/discussions.html#delete-a-commit-thread-note
        HttpDelete httpDelete = new HttpDelete(commitDiscussionNoteURL);
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            httpDelete.addHeader(entry.getKey(), entry.getValue());
        }

        if (sendRequest) {
            LOGGER.info("Deleting {} with headers {}", commitDiscussionNoteURL, headers);

            HttpResponse httpResponse = HttpClients.createDefault().execute(httpDelete);
            validateGitlabResponse(httpResponse, 204, "Commit discussions note deleted");
        }
    }

    private void postCommitComment(String commitCommentUrl, Map<String, String> headers, List<NameValuePair> params, boolean sendRequest) throws IOException {
        //https://docs.gitlab.com/ee/api/commits.html#post-comment-to-commit
        HttpPost httpPost = new HttpPost(commitCommentUrl);
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            httpPost.addHeader(entry.getKey(), entry.getValue());
        }
        httpPost.setEntity(new UrlEncodedFormEntity(params));

        if (sendRequest) {
            LOGGER.info("Posting {} with headers {} to {}", params, headers, commitCommentUrl);

            HttpResponse httpResponse = HttpClients.createDefault().execute(httpPost);
            validateGitlabResponse(httpResponse, 201, "Comment posted");
        }
    }

    private void postStatus(String statusPostUrl, Map<String, String> headers, AnalysisDetails analysis, String coverage, boolean sendRequest) throws IOException{
        //See https://docs.gitlab.com/ee/api/commits.html#post-the-build-status-to-a-commit
        statusPostUrl += "?name=SonarQube";
        String status = (analysis.getQualityGateStatus() == QualityGate.Status.OK ? "success" : "failed");
        statusPostUrl += "&state=" + status;
        statusPostUrl += "&target_url=" + URLEncoder.encode(String.format("%s/dashboard?id=%s&pullRequest=%s", server.getPublicRootUrl(),
                URLEncoder.encode(analysis.getAnalysisProjectKey(),
                        StandardCharsets.UTF_8.name()), URLEncoder
                        .encode(analysis.getBranchName(),
                                StandardCharsets.UTF_8.name())), StandardCharsets.UTF_8.name());
        statusPostUrl+="&description="+URLEncoder.encode("SonarQube Status", StandardCharsets.UTF_8.name());
        statusPostUrl+="&coverage="+coverage;
        //TODO: add pipelineId if we have it

        HttpPost httpPost = new HttpPost(statusPostUrl);
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            httpPost.addHeader(entry.getKey(), entry.getValue());
        }
        if (sendRequest) {
            HttpResponse httpResponse = HttpClients.createDefault().execute(httpPost);
            if (null != httpResponse && httpResponse.toString().contains("Cannot transition status")) {
                // Workaround for https://gitlab.com/gitlab-org/gitlab-ce/issues/25807
                LOGGER.debug("Transition status is already {}", status);
            } else {
                validateGitlabResponse(httpResponse, 201, "Comment posted");
            }
        }
    }

    private void validateGitlabResponse(HttpResponse httpResponse, int expectedStatus, String successLogMessage) throws IOException {
        if (null != httpResponse && httpResponse.getStatusLine().getStatusCode() != expectedStatus) {
            LOGGER.error(httpResponse.toString());
            LOGGER.error(EntityUtils.toString(httpResponse.getEntity(), StandardCharsets.UTF_8));
            throw new IllegalStateException("An error was returned in the response from the Gitlab API. See the previous log messages for details");
        } else if (null != httpResponse) {
            LOGGER.debug(httpResponse.toString());
            LOGGER.info(successLogMessage);
        }
    }

    private static String getMandatoryProperty(String propertyName, Configuration configuration) {
        return configuration.get(propertyName).orElseThrow(() -> new IllegalStateException(
                String.format("%s must be specified in the project configuration", propertyName)));
    }

    private static Optional<String> getNextUrl(HttpResponse httpResponse) {
        Header linkHeader = httpResponse.getFirstHeader("Link");
        if (linkHeader != null) {
            Matcher matcher = Pattern.compile("<([^>]+)>;[\\s]*rel=\"([a-z]+)\"").matcher(linkHeader.getValue());
            while (matcher.find()) {
                if (matcher.group(2).equals("next")) {
                    //found the next rel return the URL
                    return Optional.of(matcher.group(1));
                }
            }
        }
        return Optional.empty();
    }

    @Override
    public String name() {
        return "GitlabServer";
    }
}
