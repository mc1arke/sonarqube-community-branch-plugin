package com.github.mc1arke.sonarqube.plugin.almclient.azuredevops.model.enums;

/**
 * State of the status.
 */
public enum GitStatusState
{
    /**
     * Status state not set. Default state.
     */
    NOTSET,
    /**
     * Status pending.
     */
    PENDING,
    /**
     * Status succeeded.
     */
    SUCCEEDED,
    /**
     * Status failed.
     */
    FAILED,
    /**
     * Status with an error.
     */
    ERROR,
    /**
     * Status is not applicable to the target object.
     */
    NOTAPPLICABLE
}
