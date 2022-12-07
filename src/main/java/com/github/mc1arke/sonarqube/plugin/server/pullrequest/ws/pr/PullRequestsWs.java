package com.github.mc1arke.sonarqube.plugin.server.pullrequest.ws.pr;

import org.sonar.api.server.ws.WebService;

import static java.util.Arrays.stream;
import static com.github.mc1arke.sonarqube.plugin.server.pullrequest.ws.pr.PullRequestsWsParameters.PARAM_PROJECT;
import static com.github.mc1arke.sonarqube.plugin.server.pullrequest.ws.pr.PullRequestsWsParameters.PARAM_PULL_REQUEST;
import static org.sonar.server.ws.KeyExamples.KEY_PROJECT_EXAMPLE_001;

public class PullRequestsWs implements WebService {
    private final PullRequestWsAction[] actions;

    public PullRequestsWs(PullRequestWsAction... actions) {
        this.actions = actions;
    }

    @Override
    public void define(Context context) {
        NewController controller = context.createController("api/project_pull_requests")
                .setSince("7.1")
                .setDescription("Manage pull request");
        stream(actions).forEach(action -> action.define(controller));
        controller.done();
    }

    static void addProjectParam(NewAction action) {
        action
                .createParam(PARAM_PROJECT)
                .setDescription("Project key")
                .setExampleValue(KEY_PROJECT_EXAMPLE_001)
                .setRequired(true);
    }

    static void addPullRequestParam(NewAction action) {
        action
                .createParam(PARAM_PULL_REQUEST)
                .setDescription("Pull request id")
                .setExampleValue("1543")
                .setRequired(true);
    }

}
