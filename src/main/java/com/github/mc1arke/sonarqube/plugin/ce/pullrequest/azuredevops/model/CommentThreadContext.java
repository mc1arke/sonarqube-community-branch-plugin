package com.github.mc1arke.sonarqube.plugin.ce.pullrequest.azuredevops.model;

import org.sonar.db.protobuf.DbIssues;

import java.io.Serializable;

public class CommentThreadContext implements Serializable {

    private String filePath;
    private CommentPosition leftFileStart;
    private CommentPosition leftFileEnd;
    private CommentPosition rightFileStart;
    private CommentPosition rightFileEnd;

    public CommentThreadContext() {};

    public CommentThreadContext(String filePath, DbIssues.Locations locations){
        this.filePath = filePath;
        this.leftFileEnd = null;
        this.leftFileStart = null;
        this.rightFileEnd = new CommentPosition(
                locations.getTextRange().getEndLine(),
                locations.getTextRange().getEndOffset()
        );
        this.rightFileStart = new CommentPosition(
                locations.getTextRange().getStartLine(),
                locations.getTextRange().getStartOffset()
        );
    }
    /**
     * File path relative to the root of the repository. It's up to the client to
     */
    public String getFilePath(){
        return this.filePath;
    };
    /**
     * File path relative to the root of the repository. It's up to the client to
     */
    public void setFilePath(String value){
        this.filePath = value;
    };

    /**
     * Position of first character of the thread's span in left file. ///
     */
    public CommentPosition getLeftFileStart(){
        return this.leftFileStart;
    };

    /**
     * Position of first character of the thread's span in left file. ///
     */
    public void setLeftFileStart(CommentPosition value)
    {
        this.leftFileStart = value;
    };

    /**
     * Position of last character of the thread's span in left file. ///
     */
    public CommentPosition getLeftFileEnd(){
        return this.leftFileEnd;
    };
    /**
     * Position of last character of the thread's span in left file. ///
     */
    public void setLeftFileEnd(CommentPosition value)
    {
        this.leftFileEnd = value;
    };

    /**
     * Position of first character of the thread's span in right file. ///
     */
    public CommentPosition getRightFileStart(){
        return this.rightFileStart;
    };
    /**
     * Position of first character of the thread's span in right file. ///
     */
    public void setRightFileStart(CommentPosition value)
    {
        this.rightFileStart = value;
    };

    /**
     * Position of last character of the thread's span in right file. ///
     */
    public CommentPosition getRightFileEnd(){
        return this.rightFileEnd;
    };
    /**
     * Position of last character of the thread's span in right file. ///
     */
    public void setRightFileEnd(CommentPosition value)
    {
        this.rightFileEnd = value;
    };
}
