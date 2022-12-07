package com.github.mc1arke.sonarqube.plugin.server.pullrequest.ws.pr;

import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.server.ws.WebService.NewController;
import org.sonar.api.web.UserRole;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.BranchDto;
import org.sonar.db.project.ProjectDto;
import org.sonar.server.component.ComponentCleanerService;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.user.UserSession;

import static org.sonar.db.component.BranchType.PULL_REQUEST;
import static com.github.mc1arke.sonarqube.plugin.server.pullrequest.ws.pr.PullRequestsWs.addProjectParam;
import static com.github.mc1arke.sonarqube.plugin.server.pullrequest.ws.pr.PullRequestsWs.addPullRequestParam;
import static com.github.mc1arke.sonarqube.plugin.server.pullrequest.ws.pr.PullRequestsWsParameters.PARAM_PROJECT;
import static com.github.mc1arke.sonarqube.plugin.server.pullrequest.ws.pr.PullRequestsWsParameters.PARAM_PULL_REQUEST;
import static org.sonar.server.branch.ws.ProjectBranchesParameters.ACTION_DELETE;

public class DeleteAction implements PullRequestWsAction {
    private final DbClient dbClient;
    private final UserSession userSession;
    private final ComponentCleanerService componentCleanerService;
    private final ComponentFinder componentFinder;

    public DeleteAction(DbClient dbClient, ComponentFinder componentFinder, UserSession userSession, ComponentCleanerService componentCleanerService) {
        this.dbClient = dbClient;
        this.componentFinder = componentFinder;
        this.userSession = userSession;
        this.componentCleanerService = componentCleanerService;
    }

    @Override
    public void define(NewController context) {
        WebService.NewAction action = context.createAction(ACTION_DELETE)
                .setSince("7.1")
                .setDescription("Delete a pull request.<br/>" +
                        "Requires 'Administer' rights on the specified project.")
                .setPost(true)
                .setHandler(this);

        addProjectParam(action);
        addPullRequestParam(action);
    }

    @Override
    public void handle(Request request, Response response) throws Exception {
        userSession.checkLoggedIn();
        String projectKey = request.mandatoryParam(PARAM_PROJECT);
        String pullRequestId = request.mandatoryParam(PARAM_PULL_REQUEST);

        try (DbSession dbSession = dbClient.openSession(false)) {
            ProjectDto project = componentFinder.getProjectOrApplicationByKey(dbSession, projectKey);
            checkPermission(project);

            BranchDto pullRequest = dbClient.branchDao().selectByPullRequestKey(dbSession, project.getUuid(), pullRequestId)
                    .filter(branch -> branch.getBranchType() == PULL_REQUEST)
                    .orElseThrow(() -> new NotFoundException(String.format("Pull request '%s' is not found for project '%s'", pullRequestId, projectKey)));

            componentCleanerService.deleteBranch(dbSession, pullRequest);
            response.noContent();
        }
    }

    private void checkPermission(ProjectDto project) {
        userSession.checkProjectPermission(UserRole.ADMIN, project);
    }

}

