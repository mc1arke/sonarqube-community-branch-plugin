package com.github.mc1arke.sonarqube.plugin.almclient.azuredevops.model.enums;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum CommentType {
    /**
     * The comment type is not known.
     */
    @JsonProperty("unknown")
    UNKNOWN,
    /**
     * This is a regular user comment.
     */
    @JsonProperty("text")
    TEXT,
    /**
     * The comment comes as a result of a code change.
     */
    @JsonProperty("codeChange")
    CODECHANGE,
    /**
     * The comment represents a system message.
     */
    @JsonProperty("system")
    SYSTEM
}
