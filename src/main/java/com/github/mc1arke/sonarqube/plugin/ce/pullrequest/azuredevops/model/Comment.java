package com.github.mc1arke.sonarqube.plugin.ce.pullrequest.azuredevops.model;

import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.azuredevops.model.enums.CommentType;

import java.io.Serializable;
import java.util.Date;
import java.util.List;


/**
 * Represents a comment which is one of potentially many in a comment thread.
 */
public class Comment implements Serializable {

    final String content;
    final CommentType commentType;
    private Integer parentCommentId;

    public Comment(String content){
        this.content = content;
        this.parentCommentId = 0;
        this.commentType = CommentType.Text;
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
    public CommentType privateCommentType(){
        return this.commentType;
    };
    /**
     * The comment ID. IDs start at 1 and are unique to a pull request.
     */
    private Integer id;
    /**
     * The parent thread ID. Used for internal server purposes only -- note
     * that this field is not exposed to the REST client.
     */
    private Integer threadId;
    /**
     * The author of the comment.
     */
    private IdentityRef author;
    /**
     * The date the comment was first published.;
     */
    private Date publishedDate;
    /**
     * The date the comment was last updated.
     */
    private Date lastUpdatedDate;
    /**
     * The date the comment's content was last updated.
     */
    private Date lastContentUpdatedDate;
    /**
     * Whether or not this comment was soft-deleted.
     */
    private Boolean isDeleted;
    /**
     * A list of the users who have liked this comment.
     */
    private List<IdentityRef> usersLiked;
    /**
     * Links to other related objects.
     */
    private ReferenceLinks links;
}
