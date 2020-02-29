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

import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.bitbucket.response.activity.Comment;
import com.github.tomakehurst.wiremock.client.MappingBuilder;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.matching.RequestPatternBuilder;
import com.github.tomakehurst.wiremock.matching.UrlPattern;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

public class BitbucketServerStub {

    private static final String BASE_URL = String.format("/rest/api/1.0/projects/%s/repos/%s/pull-requests/%s",
            TestData.PROJECT_KEY, TestData.REPOSITORY_SLUG, TestData.PULL_REQUEST_ID);

    public static class GetActivityStub {

        private final UrlPattern endpoint = urlEqualTo(BASE_URL + "/activities?limit=250");

        public void success() {
            commonStub(
                    get(endpoint),
                    aResponse()
                            .withStatus(200)
                            .withBodyFile("bitbucket/activity.json")
            );
        }

        public void notFound() {
            commonStub(
                    get(endpoint),
                    aResponse()
                            .withStatus(404)
                            .withBody("{}")
            );
        }
    }

    public static class GetDiffStub {

        private final UrlPattern endpoint = urlEqualTo(BASE_URL + "/diff");

        public void success() {
            commonStub(
                    get(endpoint),
                    aResponse()
                            .withStatus(200)
                            .withBodyFile("bitbucket/diff.json")
            );
        }
    }

    public static class DeleteCommentStub {

        private final UrlPattern endpoint;

        DeleteCommentStub(Comment comment) {
            endpoint = urlEqualTo(BASE_URL + "/comments" + String.format("/%s?version=%s", comment.getId(), comment.getVersion()));
        }

        public void success() {
            commonStub(
                    delete(endpoint),
                    aResponse()
                            .withStatus(204)
                            .withBody("{}")
            );
        }

        public void notFound() {
            commonStub(
                    delete(endpoint),
                    aResponse()
                            .withStatus(404)
                            .withBody("{}")
            );
        }

        public RequestPatternBuilder forVerify() {
            return deleteRequestedFor(endpoint);
        }
    }

    public static class PostCommentStub {

        private final UrlPattern endpoint = urlEqualTo(BASE_URL + "/comments");

        public void success() {
            commonStub(
                    post(endpoint),
                    aResponse()
                            .withStatus(201)
                            .withBody("{}")
            );
        }

        public void validationError() {
            commonStub(
                    post(endpoint),
                    aResponse()
                            .withStatus(400)
                            .withBody("{}")
            );
        }

        public RequestPatternBuilder forVerify() {
            return postRequestedFor(endpoint);
        }
    }

    public static class AddPullRequestStatusStub {

        private final UrlPattern endpoint;

        AddPullRequestStatusStub(String userSlug) {
            endpoint = urlEqualTo(BASE_URL + "/participants" + "/" + userSlug);
        }

        public void success() {
            commonStub(
                    put(endpoint),
                    aResponse()
                            .withStatus(200)
                            .withBody("{}")
            );
        }

        public void validationError() {
            commonStub(
                    put(endpoint),
                    aResponse()
                            .withStatus(400)
                            .withBody("{}")
            );
        }

        public RequestPatternBuilder forVerify() {
            return putRequestedFor(endpoint);
        }
    }

    private static void commonStub(MappingBuilder request, ResponseDefinitionBuilder response) {
        stubFor(request
                .withHeader("Accept", equalTo("application/json"))
                .willReturn(response
                        .withHeader("Content-Type", "application/json")
                )
        );
    }

    public static GetActivityStub getActivities() {
        return new GetActivityStub();
    }

    public static GetDiffStub getDiff() {
        return new GetDiffStub();
    }

    public static DeleteCommentStub deleteComment(Comment comment) {
        return new DeleteCommentStub(comment);
    }

    public static PostCommentStub postComment() {
        return new PostCommentStub();
    }

    public static AddPullRequestStatusStub addPullRequestStatus(String userSlug) {
        return new AddPullRequestStatusStub(userSlug);
    }
}
