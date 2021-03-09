package com.github.mc1arke.sonarqube.plugin.ce.pullrequest.azuredevops.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.azuredevops.model.enums.CommentThreadStatus;
import org.sonar.db.protobuf.DbIssues;

import java.io.Serializable;
import java.util.Collections;
import java.util.Date;
import java.util.Map;
import java.util.List;

/**
 * Represents a comment thread of a pull request. A thread contains meta data about the file
 * it was left on along with one or more comments (an initial comment and the subsequent replies).
 */
public class CommentThread implements Serializable {

    private final CommentThreadStatus status;
    private final List<Comment> comments;
    private final CommentThreadContext threadContext;
    private final int id;
    private final Date publishedDate;
    private final Date lastUpdatedDate;
    private final Map<String, IdentityRef> identities;
    @JsonProperty("isDeleted")
    private final boolean deleted;
    @JsonProperty("_links")
    private final ReferenceLinks links;

    @JsonCreator
    public CommentThread(@JsonProperty("status") CommentThreadStatus status,
                         @JsonProperty("comments") List<Comment> comments,
                         @JsonProperty("threadContext") CommentThreadContext context,
                         @JsonProperty("id") int id, 
                         @JsonProperty("publishedDate") Date publishedDate, 
                         @JsonProperty("lastUpdatedDate") Date lastUpdatedDate,
                         @JsonProperty("identities") Map<String, IdentityRef> identities, 
                         @JsonProperty("isDeleted") boolean deleted,
                         @JsonProperty("_links") ReferenceLinks links) {

        this.status = status;
        this.comments = comments;
        this.threadContext = context;
        this.id = id;
        this.publishedDate = publishedDate;
        this.lastUpdatedDate = lastUpdatedDate;
        this.identities = identities;
        this.deleted = deleted;
        this.links = links;
    }

    public CommentThread(String filePath, DbIssues.Locations locations, String message) {
        this.status = CommentThreadStatus.ACTIVE;
        this.comments = Collections.singletonList(new Comment(message));
        this.threadContext = new CommentThreadContext(filePath, locations);
        this.id = 0;
        this.publishedDate = null;
        this.lastUpdatedDate = null;
        this.identities = null;
        this.deleted = false;
        this.links = null;        
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
     * The time this thread was published.
     */
    public Date getPublishedDate() {
        return this.publishedDate;
    }

    /**
     * The time this thread was last updated.
     */
    public Date getLastUpdatedDate() {
        return this.lastUpdatedDate;
    }

    /**
     * Set of identities related to this thread
     */
    public Map<String, IdentityRef> getIdentities() {
        return this.identities;
    }

    /**
     * Specify if the thread is deleted which happens when all comments are deleted.
     */
    public boolean isDeleted() {
        return this.deleted;
    }

    /**
     * Links to other related objects.
     */
    public ReferenceLinks getLinks() {
        return this.links;
    }
}
