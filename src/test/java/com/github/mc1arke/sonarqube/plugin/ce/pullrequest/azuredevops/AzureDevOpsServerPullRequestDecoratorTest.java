package com.github.mc1arke.sonarqube.plugin.ce.pullrequest.azuredevops;

import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.azuredevops.model.AzurePullRequestDetails;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.AnalysisDetails;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.DecorationResult;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.PostAnalysisIssueVisitor;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.ce.posttask.QualityGate;
import org.sonar.api.issue.Issue;
import org.sonar.api.platform.Server;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rules.RuleType;
import org.sonar.ce.task.projectanalysis.component.Component;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.db.alm.setting.ALM;
import org.sonar.db.alm.setting.AlmSettingDto;
import org.sonar.db.alm.setting.ProjectAlmSettingDto;
import org.sonar.db.protobuf.DbIssues;

import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.patch;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import com.github.tomakehurst.wiremock.junit.WireMockRule;


public class AzureDevOpsServerPullRequestDecoratorTest {
    
    @Rule
    public WireMockRule wireMockRule = new WireMockRule(wireMockConfig());

    private final String apiVersion = "5.1-preview.1";
    private final String azureProject = "azure Project";
    private final String sonarProject = "sonarProject";
    private final String pullRequestId = "8513";
    private final String baseBranchName = "master";
    private final String branchName = "feature/some-feature";
    private final String azureRepository = "my Repository";
    private final String sonarRootUrl = "http://sonar:9000/sonar";
    private final String filePath = "path/to/file";
    private final String issueMessage = "issueMessage";
    private final String issueKeyVal = "issueKeyVal";
    private final String ruleKeyVal = "ruleKeyVal";
    private final String issueUrl = "http://sonar:9000/sonar/project/issues?id=sonarProject&pullRequest=8513&issues=issueKeyVal&open=issueKeyVal";
    private final String ruleUrl = "http://sonar:9000/sonar/coding_rules?open=ruleKeyVal&rule_key=ruleKeyVal";
    private final int lineNumber = 5;
    private final String token = "token";
    private final String authHeader = "Basic OnRva2Vu";

    private ProjectAlmSettingDto projectAlmSettingDto = mock(ProjectAlmSettingDto.class);
    private AlmSettingDto almSettingDto = mock(AlmSettingDto.class);
    private Server server = mock(Server.class);
    private AzureDevOpsServerPullRequestDecorator pullRequestDecorator = new AzureDevOpsServerPullRequestDecorator(server);
    private AnalysisDetails analysisDetails = mock(AnalysisDetails.class);

    private AzurePullRequestDetails azurePullRequestDetails = mock(AzurePullRequestDetails.class);        
    private PostAnalysisIssueVisitor issueVisitor = mock(PostAnalysisIssueVisitor.class);
    private PostAnalysisIssueVisitor.ComponentIssue componentIssue = mock(PostAnalysisIssueVisitor.ComponentIssue.class);
    private PostAnalysisIssueVisitor.LightIssue defaultIssue = mock(PostAnalysisIssueVisitor.LightIssue.class);
    private Component component = mock(Component.class);

    private void configureTestDefaults() {
        when(almSettingDto.getPersonalAccessToken()).thenReturn(token);

        when(azurePullRequestDetails.getAuthorizationHeader())
                .thenReturn(authHeader);
        when(azurePullRequestDetails.getApiVersion())
                .thenReturn(AzurePullRequestDetails.API_VERSION_PREFIX + apiVersion);
        when(analysisDetails.getScannerProperty(eq(AzureDevOpsServerPullRequestDecorator.PULLREQUEST_AZUREDEVOPS_INSTANCE_URL)))
                .thenReturn(Optional.of(wireMockRule.baseUrl()));
        when(analysisDetails.getScannerProperty(eq(AzureDevOpsServerPullRequestDecorator.PULLREQUEST_AZUREDEVOPS_REPOSITORY_NAME)))
                .thenReturn(Optional.of(azureRepository));
        when(analysisDetails.getBranchName())
                .thenReturn(pullRequestId);
        when(analysisDetails.getScannerProperty(eq(AzureDevOpsServerPullRequestDecorator.PULLREQUEST_AZUREDEVOPS_PROJECT_ID)))
                .thenReturn(Optional.of(azureProject));
        when(analysisDetails.getPullRequestBase())
                .thenReturn(Optional.of(baseBranchName));
        when(analysisDetails.getPullRequestBranch())
                .thenReturn(Optional.of(branchName));
        when(analysisDetails.getScannerProperty(eq(AzureDevOpsServerPullRequestDecorator.PULLREQUEST_AZUREDEVOPS_API_VERSION)))
                .thenReturn(Optional.of(apiVersion));
        when(analysisDetails.getAnalysisProjectKey()).thenReturn(sonarProject);
        when(analysisDetails.getQualityGateStatus()).thenReturn(QualityGate.Status.OK);
        when(analysisDetails.getBranchName()).thenReturn(pullRequestId);
        when(analysisDetails.getPostAnalysisIssueVisitor()).thenReturn(issueVisitor);
        when(analysisDetails.getRuleUrlWithRuleKey(ruleKeyVal)).thenReturn(ruleUrl);
        when(analysisDetails.getIssueUrl(issueKeyVal)).thenReturn(issueUrl);
        when(analysisDetails.getSCMPathForIssue(componentIssue)).thenReturn(Optional.of(filePath));
        when(issueVisitor.getIssues()).thenReturn(Collections.singletonList(componentIssue));

        DbIssues.Locations locate = DbIssues.Locations.newBuilder().build();
        RuleType rule = RuleType.CODE_SMELL;
        RuleKey ruleKey = mock(RuleKey.class);
        when(componentIssue.getIssue()).thenReturn(defaultIssue);
        when(componentIssue.getComponent()).thenReturn(component);
        when(defaultIssue.getStatus()).thenReturn(Issue.STATUS_OPEN);
        when(defaultIssue.getLine()).thenReturn(lineNumber);
        when(defaultIssue.getLocations()).thenReturn(locate);
        when(defaultIssue.type()).thenReturn(rule);
        when(defaultIssue.getMessage()).thenReturn(issueMessage);
        when(defaultIssue.getRuleKey()).thenReturn(ruleKey);
        when(defaultIssue.key()).thenReturn(issueKeyVal);
        when(ruleKey.toString()).thenReturn(ruleKeyVal);
        when(server.getPublicRootUrl()).thenReturn(sonarRootUrl);

        setupStubs();
    }

    private void setupStubs() {
        wireMockRule.stubFor(get(urlEqualTo("/azure%20Project/_apis/git/repositories/my%20Repository/pullRequests/"+ pullRequestId +"/threads"+ azurePullRequestDetails.getApiVersion()))
                .withHeader("Accept", equalTo("application/json"))
                .withHeader("Content-Type", equalTo("application/json; charset=UTF-8"))
                .withHeader("Authorization", equalTo(azurePullRequestDetails.getAuthorizationHeader()))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody(
                        "{" + System.lineSeparator() +
                        "    \"value\": [" + System.lineSeparator() +
                        "        {" + System.lineSeparator() +
                        "            \"pullRequestThreadContext\": {" + System.lineSeparator() +
                        "                \"iterationContext\": {" + System.lineSeparator() +
                        "                    \"firstComparingIteration\": 1," + System.lineSeparator() +
                        "                    \"secondComparingIteration\": 1" + System.lineSeparator() +
                        "                }," + System.lineSeparator() +
                        "                \"changeTrackingId\": 4" + System.lineSeparator() +
                        "            }," + System.lineSeparator() +
                        "            \"id\": 80450," + System.lineSeparator() +
                        "            \"publishedDate\": \"2020-03-10T17:40:09.603Z\"," + System.lineSeparator() +
                        "            \"lastUpdatedDate\": \"2020-03-10T18:05:06.99Z\"," + System.lineSeparator() +
                        "            \"comments\": [" + System.lineSeparator() +
                        "                {" + System.lineSeparator() +
                        "                    \"id\": 1," + System.lineSeparator() +
                        "                    \"parentCommentId\": 0," + System.lineSeparator() +
                        "                    \"author\": {" + System.lineSeparator() +
                        "                        \"displayName\": \"More text\"," + System.lineSeparator() +
                        "                        \"url\": \"" + wireMockRule.baseUrl() + "/fabrikam/_apis/Identities/c27db56f-07a0-43ac-9725-d6666e8b66b5\"," + System.lineSeparator() +
                        "                        \"_links\": {" + System.lineSeparator() +
                        "                            \"avatar\": {" + System.lineSeparator() +
                        "                                \"href\": \"" + wireMockRule.baseUrl() + "/fabrikam/_apis/GraphProfile/MemberAvatars/win.Uy0xLTUtMjEtMzkwNzU4MjE0NC0yNDM3MzcyODg4LTE5Njg5NDAzMjgtMjIxNQ\"" + System.lineSeparator() +
                        "                            }" + System.lineSeparator() +
                        "                        }," + System.lineSeparator() +
                        "                        \"id\": \"c27db56f-07a0-43ac-9725-d6666e8b66b5\"," + System.lineSeparator() +
                        "                        \"uniqueName\": \"user@mail.ru\"," + System.lineSeparator() +
                        "                        \"imageUrl\": \"" + wireMockRule.baseUrl() + "/fabrikam/_api/_common/identityImage?id=c27db56f-07a0-43ac-9725-d6666e8b66b5\"," + System.lineSeparator() +
                        "                        \"descriptor\": \"win.Uy0xLTUtMjEtMzkwNzU4MjE0NC0yNDM3MzcyODg4LTE5Njg5NDAzMjgtMjIxNQ\"" + System.lineSeparator() +
                        "                    }," + System.lineSeparator() +
                        "                    \"content\": \"CODE_SMELL: Remove this unnecessary 'using'. ([rule](" + wireMockRule.baseUrl() + "/coding_rules?open=" + issueKeyVal + "&rule_key=" + issueKeyVal + "))\"," + System.lineSeparator() +
                        "                    \"publishedDate\": \"2020-03-10T17:40:09.603Z\"," + System.lineSeparator() +
                        "                    \"lastUpdatedDate\": \"2020-03-10T18:05:06.99Z\"," + System.lineSeparator() +
                        "                    \"lastContentUpdatedDate\": \"2020-03-10T18:05:06.99Z\"," + System.lineSeparator() +
                        "                    \"isDeleted\": false," + System.lineSeparator() +
                        "                    \"commentType\": \"text\"," + System.lineSeparator() +
                        "                    \"usersLiked\": []," + System.lineSeparator() +
                        "                    \"_links\": {" + System.lineSeparator() +
                        "                        \"self\": {" + System.lineSeparator() +
                        "                            \"href\": \"" + wireMockRule.baseUrl() + "/fabrikam/_apis/git/repositories/28afee9d-4e53-46b8-8deb-99ea20202b2b/pullRequests/8513/threads/80450/comments/1\"" + System.lineSeparator() +
                        "                        }" + System.lineSeparator() +
                        "                    }" + System.lineSeparator() +
                        "                }" + System.lineSeparator() +
                        "            ]," + System.lineSeparator() +
                        "            \"status\": \"active\"," + System.lineSeparator() +
                        "            \"threadContext\": {" + System.lineSeparator() +
                        "                \"filePath\": \"/" + filePath +"\"," + System.lineSeparator() +
                        "                \"rightFileStart\": {" + System.lineSeparator() +
                        "                    \"line\": 18," + System.lineSeparator() +
                        "                    \"offset\": 11" + System.lineSeparator() +
                        "                }," + System.lineSeparator() +
                        "                \"rightFileEnd\": {" + System.lineSeparator() +
                        "                    \"line\": 18," + System.lineSeparator() +
                        "                    \"offset\": 15" + System.lineSeparator() +
                        "                }" + System.lineSeparator() +
                        "            }," + System.lineSeparator() +
                        "            \"properties\": {}," + System.lineSeparator() +
                        "            \"identities\": null," + System.lineSeparator() +
                        "            \"isDeleted\": false," + System.lineSeparator() +
                        "            \"_links\": {" + System.lineSeparator() +
                        "                \"self\": {" + System.lineSeparator() +
                        "                    \"href\": \"" + wireMockRule.baseUrl() + "/fabrikam/_apis/git/repositories/28afee9d-4e53-46b8-8deb-99ea20202b2b/pullRequests/8513/threads/80450\"" + System.lineSeparator() +
                        "                }" + System.lineSeparator() +
                        "            }" + System.lineSeparator() +
                        "        }" + System.lineSeparator() +
                        "    ]," + System.lineSeparator() +
                        "    \"count\": 2" + System.lineSeparator() +
                        "}")));

        wireMockRule.stubFor(post(urlEqualTo("/fabrikam/_apis/git/repositories/28afee9d-4e53-46b8-8deb-99ea20202b2b/pullRequests/8513/threads/80450/comments"+ azurePullRequestDetails.getApiVersion()))
                .withHeader("Accept", equalTo("application/json"))
                .withHeader("Content-Type", equalTo("application/json; charset=UTF-8"))
                .withHeader("Authorization", equalTo(azurePullRequestDetails.getAuthorizationHeader()))
                .withRequestBody(equalTo("{" +
                        "\"content\":\"Issue has been closed in SonarQube\"," +
                        "\"commentType\":\"TEXT\"," +
                        "\"parentCommentId\":0," +
                        "\"id\":0," +
                        "\"threadId\":0," +
                        "\"author\":null," +
                        "\"publishedDate\":null," +
                        "\"lastUpdatedDate\":null," +
                        "\"lastContentUpdatedDate\":null," +
                        "\"isDeleted\":false," +
                        "\"usersLiked\":null," +
                        "\"_links\":null" +
                        "}")
                )
                .willReturn(ok()));


        wireMockRule.stubFor(post(urlEqualTo("/azure%20Project/_apis/git/repositories/my%20Repository/pullRequests/"+ pullRequestId +"/statuses"+ azurePullRequestDetails.getApiVersion()))
                .withHeader("Accept", equalTo("application/json"))
                .withHeader("Content-Type", equalTo("application/json; charset=UTF-8"))
                .withHeader("Authorization", equalTo(azurePullRequestDetails.getAuthorizationHeader()))
                .withRequestBody(equalTo("{" +
                        "\"state\":\"SUCCEEDED\"," +
                        "\"description\":\"SonarQube Gate\"," +
                        "\"context\":{\"genre\":\"SonarQube\",\"name\":\"QualityGate\"}," +
                        "\"targetUrl\":\"" + sonarRootUrl + "/dashboard?id=" + sonarProject + "&pullRequest=" + pullRequestId + "\"" +
                        "}")
                )
                .willReturn(ok()));

        wireMockRule.stubFor(patch(urlEqualTo("/fabrikam/_apis/git/repositories/28afee9d-4e53-46b8-8deb-99ea20202b2b/pullRequests/8513/threads/80450"+ azurePullRequestDetails.getApiVersion()))
                .withHeader("Accept", equalTo("application/json"))
                .withHeader("Content-Type", equalTo("application/json; charset=UTF-8"))
                .withHeader("Authorization", equalTo(azurePullRequestDetails.getAuthorizationHeader()))
                .withRequestBody(equalTo("{" +
                        "\"status\":\"closed\"" +
                        "}")
                )
                .willReturn(ok()));
    }

    @Test
    public void testName() {
        assertThat(new AzureDevOpsServerPullRequestDecorator(mock(Server.class)).alm()).isEqualTo(ALM.AZURE_DEVOPS);
    }

    @Test
    public void testDecorateQualityGateInstanceURLException() {
        Exception dummyException = new IllegalStateException(String.format("Could not decorate AzureDevOps pullRequest. '%s' has not been set in scanner properties", 
                AzureDevOpsServerPullRequestDecorator.PULLREQUEST_AZUREDEVOPS_INSTANCE_URL));
        when(analysisDetails.getScannerProperty(eq(AzureDevOpsServerPullRequestDecorator.PULLREQUEST_AZUREDEVOPS_REPOSITORY_NAME))).thenReturn(Optional.of("repo"));
        when(analysisDetails.getBranchName()).thenReturn("123");
        when(analysisDetails.getScannerProperty(eq(AzureDevOpsServerPullRequestDecorator.PULLREQUEST_AZUREDEVOPS_PROJECT_ID))).thenReturn(Optional.of("prj"));
        when(analysisDetails.getPullRequestBase()).thenReturn(Optional.of("master"));
        when(analysisDetails.getPullRequestBranch()).thenReturn(Optional.of("pr"));
        
        when(analysisDetails.getScannerProperty(AzureDevOpsServerPullRequestDecorator.PULLREQUEST_AZUREDEVOPS_INSTANCE_URL)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> pullRequestDecorator.decorateQualityGateStatus(analysisDetails, almSettingDto, projectAlmSettingDto))
                .hasMessage("Could not decorate Pull Request on AzureDevOps Server")
                .isExactlyInstanceOf(IllegalStateException.class).hasCause(dummyException);
    }

    @Test
    public void testDecorateQualityGateRepoNameException() {
        Exception dummyException = new IllegalStateException(String.format("Could not decorate AzureDevOps pullRequest. '%s' has not been set in scanner properties", 
                AzureDevOpsServerPullRequestDecorator.PULLREQUEST_AZUREDEVOPS_REPOSITORY_NAME));
        when(analysisDetails.getScannerProperty(eq(AzureDevOpsServerPullRequestDecorator.PULLREQUEST_AZUREDEVOPS_INSTANCE_URL))).thenReturn(Optional.of(wireMockRule.baseUrl()));
        when(analysisDetails.getBranchName()).thenReturn("123");
        when(analysisDetails.getScannerProperty(eq(AzureDevOpsServerPullRequestDecorator.PULLREQUEST_AZUREDEVOPS_PROJECT_ID))).thenReturn(Optional.of("prj"));
        when(analysisDetails.getPullRequestBase()).thenReturn(Optional.of("master"));
        when(analysisDetails.getPullRequestBranch()).thenReturn(Optional.of("pr"));
        
        when(analysisDetails.getScannerProperty(AzureDevOpsServerPullRequestDecorator.PULLREQUEST_AZUREDEVOPS_REPOSITORY_NAME)).thenReturn(Optional.empty());
        
        assertThatThrownBy(() -> pullRequestDecorator.decorateQualityGateStatus(analysisDetails, almSettingDto, projectAlmSettingDto))
                .hasMessage("Could not decorate Pull Request on AzureDevOps Server")
                .isExactlyInstanceOf(IllegalStateException.class).hasCause(dummyException);        
    }

    @Test
    public void testDecorateQualityGateProjectIDException() {
        Exception dummyException = new IllegalStateException(String.format("Could not decorate AzureDevOps pullRequest. '%s' has not been set in scanner properties", 
                AzureDevOpsServerPullRequestDecorator.PULLREQUEST_AZUREDEVOPS_PROJECT_ID));
        when(analysisDetails.getScannerProperty(eq(AzureDevOpsServerPullRequestDecorator.PULLREQUEST_AZUREDEVOPS_INSTANCE_URL))).thenReturn(Optional.of(wireMockRule.baseUrl()));
        when(analysisDetails.getScannerProperty(eq(AzureDevOpsServerPullRequestDecorator.PULLREQUEST_AZUREDEVOPS_REPOSITORY_NAME))).thenReturn(Optional.of("repo"));
        when(analysisDetails.getBranchName()).thenReturn("123");
        when(analysisDetails.getPullRequestBase()).thenReturn(Optional.of("master"));
        when(analysisDetails.getPullRequestBranch()).thenReturn(Optional.of("pr"));
        
        when(analysisDetails.getScannerProperty(AzureDevOpsServerPullRequestDecorator.PULLREQUEST_AZUREDEVOPS_PROJECT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> pullRequestDecorator.decorateQualityGateStatus(analysisDetails, almSettingDto, projectAlmSettingDto))
                .hasMessage("Could not decorate Pull Request on AzureDevOps Server")
                .isExactlyInstanceOf(IllegalStateException.class).hasCause(dummyException);        
    }

    @Test
    public void testDecorateQualityGatePRBranchException() {
        Exception dummyException = new IllegalStateException(String.format("Could not decorate AzureDevOps pullRequest. '%s' has not been set in scanner properties", 
                AnalysisDetails.SCANNERROPERTY_PULLREQUEST_BRANCH));
        when(analysisDetails.getScannerProperty(eq(AzureDevOpsServerPullRequestDecorator.PULLREQUEST_AZUREDEVOPS_INSTANCE_URL))).thenReturn(Optional.of(wireMockRule.baseUrl()));
        when(analysisDetails.getScannerProperty(eq(AzureDevOpsServerPullRequestDecorator.PULLREQUEST_AZUREDEVOPS_REPOSITORY_NAME))).thenReturn(Optional.of("repo"));
        when(analysisDetails.getBranchName()).thenReturn("123");
        when(analysisDetails.getScannerProperty(eq(AzureDevOpsServerPullRequestDecorator.PULLREQUEST_AZUREDEVOPS_PROJECT_ID))).thenReturn(Optional.of("prj"));
        when(analysisDetails.getPullRequestBase()).thenReturn(Optional.of("master"));
        
        when(analysisDetails.getPullRequestBranch()).thenReturn(Optional.empty());

        assertThatThrownBy(() -> pullRequestDecorator.decorateQualityGateStatus(analysisDetails, almSettingDto, projectAlmSettingDto))
                .hasMessage("Could not decorate Pull Request on AzureDevOps Server")
                .isExactlyInstanceOf(IllegalStateException.class).hasCause(dummyException);
    }

    @Test
    public void testDecorateQualityGatePRBaseException() {
        Exception dummyException = new IllegalStateException(String.format("Could not decorate AzureDevOps pullRequest. '%s' has not been set in scanner properties", 
                AnalysisDetails.SCANNERROPERTY_PULLREQUEST_BASE));
        when(analysisDetails.getScannerProperty(eq(AzureDevOpsServerPullRequestDecorator.PULLREQUEST_AZUREDEVOPS_INSTANCE_URL))).thenReturn(Optional.of(wireMockRule.baseUrl()));
        when(analysisDetails.getScannerProperty(eq(AzureDevOpsServerPullRequestDecorator.PULLREQUEST_AZUREDEVOPS_REPOSITORY_NAME))).thenReturn(Optional.of("repo"));
        when(analysisDetails.getBranchName()).thenReturn("123");
        when(analysisDetails.getScannerProperty(eq(AzureDevOpsServerPullRequestDecorator.PULLREQUEST_AZUREDEVOPS_PROJECT_ID))).thenReturn(Optional.of("prj"));
        when(analysisDetails.getPullRequestBranch()).thenReturn(Optional.of("pr"));        
        
        when(analysisDetails.getPullRequestBase()).thenReturn(Optional.empty());

        assertThatThrownBy(() -> pullRequestDecorator.decorateQualityGateStatus(analysisDetails, almSettingDto, projectAlmSettingDto))
                .hasMessage("Could not decorate Pull Request on AzureDevOps Server")
                .isExactlyInstanceOf(IllegalStateException.class).hasCause(dummyException);       
    }

    @Test
    public void testDecorateQualityGateAccessTokenException() {
        Exception dummyException = new IllegalStateException("Could not decorate AzureDevOps pullRequest. Access token has not been set");
        when(analysisDetails.getScannerProperty(eq(AzureDevOpsServerPullRequestDecorator.PULLREQUEST_AZUREDEVOPS_INSTANCE_URL))).thenReturn(Optional.of(wireMockRule.baseUrl()));
        when(analysisDetails.getScannerProperty(eq(AzureDevOpsServerPullRequestDecorator.PULLREQUEST_AZUREDEVOPS_REPOSITORY_NAME))).thenReturn(Optional.of("repo"));
        when(analysisDetails.getBranchName()).thenReturn("123");
        when(analysisDetails.getScannerProperty(eq(AzureDevOpsServerPullRequestDecorator.PULLREQUEST_AZUREDEVOPS_PROJECT_ID))).thenReturn(Optional.of("prj"));
        when(analysisDetails.getPullRequestBranch()).thenReturn(Optional.of("pr"));  
        when(analysisDetails.getPullRequestBase()).thenReturn(Optional.of("master"));
        
        when(almSettingDto.getPersonalAccessToken()).thenReturn(null);              
        
        assertThatThrownBy(() -> pullRequestDecorator.decorateQualityGateStatus(analysisDetails, almSettingDto, projectAlmSettingDto))
                .hasMessage("Could not decorate Pull Request on AzureDevOps Server")
                .isExactlyInstanceOf(IllegalStateException.class).hasCause(dummyException);       
    }

    @Test
    public void decorateQualityGateStatusNewIssue() {        
        configureTestDefaults();
        
        DecorationResult result = pullRequestDecorator.decorateQualityGateStatus(analysisDetails, almSettingDto, projectAlmSettingDto);
        assertThat(result.getPullRequestUrl()).isEqualTo(Optional.of(String.format("%s/%s/_git/%s/pullRequest/%s", wireMockRule.baseUrl(), azureProject, azureRepository, pullRequestId)));
    }

    @Test
    public void decorateQualityGateStatusClosedIssue() {        
        configureTestDefaults();

        when(defaultIssue.getStatus()).thenReturn(Issue.STATUS_CLOSED);
        when(defaultIssue.getLine()).thenReturn(18);

        DecorationResult result = pullRequestDecorator.decorateQualityGateStatus(analysisDetails, almSettingDto, projectAlmSettingDto);
        assertThat(result.getPullRequestUrl()).isEqualTo(Optional.of(String.format("%s/%s/_git/%s/pullRequest/%s", wireMockRule.baseUrl(), azureProject, azureRepository, pullRequestId)));
    }

}
