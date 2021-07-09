package com.github.mc1arke.sonarqube.plugin.almclient.azuredevops.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class CommentPosition {
    
    private final int line;
    private final int offset;

    @JsonCreator
    public CommentPosition(@JsonProperty("line") int line, @JsonProperty("offset") int offset){
        this.line = line;
        this.offset = offset;
    }
    
    /**
     *The line number of a thread's position. Starts at 1. ///
     */
    public int getLine() {
        return this.line;
    }

    /**
     *The character offset of a thread's position inside of a line. Starts at 0.
     */
    public int getOffset() {
        return this.offset;
    }
}
