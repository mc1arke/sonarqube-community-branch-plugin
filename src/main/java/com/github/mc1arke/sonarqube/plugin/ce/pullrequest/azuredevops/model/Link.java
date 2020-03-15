package com.github.mc1arke.sonarqube.plugin.ce.pullrequest.azuredevops.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class Link {
    final String href;
    @JsonCreator
    public Link(@JsonProperty("href") String href)
    {
        this.href = href;
    };
    public String getHref()
    {
        return this.href;
    }
}
