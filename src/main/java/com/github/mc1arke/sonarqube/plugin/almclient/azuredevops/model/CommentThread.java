package com.github.mc1arke.sonarqube.plugin.almclient.azuredevops.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.mc1arke.sonarqube.plugin.almclient.azuredevops.model.enums.CommentThreadStatus;

import java.util.List;

/**
 * Represents a comment thread of a pull request. A thread contains meta data about the file
 * it was left on along with one or more comments (an initial comment and the subsequent replies).
 */
public class CommentThread {

    private final CommentThreadStatus status;
    private final List<Comment> comments;
    private final CommentThreadContext threadContext;
    private final int id;
    @JsonProperty("isDeleted")
    private final boolean deleted;

    @JsonCreator
    public CommentThread(@JsonProperty("status") CommentThreadStatus status,
                         @JsonProperty("comments") List<Comment> comments,
                         @JsonProperty("threadContext") CommentThreadContext context,
                         @JsonProperty("id") Integer id,
                         @JsonProperty("isDeleted") Boolean deleted) {

        this.status = status;
        this.comments = comments;
        this.threadContext = context;
        this.id = id;
        this.deleted = deleted;
    }

    /**
     * A list of the comments.
     */
    public List<Comment> getComments() {
        return this.comments;
    }

    /**
     * The status of the comment thread.
     */
    public CommentThreadStatus getStatus() {
        return this.status;
    }

    /**
     * Specify thread context such as position in left/right file.
     */
    public CommentThreadContext getThreadContext() {
        return this.threadContext;
    }

    /**
     * The comment thread id.
     */
    public int getId() {
        return this.id;
    }

    /**
     * Specify if the thread is deleted which happens when all comments are deleted.
     */
    public boolean isDeleted() {
        return this.deleted;
    }

}
