/*
 * Copyright (C) 2020 Michael Clarke
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

import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.commentfilter.IssueFilterRunner;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.commentfilter.SeverityExclusionFilter;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.commentfilter.TypeExclusionFilter;
import org.checkerframework.checker.nullness.Opt;
import org.sonar.api.ce.posttask.*;
import org.sonar.api.config.Configuration;
import org.sonar.api.platform.Server;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.ce.task.projectanalysis.component.TreeRootHolder;
import org.sonar.ce.task.projectanalysis.measure.MeasureRepository;
import org.sonar.ce.task.projectanalysis.metric.MetricRepository;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.alm.setting.ALM;
import org.sonar.db.alm.setting.AlmSettingDto;
import org.sonar.db.alm.setting.ProjectAlmSettingDto;
import org.sonar.db.component.BranchDao;
import org.sonar.db.component.BranchDto;
import org.sonar.db.property.PropertyDto;
import org.sonar.db.protobuf.DbProjectBranches;
import org.sonar.server.setting.ws.Setting;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class PullRequestPostAnalysisTask implements PostProjectAnalysisTask {

    private static final Logger LOGGER = Loggers.get(PullRequestPostAnalysisTask.class);
    public static final String PULLREQUEST_FILTER_SEVERITY_EXCLUSION = "sonar.pullrequest.comment.filter.severity.exclusions";
    public static final String PULLREQUEST_FILTER_TYPE_EXCLUSION = "sonar.pullrequest.comment.filter.type.exclusions";
    public static final String PULLREQUEST_FILTER_MAXAMOUNT = "sonar.pullrequest.comment.filter.maxamount";

    private final List<PullRequestBuildStatusDecorator> pullRequestDecorators;
    private final Server server;
    private final PostAnalysisIssueVisitor postAnalysisIssueVisitor;
    private final MetricRepository metricRepository;
    private final MeasureRepository measureRepository;
    private final TreeRootHolder treeRootHolder;
    private final Configuration configuration;
    private final DbClient dbClient;

    public PullRequestPostAnalysisTask(Server server,
                                       List<PullRequestBuildStatusDecorator> pullRequestDecorators,
                                       PostAnalysisIssueVisitor postAnalysisIssueVisitor,
                                       MetricRepository metricRepository, MeasureRepository measureRepository,
                                       TreeRootHolder treeRootHolder, Configuration configuration, DbClient dbClient) {
        super();
        this.server = server;
        this.pullRequestDecorators = pullRequestDecorators;
        this.postAnalysisIssueVisitor = postAnalysisIssueVisitor;
        this.metricRepository = metricRepository;
        this.measureRepository = measureRepository;
        this.treeRootHolder = treeRootHolder;
        this.configuration = configuration;
        this.dbClient = dbClient;
    }

    @Override
    public String getDescription() {
        return "Pull Request Decoration";
    }

    @Override
    public void finished(Context context) {
        ProjectAnalysis projectAnalysis = context.getProjectAnalysis();

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

        ProjectAlmSettingDto projectAlmSettingDto;
        Optional<AlmSettingDto> optionalAlmSettingDto;
        List<PropertyDto> projectProperties;
        try (DbSession dbSession = dbClient.openSession(false)) {

            Optional<ProjectAlmSettingDto> optionalProjectAlmSettingDto =
                    dbClient.projectAlmSettingDao().selectByProject(dbSession, projectAnalysis.getProject().getUuid());

            projectProperties = dbClient.propertiesDao().selectProjectProperties(dbSession, projectAnalysis.getProject().getKey());

            if (!optionalProjectAlmSettingDto.isPresent()) {
                LOGGER.debug("No ALM has been set on the current project");
                return;
            }

            projectAlmSettingDto = optionalProjectAlmSettingDto.get();
            String almSettingUuid = projectAlmSettingDto.getAlmSettingUuid();
            optionalAlmSettingDto = dbClient.almSettingDao().selectByUuid(dbSession, almSettingUuid);

        }

        if (!optionalAlmSettingDto.isPresent()) {
            LOGGER.warn("The ALM configured for this project could not be found");
            return;
        }

        AlmSettingDto almSettingDto = optionalAlmSettingDto.get();
        Optional<PullRequestBuildStatusDecorator> optionalPullRequestDecorator =
                findCurrentPullRequestStatusDecorator(almSettingDto, pullRequestDecorators);

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
                        projectAnalysis.getProject(), configuration, server.getPublicRootUrl(),
                        projectAnalysis.getScannerContext());

        PullRequestBuildStatusDecorator pullRequestDecorator = optionalPullRequestDecorator.get();
        LOGGER.info("using pull request decorator " + pullRequestDecorator.alm().getId());

        Optional<IssueFilterRunner> optionalIssueFilterRunner = getIssueFilterList(projectProperties);

        DecorationResult decorationResult;
        if (optionalIssueFilterRunner.isPresent())
            decorationResult = pullRequestDecorator.decorateQualityGateStatus(analysisDetails, almSettingDto, projectAlmSettingDto, optionalIssueFilterRunner.get());
        else
            decorationResult = pullRequestDecorator.decorateQualityGateStatus(analysisDetails, almSettingDto, projectAlmSettingDto);

        decorationResult.getPullRequestUrl().ifPresent(pullRequestUrl -> persistPullRequestUrl(pullRequestUrl, projectAnalysis, optionalBranchName.get()));
    }

    private Optional<IssueFilterRunner> getIssueFilterList(List<PropertyDto> projectProperties) {
        List<Predicate<PostAnalysisIssueVisitor.ComponentIssue>> filterList = new ArrayList<>();


        Optional<String> optionalSeverityExclusion = projectProperties.stream().filter(propertyDto -> propertyDto.getKey().equals(PULLREQUEST_FILTER_SEVERITY_EXCLUSION))
                .map(PropertyDto::getValue)
                .filter(Objects::nonNull)
                .findAny();
        Optional<String> optionalTypeExclusion = projectProperties.stream().filter(propertyDto -> propertyDto.getKey().equals(PULLREQUEST_FILTER_TYPE_EXCLUSION))
                .map(PropertyDto::getValue)
                .filter(Objects::nonNull)
                .findAny();
        Optional<Integer> optionalMaxAmount = projectProperties.stream().filter(propertyDto -> propertyDto.getKey().equals(PULLREQUEST_FILTER_MAXAMOUNT))
                .map(PropertyDto::getValue)
                .filter(Objects::nonNull)
                .map(Integer::parseInt)
                .findAny();

        optionalSeverityExclusion = Optional.ofNullable(optionalSeverityExclusion.orElseGet(() -> configuration.get(PULLREQUEST_FILTER_SEVERITY_EXCLUSION).orElse(null)));
        optionalTypeExclusion = Optional.ofNullable(optionalTypeExclusion.orElseGet(() -> configuration.get(PULLREQUEST_FILTER_TYPE_EXCLUSION).orElse(null)));
        optionalMaxAmount = Optional.ofNullable(optionalMaxAmount.orElseGet(() -> configuration.getInt(PULLREQUEST_FILTER_MAXAMOUNT).orElse(null)));

        optionalSeverityExclusion.ifPresent(severityString -> filterList.add(new SeverityExclusionFilter(severityString)));
        optionalTypeExclusion.ifPresent(typeString -> filterList.add(new TypeExclusionFilter(typeString)));

        if (filterList.isEmpty() && !optionalMaxAmount.isPresent()) {
            return Optional.empty();
        } else {
            return Optional.of(new IssueFilterRunner(filterList, optionalMaxAmount.orElse(null)));
        }
    }


    private static Optional<PullRequestBuildStatusDecorator> findCurrentPullRequestStatusDecorator(
            AlmSettingDto almSetting, List<PullRequestBuildStatusDecorator> pullRequestDecorators) {
        ALM alm = almSetting.getAlm();

        for (PullRequestBuildStatusDecorator pullRequestDecorator : pullRequestDecorators) {
            if (alm == pullRequestDecorator.alm()) {
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
