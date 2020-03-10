package com.github.mc1arke.sonarqube.plugin.ce.pullrequest.azuredevops.model;

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

    public Comment(){}

    public Comment(String content){
        this.content = content;
        this.parentCommentId = 0;
        this.commentType = CommentType.text;
    }
    /**
     * The ID of the parent comment. This is used for replies.
     */
    public Integer getParentCommentId(){
        return this.parentCommentId;
    };
    /**
     * The comment content.
     */
    public String getContent(){
        return this.content;
    }
    /**
     * The ID of the parent comment. This is used for replies.
     */
    public void setParentCommentId(Integer value){
        this.parentCommentId = value;
    }
    /**
     * The comment type at the time of creation.
     */
    public CommentType getCommentType(){
        return this.commentType;
    };
    /**
     * The comment type at the time of creation.
     */
    public void setCommentType(CommentType value){
        this.commentType = value;
    };

    /**
     * The comment ID. IDs start at 1 and are unique to a pull request.
     */
    public Integer id;
    /**
     * The parent thread ID. Used for internal server purposes only -- note
     * that this field is not exposed to the REST client.
     */
    public Integer threadId;
    /**
     * The author of the comment.
     */
    public IdentityRef author;
    /**
     * The date the comment was first published.;
     */
    public Date publishedDate;
    /**
     * The date the comment was last updated.
     */
    public Date lastUpdatedDate;
    /**
     * The date the comment's content was last updated.
     */
    public Date lastContentUpdatedDate;
    /**
     * Whether or not this comment was soft-deleted.
     */
    public Boolean isDeleted;
    /**
     * A list of the users who have liked this comment.
     */
    public List<IdentityRef> usersLiked;
    /**
     * Links to other related objects.
     */
    public ReferenceLinks links;
}
