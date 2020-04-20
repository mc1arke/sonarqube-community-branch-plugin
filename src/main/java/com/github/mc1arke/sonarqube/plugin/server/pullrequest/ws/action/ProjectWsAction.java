package com.github.mc1arke.sonarqube.plugin.server.pullrequest.ws.action;

import static org.sonar.api.web.UserRole.ADMIN;

import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.project.ProjectDto;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.user.UserSession;

public abstract class ProjectWsAction extends AlmSettingsWsAction {

    private static final String PROJECT_PARAMETER = "project";

    private final String actionName;
    private final DbClient dbClient;
    private final ComponentFinder componentFinder;
    private final UserSession userSession;

    protected ProjectWsAction(String actionName, DbClient dbClient, ComponentFinder componentFinder, UserSession userSession) {
        super(dbClient);
        this.actionName = actionName;
        this.dbClient = dbClient;
        this.componentFinder = componentFinder;
        this.userSession = userSession;
    }

    @Override
    public void define(WebService.NewController context) {
        WebService.NewAction action = context.createAction(actionName).setHandler(this);
        action.createParam(PROJECT_PARAMETER).setRequired(true);

        configureAction(action);
    }

    protected abstract void configureAction(WebService.NewAction action);


    @Override
    public void handle(Request request, Response response) {
        String projectKey = request.mandatoryParam(PROJECT_PARAMETER);
        try (DbSession dbSession = dbClient.openSession(false)) {
            ProjectDto project = componentFinder.getProjectByKey(dbSession, projectKey);
            userSession.checkProjectPermission(ADMIN, project);

            handleProjectRequest(project, request, response, dbSession);
        }
    }

    protected abstract void handleProjectRequest(ProjectDto project, Request request, Response response, DbSession dbSession);
}
