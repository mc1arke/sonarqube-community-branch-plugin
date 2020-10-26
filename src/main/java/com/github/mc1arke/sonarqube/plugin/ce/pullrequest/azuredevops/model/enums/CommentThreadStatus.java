package com.github.mc1arke.sonarqube.plugin.ce.pullrequest.azuredevops.model.enums;

/**
 * The status of a comment thread
 */
public enum CommentThreadStatus {
    /**
     * The thread status is unknown.
     */
    UNKNOWN,
    /**
     * The thread status is active.
     */
    ACTIVE,
    /**
     * The thread status is resolved as fixed.
     */
    FIXED,
    /**
     * The thread status is resolved as won't fix.
     */
    WONTFIX,
    /**
     * The thread status is closed.
     */
    CLOSED,
    /**
     * The thread status is resolved as by design.
     */
    BYDESIGN,
    /**
     * The thread status is pending.
     */
    PENDING
}

