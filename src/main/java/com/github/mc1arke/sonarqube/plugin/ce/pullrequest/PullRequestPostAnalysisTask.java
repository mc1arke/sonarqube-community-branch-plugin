/*
 * Copyright (C) 2019 Michael Clarke
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 *
 */
package com.github.mc1arke.sonarqube.plugin.ce.pullrequest;

import org.sonar.api.ce.posttask.Branch;
import org.sonar.api.ce.posttask.PostProjectAnalysisTask;
import org.sonar.api.config.Configuration;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.ce.task.projectanalysis.component.ConfigurationRepository;

import java.util.List;
import java.util.Optional;

public class PullRequestPostAnalysisTask implements PostProjectAnalysisTask {

    private static final Logger LOGGER = Loggers.get(PullRequestPostAnalysisTask.class);

    private final List<PullRequestBuildStatusDecorator> pullRequestDecorators;
    private final ConfigurationRepository configurationRepository;

    public PullRequestPostAnalysisTask(ConfigurationRepository configurationRepository,
                                       List<PullRequestBuildStatusDecorator> pullRequestDecorators) {
        super();
        this.configurationRepository = configurationRepository;
        this.pullRequestDecorators = pullRequestDecorators;
    }

    @Override
    public void finished(PostProjectAnalysisTask.ProjectAnalysis projectAnalysis) {
        LOGGER.info("found " + pullRequestDecorators.size() + " pull request decorators");
        if (!projectAnalysis.getBranch().filter(branch -> Branch.Type.PULL_REQUEST == branch.getType()).isPresent()) {
            LOGGER.info("Current analysis is not for a Pull Request. Task being skipped");
            return;
        }

        Optional<PullRequestBuildStatusDecorator> optionalPullRequestDecorator =
                findCurrentPullRequestStatusDecorator(configurationRepository.getConfiguration(),
                                                      pullRequestDecorators);

        if (!optionalPullRequestDecorator.isPresent()) {
            LOGGER.info("No decorator found for this Pull Request");
            return;
        }

        PullRequestBuildStatusDecorator pullRequestDecorator = optionalPullRequestDecorator.get();
        LOGGER.info("using pull request decorator" + pullRequestDecorator.name());
        pullRequestDecorator.decorateQualityGateStatus(projectAnalysis);

    }

    private static Optional<PullRequestBuildStatusDecorator> findCurrentPullRequestStatusDecorator(
            Configuration configuration, List<PullRequestBuildStatusDecorator> pullRequestDecorators) {

        Optional<String> optionalImplementationName = configuration.get("sonar.pullrequest.provider");

        if (!optionalImplementationName.isPresent()) {
            LOGGER.error("'sonar.pullrequest.provider' property not set");
            return Optional.empty();
        }

        String implementationName = optionalImplementationName.get();

        for (PullRequestBuildStatusDecorator pullRequestDecorator : pullRequestDecorators) {
            if (pullRequestDecorator.name().equals(implementationName)) {
                return Optional.of(pullRequestDecorator);
            }
        }

        LOGGER.warn("No decorator could be found matching " + implementationName);
        return Optional.empty();
    }
}
