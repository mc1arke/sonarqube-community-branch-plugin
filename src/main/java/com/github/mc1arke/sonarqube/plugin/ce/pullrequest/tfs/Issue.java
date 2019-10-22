package com.github.mc1arke.sonarqube.plugin.ce.pullrequest.tfs;

import org.sonar.api.batch.rule.Severity;

public class Issue {

    private String key;

    private String ruleKey;

    private String componentKey;

    private String file;

    private Integer line;

    private String message;

    private Severity severity;

    private boolean newIssue;

    private Issue() {
        // Nothing
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public String getKey() {
        return key;
    }

    public String getRuleKey() {
        return ruleKey;
    }

    public String getComponentKey() {
        return componentKey;
    }

    public String getFile() {
        return file;
    }

    public Integer getLine() {
        return line;
    }

    public String getMessage() {
        return message;
    }

    public Severity getSeverity() {
        return severity;
    }

    public boolean isNewIssue() {
        return newIssue;
    }

    public static class Builder {

        private Issue issue;

        private Builder() {
            this.issue = new Issue();
        }

        public Builder key(String key) {
            issue.key = key;
            return this;
        }

        public Builder ruleKey(String ruleKey) {
            issue.ruleKey = ruleKey;
            return this;
        }

        public Builder componentKey(String componentKey) {
            issue.componentKey = componentKey;
            return this;
        }

        public Builder file(String file) {
            issue.file = file;

            return this;
        }

        public Builder line(Integer line) {
            issue.line = line;
            return this;
        }

        public Builder message(String message) {
            issue.message = message;
            return this;
        }

        public Builder severity(String severity) {
            issue.severity = Severity.valueOf(severity);
            return this;
        }

        public Builder newIssue(boolean newIssue) {
            issue.newIssue = newIssue;
            return this;
        }

        public Issue build() {
            return issue;
        }
    }
}
