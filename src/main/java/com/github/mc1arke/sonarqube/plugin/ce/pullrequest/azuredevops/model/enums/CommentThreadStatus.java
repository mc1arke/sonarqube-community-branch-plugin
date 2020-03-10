package com.github.mc1arke.sonarqube.plugin.ce.pullrequest.azuredevops.model.enums;

/**
 * The status of a comment thread
 */
public enum CommentThreadStatus {
    /**
     * The thread status is unknown.
     */
    unknown,
    /**
     * The thread status is active.
     */
    active,
    /**
     * The thread status is resolved as fixed.
     */
    fixed,
    /**
     * The thread status is resolved as won't fix.
     */
    wontFix,
    /**
     * The thread status is closed.
     */
    closed,
    /**
     * The thread status is resolved as by design.
     */
    byDesign,
    /**
     * The thread status is pending.
     */
    pending
}

