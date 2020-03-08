package com.github.mc1arke.sonarqube.plugin.ce.pullrequest.azuredevops.model.mappers;

import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.azuredevops.model.enums.CommentThreadStatus;


public class CommentThreadStatusMapper {

    private CommentThreadStatusMapper() {
        super();
    }

    public static CommentThreadStatus toCommentThreadStatus(String issueStatus) {
        switch (issueStatus) {
            case "Open":
                return CommentThreadStatus.Active;
            default:
                return CommentThreadStatus.Fixed;
        }
    }
}
