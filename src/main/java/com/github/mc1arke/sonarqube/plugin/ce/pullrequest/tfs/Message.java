package com.github.mc1arke.sonarqube.plugin.ce.pullrequest.tfs;

import java.util.List;

public class Message {
    public List<Issue> issues;
    public String projectName;
    public String repositoryId;
    public int pullRequestId;
}
