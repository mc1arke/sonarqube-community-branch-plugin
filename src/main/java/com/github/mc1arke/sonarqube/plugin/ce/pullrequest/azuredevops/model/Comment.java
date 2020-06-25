package com.github.mc1arke.sonarqube.plugin.ce.pullrequest.azuredevops.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.azuredevops.model.enums.CommentType;

import java.io.Serializable;
import java.util.Date;
import java.util.List;


/**
 * Represents a comment which is one of potentially many in a comment thread.
 */
public class Comment implements Serializable {

    private String content;
    private CommentType commentType;
    private Integer parentCommentId;
    private int id;
    private int threadId;
    private IdentityRef author;
    private Date publishedDate;
    private Date lastUpdatedDate;
    private Date lastContentUpdatedDate;
    @JsonProperty("isDeleted")
    private Boolean deleted;
    private List<IdentityRef> usersLiked;
    @JsonProperty("_links")
    private ReferenceLinks links;

    public Comment() {
    }

    public Comment(String content) {
        this.content = content;
        this.parentCommentId = 0;
        this.commentType = CommentType.TEXT;
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
    public Boolean isDeleted() {
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
