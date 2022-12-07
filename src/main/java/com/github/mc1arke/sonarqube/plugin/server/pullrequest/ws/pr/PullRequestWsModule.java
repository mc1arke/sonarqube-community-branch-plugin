package com.github.mc1arke.sonarqube.plugin.server.pullrequest.ws.pr;

import org.sonar.core.platform.Module;

public class PullRequestWsModule extends Module {
    @Override
    protected void configureModule() {
        add(ListAction.class, DeleteAction.class, PullRequestsWs.class);
    }
}

