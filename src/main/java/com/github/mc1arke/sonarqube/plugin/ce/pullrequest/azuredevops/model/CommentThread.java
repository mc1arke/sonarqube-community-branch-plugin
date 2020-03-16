package com.github.mc1arke.sonarqube.plugin.ce.pullrequest.azuredevops.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.azuredevops.model.enums.CommentThreadStatus;
import org.sonar.db.protobuf.DbIssues;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

/**
 * Represents a comment thread of a pull request. A thread contains meta data about the file
 * it was left on along with one or more comments (an initial comment and the subsequent replies).
 */
public class CommentThread implements Serializable {

    private CommentThreadStatus status;
    private List<Comment> comments;
    private CommentThreadContext threadContext;
    private int id;
    private Date publishedDate;
    private Date lastUpdatedDate;
    private HashMap<String, IdentityRef> identities;
    @JsonProperty("isDeleted")
    private Boolean deleted;
    @JsonProperty("_links")
    private ReferenceLinks links;
    public CommentThread(){};

    public CommentThread(String filePath, DbIssues.Locations locations, String message){
        comments = Arrays.asList(
                new Comment(message)
        );
        status = CommentThreadStatus.ACTIVE; //CommentThreadStatusMapper.toCommentThreadStatus(issue.status());
        threadContext = new CommentThreadContext(
                filePath,
                locations
        );

    }
    /**
     * A list of the comments.
     */
    public List<Comment> getComments(){
        return this.comments;
    };
    /**
     * The status of the comment thread.
     */
    public CommentThreadStatus getStatus(){
        return this.status;
    };
    /**
     * Specify thread context such as position in left/right file.
     */
    public CommentThreadContext getThreadContext() {
        return this.threadContext;
    };
    /**
     * The comment thread id.
     */
    public int getId()
    {
        return this.id;
    };
    /**
     * The time this thread was published.
     */
    public Date getPublishedDate()
    {
        return this.publishedDate;
    };
    /**
     * The time this thread was last updated.
     */
    public Date getLastUpdatedDate()
    {
        return this.lastUpdatedDate;
    };
    /**
     * Set of identities related to this thread
     */
    public HashMap<String, IdentityRef> getIdentities()
    {
        return this.identities;
    };
    /**
     * Specify if the thread is deleted which happens when all comments are deleted.
     */
    public Boolean isDeleted()
    {
        return this.deleted;
    };
    /**
     * Links to other related objects.
     */
    public ReferenceLinks getLinks()
    {
        return this.links;
    };
}
