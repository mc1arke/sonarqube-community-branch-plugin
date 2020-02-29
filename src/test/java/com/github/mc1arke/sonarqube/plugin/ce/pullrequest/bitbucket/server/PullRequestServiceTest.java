/*
 * Copyright (C) 2020 Artemy Osipov
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
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.PostAnalysisIssueVisitor;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.PostAnalysisIssueVisitor.ComponentIssue;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.bitbucket.Anchor;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.bitbucket.PullRequestParticipant;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.bitbucket.response.activity.Comment;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.bitbucket.response.activity.User;
import com.google.common.collect.Lists;
import org.junit.Before;
import org.junit.Test;
import org.sonar.api.ce.posttask.QualityGate;
import org.sonar.core.issue.DefaultIssue;

import java.io.IOException;
import java.util.Optional;

import static com.github.mc1arke.sonarqube.plugin.ce.pullrequest.bitbucket.server.TestData.PULL_REQUEST_ID;
import static com.github.mc1arke.sonarqube.plugin.ce.pullrequest.bitbucket.server.TestData.REPOSITORY;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class PullRequestServiceTest {

    private final BitbucketServerClient bitbucketClient = mock(BitbucketServerClient.class);
    private final AnalysisDetails analysisDetails = mock(AnalysisDetails.class);
    private final PostAnalysisIssueVisitor issueVisitor = mock(PostAnalysisIssueVisitor.class);
    private final ComponentIssue componentIssue = mock(ComponentIssue.class);
    private final DefaultIssue issue = mock(DefaultIssue.class);

    private final PullRequestService service = new PullRequestService(bitbucketClient, REPOSITORY, PULL_REQUEST_ID);

    @Before
    public void init() {
        doReturn(issueVisitor)
                .when(analysisDetails)
                .getPostAnalysisIssueVisitor();
        doReturn(Lists.newArrayList(componentIssue))
                .when(issueVisitor)
                .getDecoratedIssues();
        doReturn(Optional.of(TestData.ISSUE_FILE_PATH))
                .when(componentIssue)
                .getSCMPath();
        doReturn(issue)
                .when(componentIssue)
                .getIssue();
        doReturn(TestData.ISSUE_LINE)
                .when(issue)
                .getLine();
    }

    @Test
    public void shouldSkipDeleteOldCommentsWhenFailedGetActivityPage() throws Exception {
        doThrow(new IOException())
                .when(bitbucketClient)
                .getActivityPage(REPOSITORY, PULL_REQUEST_ID);

        service.deleteOldComments(TestData.COMMENT_USER_SLUG);

        verify(bitbucketClient, never()).deleteCommentFromPullRequest(eq(REPOSITORY), eq(PULL_REQUEST_ID), any());
    }

    @Test
    public void shouldDeleteOnlySpecificComments() throws Exception {
        Comment validComment = TestData.comment();
        Comment invalidComment = new Comment(1, 1, null, null, null);
        Comment someoneComment = new Comment(1, 1, null, new User("other", "other"), null);
        doReturn(TestData.activityPageWithComments(validComment, invalidComment, someoneComment))
                .when(bitbucketClient)
                .getActivityPage(REPOSITORY, PULL_REQUEST_ID);

        service.deleteOldComments(TestData.COMMENT_USER_SLUG);

        verify(bitbucketClient).deleteCommentFromPullRequest(REPOSITORY, PULL_REQUEST_ID, validComment);
        verify(bitbucketClient).deleteCommentFromPullRequest(eq(REPOSITORY), eq(PULL_REQUEST_ID), any());
    }

    @Test
    public void shouldSkipDeleteCommentWhenFailed() throws Exception {
        Comment failedComment = TestData.comment();
        Comment successComment = TestData.comment();
        doReturn(TestData.activityPageWithComments(failedComment, successComment))
                .when(bitbucketClient)
                .getActivityPage(REPOSITORY, PULL_REQUEST_ID);
        doThrow(new IOException())
                .when(bitbucketClient)
                .deleteCommentFromPullRequest(REPOSITORY, PULL_REQUEST_ID, failedComment);

        service.deleteOldComments(TestData.COMMENT_USER_SLUG);

        verify(bitbucketClient).deleteCommentFromPullRequest(REPOSITORY, PULL_REQUEST_ID, successComment);
        verify(bitbucketClient, times(2)).deleteCommentFromPullRequest(eq(REPOSITORY), eq(PULL_REQUEST_ID), any());
    }

    @Test
    public void shouldPostSummaryComment() throws Exception {
        AnalysisDetails analysisDetails = mock(AnalysisDetails.class);

        service.postSummaryComment(analysisDetails);

        verify(bitbucketClient).postCommentToPullRequest(eq(REPOSITORY), eq(PULL_REQUEST_ID), any());
    }

    @Test
    public void shouldSkipPostSummaryCommentWhenFailed() throws Exception {
        doThrow(new IOException())
                .when(bitbucketClient)
                .postCommentToPullRequest(eq(REPOSITORY), eq(PULL_REQUEST_ID), any());

        service.postSummaryComment(analysisDetails);

        verify(bitbucketClient).postCommentToPullRequest(eq(REPOSITORY), eq(PULL_REQUEST_ID), any());
    }

    @Test
    public void shouldSkipPostIssueCommentsWhenFailedGetDiffPage() throws Exception {
        doThrow(new IOException())
                .when(bitbucketClient)
                .getDiffPage(REPOSITORY, PULL_REQUEST_ID);

        service.postIssueComments(analysisDetails);

        verify(bitbucketClient, never()).postCommentToPullRequest(eq(REPOSITORY), eq(PULL_REQUEST_ID), any());
    }

    @Test
    public void shouldPostIssueComments() throws Exception {
        doReturn(TestData.diffPage())
                .when(bitbucketClient)
                .getDiffPage(REPOSITORY, PULL_REQUEST_ID);

        service.postIssueComments(analysisDetails);

        verify(bitbucketClient).postCommentToPullRequest(eq(REPOSITORY), eq(PULL_REQUEST_ID), any());
    }

    @Test
    public void shouldBuildAnchor() {
        Anchor anchor = service.buildAnchor(TestData.diffPage(), componentIssue);

        assertThat(anchor.getLine(), is(TestData.ISSUE_LINE));
        assertThat(anchor.getLineType(), is("ADDED"));
        assertThat(anchor.getPath(), is(TestData.ISSUE_FILE_PATH));
        assertThat(anchor.getFileType(), is("TO"));
    }

    @Test
    public void shouldApprovePullRequestWhenQualityGatePassed() throws Exception {
        doReturn(QualityGate.Status.OK)
                .when(analysisDetails)
                .getQualityGateStatus();

        service.changePullRequestStatus(analysisDetails, TestData.COMMENT_USER_SLUG);

        verify(bitbucketClient).addPullRequestStatus(REPOSITORY, PULL_REQUEST_ID, TestData.COMMENT_USER_SLUG, PullRequestParticipant.Status.APPROVED);
    }

    @Test
    public void shouldUnapprovePullRequestWhenQualityGateNotPassed() throws Exception {
        doReturn(QualityGate.Status.ERROR)
                .when(analysisDetails)
                .getQualityGateStatus();

        service.changePullRequestStatus(analysisDetails, TestData.COMMENT_USER_SLUG);

        verify(bitbucketClient).addPullRequestStatus(REPOSITORY, PULL_REQUEST_ID, TestData.COMMENT_USER_SLUG, PullRequestParticipant.Status.UNAPPROVED);
    }

    @Test
    public void shouldSkipChangePullRequestStatusWhenFailed() throws Exception {
        doThrow(new IOException())
                .when(bitbucketClient)
                .addPullRequestStatus(eq(REPOSITORY), eq(PULL_REQUEST_ID), eq(TestData.COMMENT_USER_SLUG), any());

        service.changePullRequestStatus(analysisDetails, TestData.COMMENT_USER_SLUG);
    }
}