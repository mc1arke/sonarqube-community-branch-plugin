package com.github.mc1arke.sonarqube.plugin.ce.pullrequest.azuredevops;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.AnalysisDetails;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.DecorationResult;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.PostAnalysisIssueVisitor;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.PullRequestBuildStatusDecorator;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.azuredevops.model.AzurePullRequestDetails;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.azuredevops.model.Comment;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.azuredevops.model.CommentThread;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.azuredevops.model.GitStatusContext;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.azuredevops.model.GitPullRequestStatus;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.azuredevops.model.CommentThreadResponse;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.azuredevops.model.enums.CommentThreadStatus;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.azuredevops.model.mappers.GitStatusStateMapper;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.sonar.api.issue.Issue;
import org.sonar.api.platform.Server;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.db.alm.setting.ALM;
import org.sonar.db.alm.setting.AlmSettingDto;
import org.sonar.db.alm.setting.ProjectAlmSettingDto;
import org.sonar.db.protobuf.DbIssues;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class AzureDevOpsServerPullRequestDecorator implements PullRequestBuildStatusDecorator {

    // SCANNER PROPERTIES
    public static final String PULLREQUEST_AZUREDEVOPS_API_VERSION = "sonar.pullrequest.vsts.apiVersion";     // sonar.pullrequest.vsts.apiVersion=5.1-preview.1
    public static final String PULLREQUEST_AZUREDEVOPS_INSTANCE_URL = "sonar.pullrequest.vsts.instanceUrl";   // sonar.pullrequest.vsts.instanceUrl=https://dev.azure.com/fabrikam/
    public static final String PULLREQUEST_AZUREDEVOPS_PROJECT_ID = "sonar.pullrequest.vsts.project";         // sonar.pullrequest.vsts.project=MyProject
    public static final String PULLREQUEST_AZUREDEVOPS_REPOSITORY_NAME = "sonar.pullrequest.vsts.repository"; // sonar.pullrequest.vsts.repository=MyReposytory

    // AZURE DEVOPS ENVIRONMENT VARIABLES: https://docs.microsoft.com/en-us/azure/devops/pipelines/build/variables?view=azure-devops&tabs=yaml
    public static final String AZUREDEVOPS_ENV_INSTANCE_URL = "System.TeamFoundationCollectionUri";
    public static final String AZUREDEVOPS_ENV_BASE_BRANCH = "System.PullRequest.TargetBranch";
    public static final String AZUREDEVOPS_ENV_BRANCH = "System.PullRequest.SourceBranch";
    public static final String AZUREDEVOPS_ENV_PULLREQUEST_ID = "System.PullRequest.PullRequestId";
    public static final String AZUREDEVOPS_ENV_TEAMPROJECT_ID = "System.TeamProjectId";
    public static final String AZUREDEVOPS_ENV_REPOSITORY_NAME = "Build.Repository.Name";
            
    private static final Logger LOGGER = Loggers.get(AzureDevOpsServerPullRequestDecorator.class);
    private static final String GENERAL_ERROR_MESSAGE = "An error was returned in the response from the Azure DevOps server. See the previous log messages for details";

    private final Server server;
        
    public AzureDevOpsServerPullRequestDecorator(Server server) {
        super();
        this.server = server;
    }

    @Override
    public DecorationResult decorateQualityGateStatus(AnalysisDetails analysisDetails, AlmSettingDto almSettingDto, ProjectAlmSettingDto projectAlmSettingDto) {
       
        LOGGER.info(String.format("starting to analyze with %s", analysisDetails.toString()));

        final String missingPropertyMessage = "Could not decorate AzureDevOps pullRequest. '%s' has not been set in scanner properties";
        
        try {
            String pullRequestId = analysisDetails.getBranchName();
            String azureUrl = analysisDetails.getScannerProperty(PULLREQUEST_AZUREDEVOPS_INSTANCE_URL)
                    .map(url -> url.endsWith("/") ? url : url + "/").orElseThrow(
                    () -> new IllegalStateException(String.format(missingPropertyMessage, PULLREQUEST_AZUREDEVOPS_INSTANCE_URL)));
            String baseBranch = analysisDetails.getPullRequestBase().orElseThrow(
                    () -> new IllegalStateException(String.format(missingPropertyMessage, AnalysisDetails.SCANNERROPERTY_PULLREQUEST_BASE)));
            String branch = analysisDetails.getPullRequestBranch().orElseThrow(
                    () -> new IllegalStateException(String.format(missingPropertyMessage, AnalysisDetails.SCANNERROPERTY_PULLREQUEST_BRANCH))); 
            String azureRepositoryName = analysisDetails.getScannerProperty(PULLREQUEST_AZUREDEVOPS_REPOSITORY_NAME).orElseThrow(
                    () -> new IllegalStateException(String.format(missingPropertyMessage,PULLREQUEST_AZUREDEVOPS_REPOSITORY_NAME)));
            String azureProjectId = analysisDetails.getScannerProperty(PULLREQUEST_AZUREDEVOPS_PROJECT_ID).orElseThrow(
                    () -> new IllegalStateException(String.format(missingPropertyMessage, PULLREQUEST_AZUREDEVOPS_PROJECT_ID)));
            String apiVersion = analysisDetails.getScannerProperty(PULLREQUEST_AZUREDEVOPS_API_VERSION).orElse("6.0-preview.1");

            if (almSettingDto.getPersonalAccessToken() == null) {
                throw new IllegalStateException("Could not decorate AzureDevOps pullRequest. Access token has not been set");
            }
                        
            LOGGER.trace(String.format("azureUrl is: %s ", azureUrl));
            LOGGER.trace(String.format("baseBranch is: %s ", baseBranch));
            LOGGER.trace(String.format("branch is: %s ", branch));
            LOGGER.trace(String.format("pullRequestId is: %s ", pullRequestId));
            LOGGER.trace(String.format("azureProjectId is: %s ", azureProjectId));
            LOGGER.trace(String.format("azureRepositoryName is: %s ", azureRepositoryName));
            LOGGER.trace(String.format("apiVersion is: %s ", apiVersion));

            AzurePullRequestDetails azurePullRequestDetails = new AzurePullRequestDetails(apiVersion, azureRepositoryName, azureProjectId, azureUrl, 
                                                                                          almSettingDto.getPersonalAccessToken(), pullRequestId);

            sendPost(
                    getStatusApiUrl(azurePullRequestDetails),
                    getGitPullRequestStatus(analysisDetails),
                    "Status set successfully",
                    azurePullRequestDetails.getAuthorizationHeader()
            );

            List<PostAnalysisIssueVisitor.ComponentIssue> openIssues = analysisDetails.getPostAnalysisIssueVisitor()
                    .getIssues();
                 
            LOGGER.trace(String.format("Analyze issue count: %s ", openIssues.size()));

            ArrayList<CommentThread> azureCommentThreads = new ArrayList<>(Arrays.asList(
                sendGet(getThreadApiUrl(azurePullRequestDetails), 
                    CommentThreadResponse.class, azurePullRequestDetails.getAuthorizationHeader()).getValue()));

            LOGGER.trace(String.format("Azure commentThreads count: %s ", azureCommentThreads.size()));
            // Filter out all the threads unrelated to code changes
            azureCommentThreads.removeIf(x -> x.isDeleted() || 
                                              x.getThreadContext() == null || 
                                              x.getThreadContext().getFilePath() == null || 
                                              x.getThreadContext().getFilePath().isEmpty() ||
                                              x.getThreadContext().getRightFileStart() == null);
            LOGGER.trace(String.format("Azure commentThreads AFTER REMOVE count: %s ", azureCommentThreads.size()));

            for (PostAnalysisIssueVisitor.ComponentIssue issue : openIssues) {                                             
                handleIssue(analysisDetails, azurePullRequestDetails, issue, azureCommentThreads);
            }
            return DecorationResult.builder().withPullRequestUrl(getPullRequestUrl(azurePullRequestDetails)).build();
        } catch (Exception ex) {
            throw new IllegalStateException("Could not decorate Pull Request on AzureDevOps Server", ex);
        }
    }

    private void handleIssue(AnalysisDetails analysisDetails, AzurePullRequestDetails azurePullRequestDetails,
                             PostAnalysisIssueVisitor.ComponentIssue issue, ArrayList<CommentThread> azureCommentThreads) {
        String filePath = analysisDetails.getSCMPathForIssue(issue).orElse(null);   

        if (filePath != null) {
            try {
                Integer line = issue.getIssue().getLine();
                filePath = filePath.endsWith("/") ? filePath : "/" + filePath;
                LOGGER.trace(String.format("ISSUE: key: %s ", issue.getIssue().key()));
                LOGGER.trace(String.format("ISSUE: type: %s ", issue.getIssue().type().toString()));
                LOGGER.trace(String.format("ISSUE: severity: %s ", issue.getIssue().severity()));
                LOGGER.trace(String.format("ISSUE: getLocations: %s ", Objects.requireNonNull(issue.getIssue().getLocations()).toString()));
                LOGGER.trace(String.format("ISSUE: getRuleKey: %s ", issue.getIssue().getRuleKey()));
                LOGGER.trace(String.format("COMPONENT: getDescription: %s ", issue.getComponent().getDescription()));
                DbIssues.Locations locate = Objects.requireNonNull(issue.getIssue().getLocations());
                boolean threadExists = false;
                
                for (CommentThread azureThread : azureCommentThreads) {                    
                    LOGGER.trace(String.format("azureFilePath: %s", azureThread.getThreadContext().getFilePath()));
                    LOGGER.trace(String.format("filePath: %s (%s)", filePath, azureThread.getThreadContext().getFilePath().equals(filePath)));
                    LOGGER.trace(String.format("azureLine: %d", azureThread.getThreadContext().getRightFileStart().getLine()));
                    LOGGER.trace(String.format("line: %d (%s)", line, azureThread.getThreadContext().getRightFileStart().getLine() == locate.getTextRange().getEndLine()));

                    // Check if thread already exists and close thread depending on issue status
                    if (checkAzureThread(issue, filePath, azureThread, azurePullRequestDetails)) {
                        threadExists = true;
                        break;
                    }
                }

                if (!issue.getIssue().getStatus().equals(Issue.STATUS_OPEN)) {
                    LOGGER.info(String.format("SKIPPED ISSUE: Issue status is %s", issue.getIssue().getStatus()));
                    return;
                }

                if (threadExists) {
                    LOGGER.info(String.format("SKIPPED ISSUE: %s %nFile: %s %nLine: %d %nIssue already exists in Azure",
                            issue.getIssue().getMessage(),
                            filePath,
                            line));
                    return;
                }

                String message = String.format("%s: %s ([rule](%s))%n%n[See in SonarQube](%s)",
                        issue.getIssue().type().name(),
                        issue.getIssue().getMessage(),
                        analysisDetails.getRuleUrlWithRuleKey(issue.getIssue().getRuleKey().toString()),
                        analysisDetails.getIssueUrl(issue.getIssue().key())
                );

                CommentThread thread = new CommentThread(filePath, locate, message);
                LOGGER.info(String.format("Creating thread: %s", new ObjectMapper().writeValueAsString(thread)));
                sendPost(
                        getThreadApiUrl(azurePullRequestDetails),
                        new ObjectMapper().writeValueAsString(thread),
                        "Thread created successfully",
                        azurePullRequestDetails.getAuthorizationHeader()
                );
            } catch (Exception e) {
                LOGGER.error("Could not create thread on AzureDevOps Server", e);
                throw new IllegalStateException("Could not create thread on AzureDevOps Server", e);
            }
        }
    }

    private boolean checkAzureThread(PostAnalysisIssueVisitor.ComponentIssue issue, String filePath, CommentThread azureThread,
                                     AzurePullRequestDetails azurePullRequestDetails) {
        try {
            if (azureThread.getThreadContext().getFilePath().equals(filePath)
                    && azureThread.getComments()
                    .stream()
                    .filter(c -> c.getContent().contains(issue.getIssue().key()))
                    .count() > 0 ) {
                
                Comment comment = null;
                String status = "";

                // Close Azure Thread if SonarQube issue is closed                
                if (!issue.getIssue().getStatus().equals(Issue.STATUS_OPEN)
                        && azureThread.getStatus() == CommentThreadStatus.ACTIVE) {
                    comment = new Comment("Issue has been closed in SonarQube");
                    status = "closed";
                    LOGGER.info("Issue has been closed in SonarQube. Try to close in Azure");                    
                } 

                if (comment != null && !status.isEmpty()) {
                    // Add new comment to the Azure Thread
                    sendPost(azureThread.getLinks().getSelf().getHref() + "/comments" + azurePullRequestDetails.getApiVersion(),
                        new ObjectMapper().writeValueAsString(comment),
                        "Comment added success",
                        azurePullRequestDetails.getAuthorizationHeader()
                    );
                    // Close Azure Thread
                    sendPatch(azureThread.getLinks().getSelf().getHref() + azurePullRequestDetails.getApiVersion(),
                        String.format("{\"status\":\"%s\"}", status),
                        azurePullRequestDetails.getAuthorizationHeader()
                    );
                }
                // Thread Exists
                return true;
            }
            return false;
        } catch (Exception e) {
            LOGGER.error("Could not update thread on AzureDevOps Server", e);
            throw new IllegalStateException("Could not update thread on AzureDevOps Server", e);
        }
    }       
    
    private static String getPullRequestUrl(AzurePullRequestDetails azurePullRequestDetails) {
        return azurePullRequestDetails.getAzureUrl() + azurePullRequestDetails.getAzureProjectId() +
                "/_git/" +
                azurePullRequestDetails.getAzureRepositoryName() +
                "/pullRequest/" +
                azurePullRequestDetails.getPullRequestId();        
    }

    private static String getStatusApiUrl(AzurePullRequestDetails azurePullRequestDetails) {
        return azurePullRequestDetails.getAzureUrl() + azurePullRequestDetails.getAzureProjectId() +
                "/_apis/git/repositories/" +
                azurePullRequestDetails.getAzureRepositoryName() +
                "/pullRequests/" +
                azurePullRequestDetails.getPullRequestId() +
                "/statuses" +
                azurePullRequestDetails.getApiVersion();
    }

    private static String getThreadApiUrl(AzurePullRequestDetails azurePullRequestDetails) {
        return azurePullRequestDetails.getAzureUrl() + azurePullRequestDetails.getAzureProjectId() +
                "/_apis/git/repositories/" +
                azurePullRequestDetails.getAzureRepositoryName() +
                "/pullRequests/" +
                azurePullRequestDetails.getPullRequestId() +
                "/threads" +
                azurePullRequestDetails.getApiVersion();
    }

    private String getGitPullRequestStatus(AnalysisDetails analysisDetails) throws IOException {
        final String gitStatusContextGenre = "SonarQube";
        final String gitStatusContextName = "QualityGate";
        final String gitStatusDescription = "SonarQube Gate";

        GitPullRequestStatus status = new GitPullRequestStatus(
                GitStatusStateMapper.toGitStatusState(analysisDetails.getQualityGateStatus()),
                gitStatusDescription,
                new GitStatusContext(gitStatusContextGenre, gitStatusContextName),
                String.format("%s/dashboard?id=%s&pullRequest=%s", server.getPublicRootUrl(),
                        URLEncoder.encode(analysisDetails.getAnalysisProjectKey(),
                                StandardCharsets.UTF_8.name()),
                        URLEncoder.encode(analysisDetails.getBranchName(),
                                StandardCharsets.UTF_8.name())
                )
        );
        return new ObjectMapper().writeValueAsString(status);
    }

    private void sendPost(String apiUrl, String body, String successMessage, String authorizationHeader) throws IOException {
        apiUrl = encodeURI(apiUrl);
        LOGGER.trace(String.format("sendPost: URL: %s ", apiUrl));
        LOGGER.trace(String.format("sendPost: BODY: %s ", body));
        HttpPost httpPost = new HttpPost(apiUrl);
        addHttpHeaders(httpPost, authorizationHeader);
        StringEntity entity = new StringEntity(body, StandardCharsets.UTF_8);
        httpPost.setEntity(entity);

        try (CloseableHttpClient httpClient = HttpClients.createSystem()) {
            HttpResponse httpResponse = httpClient.execute(httpPost);
            if (null != httpResponse && httpResponse.getStatusLine().getStatusCode() != 200) {
                LOGGER.error(String.format("sendPost: %s", httpResponse.toString()));
                LOGGER.error(String.format("sendPost: %s", EntityUtils.toString(httpResponse.getEntity(), StandardCharsets.UTF_8)));
                throw new IllegalStateException(GENERAL_ERROR_MESSAGE);
            } else if (null != httpResponse) {
                LOGGER.debug(String.format("sendPost: %s", httpResponse.toString()));
                LOGGER.info(String.format("sendPost: %s", successMessage));
            }
        }
    }

    private <T> T sendGet(String apiUrl, Class<T> type, String authorizationHeader) throws IOException {
        apiUrl = encodeURI(apiUrl);
        LOGGER.info(String.format("sendGet: URL: %s ", apiUrl));
        HttpGet httpGet = new HttpGet(apiUrl);
        addHttpHeaders(httpGet, authorizationHeader);

        try (CloseableHttpClient httpClient = HttpClients.createSystem()) {
            HttpResponse httpResponse = httpClient.execute(httpGet);
            if (null != httpResponse && httpResponse.getStatusLine().getStatusCode() != 200) {
                LOGGER.error(httpResponse.toString());
                LOGGER.error(EntityUtils.toString(httpResponse.getEntity(), StandardCharsets.UTF_8));
                throw new IllegalStateException(GENERAL_ERROR_MESSAGE);
            } else if (null != httpResponse) {
                HttpEntity entity = httpResponse.getEntity();
                T obj = new ObjectMapper()
                        .configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true)
                        .configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS, true)
                        .configure(DeserializationFeature.ACCEPT_EMPTY_ARRAY_AS_NULL_OBJECT, true)
                        .configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true)
                        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                        .readValue(IOUtils.toString(entity.getContent(), StandardCharsets.UTF_8), type);

                LOGGER.info(String.format("%s recieved", type));

                return obj;
            } else {
                throw new IOException("No response reveived");
            }
        }
    }

    private void sendPatch(String apiUrl, String body, String authorizationHeader) throws IOException {
        apiUrl = encodeURI(apiUrl);
        LOGGER.trace(String.format("sendPatch: URL: %s ", apiUrl));
        LOGGER.trace(String.format("sendPatch: BODY: %s ", body));
        HttpPatch httpPatch = new HttpPatch(apiUrl);
        addHttpHeaders(httpPatch, authorizationHeader);
        StringEntity entity = new StringEntity(body, StandardCharsets.UTF_8);
        httpPatch.setEntity(entity);

        try (CloseableHttpClient httpClient = HttpClients.createSystem()) {
            HttpResponse httpResponse = httpClient.execute(httpPatch);
            if (null != httpResponse && httpResponse.getStatusLine().getStatusCode() != 200) {
                LOGGER.error(String.format("sendPatch: %s", httpResponse.toString()));
                LOGGER.error(String.format("sendPatch: %s", EntityUtils.toString(httpResponse.getEntity(), StandardCharsets.UTF_8)));
                throw new IllegalStateException(GENERAL_ERROR_MESSAGE);
            } else if (null != httpResponse) {
                LOGGER.debug(String.format("sendPatch: %s", httpResponse.toString()));
                LOGGER.info(String.format("sendPatch: %s", "Patch success!"));
            }
        }
    }

    private static HttpRequestBase addHttpHeaders(HttpRequestBase request, String authorizationHeader) {
        request.addHeader("Accept", "application/json");
        request.addHeader("Content-Type", "application/json; charset=utf-8");
        request.addHeader("Authorization", authorizationHeader);

        return request;
    }

    private static String encodeURI(String uri) throws IOException {
        try {
            URL url = new URL(uri);
            return (new URI(url.getProtocol(), url.getUserInfo(), url.getHost(), url.getPort(), url.getPath(), url.getQuery(), url.getRef())).toString();
        }
        catch (Exception ex) {
            LOGGER.error(String.format("Error trying to encode URI: %s", uri));
            throw new IOException(String.format("Error trying to encode URI: %s", uri), ex);
        }        
    }

    @Override
    public List<ALM> alm() {
        return Collections.singletonList(ALM.AZURE_DEVOPS);
    }
}
