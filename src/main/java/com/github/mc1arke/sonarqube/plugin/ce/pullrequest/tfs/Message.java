package com.github.mc1arke.sonarqube.plugin.ce.pullrequest.tfs;

import java.util.List;

public class Message {

    private List<Issue> issues;

    private String projectName;

    private String repositoryId;

    private int pullRequestId;

    private String buildId;

    private Message() {
        // Nothing
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public List<Issue> getIssues() {
        return issues;
    }

    public String getProjectName() {
        return projectName;
    }

    public String getRepositoryId() {
        return repositoryId;
    }

    public int getPullRequestId() {
        return pullRequestId;
    }

    public String getBuildId() {
        return buildId;
    }

    public static class Builder {

        private Message message;

        private Builder() {
            this.message = new Message();
        }

        public Builder issues(List<Issue> issues) {
            message.issues = issues;
            return this;
        }

        public Builder projectName(String projectName) {
            message.projectName = projectName;
            return this;
        }

        public Builder repositoryId(String repositoryId) {
            message.repositoryId = repositoryId;
            return this;
        }

        public Builder pullRequestId(int pullRequestId) {
            message.pullRequestId = pullRequestId;
            return this;
        }

        public Builder buildId(String buildId) {
            message.buildId = buildId;
            return this;
        }

        public Message build() {
            return message;
        }
    }
}
