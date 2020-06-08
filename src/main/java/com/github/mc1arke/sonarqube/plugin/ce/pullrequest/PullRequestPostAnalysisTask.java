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

import com.github.mc1arke.sonarqube.plugin.CommunityBranchPlugin;
import org.sonar.api.ce.posttask.Analysis;
import org.sonar.api.ce.posttask.Branch;
import org.sonar.api.ce.posttask.PostProjectAnalysisTask;
import org.sonar.api.ce.posttask.QualityGate;
import org.sonar.api.config.Configuration;
import org.sonar.api.platform.Server;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.ce.task.projectanalysis.component.ConfigurationRepository;
import org.sonar.ce.task.projectanalysis.component.TreeRootHolder;
import org.sonar.ce.task.projectanalysis.measure.MeasureRepository;
import org.sonar.ce.task.projectanalysis.metric.MetricRepository;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.BranchDao;
import org.sonar.db.component.BranchDto;
import org.sonar.db.protobuf.DbProjectBranches;

import java.util.List;
import java.util.Optional;

public class PullRequestPostAnalysisTask implements PostProjectAnalysisTask,
                                                    PostProjectAnalysisTaskCompatibility.PostProjectAnalysisTaskCompatibilityMajor8.PostProjectAnalysisTaskCompatibilityMinor0 {

    private static final Logger LOGGER = Loggers.get(PullRequestPostAnalysisTask.class);

    private final List<PullRequestBuildStatusDecorator> pullRequestDecorators;
    private final Server server;
    private final ConfigurationRepository configurationRepository;
    private final PostAnalysisIssueVisitor postAnalysisIssueVisitor;
    private final MetricRepository metricRepository;
    private final MeasureRepository measureRepository;
    private final TreeRootHolder treeRootHolder;
    private final DbClient dbClient;

    public PullRequestPostAnalysisTask(Server server,
                                       ConfigurationRepository configurationRepository,
                                       List<PullRequestBuildStatusDecorator> pullRequestDecorators,
                                       PostAnalysisIssueVisitor postAnalysisIssueVisitor,
                                       MetricRepository metricRepository, MeasureRepository measureRepository,
                                       TreeRootHolder treeRootHolder, DbClient dbClient) {
        super();
        this.server = server;
        this.configurationRepository = configurationRepository;
        this.pullRequestDecorators = pullRequestDecorators;
        this.postAnalysisIssueVisitor = postAnalysisIssueVisitor;
        this.metricRepository = metricRepository;
        this.measureRepository = measureRepository;
        this.treeRootHolder = treeRootHolder;
        this.dbClient = dbClient;
    }

    @Override
    public String getDescription() {
        return "Pull Request Decoration";
    }

    @Deprecated
    @Override
    public void finished(PostProjectAnalysisTask.ProjectAnalysis projectAnalysis) {
        LOGGER.debug("found " + pullRequestDecorators.size() + " pull request decorators");
        Optional<Branch> optionalPullRequest =
                projectAnalysis.getBranch().filter(branch -> Branch.Type.PULL_REQUEST == branch.getType());
        if (!optionalPullRequest.isPresent()) {
            LOGGER.trace("Current analysis is not for a Pull Request. Task being skipped");
            return;
        }

        Optional<String> optionalBranchName = optionalPullRequest.get().getName();
        if (!optionalBranchName.isPresent()) {
            LOGGER.warn("No branch name has been submitted with the Pull Request. Analysis will be skipped");
            return;
        }

        Configuration configuration = configurationRepository.getConfiguration();
        UnifyConfiguration unifyConfiguration = new UnifyConfiguration(configuration, projectAnalysis.getScannerContext());

        Optional<PullRequestBuildStatusDecorator> optionalPullRequestDecorator =
                findCurrentPullRequestStatusDecorator(unifyConfiguration, pullRequestDecorators);

        if (!optionalPullRequestDecorator.isPresent()) {
            LOGGER.info("No decorator found for this Pull Request");
            return;
        }

        Optional<Analysis> optionalAnalysis = projectAnalysis.getAnalysis();
        if (!optionalAnalysis.isPresent()) {
            LOGGER.warn(
                    "No analysis results were created for this project analysis. This is likely to be due to an earlier failure");
            return;
        }

        Analysis analysis = optionalAnalysis.get();

        Optional<String> revision = analysis.getRevision();
        if (!revision.isPresent()) {
            LOGGER.warn("No commit details were submitted with this analysis. Check the project is committed to Git");
            return;
        }

        QualityGate qualityGate = projectAnalysis.getQualityGate();
        if (null == qualityGate) {
            LOGGER.warn("No quality gate was found on the analysis, so no results are available");
            return;
        }

        String commitId = revision.get();

        AnalysisDetails analysisDetails =
                new AnalysisDetails(new AnalysisDetails.BranchDetails(optionalBranchName.get(), commitId),
                                    postAnalysisIssueVisitor, qualityGate,
                                    new AnalysisDetails.MeasuresHolder(metricRepository, measureRepository,
                                                                       treeRootHolder), analysis,
                                    projectAnalysis.getProject(), configuration, server.getPublicRootUrl());

        PullRequestBuildStatusDecorator pullRequestDecorator = optionalPullRequestDecorator.get();
        LOGGER.info("using pull request decorator " + pullRequestDecorator.name());
        DecorationResult decorationResult = pullRequestDecorator.decorateQualityGateStatus(analysisDetails, unifyConfiguration);

        decorationResult.getPullRequestUrl().ifPresent(pullRequestUrl -> persistPullRequestUrl(pullRequestUrl, projectAnalysis, optionalBranchName.get()));
    }

    private static Optional<PullRequestBuildStatusDecorator> findCurrentPullRequestStatusDecorator(
            UnifyConfiguration unifyConfiguration, List<PullRequestBuildStatusDecorator> pullRequestDecorators) {

        Optional<String> optionalImplementationName = unifyConfiguration.getProperty(CommunityBranchPlugin.PULL_REQUEST_PROVIDER);

        if (!optionalImplementationName.isPresent()) {
            LOGGER.debug(CommunityBranchPlugin.PULL_REQUEST_PROVIDER + " property not set");
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

    private void persistPullRequestUrl(String pullRequestUrl, ProjectAnalysis projectAnalysis, String branchName) {
        try (DbSession dbSession = dbClient.openSession(false)) {
            BranchDao branchDao = dbClient.branchDao();
            Optional<BranchDto> optionalBranchDto = branchDao
                    .selectByPullRequestKey(dbSession, projectAnalysis.getProject().getUuid(), branchName);
            if (optionalBranchDto.isPresent()) {
                BranchDto branchDto = optionalBranchDto.get();
                DbProjectBranches.PullRequestData.Builder pullRequestDataBuilder = DbProjectBranches.PullRequestData.newBuilder(branchDto.getPullRequestData());
                pullRequestDataBuilder.setUrl(pullRequestUrl);
                branchDto.setPullRequestData(pullRequestDataBuilder.build());
                branchDao.upsert(dbSession, branchDto);
                dbSession.commit();
            }
        }
    }
}
