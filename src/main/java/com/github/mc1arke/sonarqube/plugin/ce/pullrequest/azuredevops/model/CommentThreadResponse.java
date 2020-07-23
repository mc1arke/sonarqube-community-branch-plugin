package com.github.mc1arke.sonarqube.plugin.ce.pullrequest.azuredevops.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class CommentThreadResponse {
    
    private final CommentThread[] value;

    @JsonCreator
    public CommentThreadResponse(@JsonProperty("value") CommentThread[] value) {
        this.value = value;
    }

    public CommentThread[] getValue() {
        return this.value;
    }
}
