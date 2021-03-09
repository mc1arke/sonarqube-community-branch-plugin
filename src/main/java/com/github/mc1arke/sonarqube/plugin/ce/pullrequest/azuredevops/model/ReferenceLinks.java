package com.github.mc1arke.sonarqube.plugin.ce.pullrequest.azuredevops.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;

public class ReferenceLinks implements Serializable {
    
    private final Link self;
    
    @JsonCreator
    public ReferenceLinks(@JsonProperty("self") Link self) {
        this.self = self;
    }

    public Link getSelf() {
        return this.self;
    }
}
