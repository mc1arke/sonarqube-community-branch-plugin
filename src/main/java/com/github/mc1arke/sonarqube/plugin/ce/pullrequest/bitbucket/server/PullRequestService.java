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

import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.AnalysisDetails;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.PostAnalysisIssueVisitor.ComponentIssue;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.bitbucket.Anchor;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.bitbucket.FileComment;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.bitbucket.PullRequestParticipant;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.bitbucket.SummaryComment;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.bitbucket.response.activity.Activity;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.bitbucket.response.activity.ActivityPage;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.bitbucket.response.activity.Comment;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.bitbucket.response.diff.DiffPage;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.bitbucket.response.diff.Segment;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.markup.MarkdownFormatterFactory;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.ce.posttask.QualityGate;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public class PullRequestService {

    private static final Logger LOGGER = Loggers.get(PullRequestService.class);

    private final BitbucketServerClient bitbucketClient;
    private final BitbucketServerRepository repository;
    private final String pullRequestId;

    public PullRequestService(BitbucketServerClient bitbucketClient, BitbucketServerRepository repository, String pullRequestId) {
        this.bitbucketClient = bitbucketClient;
        this.repository = repository;
        this.pullRequestId = pullRequestId;
    }

    public void deleteOldComments(String commentUserSlug) {
        ActivityPage activityPage;
        try {
            activityPage = bitbucketClient.getActivityPage(repository, pullRequestId);
        } catch (Exception ex) {
            LOGGER.error("Error occurred while getting activity page from BitbucketServer", ex);
            LOGGER.warn("Skip deleting old comments");
            return;
        }

        List<Comment> commentsToDelete = listCommentsToDelete(activityPage, commentUserSlug);
        LOGGER.debug(String.format("Deleting %s comments", commentsToDelete));

        for (Comment comment : commentsToDelete) {
            try {
                bitbucketClient.deleteCommentFromPullRequest(repository, pullRequestId, comment);
            } catch (Exception ex) {
                LOGGER.error("Error occurred while deleting comment from BitbucketServer", ex);
                LOGGER.warn("Skip deleting comment {}", comment.getId());
            }
        }
    }

    private List<Comment> listCommentsToDelete(ActivityPage page, String userSlug) {
        return Arrays.stream(page.getValues())
                .map(Activity::getComment)
                .filter(Objects::nonNull)
                .filter(comment -> comment.getAuthor() != null)
                .filter(comment -> userSlug.equals(comment.getAuthor().getSlug()))
                .filter(comment -> ArrayUtils.isEmpty(comment.getComments()))
                .collect(Collectors.toList());
    }

    public void postSummaryComment(AnalysisDetails analysisDetails) {
        String analysisSummary = analysisDetails.createAnalysisSummary(new MarkdownFormatterFactory());
        SummaryComment comment = new SummaryComment(analysisSummary);

        try {
            bitbucketClient.postCommentToPullRequest(repository, pullRequestId, comment);
        } catch (Exception ex) {
            LOGGER.error("Error occurred while posting summary comment to BitbucketServer", ex);
            LOGGER.warn("Skip posting summary comment");
        }
    }

    public void postIssueComments(AnalysisDetails analysisDetails) {
        DiffPage diffPage;
        try {
            diffPage = bitbucketClient.getDiffPage(repository, pullRequestId);
        } catch (Exception ex) {
            LOGGER.error("Error occurred while getting diff page from BitbucketServer", ex);
            LOGGER.warn("Skip posting issue comments");
            return;
        }

        List<ComponentIssue> componentIssues = analysisDetails.getPostAnalysisIssueVisitor().getDecoratedIssues();

        for (ComponentIssue componentIssue : componentIssues) {
            String analysisIssueSummary = analysisDetails.createAnalysisIssueSummary(componentIssue, new MarkdownFormatterFactory());
            Anchor anchor = buildAnchor(diffPage, componentIssue);
            FileComment comment = new FileComment(analysisIssueSummary, anchor);

            try {
                bitbucketClient.postCommentToPullRequest(repository, pullRequestId, comment);
            } catch (Exception ex) {
                LOGGER.error("Error occurred while posting comment to BitbucketServer", ex);
                LOGGER.warn("Skip posting issue comment {}", componentIssue.getComponent().getUuid());
            }
        }
    }

    Anchor buildAnchor(DiffPage diffPage, ComponentIssue componentIssue) {
        String issuePath = componentIssue.getSCMPath().orElse(StringUtils.EMPTY);
        int issueLine = Optional.ofNullable(componentIssue.getIssue().getLine()).orElse(0);
        String issueType = resolveIssueType(diffPage, issuePath, issueLine);
        String fileType = issueType.equals("CONTEXT") ? "FROM" : "TO";
        return new Anchor(issueLine, issueType, issuePath, fileType);
    }

    private String resolveIssueType(DiffPage diffPage, String issuePath, int issueLine) {
        return diffPage.getDiffs()
                .stream()
                .filter(diff -> diff.getDestination() != null && issuePath.equals(diff.getDestination().getToString()))
                .flatMap(diff -> diff.getHunks().stream())
                .flatMap(hunk -> hunk.getSegments().stream())
                .filter(segment ->
                        segment.getLines()
                                .stream()
                                .anyMatch(diffLine -> diffLine.getDestination() == issueLine))
                .findFirst()
                .map(Segment::getType)
                .orElse("CONTEXT");
    }

    public void changePullRequestStatus(AnalysisDetails analysisDetails, String commentUserSlug) {
        PullRequestParticipant.Status newStatus = analysisDetails.getQualityGateStatus() == QualityGate.Status.OK
                ? PullRequestParticipant.Status.APPROVED
                : PullRequestParticipant.Status.UNAPPROVED;

        try {
            bitbucketClient.addPullRequestStatus(repository, pullRequestId, commentUserSlug, newStatus);
        } catch (Exception ex) {
            LOGGER.error("Error occurred while changing pull request status at BitbucketServer", ex);
            LOGGER.warn("Skip changing pull request status");
        }
    }
}
