package com.github.mc1arke.sonarqube.plugin.ce.pullrequest.bitbucket;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class Insights implements Serializable {
    private final String title;

    private final String result;

    @JsonCreator
    public Insights(@JsonProperty("title") String title, @JsonProperty("result") String result) {
        this.title = title;
        this.result = result;
    }

    public String getTitle() {
        return title;
    }

    public String getResult() {
        return result;
    }
}
