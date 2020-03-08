package com.github.mc1arke.sonarqube.plugin.ce.pullrequest.azuredevops;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.AnalysisDetails;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.PostAnalysisIssueVisitor;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.PullRequestBuildStatusDecorator;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.azuredevops.model.CommentThread;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.azuredevops.model.GitPullRequestStatus;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.azuredevops.model.GitStatusContext;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.azuredevops.model.mappers.GitStatusStateMapper;
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
import java.util.*;
import java.util.stream.Collectors;

public class AzureDevOpsServerPullRequestDecorator implements PullRequestBuildStatusDecorator {
    private enum ApiZone {
        status,
        thread
    }
    private String authorizationHeader;
    private static final String AZURE_API_VERSION = "api-version=5.0-preview.1";
    private static final Logger LOGGER = Loggers.get(AzureDevOpsServerPullRequestDecorator.class);
    private static final List<String> OPEN_ISSUE_STATUSES =
            Issue.STATUSES.stream().filter(s -> !Issue.STATUS_CLOSED.equals(s) && !Issue.STATUS_RESOLVED.equals(s))
                    .collect(Collectors.toList());
    // SCANNER PROPERTY
    public static final String PULLREQUEST_AZUREDEVOPS_INSTANCE_URL = "sonar.pullrequest.vsts.instanceUrl"; // sonar.pullrequest.vsts.instanceUrl=https://dev.azure.com/fabrikam/
    public static final String PULLREQUEST_AZUREDEVOPS_BASE_BRANCH = "sonar.pullrequest.base";              // sonar.pullrequest.base=master
    public static final String PULLREQUEST_AZUREDEVOPS_BRANCH = "sonar.pullrequest.branch";                 // sonar.pullrequest.branch=feature/some-feature
    public static final String PULLREQUEST_AZUREDEVOPS_PULLREQUEST_ID = "sonar.pullrequest.key";            // sonar.pullrequest.key=222
    public static final String PULLREQUEST_AZUREDEVOPS_PROJECT_ID = "sonar.pullrequest.vsts.project";       // sonar.pullrequest.vsts.project=MyProject
    public static final String PULLREQUEST_AZUREDEVOPS_REPOSITORY_NAME = "sonar.pullrequest.vsts.repository";//sonar.pullrequest.vsts.repository=MyReposytory
    // SONAR URL MASK
    public static final String SONAR_ISSUE_URL_MASK = "%s/project/issues?id=%s&issues=%s&open=%s&pullRequest=%s"; //http://sonarqube.shtormtech.ru/project/issues?id=ProjId&issues=AXCuh6CgT2BpyN1RPU03&open=AXCuh6CgT2BpyN1RPU03&pullRequest=8513
    public static final String SONAR_RULE_URL_MASK = "%s/coding_rules?open=%s&rule_key=%s"; //http://sonarqube.shtormtech.ru/coding_rules?open=csharpsquid%3AS1135&rule_key=csharpsquid%3AS1135

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
            final String azureUrl = analysisDetails.getScannerProperty(PULLREQUEST_AZUREDEVOPS_INSTANCE_URL).orElseThrow(
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
            final String sonarBranch = analysisDetails.getBranchName();
            if (almSettingDto.getPersonalAccessToken() == null ) {
                throw new IllegalStateException("Could not decorate AzureDevops pullRequest. Access token has not been set");
            }
            setAuthorizationHeader(almSettingDto.getPersonalAccessToken());

            LOGGER.info(String.format("AZURE: azureUrl is: %s ", azureUrl));
            LOGGER.info(String.format("AZURE: baseBranch is: %s ", baseBranch));
            LOGGER.info(String.format("AZURE: branch is: %s ", branch));
            LOGGER.info(String.format("AZURE: pullRequestId is: %s ", pullRequestId));
            LOGGER.info(String.format("AZURE: projectId is: %s ", projectId));
            LOGGER.info(String.format("AZURE: repositoryName is: %s ", repositoryName));
            LOGGER.info(String.format("AZURE: revision Commit/revision is: %s ", revision));
            LOGGER.info(String.format("AZURE: sonarBranch is: %s ", sonarBranch));

            sendPost(
                    getApiUrl(ApiZone.status, analysisDetails),
                    getGitPullRequestStatus(analysisDetails)
            );

            List<PostAnalysisIssueVisitor.ComponentIssue> openIssues = analysisDetails.getPostAnalysisIssueVisitor().getIssues().stream().filter(i -> OPEN_ISSUE_STATUSES.contains(i.getIssue().getStatus())).collect(Collectors.toList());
            LOGGER.info(String.format("AZURE: issue count: %s ", openIssues.size()));

            for (PostAnalysisIssueVisitor.ComponentIssue issue : openIssues) {
                String filePath = "/" + analysisDetails.getSCMPathForIssue(issue).orElse(null);
                Integer line = issue.getIssue().getLine();
                if (filePath != null && line != null) {
                    try {
                        LOGGER.info(String.format("AZURE ISSUE: authorLogin: %s ", issue.getIssue().authorLogin()));
                        LOGGER.info(String.format("AZURE ISSUE: type: %s ", issue.getIssue().type().toString()));
                        LOGGER.info(String.format("AZURE ISSUE: type: %s ", issue.getIssue().severity()));
                        LOGGER.info(String.format("AZURE ISSUE: changes size: %s ", issue.getIssue().changes().size()));
                        LOGGER.info(String.format("AZURE ISSUE: key: %s ", issue.getIssue().key()));
                        LOGGER.info(String.format("AZURE ISSUE: selectedAt: %s ", issue.getIssue().selectedAt()));
                        LOGGER.info(String.format("AZURE ISSUE: componentKey: %s ", issue.getIssue().componentKey()));
                        LOGGER.info(String.format("AZURE ISSUE: getLocations: %s ", Objects.requireNonNull(issue.getIssue().getLocations()).toString()));
                        LOGGER.info(String.format("AZURE ISSUE: getRuleKey: %s ", issue.getIssue().getRuleKey()));
                        LOGGER.info(String.format("AZURE COMPONENT: getDescription: %s ", issue.getComponent().getDescription()));
                        LOGGER.info(String.format("AZURE COMPONENT: getFileAttributes: %s ", issue.getComponent().getFileAttributes().toString()));
                        LOGGER.info(String.format("AZURE COMPONENT: getReportAttributes: %s ", issue.getComponent().getReportAttributes().toString()));
                        LOGGER.info(String.format("AZURE COMPONENT: getViewAttributes: %s ", issue.getComponent().getViewAttributes().toString()));
                        LOGGER.info(String.format("AZURE COMPONENT: getSubViewAttributes: %s ", issue.getComponent().getSubViewAttributes().toString()));

                        //LOGGER.info(String.format("AZURE ISSUE: currentChange.diffs: %s ", issue.getIssue().currentChange().diffs().size()));

                        CommentThread thread = new CommentThread(filePath, line, issue.getIssue().getMessage());
                        LOGGER.info(String.format("AZURE: thread: %s ", new ObjectMapper().writeValueAsString(thread)));
                    } catch (Exception e) {
                        LOGGER.error(e.toString());
                    }
                }

            }
        } catch (IOException ex) {
            throw new IllegalStateException("Could not decorate Pull Request on AzureDevOps Server", ex);
        }
    }

    private void setAuthorizationHeader(String apiToken) {
        String encodeBytes = Base64.getEncoder().encodeToString((":" + apiToken).getBytes());
        authorizationHeader = "Basic " + encodeBytes;
    }

    public String getIssueUrl(String projectKey, String issueKey, String pullRequestId) throws IOException
    {
        //ISSUE http://sonarqube.shtormtech.ru/project/issues?id=ProjId&issues=AXCuh6CgT2BpyN1RPU03&open=AXCuh6CgT2BpyN1RPU03&pullRequest=8513
        return String.format(SONAR_ISSUE_URL_MASK,
                server.getPublicRootUrl(),
                URLEncoder.encode(projectKey, StandardCharsets.UTF_8.name()),
                URLEncoder.encode(issueKey, StandardCharsets.UTF_8.name()),
                URLEncoder.encode(issueKey, StandardCharsets.UTF_8.name()),
                URLEncoder.encode(pullRequestId, StandardCharsets.UTF_8.name())
        );
    }
    public String getRuleUrlWithRuleKey(String ruleKey) throws IOException
    {
        //RULE http://sonarqube.shtormtech.ru/coding_rules?open=csharpsquid%3AS1135&rule_key=csharpsquid%3AS1135
        return String.format(SONAR_RULE_URL_MASK,
                server.getPublicRootUrl(),
                URLEncoder.encode(ruleKey, StandardCharsets.UTF_8.name()),
                URLEncoder.encode(ruleKey, StandardCharsets.UTF_8.name())
        );
    }
    private String getApiUrl(ApiZone apiZone, AnalysisDetails analysisDetails) throws UnsupportedOperationException {
        StringBuilder postUrl = new StringBuilder(analysisDetails.getScannerProperty(PULLREQUEST_AZUREDEVOPS_INSTANCE_URL).get()); //instance
        postUrl.append(analysisDetails.getScannerProperty(PULLREQUEST_AZUREDEVOPS_PROJECT_ID).get());       //project
        postUrl.append("/_apis/git/repositories/");
        postUrl.append(analysisDetails.getScannerProperty(PULLREQUEST_AZUREDEVOPS_REPOSITORY_NAME).get());  // repositoryId
        postUrl.append("/pullRequests/");
        postUrl.append(analysisDetails.getScannerProperty(PULLREQUEST_AZUREDEVOPS_PULLREQUEST_ID).get());   // pullRequestId

        switch (apiZone) {
            case status: {
                // POST https://{instance}/{collection}/{project}/_apis/git/repositories/{repositoryId}/pullRequests/{pullRequestId}/statuses?api-version=5.0-preview.1
                postUrl.append("/statuses?");
                break;
            }
            case thread: {
                //POST https://{instance}/{collection}/{project}/_apis/git/repositories/{repositoryId}/pullRequests/{pullRequestId}/threads?api-version=5.0
                postUrl.append("/threads?");
                break;
            }
            default: {
                throw new UnsupportedOperationException("Not implemented method");
            }
        }
        postUrl.append(AZURE_API_VERSION);
        return postUrl.toString();
    }

    private String getGitPullRequestStatus(AnalysisDetails analysisDetails) throws IOException {
        final String GIT_STATUS_CONTEXT_GENRE = "SonarQube";
        final String GIT_STATUS_CONTEXT_NAME = "PullRequestDecoration";
        final String GIT_STATUS_DESCRIPTION = "SonarQube Status";

        GitPullRequestStatus status = new GitPullRequestStatus();
        status.state = GitStatusStateMapper.toGitStatusState(analysisDetails.getQualityGateStatus());
        status.description = GIT_STATUS_DESCRIPTION;
        status.context = new GitStatusContext(GIT_STATUS_CONTEXT_GENRE, GIT_STATUS_CONTEXT_NAME); // "SonarQube/PullRequestDecoration"
        status.targetUrl = String.format("%s/dashboard?id=%s&pullRequest=%s", server.getPublicRootUrl(),
                URLEncoder.encode(analysisDetails.getAnalysisProjectKey(),
                        StandardCharsets.UTF_8.name()),
                URLEncoder.encode(analysisDetails.getBranchName(),
                        StandardCharsets.UTF_8.name())
        );

        return new ObjectMapper().writeValueAsString(status);
    }

    private void sendPost(String apiUrl, String body) throws IOException {
        LOGGER.trace(String.format("AZURE: sendPost-URL: %s ", apiUrl));
        LOGGER.trace(String.format("AZURE: sendPost-BODY: %s ", body));
        HttpPost httpPost = new HttpPost(apiUrl);
        httpPost.addHeader("Accept", "application/json");
        httpPost.addHeader("Content-Type", "application/json; charset=utf-8");
        httpPost.addHeader("Authorization", authorizationHeader);
        StringEntity entity = new StringEntity(body, StandardCharsets.UTF_8);
        httpPost.setEntity(entity);

        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpResponse httpResponse = httpClient.execute(httpPost);
            if (null != httpResponse && httpResponse.getStatusLine().getStatusCode() != 200) {
                LOGGER.error(httpResponse.toString());
                LOGGER.error(EntityUtils.toString(httpResponse.getEntity(), StandardCharsets.UTF_8));
                throw new IllegalStateException("An error was returned in the response from the Azure DevOps server. See the previous log messages for details");
            } else if (null != httpResponse) {
                LOGGER.debug(httpResponse.toString());
                LOGGER.info("Post success!");
            }
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
