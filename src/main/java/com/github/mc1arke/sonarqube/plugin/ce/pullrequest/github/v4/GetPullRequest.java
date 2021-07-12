/*
 * Copyright (C) 2021 Julien Roy
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
package com.github.mc1arke.sonarqube.plugin.ce.pullrequest.github.v4;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.aexp.nodes.graphql.annotations.GraphQLArgument;
import io.aexp.nodes.graphql.annotations.GraphQLProperty;

@GraphQLProperty(name = "repository", arguments = {@GraphQLArgument(name = "owner"), @GraphQLArgument(name = "name")})
public class GetPullRequest {

    private final String url;

    @GraphQLProperty(name = "pullRequest", arguments = {@GraphQLArgument(name = "number")})
    private final PullRequest pullRequest;

    @JsonCreator
    public GetPullRequest(@JsonProperty("url") String url, @JsonProperty("pullRequest") PullRequest pullRequest) {
        this.url = url;
        this.pullRequest = pullRequest;
    }

    public String getUrl() {
        return url;
    }

    public PullRequest getPullRequest() {
        return pullRequest;
    }

    public static class PullRequest {

        private final String id;

        @GraphQLProperty(name = "comments", arguments = {@GraphQLArgument(name = "last", value = "100", type = "Integer")})
        private final Comments comments;

        @JsonCreator
        public PullRequest(@JsonProperty("id") String id, @JsonProperty("comments") Comments comments) {
            this.id = id;
            this.comments = comments;
        }

        public String getId() {
            return id;
        }

        public Comments getComments() {
            return comments;
        }
    }

    public static class Comments {
        private final List<CommentNode> nodes;

        @JsonCreator
        public Comments(@JsonProperty("nodes") List<CommentNode> nodes) {
            this.nodes = nodes;
        }

        public List<CommentNode> getNodes() {
            return nodes;
        }
    }

    public static class CommentNode {

        private final String id;
        private final Actor author;
        @GraphQLProperty(name = "isMinimized")
        private final boolean minimized;

        @JsonCreator
        public CommentNode(@JsonProperty("id") String id, @JsonProperty("author") Actor author, @JsonProperty("isMinimized") boolean minimized) {
            this.id = id;
            this.author = author;
            this.minimized = minimized;
        }

        public String getId() {
            return id;
        }

        public Actor getAuthor() {
            return author;
        }

        public boolean isMinimized() {
            return minimized;
        }
    }

    public static class Actor {
        @GraphQLProperty(name = "__typename")
        private final String type;
        private final String login;

        @JsonCreator
        public Actor(@JsonProperty("__typename") String type, @JsonProperty("login") String login) {
            this.type = type;
            this.login = login;
        }

        public String getType() {
            return type;
        }

        public String getLogin() {
            return login;
        }
    }
}
