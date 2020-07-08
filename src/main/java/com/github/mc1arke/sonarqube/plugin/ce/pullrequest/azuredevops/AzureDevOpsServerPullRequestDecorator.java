package com.github.mc1arke.sonarqube.plugin.ce.pullrequest.azuredevops;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.AnalysisDetails;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.DecorationResult;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.PostAnalysisIssueVisitor;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.PullRequestBuildStatusDecorator;
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
import org.sonar.db.protobuf.DbIssues;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Map;



public class AzureDevOpsServerPullRequestDecorator implements PullRequestBuildStatusDecorator {

    private String authorizationHeader;    
    private static final Logger LOGGER = Loggers.get(AzureDevOpsServerPullRequestDecorator.class);
    public static final String API_VERSION_PREFIX = "?api-version=";

    private String apiVersion = "6.0-preview.1";
    private String azureUrl = "";
    private String baseBranch = "";
    private String branch = "";
    private String pullRequestId = "";
    private String azureRepositoryName = "";
    private String azureProjectId = "";

    /*private static final List<String> OPEN_ISSUE_STATUSES =
            Issue.STATUSES.stream().filter(s -> !Issue.STATUS_CLOSED.equals(s) && !Issue.STATUS_RESOLVED.equals(s))
                    .collect(Collectors.toList());*/
    // SCANNER PROPERTY
    public static final String PULLREQUEST_AZUREDEVOPS_API_VERSION = "sonar.pullrequest.vsts.apiVersion";   // sonar.pullrequest.vsts.apiVersion=5.1-preview.1
    public static final String PULLREQUEST_AZUREDEVOPS_INSTANCE_URL = "sonar.pullrequest.vsts.instanceUrl"; // sonar.pullrequest.vsts.instanceUrl=https://dev.azure.com/fabrikam/
    public static final String PULLREQUEST_AZUREDEVOPS_BASE_BRANCH = "sonar.pullrequest.base";              // sonar.pullrequest.base=master
    public static final String PULLREQUEST_AZUREDEVOPS_BRANCH = "sonar.pullrequest.branch";                 // sonar.pullrequest.branch=feature/some-feature
    public static final String PULLREQUEST_AZUREDEVOPS_PULLREQUEST_ID = "sonar.pullrequest.key";            // sonar.pullrequest.key=222
    public static final String PULLREQUEST_AZUREDEVOPS_PROJECT_ID = "sonar.pullrequest.vsts.project";       // sonar.pullrequest.vsts.project=MyProject
    public static final String PULLREQUEST_AZUREDEVOPS_REPOSITORY_NAME = "sonar.pullrequest.vsts.repository";//sonar.pullrequest.vsts.repository=MyReposytory
    // SONAR URL MASK
    public static final String SONAR_ISSUE_URL_MASK = "%s/project/issues?id=%s&issues=%s&open=%s&pullRequest=%s"; //http://localhost/project/issues?id=ProjId&issues=AXCuh6CgT2BpyN1RPU03&open=AXCuh6CgT2BpyN1RPU03&pullRequest=8513
    public static final String SONAR_RULE_URL_MASK = "%s/coding_rules?open=%s&rule_key=%s"; //http://localhost/coding_rules?open=csharpsquid%3AS1135&rule_key=csharpsquid%3AS1135

    private final Server server;
    private final ScmInfoRepository scmInfoRepository;

    public AzureDevOpsServerPullRequestDecorator(Server server, ScmInfoRepository scmInfoRepository) {
        super();
        this.server = server;
        this.scmInfoRepository = scmInfoRepository;
    }

    @Override
    public DecorationResult decorateQualityGateStatus(AnalysisDetails analysisDetails, AlmSettingDto almSettingDto, ProjectAlmSettingDto projectAlmSettingDto) {
        LOGGER.info("starting to analyze with " + analysisDetails.toString());
        
        Map<String,String> properties = analysisDetails.getScannerProperties();
        
        LOGGER.info("Found " + properties.size() + " scanner properties...");

        for (Map.Entry<String,String> entry : properties.entrySet())  
            LOGGER.debug("Key = " + entry.getKey() + ", Value = " + entry.getValue()); 

        try {
            azureUrl = ensureTrailingSlash(analysisDetails.getScannerProperty(PULLREQUEST_AZUREDEVOPS_INSTANCE_URL)).orElseThrow(
                    () -> new IllegalStateException(String.format(
                            "Could not decorate AzureDevOps pullRequest. '%s' has not been set in scanner properties",
                            PULLREQUEST_AZUREDEVOPS_INSTANCE_URL)));
            baseBranch = analysisDetails.getScannerProperty(PULLREQUEST_AZUREDEVOPS_BASE_BRANCH).orElseThrow(
                    () -> new IllegalStateException(String.format(
                            "Could not decorate AzureDevOps pullRequest. '%s' has not been set in scanner properties",
                            PULLREQUEST_AZUREDEVOPS_BASE_BRANCH)));
            branch = analysisDetails.getScannerProperty(PULLREQUEST_AZUREDEVOPS_BRANCH).orElseThrow(
                    () -> new IllegalStateException(String.format(
                            "Could not decorate AzureDevOps pullRequest. '%s' has not been set in scanner properties",
                            PULLREQUEST_AZUREDEVOPS_BRANCH)));
            pullRequestId = analysisDetails.getScannerProperty(PULLREQUEST_AZUREDEVOPS_PULLREQUEST_ID).orElseThrow(
                    () -> new IllegalStateException(String.format(
                            "Could not decorate AzureDevOps pullRequest. '%s' has not been set in scanner properties",
                            PULLREQUEST_AZUREDEVOPS_PULLREQUEST_ID)));
            azureRepositoryName = analysisDetails.getScannerProperty(PULLREQUEST_AZUREDEVOPS_REPOSITORY_NAME).orElseThrow(
                    () -> new IllegalStateException(String.format(
                            "Could not decorate AzureDevOps pullRequest. '%s' has not been set in scanner properties",
                            PULLREQUEST_AZUREDEVOPS_REPOSITORY_NAME)));
            azureProjectId = analysisDetails.getScannerProperty(PULLREQUEST_AZUREDEVOPS_PROJECT_ID).orElseThrow(
                    () -> new IllegalStateException(String.format(
                            "Could not decorate AzureDevOps pullRequest. '%s' has not been set in scanner properties",
                            PULLREQUEST_AZUREDEVOPS_PROJECT_ID)));
            apiVersion = analysisDetails.getScannerProperty(PULLREQUEST_AZUREDEVOPS_API_VERSION).orElseGet(
                    () -> apiVersion);
            if (almSettingDto.getPersonalAccessToken() == null) {
                throw new IllegalStateException("Could not decorate AzureDevOps pullRequest. Access token has not been set");
            }
            setAuthorizationHeader(almSettingDto.getPersonalAccessToken());

            LOGGER.trace(String.format("azureUrl is: %s ", azureUrl));
            LOGGER.trace(String.format("baseBranch is: %s ", baseBranch));
            LOGGER.trace(String.format("branch is: %s ", branch));
            LOGGER.trace(String.format("pullRequestId is: %s ", pullRequestId));
            LOGGER.trace(String.format("azureProjectId is: %s ", azureProjectId));
            LOGGER.trace(String.format("azureRepositoryName is: %s ", azureRepositoryName));
            LOGGER.trace(String.format("apiVersion is: %s ", apiVersion));

            sendPost(
                    getStatusApiUrl(),
                    getGitPullRequestStatus(analysisDetails),
                    "Status set successfully"
            );

            List<PostAnalysisIssueVisitor.ComponentIssue> openIssues = analysisDetails.getPostAnalysisIssueVisitor()
                    .getIssues();
                    /*.stream()
                    .filter(i -> OPEN_ISSUE_STATUSES.contains(i.getIssue().getStatus()))
                    .collect(Collectors.toList());*/
            LOGGER.trace(String.format("Analyze issue count: %s ", openIssues.size()));

            ArrayList<CommentThread> azureCommentThreads = new ArrayList<CommentThread>(Arrays.asList(sendGet(getThreadApiUrl(), CommentThreadResponse.class).getValue()));
            LOGGER.trace(String.format("Azure commentThreads count: %s ", azureCommentThreads.size()));
            azureCommentThreads.removeIf(x -> x.getThreadContext() == null || x.isDeleted());
            LOGGER.trace(String.format("Azure commentThreads AFTER REMOVE count: %s ", azureCommentThreads.size()));

            for (PostAnalysisIssueVisitor.ComponentIssue issue : openIssues) {
                String filePath = analysisDetails.getSCMPathForIssue(issue).orElse(null);
                Integer line = issue.getIssue().getLine();
                if (filePath != null) {
                    try {
                        filePath = "/" + filePath;
                        LOGGER.trace(String.format("ISSUE: authorLogin: %s ", issue.getIssue().authorLogin()));
                        LOGGER.trace(String.format("ISSUE: key: %s ", issue.getIssue().key()));
                        LOGGER.trace(String.format("ISSUE: type: %s ", issue.getIssue().type().toString()));
                        LOGGER.trace(String.format("ISSUE: severity: %s ", issue.getIssue().severity()));
                        LOGGER.trace(String.format("ISSUE: componentKey: %s ", issue.getIssue().componentKey()));
                        LOGGER.trace(String.format("ISSUE: getLocations: %s ", Objects.requireNonNull(issue.getIssue().getLocations()).toString()));
                        LOGGER.trace(String.format("ISSUE: getRuleKey: %s ", issue.getIssue().getRuleKey()));
                        LOGGER.trace(String.format("COMPONENT: getDescription: %s ", issue.getComponent().getDescription()));
                        DbIssues.Locations locate = Objects.requireNonNull(issue.getIssue().getLocations());
                        boolean isExitsThread = false;
                        for (CommentThread azureThread : azureCommentThreads) {
                            LOGGER.trace(String.format("azureFilePath: %s", azureThread.getThreadContext().getFilePath()));
                            LOGGER.trace(String.format("filePath: %s (%s)", filePath, azureThread.getThreadContext().getFilePath().equals(filePath)));
                            LOGGER.trace(String.format("azureLine: %d", azureThread.getThreadContext().getRightFileStart().getLine()));
                            LOGGER.trace(String.format("line: %d (%s)", line, azureThread.getThreadContext().getRightFileStart().getLine() == locate.getTextRange().getEndLine()));

                            if (azureThread.getThreadContext().getFilePath().equals(filePath)
                                    && azureThread.getComments()
                                    .stream()
                                    .filter(c -> c.getContent().contains(issue.getIssue().key()))
                                    .count() > 0 ) {

                                if(!issue.getIssue().getStatus().equals(Issue.STATUS_OPEN)
                                        && azureThread.getStatus().equals(CommentThreadStatus.ACTIVE)) {
                                    Comment comment = new Comment("Closed in SonarQube");
                                    LOGGER.info("Issue closed in Sonar. try close in Azure");
                                    sendPost(
                                            azureThread.getLinks().getSelf().getHref() + "/comments" + getApiVersion(),
                                            new ObjectMapper().writeValueAsString(comment),
                                            "Comment added success"
                                    );
                                    sendPatch(
                                            azureThread.getLinks().getSelf().getHref() + getApiVersion(),
                                            "{\"status\":\"closed\"}"
                                    );
                                }
                                isExitsThread = true;
                                break;
                            }
                        }
                        if (!issue.getIssue().getStatus().equals(Issue.STATUS_OPEN)) {
                            LOGGER.info(String.format("SKIPPED ISSUE: Issue status is %s", issue.getIssue().getStatus()));
                            continue;
                        }

                        if (isExitsThread || !issue.getIssue().getStatus().equals(Issue.STATUS_OPEN)) {
                            LOGGER.info(String.format("SKIPPED ISSUE: %s"
                                            + System.lineSeparator()
                                            + "File: %s"
                                            + System.lineSeparator()
                                            + "Line: %d"
                                            + System.lineSeparator()
                                            + "Issue is already exist in azure",
                                    issue.getIssue().getMessage(),
                                    filePath,
                                    line));
                            continue;
                        }

                        String message = String.format("%s: %s ([rule](%s))" + System.lineSeparator()
                                        + System.lineSeparator()
                                        + "[See in SonarQube](%s)",
                                issue.getIssue().type().name(),
                                issue.getIssue().getMessage(),
                                getRuleUrlWithRuleKey(issue.getIssue().getRuleKey().toString()),
                                getIssueUrl(
                                        analysisDetails.getAnalysisProjectKey(),
                                        issue.getIssue().key(),
                                        pullRequestId)
                        );

                        CommentThread thread = new CommentThread(filePath, locate, message);
                        LOGGER.info(String.format("Creating thread: %s ", new ObjectMapper().writeValueAsString(thread)));
                        sendPost(
                                getThreadApiUrl(),
                                new ObjectMapper().writeValueAsString(thread),
                                "Thread created successfully"
                        );
                    } catch (Exception e) {
                        LOGGER.error(e.toString());
                        throw new IllegalStateException(e); // Uncomment to run unit tests
                    }
                }
            }
            return DecorationResult.builder().withPullRequestUrl(getPullRequestUrl()).build();
        } catch (Exception ex) {
            throw new IllegalStateException("Could not decorate Pull Request on AzureDevOps Server", ex);
        }
    }

    private void setAuthorizationHeader(String apiToken) {
        String encodeBytes = Base64.getEncoder().encodeToString((":" + apiToken).getBytes());
        authorizationHeader = "Basic " + encodeBytes;
    }

    public String getIssueUrl(String sonarProjectKey, String issueKey, String pullRequestId) throws IOException {
        //ISSUE http://localhost/project/issues?id=ProjId&issues=AXCuh6CgT2BpyN1RPU03&open=AXCuh6CgT2BpyN1RPU03&pullRequest=8513
        return String.format(SONAR_ISSUE_URL_MASK,
                server.getPublicRootUrl(),
                URLEncoder.encode(sonarProjectKey, StandardCharsets.UTF_8.name()),
                URLEncoder.encode(issueKey, StandardCharsets.UTF_8.name()),
                URLEncoder.encode(issueKey, StandardCharsets.UTF_8.name()),
                URLEncoder.encode(pullRequestId, StandardCharsets.UTF_8.name())
        );
    }

    public String getRuleUrlWithRuleKey(String ruleKey) throws IOException {
        //RULE http://localhost/coding_rules?open=csharpsquid%3AS1135&rule_key=csharpsquid%3AS1135
        return String.format(SONAR_RULE_URL_MASK,
                server.getPublicRootUrl(),
                URLEncoder.encode(ruleKey, StandardCharsets.UTF_8.name()),
                URLEncoder.encode(ruleKey, StandardCharsets.UTF_8.name())
        );
    }

    private String getApiVersion() { 
        return API_VERSION_PREFIX + apiVersion; 
    }

    private String getPullRequestUrl() {
        // GET https://{instance}/{collection}/{project}/_apis/git/repositories/{repositoryId}/pullRequests/{pullRequestId}
        return azureUrl + azureProjectId +
                "/_apis/git/repositories/" +
                azureRepositoryName +
                "/pullRequests/" +
                pullRequestId;
    }

    private String getStatusApiUrl() {
        // POST https://{instance}/{project}/_apis/git/repositories/{repositoryId}/pullRequests/{pullRequestId}/statuses?api-version=6.0-preview.1
        return azureUrl + azureProjectId +
                "/_apis/git/repositories/" +
                azureRepositoryName +
                "/pullRequests/" +
                pullRequestId +
                "/statuses" +
                getApiVersion();
    }

    private String getThreadApiUrl() {
        //POST https://{instance}/{project}/_apis/git/repositories/{repositoryId}/pullRequests/{pullRequestId}/threads?api-version=6.0-preview.1
        return azureUrl + azureProjectId +
                "/_apis/git/repositories/" +
                azureRepositoryName +
                "/pullRequests/" +
                pullRequestId +
                "/threads" +
                getApiVersion();
    }

    private String getGitPullRequestStatus(AnalysisDetails analysisDetails) throws IOException {
        final String GIT_STATUS_CONTEXT_GENRE = "SonarQube";
        final String GIT_STATUS_CONTEXT_NAME = "QualityGate";
        final String GIT_STATUS_DESCRIPTION = "SonarQube Gate";

        GitPullRequestStatus status = new GitPullRequestStatus(
                GitStatusStateMapper.toGitStatusState(analysisDetails.getQualityGateStatus()),
                GIT_STATUS_DESCRIPTION,
                new GitStatusContext(GIT_STATUS_CONTEXT_GENRE, GIT_STATUS_CONTEXT_NAME), // "SonarQube/PullRequestDecoration",
                String.format("%s/dashboard?id=%s&pullRequest=%s", server.getPublicRootUrl(),
                        URLEncoder.encode(analysisDetails.getAnalysisProjectKey(),
                                StandardCharsets.UTF_8.name()),
                        URLEncoder.encode(analysisDetails.getBranchName(),
                                StandardCharsets.UTF_8.name())
                )
        );
        return new ObjectMapper().writeValueAsString(status);
    }

    private void sendPost(String apiUrl, String body, String successMessage) throws IOException, MalformedURLException, URISyntaxException {
        apiUrl = encodeURI(apiUrl);
        LOGGER.trace(String.format("sendPost: URL: %s ", apiUrl));
        LOGGER.trace(String.format("sendPost: BODY: %s ", body));
        HttpPost httpPost = new HttpPost(apiUrl);
        httpPost.addHeader("Accept", "application/json");
        httpPost.addHeader("Content-Type", "application/json; charset=utf-8");
        httpPost.addHeader("Authorization", authorizationHeader);
        StringEntity entity = new StringEntity(body, StandardCharsets.UTF_8);
        httpPost.setEntity(entity);

        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpResponse httpResponse = httpClient.execute(httpPost);
            if (null != httpResponse && httpResponse.getStatusLine().getStatusCode() != 200) {
                LOGGER.error("sendPost: " + httpResponse.toString());
                LOGGER.error("sendPost: " + EntityUtils.toString(httpResponse.getEntity(), StandardCharsets.UTF_8));
                throw new IllegalStateException("An error was returned in the response from the Azure DevOps server. See the previous log messages for details");
            } else if (null != httpResponse) {
                LOGGER.debug("sendPost: " + httpResponse.toString());
                LOGGER.info("sendPost: " + successMessage);
            }
        }
    }

    private <T> T sendGet(String apiUrl, Class<T> type) throws IOException, MalformedURLException, URISyntaxException {
        apiUrl = encodeURI(apiUrl);
        LOGGER.info(String.format("sendGet: URL: %s ", apiUrl));
        HttpGet httpGet = new HttpGet(apiUrl);
        httpGet.addHeader("Accept", "application/json");
        httpGet.addHeader("Content-Type", "application/json; charset=utf-8");
        httpGet.addHeader("Authorization", authorizationHeader);

        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpResponse httpResponse = httpClient.execute(httpGet);
            if (null != httpResponse && httpResponse.getStatusLine().getStatusCode() != 200) {
                LOGGER.error(httpResponse.toString());
                LOGGER.error(EntityUtils.toString(httpResponse.getEntity(), StandardCharsets.UTF_8));
                throw new IllegalStateException("An error was returned in the response from the Azure DevOps server. See the previous log messages for details");
            } else if (null != httpResponse) {
                //LOGGER.info(httpResponse.toString());
                HttpEntity entity = httpResponse.getEntity();
                T obj = new ObjectMapper()
                        .configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true)
                        .configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS, true)
                        .configure(DeserializationFeature.ACCEPT_EMPTY_ARRAY_AS_NULL_OBJECT, true)
                        .configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true)
                        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                        .readValue(IOUtils.toString(entity.getContent(), StandardCharsets.UTF_8), type);

                LOGGER.info(type + " received");

                return obj;
            } else {
                throw new IOException("No response reveived");
            }
        }
    }

    private void sendPatch(String apiUrl, String body) throws IOException, MalformedURLException, URISyntaxException {
        apiUrl = encodeURI(apiUrl);
        LOGGER.trace(String.format("sendPatch: URL: %s ", apiUrl));
        LOGGER.trace(String.format("sendPatch: BODY: %s ", body));
        HttpPatch httpPatch = new HttpPatch(apiUrl);
        httpPatch.addHeader("Accept", "application/json");
        httpPatch.addHeader("Content-Type", "application/json; charset=utf-8");
        httpPatch.addHeader("Authorization", authorizationHeader);
        StringEntity entity = new StringEntity(body, StandardCharsets.UTF_8);
        httpPatch.setEntity(entity);

        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpResponse httpResponse = httpClient.execute(httpPatch);
            if (null != httpResponse && httpResponse.getStatusLine().getStatusCode() != 200) {
                LOGGER.error("sendPatch: " + httpResponse.toString());
                LOGGER.error("sendPatch: " + EntityUtils.toString(httpResponse.getEntity(), StandardCharsets.UTF_8));
                throw new IllegalStateException("An error was returned in the response from the Azure DevOps server. See the previous log messages for details");
            } else if (null != httpResponse) {
                LOGGER.debug("sendPatch: " + httpResponse.toString());
                LOGGER.info("sendPatch: Patch success!");
            }
        }
    }

    private Optional<String> ensureTrailingSlash(Optional<String> uri) {
        if (uri != null && uri.isPresent()) {
            return Optional.of(uri.get().endsWith("/") ? uri.get() : uri.get() + "/");
        }
        return Optional.empty();
    }

    private String encodeURI(String uri) throws MalformedURLException, URISyntaxException {
        URL url = new URL(uri);
        return (new URI(url.getProtocol(), url.getUserInfo(), url.getHost(), url.getPort(), url.getPath(), url.getQuery(), url.getRef())).toString();
    }
    
    public String name() {
        return "Azure";
    }

    @Override
    public ALM alm() {
        return ALM.AZURE_DEVOPS;
    }
}
