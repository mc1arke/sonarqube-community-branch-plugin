package com.github.mc1arke.sonarqube.plugin.ce.pullrequest.azuredevops.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;

public class Link implements Serializable {
    
    private final String href;

    @JsonCreator
    public Link(@JsonProperty("href") String href) {
        this.href = href;
    }
    
    public String getHref() {
        return this.href;
    }
}
