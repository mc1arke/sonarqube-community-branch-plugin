/*
 * Copyright (C) 2020-2024 Michael Clarke
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
package com.github.mc1arke.sonarqube.plugin.almclient.azuredevops.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.mc1arke.sonarqube.plugin.almclient.azuredevops.model.enums.CommentType;


/**
 * Represents a comment which is one of potentially many in a comment thread.
 */
public class Comment {

    private final int id;
    private final String content;
    private final IdentityRef author;
    private final CommentType commentType;

    @JsonCreator
    public Comment(@JsonProperty("id") int id, @JsonProperty("content") String content, @JsonProperty("author") IdentityRef author,
                   @JsonProperty("commentType") CommentType commentType) {
        this.id = id;
        this.content = content;
        this.author = author;
        this.commentType = commentType;
    }

    public int getId() {
        return id;
    }

    public String getContent() {
        return this.content;
    }

    public IdentityRef getAuthor() {
        return author;
    }

    public CommentType getCommentType() {
        return commentType;
    }


}
