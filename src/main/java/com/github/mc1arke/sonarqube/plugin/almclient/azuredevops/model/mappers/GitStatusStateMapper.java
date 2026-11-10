package com.github.mc1arke.sonarqube.plugin.almclient.azuredevops.model.mappers;

import com.github.mc1arke.sonarqube.plugin.almclient.azuredevops.model.enums.GitStatusState;
import org.sonar.api.ce.posttask.QualityGate;

public final class GitStatusStateMapper {

    private GitStatusStateMapper() {
        super();
    }

    public static GitStatusState toGitStatusState(QualityGate.Status analysisStatus) {
        return switch (analysisStatus) {
            case OK -> GitStatusState.SUCCEEDED;
            case ERROR -> GitStatusState.ERROR;
        };
    }
}
