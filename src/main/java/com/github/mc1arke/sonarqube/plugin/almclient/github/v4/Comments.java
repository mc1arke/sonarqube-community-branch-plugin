/*
 * Copyright (C) 2021 Julien Roy, Michael Clarke
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
package com.github.mc1arke.sonarqube.plugin.almclient.github.v4;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.aexp.nodes.graphql.annotations.GraphQLProperty;

import java.util.List;

public class Comments {
    private final List<CommentNode> nodes;
    private final PageInfo pageInfo;

    @JsonCreator
    public Comments(@JsonProperty("nodes") List<CommentNode> nodes, @JsonProperty("pageInfo") PageInfo pageInfo) {
        this.nodes = nodes;
        this.pageInfo = pageInfo;
    }

    public List<CommentNode> getNodes() {
        return nodes;
    }

    public PageInfo getPageInfo() {
        return pageInfo;
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
}
