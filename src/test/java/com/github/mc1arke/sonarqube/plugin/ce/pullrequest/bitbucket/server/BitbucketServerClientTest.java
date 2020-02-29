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

import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.bitbucket.PullRequestParticipant;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.bitbucket.response.activity.ActivityPage;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.bitbucket.response.diff.DiffPage;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.junit.Rule;
import org.junit.Test;

import java.io.IOException;

import static com.github.mc1arke.sonarqube.plugin.ce.pullrequest.bitbucket.server.TestData.BITBUCKET_HOST;
import static com.github.mc1arke.sonarqube.plugin.ce.pullrequest.bitbucket.server.TestData.BITBUCKET_TOKEN;
import static com.github.mc1arke.sonarqube.plugin.ce.pullrequest.bitbucket.server.TestData.COMMENT_USER_SLUG;
import static com.github.mc1arke.sonarqube.plugin.ce.pullrequest.bitbucket.server.TestData.PULL_REQUEST_ID;
import static com.github.mc1arke.sonarqube.plugin.ce.pullrequest.bitbucket.server.TestData.REPOSITORY;
import static com.github.mc1arke.sonarqube.plugin.ce.pullrequest.bitbucket.server.TestData.comment;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;

public class BitbucketServerClientTest {

    private final BitbucketServerClient client = new BitbucketServerClient(BITBUCKET_HOST, BITBUCKET_TOKEN);

    @Rule
    public final WireMockRule wireMockRule = new WireMockRule(wireMockConfig().port(8089));

    @Test
    public void shouldReturnActivityPage() throws Exception {
        BitbucketServerStub.getActivities().success();

        ActivityPage activityPage = client.getActivityPage(REPOSITORY, PULL_REQUEST_ID);

        assertThat(activityPage, notNullValue());
        assertThat(activityPage.getSize(), is(3));
    }

    @Test(expected = IOException.class)
    public void shouldFailGetActivityPageWhenUnexpectedResponseStatus() throws Exception {
        BitbucketServerStub.getActivities().notFound();

        client.getActivityPage(REPOSITORY, PULL_REQUEST_ID);
    }

    @Test
    public void shouldReturnDiffPage() throws Exception {
        BitbucketServerStub.getDiff().success();

        DiffPage page = client.getDiffPage(REPOSITORY, PULL_REQUEST_ID);

        assertThat(page, notNullValue());
        assertThat(page.getDiffs().size(), is(1));
    }

    @Test
    public void shouldDeleteCommentFromPullRequest() throws Exception {
        BitbucketServerStub.deleteComment(comment()).success();

        client.deleteCommentFromPullRequest(REPOSITORY, PULL_REQUEST_ID, comment());

        WireMock.verify(
                BitbucketServerStub.deleteComment(comment()).forVerify()
        );
    }

    @Test(expected = IOException.class)
    public void shouldFailDeleteCommentWhenUnexpectedResponseStatus() throws Exception {
        BitbucketServerStub.deleteComment(comment()).notFound();

        client.deleteCommentFromPullRequest(REPOSITORY, PULL_REQUEST_ID, comment());
    }

    @Test
    public void shouldPostCommentToPullRequest() throws Exception {
        BitbucketServerStub.postComment().success();

        client.postCommentToPullRequest(REPOSITORY, PULL_REQUEST_ID, TestData.summaryComment());

        WireMock.verify(
                BitbucketServerStub.postComment().forVerify()
        );
    }

    @Test(expected = IOException.class)
    public void shouldFailPostCommentToPullRequestWhenUnexpectedResponseStatus() throws Exception {
        BitbucketServerStub.postComment().validationError();

        client.postCommentToPullRequest(REPOSITORY, PULL_REQUEST_ID, TestData.summaryComment());
    }

    @Test
    public void shouldAddPullRequestStatus() throws Exception {
        BitbucketServerStub.addPullRequestStatus(COMMENT_USER_SLUG).success();

        client.addPullRequestStatus(REPOSITORY, PULL_REQUEST_ID, COMMENT_USER_SLUG, PullRequestParticipant.Status.APPROVED);

        WireMock.verify(
                BitbucketServerStub.addPullRequestStatus(COMMENT_USER_SLUG).forVerify()
        );
    }

    @Test(expected = IOException.class)
    public void shouldFailAddPullRequestStatusWhenUnexpectedResponseStatus() throws Exception {
        BitbucketServerStub.addPullRequestStatus(COMMENT_USER_SLUG).validationError();

        client.addPullRequestStatus(REPOSITORY, PULL_REQUEST_ID, COMMENT_USER_SLUG, PullRequestParticipant.Status.APPROVED);
    }
}