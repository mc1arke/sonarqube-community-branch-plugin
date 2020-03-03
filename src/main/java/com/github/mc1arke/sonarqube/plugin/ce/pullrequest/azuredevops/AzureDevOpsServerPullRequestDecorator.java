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

import java.util.List;
import java.util.stream.Collectors;

public class AzureDevOpsServerPullRequestDecorator implements PullRequestBuildStatusDecorator {

    private static final Logger LOGGER = Loggers.get(AzureDevOpsServerPullRequestDecorator.class);
    private static final List<String> OPEN_ISSUE_STATUSES =
            Issue.STATUSES.stream().filter(s -> !Issue.STATUS_CLOSED.equals(s) && !Issue.STATUS_RESOLVED.equals(s))
                    .collect(Collectors.toList());

    public static final String PULLREQUEST_AZUREDEVOPS_INSTANCE_URL = "sonar.pullrequest.vsts.instanceUrl";
    public static final String PULLREQUEST_AZUREDEVOPS_BASE_BRANCH = "sonar.pullrequest.base";
    public static final String PULLREQUEST_AZUREDEVOPS_BRANCH = "sonar.pullrequest.branch";
    public static final String PULLREQUEST_AZUREDEVOPS_PULLREQUEST_ID = "sonar.pullrequest.key";
    public static final String PULLREQUEST_AZUREDEVOPS_PROJECT_ID = "sonar.pullrequest.vsts.project";
    public static final String PULLREQUEST_AZUREDEVOPS_REPOSITORY_NAME = "sonar.pullrequest.vsts.repository";

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
            /*final String apiURL = analysisDetails.getScannerProperty(PULLREQUEST_AZUREDEVOPS_INSTANCE_URL).orElseThrow(
                    () -> new IllegalStateException(String.format(
                            "Could not decorate AzureDevops pullRequest. '%s' has not been set in scanner properties",
                            PULLREQUEST_AZUREDEVOPS_INSTANCE_URL)));
            final String baseBranch = analysisDetails.getScannerProperty(PULLREQUEST_AZUREDEVOPS_BASE_BRANCH).orElseThrow(
                    () -> new IllegalStateException(String.format(
                            "Could not decorate AzureDevops pullRequest. '%s' has not been set in scanner properties",
                            PULLREQUEST_AZUREDEVOPS_BASE_BRANCH)));
            final String branch = analysisDetails.getScannerProperty(PULLREQUEST_AZUREDEVOPS_BRANCH).orElseThrow(
                    () -> new IllegalStateException(String.format(
                            "Could not decorate AzureDevops pullRequest. '%s' has not been set in scanner properties",
                            PULLREQUEST_AZUREDEVOPS_BRANCH)));
            final String pullRequestId = analysisDetails.getScannerProperty(PULLREQUEST_AZUREDEVOPS_PULLREQUEST_ID).orElseThrow(
                    () -> new IllegalStateException(String.format(
                            "Could not decorate AzureDevops pullRequest. '%s' has not been set in scanner properties",
                            PULLREQUEST_AZUREDEVOPS_PULLREQUEST_ID)));
            final String projectId = analysisDetails.getScannerProperty(PULLREQUEST_AZUREDEVOPS_PROJECT_ID).orElseThrow(
                    () -> new IllegalStateException(String.format(
                            "Could not decorate AzureDevops pullRequest. '%s' has not been set in scanner properties",
                            PULLREQUEST_AZUREDEVOPS_PROJECT_ID)));
            final String repositoryName = analysisDetails.getScannerProperty(PULLREQUEST_AZUREDEVOPS_REPOSITORY_NAME).orElseThrow(
                    () -> new IllegalStateException(String.format(
                            "Could not decorate AzureDevops pullRequest. '%s' has not been set in scanner properties",
                            PULLREQUEST_AZUREDEVOPS_REPOSITORY_NAME)));*/

            //Configuration configuration = configurationRepository.getConfiguration();

            //LOGGER.info(String.format("AZURE: ***prop count*** : %s ", analysisDetails.getScannerContextProp().size() ));
            //analysisDetails.getScannerContextProp().forEach((sn, ss) -> LOGGER.info(String.format("AZURE: ***%s*** : %s ", sn, ss)));

            final String apiURL = analysisDetails.getScannerProperty(PULLREQUEST_AZUREDEVOPS_INSTANCE_URL).orElse("Not found: PULLREQUEST_AZUREDEVOPS_INSTANCE_URL");
            final String baseBranch = analysisDetails.getScannerProperty(PULLREQUEST_AZUREDEVOPS_BASE_BRANCH).orElse("Not found: PULLREQUEST_AZUREDEVOPS_BASE_BRANCH");
            final String branch = analysisDetails.getScannerProperty(PULLREQUEST_AZUREDEVOPS_BRANCH).orElse("Not found: PULLREQUEST_AZUREDEVOPS_BRANCH");

            final String pullRequestId = analysisDetails.getScannerProperty(PULLREQUEST_AZUREDEVOPS_PULLREQUEST_ID).orElse("Not found: PULLREQUEST_AZUREDEVOPS_PULLREQUEST_ID");
            final String projectId = analysisDetails.getScannerProperty(PULLREQUEST_AZUREDEVOPS_PROJECT_ID).orElse("Not found: PULLREQUEST_AZUREDEVOPS_PROJECT_ID");
            final String repositoryName = analysisDetails.getScannerProperty(PULLREQUEST_AZUREDEVOPS_REPOSITORY_NAME).orElse("Not found: PULLREQUEST_AZUREDEVOPS_REPOSITORY_NAME");
            final String apiToken = almSettingDto.getPersonalAccessToken();
            final String sonarBranch = analysisDetails.getBranchName();

            LOGGER.info(String.format("AZURE: apiURL is: %s ", apiToken));
            LOGGER.info(String.format("AZURE: apiURL is: %s ", apiURL));
            LOGGER.info(String.format("AZURE: baseBranch is: %s ", baseBranch));
            LOGGER.info(String.format("AZURE: branch is: %s ", branch));
            LOGGER.info(String.format("AZURE: pullRequestId is: %s ", pullRequestId));
            LOGGER.info(String.format("AZURE: projectId is: %s ", projectId));
            LOGGER.info(String.format("AZURE: repositoryName is: %s ", repositoryName));
            LOGGER.info(String.format("AZURE: revision Commit/revision is: %s ", revision));
            LOGGER.info(String.format("AZURE: sonarBranch is: %s ", sonarBranch));

        }
        catch (IllegalStateException ex){
            throw new IllegalStateException("Could not decorate Pull Request on AzureDevOps Server", ex);
        }
    }

    @Override
    public String name() {
        return "Azure";
    }

    @Override
    public ALM alm() {
        return ALM.AZURE_DEVOPS;
    }
}
