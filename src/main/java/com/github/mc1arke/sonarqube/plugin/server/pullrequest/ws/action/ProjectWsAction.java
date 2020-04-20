package com.github.mc1arke.sonarqube.plugin.server.pullrequest.ws.action;

import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.project.ProjectDto;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.user.UserSession;

import java.util.Optional;

import static org.sonar.api.web.UserRole.ADMIN;

public abstract class ProjectWsAction extends AlmSettingsWsAction {

    private static final String PROJECT_PARAMETER = "project";

    private final String actionName;
    private final DbClient dbClient;
    private final ComponentFinder componentFinder;
    private final UserSession userSession;
    private final boolean projectParameterRequired;

    protected ProjectWsAction(String actionName, DbClient dbClient, ComponentFinder componentFinder, UserSession userSession, boolean projectParameterRequired) {
        super(dbClient);
        this.actionName = actionName;
        this.dbClient = dbClient;
        this.componentFinder = componentFinder;
        this.userSession = userSession;
        this.projectParameterRequired = projectParameterRequired;
    }

    @Override
    public void define(WebService.NewController context) {
        WebService.NewAction action = context.createAction(actionName).setHandler(this);
        action.createParam(PROJECT_PARAMETER).setRequired(projectParameterRequired);

        configureAction(action);
    }

    protected abstract void configureAction(WebService.NewAction action);


    @Override
    public void handle(Request request, Response response) {
        Optional<String> projectKey = Optional.ofNullable(request.param(PROJECT_PARAMETER));

        try (DbSession dbSession = dbClient.openSession(false)) {
            ProjectDto project;
            if (projectKey.isPresent()) {
                project = componentFinder.getProjectByKey(dbSession, projectKey.get());
                userSession.checkProjectPermission(ADMIN, project);
            } else {
                if (projectParameterRequired) {
                    throw new IllegalArgumentException("The 'project' parameter is missing");
                } else {
                    project = null;
                }
            }
            handleProjectRequest(project, request, response, dbSession);
        }
    }

    protected abstract void handleProjectRequest(ProjectDto project, Request request, Response response, DbSession dbSession);
}
