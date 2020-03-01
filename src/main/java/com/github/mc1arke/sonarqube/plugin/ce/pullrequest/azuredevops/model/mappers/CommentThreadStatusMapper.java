package com.github.mc1arke.sonarqube.plugin.ce.pullrequest.azuredevops.model.mappers;

import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.azuredevops.model.enums.CommentThreadStatus;

import static org.sonar.api.issue.Issue.STATUS_OPEN;
import static org.sonar.api.issue.Issue.STATUS_CLOSED;


public final class CommentThreadStatusMapper {

    private CommentThreadStatusMapper() {
        super();
    }

    public static CommentThreadStatus toCommentThreadStatus(String issueStatus) {
        switch (issueStatus) {
            case STATUS_OPEN:
                return CommentThreadStatus.active;
            default:
                return CommentThreadStatus.fixed;
        }
    }
    public static String toIssueStatus(CommentThreadStatus commentThreadStatus) {
        switch (commentThreadStatus) {
            case active:
                return STATUS_OPEN;
            default:
                return STATUS_CLOSED;
        }
    }
}
