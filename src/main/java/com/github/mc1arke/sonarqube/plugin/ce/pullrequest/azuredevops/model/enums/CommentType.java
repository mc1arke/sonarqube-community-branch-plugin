package com.github.mc1arke.sonarqube.plugin.ce.pullrequest.azuredevops.model.enums;

public enum CommentType {
    /**
     * The comment type is not known.
     */
    unknown,
    /**
     * This is a regular user comment.
     */
    text,
    /**
     * The comment comes as a result of a code change.
     */
    codeChange,
    /**
     * The comment represents a system message.
     */
    system
}
