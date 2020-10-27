package com.github.mc1arke.sonarqube.plugin.ce.pullrequest.azuredevops.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.azuredevops.model.enums.CommentType;

import java.io.Serializable;
import java.util.Date;
import java.util.List;


/**
 * Represents a comment which is one of potentially many in a comment thread.
 */
public class Comment implements Serializable {

    private final String content;
    private final CommentType commentType;
    private final Integer parentCommentId;
    private final int id;
    private final int threadId;
    private final IdentityRef author;
    private final Date publishedDate;
    private final Date lastUpdatedDate;
    private final Date lastContentUpdatedDate;
    @JsonProperty("isDeleted")
    private final boolean deleted;
    private final List<IdentityRef> usersLiked;
    @JsonProperty("_links")
    private final ReferenceLinks links;

    @JsonCreator
    public Comment(@JsonProperty("content") String content, 
                   @JsonProperty("commentType") CommentType commentType,
                   @JsonProperty("parentCommentId") Integer parentCommentId, 
                   @JsonProperty("id") int id, 
                   @JsonProperty("threadId") int threadId, 
                   @JsonProperty("author") IdentityRef author,
                   @JsonProperty("publishedDate") Date publishedDate, 
                   @JsonProperty("lastUpdatedDate") Date lastUpdatedDate, 
                   @JsonProperty("lastContentUpdatedDate") Date lastContentUpdatedDate, 
                   @JsonProperty("isDeleted") boolean deleted,
                   @JsonProperty("usersLiked") List<IdentityRef> usersLiked,
                   @JsonProperty("_links") ReferenceLinks links) {

        this.content = content;
        this.commentType = commentType;
        this.parentCommentId = parentCommentId;
        this.id = id;
        this.threadId = threadId;
        this.author = author;
        this.publishedDate = publishedDate;
        this.lastUpdatedDate = lastUpdatedDate;
        this.lastContentUpdatedDate = lastContentUpdatedDate;
        this.deleted = deleted;
        this.usersLiked = usersLiked;
        this.links = links;
    }

    public Comment(String content) {
        this.content = content;
        this.parentCommentId = 0;
        this.commentType = CommentType.TEXT;

        this.id = 0;
        this.threadId = 0;
        this.author = null;
        this.publishedDate = null;
        this.lastUpdatedDate = null;
        this.lastContentUpdatedDate = null;
        this.deleted = false;
        this.usersLiked = null;
        this.links = null;
    }

    /**
     * The ID of the parent comment. This is used for replies.
     */
    public Integer getParentCommentId() {
        return this.parentCommentId;
    }
    
    /**
     * The comment content.
     */
    public String getContent() {
        return this.content;
    }

    /**
     * The comment type at the time of creation.
     */
    public CommentType getCommentType() {
        return this.commentType;
    }

    /**
     * The comment ID. IDs start at 1 and are unique to a pull request.
     */
    public int getId() {
        return this.id;
    }

    /**
     * The parent thread ID. Used for internal server purposes only -- note
     * that this field is not exposed to the REST client.
     */
    public int getThreadId() {
        return this.threadId;
    }

    /**
     * The author of the comment.
     */
    public IdentityRef getAuthor() {
        return this.author;
    }

    /**
     * The date the comment was first published.;
     */
    public Date getPublishedDate() {
        return this.publishedDate;
    }

    /**
     * The date the comment was last updated.
     */
    public Date getLastUpdatedDate() {
        return this.lastUpdatedDate;
    }

    /**
     * The date the comment's content was last updated.
     */
    public Date getLastContentUpdatedDate() {
        return this.lastContentUpdatedDate;
    }

    /**
     * Whether or not this comment was soft-deleted.
     */
    public boolean isDeleted() {
        return this.deleted;
    }

    /**
     * A list of the users who have liked this comment.
     */
    public List<IdentityRef> getUsersLiked() {
        return this.usersLiked;
    }

    /**
     * Links to other related objects.
     */
    public ReferenceLinks getLinks() {
        return this.links;
    }
}
