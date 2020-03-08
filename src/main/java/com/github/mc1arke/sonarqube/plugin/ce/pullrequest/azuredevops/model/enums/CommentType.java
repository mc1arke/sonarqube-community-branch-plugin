package com.github.mc1arke.sonarqube.plugin.ce.pullrequest.azuredevops.model.enums;

public enum CommentType {
    /**
     * The comment type is not known.
     */
    Unknown,
    /**
     * This is a regular user comment.
     */
    Text,
    /**
     * The comment comes as a result of a code change.
     */
    CodeChange,
    /**
     * The comment represents a system message.
     */
    System
}
