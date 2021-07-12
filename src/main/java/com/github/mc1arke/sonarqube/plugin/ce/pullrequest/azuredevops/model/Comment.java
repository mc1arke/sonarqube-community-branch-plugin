package com.github.mc1arke.sonarqube.plugin.ce.pullrequest.azuredevops.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;


/**
 * Represents a comment which is one of potentially many in a comment thread.
 */
public class Comment implements Serializable {

    private final String content;

    @JsonCreator
    public Comment(@JsonProperty("content") String content) {
        this.content = content;
    }

    /**
     * The comment content.
     */
    public String getContent() {
        return this.content;
    }

}
