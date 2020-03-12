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

import org.sonar.api.ce.posttask.ScannerContext;
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
import org.sonar.db.alm.setting.ALM;
import org.sonar.db.alm.setting.AlmSettingDto;
import org.sonar.db.alm.setting.ProjectAlmSettingDto;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PullRequestPostAnalysisTask implements PostProjectAnalysisTask {

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

        Configuration configuration = configurationRepository.getConfiguration();

        ProjectAlmSettingDto projectAlmSettingDto;
        Optional<AlmSettingDto> optionalAlmSettingDto;
        try (DbSession dbSession = dbClient.openSession(false)) {

            Optional<ProjectAlmSettingDto> optionalProjectAlmSettingDto =
                    dbClient.projectAlmSettingDao().selectByProject(dbSession, projectAnalysis.getProject().getUuid());

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

        ScannerContext scannerContext = projectAnalysis.getScannerContext().getProperties().size() > 0
                ? projectAnalysis.getScannerContext()
                : GetScannerContext(projectAnalysis.getCeTask().getId());

        AnalysisDetails analysisDetails =
                new AnalysisDetails(new AnalysisDetails.BranchDetails(optionalBranchName.get(), commitId),
                                    postAnalysisIssueVisitor, qualityGate,
                                    new AnalysisDetails.MeasuresHolder(metricRepository, measureRepository,
                                                                       treeRootHolder), analysis,
                                    projectAnalysis.getProject(), configuration, server.getPublicRootUrl(),
                                    scannerContext);

        PullRequestBuildStatusDecorator pullRequestDecorator = optionalPullRequestDecorator.get();
        LOGGER.info("using pull request decorator" + pullRequestDecorator.name());
        pullRequestDecorator.decorateQualityGateStatus(analysisDetails, almSettingDto, projectAlmSettingDto);
    }

    public ScannerContext GetScannerContext(String ceTaskId)
    {
        ScannerContext scannerContext =  new ScannerContext() {
            private final Map<String, String> props = new HashMap<String, String>();
            @Override
            public Map<String, String> getProperties() { return this.props; }
        };

        try (DbSession dbSession = dbClient.openSession(false)) {

            try {
                String scannerContextStr = dbClient.ceScannerContextDao().selectScannerContext(dbSession, ceTaskId).orElse("");

                LOGGER.trace("GetScannerContext: ScannerContextStr =" + scannerContextStr);

                Pattern p = Pattern.compile("(\\w.+)=(.+)");
                Matcher m = p.matcher(scannerContextStr);
                HashMap<String, String> map = new HashMap<String, String>();

                while (m.find()) {
                    map.put(m.group(1), m.group(2));
                }
                LOGGER.trace("GetScannerContext: map =" + map);

                scannerContext.getProperties().putAll(map);

            } catch (Exception ex)
            {
                LOGGER.error("GetScannerContext: " + ex);
            }
            return scannerContext;
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
}
