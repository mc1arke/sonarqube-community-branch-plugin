package com.github.mc1arke.sonarqube.plugin.ce.pullrequest.azuredevops.model.enums;

public enum CommentType {
    /**
     * The comment type is not known.
     */
    UNKNOWN,
    /**
     * This is a regular user comment.
     */
    TEXT,
    /**
     * The comment comes as a result of a code change.
     */
    CODECHANGE,
    /**
     * The comment represents a system message.
     */
    SYSTEM
}
