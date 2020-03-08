package com.github.mc1arke.sonarqube.plugin.ce.pullrequest.azuredevops.model;

import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.azuredevops.model.enums.CommentThreadStatus;
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

    final CommentThreadStatus status;
    private List<Comment> comments;
    private CommentThreadContext threadContext;

    public CommentThread(String filePath, Integer line, String message){
        comments = Arrays.asList(
                new Comment(message)
        );
        status = CommentThreadStatus.Active; //CommentThreadStatusMapper.toCommentThreadStatus(issue.status());
        threadContext = new CommentThreadContext(filePath, line);

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
    private int id;
    /**
     * The time this thread was published.
     */
    private Date publishedDate;
    /**
     * The time this thread was last updated.
     */
    private Date lastUpdatedDate;
    /**
     * Optional properties associated with the thread as a collection of key-value
     */
    private PropertiesCollection properties;
    /**
     * Set of identities related to this thread
     */
    private HashMap<String, IdentityRef> identities;
    /**
     * Specify if the thread is deleted which happens when all comments are deleted.
     */
    private Boolean isDeleted;
    /**
     * Links to other related objects.
     */
    private ReferenceLinks links;
}
