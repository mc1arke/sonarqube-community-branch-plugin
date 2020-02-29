package com.github.mc1arke.sonarqube.plugin.ce.pullrequest.bitbucket.cloud.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class InlineDTO implements Serializable {
    private final Integer from;
    private final Integer to;
    private final String path;

    @JsonCreator
    public InlineDTO(@JsonProperty("from") Integer from, @JsonProperty("to") Integer to, @JsonProperty("path") String path) {
        this.from = from;
        this.to = to;
        this.path = path;
    }

    public Integer getFrom() {
        return from;
    }

    public Integer getTo() {
        return to;
    }

    public String getPath() {
        return path;
    }
}
