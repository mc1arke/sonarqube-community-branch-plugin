/*
 * Copyright (C) 2020-2022 Michael Clarke
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

import org.sonar.api.ce.posttask.Analysis;
import org.sonar.api.ce.posttask.Branch;
import org.sonar.api.ce.posttask.PostProjectAnalysisTask;
import org.sonar.api.ce.posttask.QualityGate;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.alm.setting.ALM;
import org.sonar.db.alm.setting.AlmSettingDto;
import org.sonar.db.alm.setting.ProjectAlmSettingDto;
import org.sonar.db.component.BranchDao;
import org.sonar.db.component.BranchDto;
import org.sonar.db.protobuf.DbProjectBranches;

import java.util.List;
import java.util.Optional;

public class PullRequestPostAnalysisTask implements PostProjectAnalysisTask {

    private static final Logger LOGGER = Loggers.get(PullRequestPostAnalysisTask.class);

    private final List<PullRequestBuildStatusDecorator> pullRequestDecorators;
    private final PostAnalysisIssueVisitor postAnalysisIssueVisitor;
    private final DbClient dbClient;

    public PullRequestPostAnalysisTask(List<PullRequestBuildStatusDecorator> pullRequestDecorators,
                                       PostAnalysisIssueVisitor postAnalysisIssueVisitor, DbClient dbClient) {
        super();
        this.pullRequestDecorators = pullRequestDecorators;
        this.postAnalysisIssueVisitor = postAnalysisIssueVisitor;
        this.dbClient = dbClient;
    }

    @Override
    public String getDescription() {
        return "Pull Request Decoration";
    }

    @Override
    public void finished(Context context) {
        ProjectAnalysis projectAnalysis = context.getProjectAnalysis();
        LOGGER.debug("Found " + pullRequestDecorators.size() + " pull request decorators");
        Optional<Branch> optionalPullRequest =
                projectAnalysis.getBranch().filter(branch -> Branch.Type.PULL_REQUEST == branch.getType());
        if (optionalPullRequest.isEmpty()) {
            LOGGER.trace("Current analysis is not for a Pull Request. Task being skipped");
            return;
        }

        Optional<String> optionalPullRequestId = optionalPullRequest.get().getName();
        if (optionalPullRequestId.isEmpty()) {
            LOGGER.warn("No pull request ID has been submitted with the Pull Request. Analysis will be skipped");
            return;
        }

        ProjectAlmSettingDto projectAlmSettingDto;
        Optional<AlmSettingDto> optionalAlmSettingDto;
        try (DbSession dbSession = dbClient.openSession(false)) {

            Optional<ProjectAlmSettingDto> optionalProjectAlmSettingDto =
                    dbClient.projectAlmSettingDao().selectByProject(dbSession, projectAnalysis.getProject().getUuid());

            if (optionalProjectAlmSettingDto.isEmpty()) {
                LOGGER.debug("No ALM has been set on the current project");
                return;
            }

            projectAlmSettingDto = optionalProjectAlmSettingDto.get();
            String almSettingUuid = projectAlmSettingDto.getAlmSettingUuid();
            optionalAlmSettingDto = dbClient.almSettingDao().selectByUuid(dbSession, almSettingUuid);

        }

        if (optionalAlmSettingDto.isEmpty()) {
            LOGGER.warn("The ALM configured for this project could not be found");
            return;
        }

        AlmSettingDto almSettingDto = optionalAlmSettingDto.get();
        Optional<PullRequestBuildStatusDecorator> optionalPullRequestDecorator =
                findCurrentPullRequestStatusDecorator(almSettingDto, pullRequestDecorators);

        if (optionalPullRequestDecorator.isEmpty()) {
            LOGGER.info("No decorator found for this Pull Request");
            return;
        }

        Optional<Analysis> optionalAnalysis = projectAnalysis.getAnalysis();
        if (optionalAnalysis.isEmpty()) {
            LOGGER.warn(
                    "No analysis results were created for this project analysis. This is likely to be due to an earlier failure");
            return;
        }

        Analysis analysis = optionalAnalysis.get();

        Optional<String> revision = analysis.getRevision();
        if (revision.isEmpty()) {
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
                new AnalysisDetails(optionalPullRequestId.get(), commitId,
                                    postAnalysisIssueVisitor.getIssues(), qualityGate, projectAnalysis);

        PullRequestBuildStatusDecorator pullRequestDecorator = optionalPullRequestDecorator.get();
        LOGGER.info("Using pull request decorator " + pullRequestDecorator.getClass().getName());
        DecorationResult decorationResult = pullRequestDecorator.decorateQualityGateStatus(analysisDetails, almSettingDto, projectAlmSettingDto);

        decorationResult.getPullRequestUrl().ifPresent(pullRequestUrl -> persistPullRequestUrl(pullRequestUrl, projectAnalysis, optionalPullRequestId.get()));
    }


    private static Optional<PullRequestBuildStatusDecorator> findCurrentPullRequestStatusDecorator(
            AlmSettingDto almSetting, List<PullRequestBuildStatusDecorator> pullRequestDecorators) {
        ALM alm = almSetting.getAlm();

        for (PullRequestBuildStatusDecorator pullRequestDecorator : pullRequestDecorators) {
            if (pullRequestDecorator.alm().contains(alm)) {
                return Optional.of(pullRequestDecorator);
            }
        }

        LOGGER.warn("No decorator could be found matching " + alm);
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
