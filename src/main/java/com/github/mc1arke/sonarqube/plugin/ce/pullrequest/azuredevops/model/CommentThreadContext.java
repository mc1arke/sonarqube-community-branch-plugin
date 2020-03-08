package com.github.mc1arke.sonarqube.plugin.ce.pullrequest.azuredevops.model;

import org.sonar.core.issue.DefaultIssue;

import java.io.Serializable;

public class CommentThreadContext implements Serializable {

    final String filePath;
    final CommentPosition leftFileStart;
    final CommentPosition leftFileEnd;
    final CommentPosition rightFileStart;
    final CommentPosition rightFileEnd;

    public CommentThreadContext(String filePath, Integer line){
        this.filePath = filePath;
        this.leftFileEnd = null;
        this.leftFileStart = null;
        this.rightFileEnd = new CommentPosition(
                line,
                1
        );
        this.rightFileStart = new CommentPosition(
                line,
                0
        );
    }
    /**
     * File path relative to the root of the repository. It's up to the client to
     */
    public String getFilePath(){
        return this.filePath;
    };

    /**
     * Position of first character of the thread's span in left file. ///
     */
    public CommentPosition getLeftFileStart(){
        return this.leftFileStart;
    };

    /**
     * Position of last character of the thread's span in left file. ///
     */
    public CommentPosition getLeftFileEnd(){
        return this.leftFileEnd;
    };

    /**
     * Position of first character of the thread's span in right file. ///
     */
    public CommentPosition getRightFileStart(){
        return this.rightFileStart;
    };

    /**
     * Position of last character of the thread's span in right file. ///
     */
    public CommentPosition getRightFileEnd(){
        return this.rightFileEnd;
    };
}
