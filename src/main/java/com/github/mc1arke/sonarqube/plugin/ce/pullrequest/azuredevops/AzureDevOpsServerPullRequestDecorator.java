package com.github.mc1arke.sonarqube.plugin.ce.pullrequest.azuredevops;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.AnalysisDetails;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.PullRequestBuildStatusDecorator;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.azuredevops.model.GitPullRequestStatus;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.azuredevops.model.GitStatusContext;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.azuredevops.model.GitStatusStateMapper;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
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
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class AzureDevOpsServerPullRequestDecorator implements PullRequestBuildStatusDecorator {

    private static final String AZURE_API_VERSION = "api-version=5.0-preview.1";

    private static final Logger LOGGER = Loggers.get(AzureDevOpsServerPullRequestDecorator.class);
    private static final List<String> OPEN_ISSUE_STATUSES =
            Issue.STATUSES.stream().filter(s -> !Issue.STATUS_CLOSED.equals(s) && !Issue.STATUS_RESOLVED.equals(s))
                    .collect(Collectors.toList());

    public static final String PULLREQUEST_AZUREDEVOPS_INSTANCE_URL = "sonar.pullrequest.vsts.instanceUrl"; // sonar.pullrequest.vsts.instanceUrl=https://dev.azure.com/fabrikam/
    public static final String PULLREQUEST_AZUREDEVOPS_BASE_BRANCH = "sonar.pullrequest.base";              // sonar.pullrequest.base=master
    public static final String PULLREQUEST_AZUREDEVOPS_BRANCH = "sonar.pullrequest.branch";                 // sonar.pullrequest.branch=feature/some-feature
    public static final String PULLREQUEST_AZUREDEVOPS_PULLREQUEST_ID = "sonar.pullrequest.key";            // sonar.pullrequest.key=222
    public static final String PULLREQUEST_AZUREDEVOPS_PROJECT_ID = "sonar.pullrequest.vsts.project";       // sonar.pullrequest.vsts.project=MyProject
    public static final String PULLREQUEST_AZUREDEVOPS_REPOSITORY_NAME = "sonar.pullrequest.vsts.repository";//sonar.pullrequest.vsts.repository=MyReposytory

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
            final String apiURL = analysisDetails.getScannerProperty(PULLREQUEST_AZUREDEVOPS_INSTANCE_URL).orElseThrow(
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
            final String repositoryName = analysisDetails.getScannerProperty(PULLREQUEST_AZUREDEVOPS_REPOSITORY_NAME).orElseThrow(
                    () -> new IllegalStateException(String.format(
                            "Could not decorate AzureDevops pullRequest. '%s' has not been set in scanner properties",
                            PULLREQUEST_AZUREDEVOPS_REPOSITORY_NAME)));

            final String projectId = analysisDetails.getScannerProperty(PULLREQUEST_AZUREDEVOPS_PROJECT_ID).orElse("Not found: PULLREQUEST_AZUREDEVOPS_PROJECT_ID");
            final String apiToken = almSettingDto.getPersonalAccessToken();
            final String sonarBranch = analysisDetails.getBranchName();

            LOGGER.info(String.format("AZURE: apiURL is: %s ", apiURL));
            LOGGER.info(String.format("AZURE: baseBranch is: %s ", baseBranch));
            LOGGER.info(String.format("AZURE: branch is: %s ", branch));
            LOGGER.info(String.format("AZURE: pullRequestId is: %s ", pullRequestId));
            LOGGER.info(String.format("AZURE: projectId is: %s ", projectId));
            LOGGER.info(String.format("AZURE: repositoryName is: %s ", repositoryName));
            LOGGER.info(String.format("AZURE: revision Commit/revision is: %s ", revision));
            LOGGER.info(String.format("AZURE: sonarBranch is: %s ", sonarBranch));

            String encodeBytes = Base64.getEncoder().encodeToString((":" + apiToken).getBytes());
            Map<String, String> headers = new HashMap<>();
            headers.put("Accept", "application/json");
            headers.put("Content-Type", "application/json; charset=utf-8");
            headers.put("Authorization", "Basic " + encodeBytes);

            postStatus(headers, analysisDetails);

        }
        catch (IOException ex){
            throw new IllegalStateException("Could not decorate Pull Request on AzureDevOps Server", ex);
        }
    }

    private void postStatus(Map<String, String> headers, AnalysisDetails analysisDetails) throws IOException {
        // POST https://dev.azure.com/{organization}/{project}/_apis/git/repositories/{repositoryId}/pullRequests/{pullRequestId}/statuses?api-version=5.1-preview.1
        final String GIT_STATUS_CONTEXT_GENRE = "SonarQube";
        final String GIT_STATUS_CONTEXT_NAME = "PullRequestDecoration";
        final String GIT_STATUS_DESCRIPTION = "SonarQube Status";

        StringBuilder statusPostUrl = new StringBuilder(analysisDetails.getScannerProperty(PULLREQUEST_AZUREDEVOPS_INSTANCE_URL).get());
        statusPostUrl.append(analysisDetails.getScannerProperty(PULLREQUEST_AZUREDEVOPS_PROJECT_ID).get());
        statusPostUrl.append("/_apis/git/repositories/");
        statusPostUrl.append(analysisDetails.getScannerProperty(PULLREQUEST_AZUREDEVOPS_REPOSITORY_NAME).get());
        statusPostUrl.append("/pullRequests/");
        statusPostUrl.append(analysisDetails.getScannerProperty(PULLREQUEST_AZUREDEVOPS_PULLREQUEST_ID).get());
        statusPostUrl.append("/statuses?");
        statusPostUrl.append(AZURE_API_VERSION);

        GitPullRequestStatus status = new GitPullRequestStatus();
        status.state = GitStatusStateMapper.toGitStatusState(analysisDetails.getQualityGateStatus());
        status.description = GIT_STATUS_DESCRIPTION;
        status.context = new GitStatusContext(GIT_STATUS_CONTEXT_GENRE, GIT_STATUS_CONTEXT_NAME); // "SonarQube/PullRequestDecoration"
        status.targetUrl = String.format("%s/dashboard?id=%s&pullRequest=%s", server.getPublicRootUrl(),
                                               URLEncoder.encode(analysisDetails.getAnalysisProjectKey(),
                                                        StandardCharsets.UTF_8.name()),
                                               URLEncoder.encode(analysisDetails.getBranchName(),
                                                        StandardCharsets.UTF_8.name()));

        LOGGER.trace(String.format("AZURE: postStatus-URL: %s ", statusPostUrl.toString()));
        LOGGER.trace(String.format("AZURE: postStatus-BODY: %s ", new ObjectMapper().writeValueAsString(status)));

        HttpPost httpPost = new HttpPost(statusPostUrl.toString());
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            httpPost.addHeader(entry.getKey(), entry.getValue());
        }
        StringEntity entity = new StringEntity(new ObjectMapper().writeValueAsString(status), StandardCharsets.UTF_8);
        httpPost.setEntity(entity);

        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpResponse httpResponse = httpClient.execute(httpPost);
            validateAzureDevOpsResponse(httpResponse, 200, "Status posted");
        }
    }

    private void validateAzureDevOpsResponse(HttpResponse httpResponse, int expectedStatus, String successLogMessage) throws IOException {
        if (null != httpResponse && httpResponse.getStatusLine().getStatusCode() != expectedStatus) {
            LOGGER.error(httpResponse.toString());
            LOGGER.error(EntityUtils.toString(httpResponse.getEntity(), StandardCharsets.UTF_8));
            throw new IllegalStateException("An error was returned in the response from the Azure DevOps server. See the previous log messages for details");
        } else if (null != httpResponse) {
            LOGGER.debug(httpResponse.toString());
            LOGGER.info(successLogMessage);
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
