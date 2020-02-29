/*
 * Copyright (C) 2019 Oliver Jedinger
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

import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.UnifyConfiguration;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.AnalysisDetails;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.PullRequestBuildStatusDecorator;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;

public class BitbucketServerPullRequestDecorator implements PullRequestBuildStatusDecorator {

    public static final String PULL_REQUEST_BITBUCKET_URL = "com.github.mc1arke.sonarqube.plugin.branch.pullrequest.bitbucket.url";

    public static final String PULL_REQUEST_BITBUCKET_TOKEN = "com.github.mc1arke.sonarqube.plugin.branch.pullrequest.bitbucket.token";

    public static final String PULL_REQUEST_BITBUCKET_PROJECT_KEY = "sonar.pullrequest.bitbucket.projectKey";

    public static final String PULL_REQUEST_BITBUCKET_USER_SLUG = "sonar.pullrequest.bitbucket.userSlug";

    public static final String PULL_REQUEST_BITBUCKET_REPOSITORY_SLUG = "sonar.pullrequest.bitbucket.repositorySlug";

    public static final String PULL_REQUEST_BITBUCKET_COMMENT_USER_SLUG = "com.github.mc1arke.sonarqube.plugin.branch.pullrequest.bitbucket.comment.userSlug";

    private static final Logger LOGGER = Loggers.get(BitbucketServerPullRequestDecorator.class);

    @Override
    public void decorateQualityGateStatus(AnalysisDetails analysisDetails, UnifyConfiguration configuration) {
        LOGGER.info("Starting to analyze with " + analysisDetails.toString());

        final String hostURL = configuration.getRequiredServerProperty(PULL_REQUEST_BITBUCKET_URL);
        final String apiToken = configuration.getRequiredServerProperty(PULL_REQUEST_BITBUCKET_TOKEN);
        final String repositorySlug = configuration.getRequiredProperty(PULL_REQUEST_BITBUCKET_REPOSITORY_SLUG);
        final String pullRequestId = analysisDetails.getBranchName();
        final String userSlug = configuration.getProperty(PULL_REQUEST_BITBUCKET_USER_SLUG).orElse(StringUtils.EMPTY);
        final String projectKey = configuration.getProperty(PULL_REQUEST_BITBUCKET_PROJECT_KEY).orElse(StringUtils.EMPTY);
        final String commentUserSlug = configuration.getServerProperty(PULL_REQUEST_BITBUCKET_COMMENT_USER_SLUG).orElse(StringUtils.EMPTY);

        final boolean summaryCommentEnabled = Boolean.parseBoolean(configuration.getRequiredServerProperty(PULL_REQUEST_COMMENT_SUMMARY_ENABLED));
        final boolean fileCommentEnabled = Boolean.parseBoolean(configuration.getRequiredServerProperty(PULL_REQUEST_FILE_COMMENT_ENABLED));
        final boolean deleteCommentsEnabled = Boolean.parseBoolean(configuration.getRequiredServerProperty(PULL_REQUEST_DELETE_COMMENTS_ENABLED));
        final boolean approveEnabled = Boolean.parseBoolean(configuration.getRequiredServerProperty(PULL_REQUEST_APPROVE_ENABLED));

        BitbucketServerRepository repository = resolveRepository(userSlug, projectKey, repositorySlug);
        BitbucketServerClient bitbucketServerClient = new BitbucketServerClient(hostURL, apiToken);
        PullRequestService pullRequestService = new PullRequestService(bitbucketServerClient, repository, pullRequestId);

        if (deleteCommentsEnabled && StringUtils.isNotBlank(commentUserSlug)) {
            LOGGER.info("Dropping old comments from pull request");
            pullRequestService.deleteOldComments(commentUserSlug);
        }

        if (summaryCommentEnabled) {
            LOGGER.info("Posting summary comment");
            pullRequestService.postSummaryComment(analysisDetails);
        }

        if (fileCommentEnabled) {
            LOGGER.info("Posting issue comments");
            pullRequestService.postIssueComments(analysisDetails);
        }

        if (approveEnabled && StringUtils.isNotBlank(commentUserSlug)) {
            LOGGER.info("Changing pull request status");
            pullRequestService.changePullRequestStatus(analysisDetails, commentUserSlug);
        }
    }

    private BitbucketServerRepository resolveRepository(String userSlug, String projectKey, String repositorySlug) {
        if (StringUtils.isNotBlank(userSlug)) {
            return BitbucketServerRepository.userRepository(userSlug, repositorySlug);
        } else if (StringUtils.isNotBlank(projectKey)) {
            return BitbucketServerRepository.projectRepository(projectKey, repositorySlug);
        } else {
            throw new IllegalStateException(
                    String.format("Property userSlug (%s) for /user repo or projectKey (%s) for /projects repo needs to be set.",
                            PULL_REQUEST_BITBUCKET_USER_SLUG, PULL_REQUEST_BITBUCKET_PROJECT_KEY));
        }
    }

    @Override
    public String name() {
        return "BitbucketServer";
    }
}
