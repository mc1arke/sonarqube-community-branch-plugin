package com.github.mc1arke.sonarqube.plugin.almclient.azuredevops.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class CommentThreadResponse {
    
    private final List<CommentThread> value;

    @JsonCreator
    public CommentThreadResponse(@JsonProperty("value") List<CommentThread> value) {
        this.value = value;
    }

    public List<CommentThread> getValue() {
        return this.value;
    }
}
