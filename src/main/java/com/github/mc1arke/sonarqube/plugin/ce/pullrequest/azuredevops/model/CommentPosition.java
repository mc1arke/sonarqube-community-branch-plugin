package com.github.mc1arke.sonarqube.plugin.ce.pullrequest.azuredevops.model;

import java.io.Serializable;

public class CommentPosition implements Serializable {
    private Integer line;
    private Integer offset;

    public CommentPosition() {};

    public CommentPosition(Integer line, Integer offset){
        this.line = line;
        this.offset = offset + 1;
    }
    /**
     *The line number of a thread's position. Starts at 1. ///
     */
    public Integer getLine()
    {
        return this.line;
    };

    /**
     *The character offset of a thread's position inside of a line. Starts at 0.
     */
    public Integer getOffset(){
        return this.offset;
    };
}
