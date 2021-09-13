package com.github.mc1arke.sonarqube.plugin.almclient.azuredevops.model.enums;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * The status of a comment thread
 * Enum names match those here: https://docs.microsoft.com/en-us/rest/api/azure/devops/git/pull-request-threads/get?#commentthreadstatus
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
    @JsonProperty("wontFix")
    WONTFIX,
    /**
     * The thread status is closed.
     */
    @JsonProperty("closed")
    CLOSED,
    /**
     * The thread status is resolved as by design.
     */
    @JsonProperty("byDesign")
    BYDESIGN,
    /**
     * The thread status is pending.
     */
    @JsonProperty("pending")
    PENDING
}

