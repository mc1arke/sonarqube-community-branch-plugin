package com.github.mc1arke.sonarqube.plugin.ce.pullrequest.azuredevops.model;

/**
 * State of the status.
 */
public enum GitStatusState
{
    /**
     * Status state not set. Default state.
     */
    NotSet,
    /**
     * Status pending.
     */
    Pending,
    /**
     * Status succeeded.
     */
    Succeeded,
    /**
     * Status failed.
     */
    Failed,
    /**
     * Status with an error.
     */
    Error,
    /**
     * Status is not applicable to the target object.
     */
    NotApplicable
}
