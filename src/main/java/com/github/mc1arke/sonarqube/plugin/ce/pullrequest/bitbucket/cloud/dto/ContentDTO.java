package com.github.mc1arke.sonarqube.plugin.ce.pullrequest.bitbucket.cloud.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ContentDTO implements Serializable {
    private final String html;
    private final String markup;
    private final String raw;
    private final String type;

    @JsonCreator
    public ContentDTO(@JsonProperty("html") String html, @JsonProperty("markup") String markup, @JsonProperty("raw") String raw, @JsonProperty("type") String type) {
        this.html = html;
        this.markup = markup;
        this.raw = raw;
        this.type = type;
    }

    public String getHtml() {
        return html;
    }

    public String getMarkup() {
        return markup;
    }

    public String getRaw() {
        return raw;
    }

    public String getType() {
        return type;
    }
}
