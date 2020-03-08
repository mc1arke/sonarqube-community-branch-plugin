package com.github.mc1arke.sonarqube.plugin.ce.pullrequest.azuredevops.model.mappers;

import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.azuredevops.model.enums.GitStatusState;
import org.sonar.api.ce.posttask.QualityGate;

public class GitStatusStateMapper {

    private GitStatusStateMapper() {
        super();
    }

    public static GitStatusState toGitStatusState(QualityGate.Status AnnalyzeStatus) {
        switch (AnnalyzeStatus) {
            case OK:
                return GitStatusState.Succeeded;
            case ERROR:
                return GitStatusState.Error;
            default:
                return GitStatusState.NotSet;
        }
    }
}
