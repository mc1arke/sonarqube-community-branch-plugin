package com.github.mc1arke.sonarqube.plugin.ce.pullrequest.azuredevops.model.enums;

/**
 * The status of a comment thread
 */
public enum CommentThreadStatus {
    /**
     * The thread status is unknown.
     */
    Unknown,
    /**
     * The thread status is active.
     */
    Active,
    /**
     * The thread status is resolved as fixed.
     */
    Fixed,
    /**
     * The thread status is resolved as won't fix.
     */
    WontFix,
    /**
     * The thread status is closed.
     */
    Closed,
    /**
     * The thread status is resolved as by design.
     */
    ByDesign,
    /**
     * The thread status is pending.
     */
    Pending
}

