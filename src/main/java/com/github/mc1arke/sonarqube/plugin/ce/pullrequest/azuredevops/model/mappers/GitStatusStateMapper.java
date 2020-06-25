package com.github.mc1arke.sonarqube.plugin.ce.pullrequest.azuredevops.model.mappers;

import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.azuredevops.model.enums.GitStatusState;
import org.sonar.api.ce.posttask.QualityGate;

public final class GitStatusStateMapper {

    private GitStatusStateMapper() {
        super();
    }

    public static GitStatusState toGitStatusState(QualityGate.Status AnnalyzeStatus) {
        switch (AnnalyzeStatus) {
            case OK:
                return GitStatusState.SUCCEEDED;
            case ERROR:
                return GitStatusState.ERROR;
            default:
                return GitStatusState.NOT_SET;
        }
    }
}
