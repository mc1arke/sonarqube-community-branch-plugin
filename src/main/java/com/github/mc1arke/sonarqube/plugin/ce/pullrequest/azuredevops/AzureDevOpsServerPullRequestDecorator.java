package com.github.mc1arke.sonarqube.plugin.ce.pullrequest.azuredevops;

import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.AnalysisDetails;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.PullRequestBuildStatusDecorator;
import org.sonar.api.issue.Issue;
import org.sonar.api.platform.Server;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.ce.task.projectanalysis.scm.ScmInfoRepository;
import org.sonar.db.alm.setting.ALM;
import org.sonar.db.alm.setting.AlmSettingDto;
import org.sonar.db.alm.setting.ProjectAlmSettingDto;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

public class AzureDevOpsServerPullRequestDecorator implements PullRequestBuildStatusDecorator {

    private static final Logger LOGGER = Loggers.get(AzureDevOpsServerPullRequestDecorator.class);
    private static final List<String> OPEN_ISSUE_STATUSES =
            Issue.STATUSES.stream().filter(s -> !Issue.STATUS_CLOSED.equals(s) && !Issue.STATUS_RESOLVED.equals(s))
                    .collect(Collectors.toList());

    public static final String PULLREQUEST_AZUREDEVOPS_INSTANCE_URL = "sonar.pullrequest.azuredevops.instanceUrl";
    public static final String PULLREQUEST_AZUREDEVOPS_PROJECT_ID = "sonar.pullrequest.azuredevops.projectId";
    public static final String PULLREQUEST_AZUREDEVOPS_PROJECT_URL = "sonar.pullrequest.azuredevops.projectUrl";
    public static final String PULLREQUEST_AZUREDEVOPS_BUILD_ID = "com.github.mc1arke.sonarqube.plugin.branch.pullrequest.azuredevops.buildId";

    private final Server server;
    private final ScmInfoRepository scmInfoRepository;

    public AzureDevOpsServerPullRequestDecorator(Server server, ScmInfoRepository scmInfoRepository) {
        super();
        this.server = server;
        this.scmInfoRepository = scmInfoRepository;
    }

    @Override
    public void decorateQualityGateStatus(AnalysisDetails analysisDetails, AlmSettingDto almSettingDto, ProjectAlmSettingDto projectAlmSettingDto) {
        LOGGER.info("starting to analyze with " + analysisDetails.toString());
        String revision = analysisDetails.getCommitSha();

        try {
            final String apiURL = analysisDetails.getScannerProperty(PULLREQUEST_AZUREDEVOPS_INSTANCE_URL).orElse("");
            final String apiToken = almSettingDto.getPersonalAccessToken();
            final String projectId = analysisDetails.getScannerProperty(PULLREQUEST_AZUREDEVOPS_PROJECT_ID).orElse("");
            final String pullRequestId = analysisDetails.getBranchName();

            final String projectURL = apiURL + String.format("/projects/%s", URLEncoder.encode(projectId, StandardCharsets.UTF_8.name()));
            final String userURL = apiURL + "/user";
            final String statusUrl = projectURL + String.format("/statuses/%s", revision);
            final String mergeRequestURl = projectURL + String.format("/merge_requests/%s", pullRequestId);
            final String prCommitsURL = mergeRequestURl + "/commits";
            final String mergeRequestDiscussionURL = mergeRequestURl + "/discussions";




            LOGGER.info(String.format("Status url is: %s ", statusUrl));
            LOGGER.info(String.format("PR commits url is: %s ", prCommitsURL));
            LOGGER.info(String.format("MR discussion url is: %s ", mergeRequestDiscussionURL));
            LOGGER.info(String.format("User url is: %s ", userURL));
        }
        catch (IOException ex){
            throw new IllegalStateException("Could not decorate Pull Request on AzureDevOps Server", ex);
        }
    }

    @Override
    public String name() {
        return "AzureDevOpsServer";
    }

    @Override
    public ALM alm() {
        return ALM.AZURE_DEVOPS;
    }
}
