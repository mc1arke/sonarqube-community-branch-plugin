package com.github.mc1arke.sonarqube.plugin.ce.pullrequest.azuredevops.model;

import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.azuredevops.model.enums.GitStatusState;

public class GitPullRequestStatus {
    /**
     * ID of the iteration to associate status with. Minimum value is 1.
     */
    public Integer iterationId = null;
    /**
     * State of the status.
     */
    public GitStatusState state;
    /**
     * Description of the status
     */
    public String description;
    /**
     * Status context that uniquely identifies the status.
     */
    public GitStatusContext context;
    /**
     * TargetUrl of the status
     */
    public String targetUrl;
}