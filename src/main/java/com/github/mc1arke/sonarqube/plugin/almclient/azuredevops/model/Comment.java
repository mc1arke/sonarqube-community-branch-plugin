package com.github.mc1arke.sonarqube.plugin.almclient.azuredevops.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.mc1arke.sonarqube.plugin.almclient.azuredevops.model.enums.CommentType;


/**
 * Represents a comment which is one of potentially many in a comment thread.
 */
public class Comment {

    private final String content;
    private final IdentityRef author;
    private final CommentType commentType;

    @JsonCreator
    public Comment(@JsonProperty("content") String content, @JsonProperty("author") IdentityRef author,
                   @JsonProperty("commentType") CommentType commentType) {
        this.content = content;
        this.author = author;
        this.commentType = commentType;
    }

    /**
     * The comment content.
     */
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
