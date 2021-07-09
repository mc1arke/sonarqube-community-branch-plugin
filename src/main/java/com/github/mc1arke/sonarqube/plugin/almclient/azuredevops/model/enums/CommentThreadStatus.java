package com.github.mc1arke.sonarqube.plugin.almclient.azuredevops.model.enums;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * The status of a comment thread
 */
public enum CommentThreadStatus {
    /**
     * The thread status is unknown.
     */
    @JsonProperty("unknown")
    UNKNOWN,
    /**
     * The thread status is active.
     */
    @JsonProperty("active")
    ACTIVE,
    /**
     * The thread status is resolved as fixed.
     */
    @JsonProperty("fixed")
    FIXED,
    /**
     * The thread status is resolved as won't fix.
     */
    @JsonProperty("wontfix")
    WONTFIX,
    /**
     * The thread status is closed.
     */
    @JsonProperty("closed")
    CLOSED,
    /**
     * The thread status is resolved as by design.
     */
    @JsonProperty("bydesign")
    BYDESIGN,
    /**
     * The thread status is pending.
     */
    @JsonProperty("pending")
    PENDING
}

