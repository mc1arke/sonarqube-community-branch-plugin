package com.github.mc1arke.sonarqube.plugin.almclient.azuredevops.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class CommentThreadContext {

    private final String filePath;
    private final CommentPosition rightFileStart;
    private final CommentPosition rightFileEnd;

    @JsonCreator
    public CommentThreadContext(@JsonProperty("filePath") String filePath, @JsonProperty("rightFileStart") CommentPosition rightFileStart,
                                @JsonProperty("rightFileEnd") CommentPosition rightFileEnd) {
        this.filePath = filePath;
        this.rightFileStart = rightFileStart;
        this.rightFileEnd = rightFileEnd;
    }

    /**
     * File path relative to the root of the repository. It's up to the client to
     */
    public String getFilePath() {
        return this.filePath;
    }

    /**
     * Position of first character of the thread's span in right file. ///
     */
    public CommentPosition getRightFileStart() {
        return this.rightFileStart;
    }

    /**
     * Position of last character of the thread's span in right file. ///
     */
    public CommentPosition getRightFileEnd() {
        return this.rightFileEnd;
    }
}
