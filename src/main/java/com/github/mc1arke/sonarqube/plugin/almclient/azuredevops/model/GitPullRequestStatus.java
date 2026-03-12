package com.github.mc1arke.sonarqube.plugin.almclient.azuredevops.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.mc1arke.sonarqube.plugin.almclient.azuredevops.model.enums.GitStatusState;

public class GitPullRequestStatus {

    private final GitStatusState state;
    private final String description;
    private final GitStatusContext context;
    private final String targetUrl;
    private final int iterationId;

    @JsonCreator
    public GitPullRequestStatus(
            @JsonProperty("state") GitStatusState state,
            @JsonProperty("description") String description,
            @JsonProperty("context") GitStatusContext context,
            @JsonProperty("targetUrl") String targetUrl,
            @JsonProperty("iterationId") int iterationId) {
        this.state = state;
        this.description = description;
        this.context = context;
        this.targetUrl = targetUrl;
        this.iterationId = iterationId;
    }

    /**
     * State of the status.
     */
    public GitStatusState getState() {
        return this.state;
    }

    /**
     * Description of the status
     */
    public String getDescription() {
        return this.description;
    }

    /**
     * Status context that uniquely identifies the status.
     */
    public GitStatusContext getContext() {
        return this.context;
    }

    /**
     * TargetUrl of the status
     */
    public String getTargetUrl() {
        return this.targetUrl;
    }

    /**
     * ID of the iteration to associate status with. Minimum value is 1.
     */
    public int getIterationId() {
        return this.iterationId;
    }
}
