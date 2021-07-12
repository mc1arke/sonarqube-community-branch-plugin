package com.github.mc1arke.sonarqube.plugin.ce.pullrequest.azuredevops;

import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.AnalysisDetails;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.DecorationResult;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.PostAnalysisIssueVisitor;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.ce.posttask.QualityGate;
import org.sonar.api.issue.Issue;
import org.sonar.api.platform.Server;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rules.RuleType;
import org.sonar.ce.task.projectanalysis.component.Component;
import org.sonar.db.alm.setting.ALM;
import org.sonar.db.alm.setting.AlmSettingDto;
import org.sonar.db.alm.setting.ProjectAlmSettingDto;
import org.sonar.db.protobuf.DbIssues;

import java.util.Collections;
import java.util.Optional;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.patch;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


public class AzureDevOpsPullRequestDecoratorTest {

    @Rule
    public final WireMockRule wireMockRule = new WireMockRule(wireMockConfig());

    private final String azureProject = "azure Project";
    private final String sonarProject = "sonarProject";
    private final String pullRequestId = "8513";
    private final String azureRepository = "my Repository";
    private final String sonarRootUrl = "http://sonar:9000/sonar";
    private final String filePath = "path/to/file";
    private final String issueMessage = "issueMessage";
    private final String issueKeyVal = "issueKeyVal";
    private final String ruleKeyVal = "ruleKeyVal";
    private final String threadId = "1468";
    private final String issueUrl = "http://sonar:9000/sonar/project/issues?id=sonarProject&pullRequest=8513&issues=issueKeyVal&open=issueKeyVal";
    private final String ruleUrl = "http://sonar:9000/sonar/coding_rules?open=ruleKeyVal&rule_key=ruleKeyVal";
    private final int lineNumber = 5;
    private final String token = "token";
    private final String authHeader = "Basic OnRva2Vu";

    private ProjectAlmSettingDto projectAlmSettingDto = mock(ProjectAlmSettingDto.class);
    private AlmSettingDto almSettingDto = mock(AlmSettingDto.class);
    private Server server = mock(Server.class);
    private AzureDevOpsPullRequestDecorator pullRequestDecorator = new AzureDevOpsPullRequestDecorator(server, new DefaultAzureDevopsClientFactory());
    private AnalysisDetails analysisDetails = mock(AnalysisDetails.class);

    private PostAnalysisIssueVisitor issueVisitor = mock(PostAnalysisIssueVisitor.class);
    private PostAnalysisIssueVisitor.ComponentIssue componentIssue = mock(PostAnalysisIssueVisitor.ComponentIssue.class);
    private PostAnalysisIssueVisitor.LightIssue defaultIssue = mock(PostAnalysisIssueVisitor.LightIssue.class);
    private Component component = mock(Component.class);

    private void configureTestDefaults() {
        when(almSettingDto.getPersonalAccessToken()).thenReturn(token);
        when(almSettingDto.getUrl()).thenReturn(wireMockRule.baseUrl());

        when(analysisDetails.getAnalysisProjectKey()).thenReturn(sonarProject);
        when(analysisDetails.getQualityGateStatus()).thenReturn(QualityGate.Status.OK);
        when(analysisDetails.getBranchName()).thenReturn(pullRequestId);
        when(analysisDetails.getPostAnalysisIssueVisitor()).thenReturn(issueVisitor);
        when(analysisDetails.getRuleUrlWithRuleKey(ruleKeyVal)).thenReturn(ruleUrl);
        when(analysisDetails.getIssueUrl(defaultIssue)).thenReturn(issueUrl);
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

        when(projectAlmSettingDto.getAlmSlug()).thenReturn(azureProject);
        when(projectAlmSettingDto.getAlmRepo()).thenReturn(azureRepository);

        setupStubs();
    }

    private void setupStubs() {
        wireMockRule.stubFor(get(urlEqualTo("/azure+Project/_apis/git/repositories/my+Repository/pullRequests/"+ pullRequestId +"/threads?api-version=4.1"))
                .withHeader("Accept", equalTo("application/json"))
                .withHeader("Authorization", equalTo(authHeader))
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
                        "            \"id\": " + threadId + "," + System.lineSeparator() +
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

        wireMockRule.stubFor(get(urlEqualTo("/azure+Project/_apis/git/repositories/my+Repository/pullRequests/" + pullRequestId +"?api-version=4.1"))
                .withHeader("Accept", equalTo("application/json"))
                .withHeader("Authorization", equalTo(authHeader))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody("{" + System.lineSeparator() +
                                "  \"repository\": {" + System.lineSeparator() +
                                "    \"id\": \"3411ebc1-d5aa-464f-9615-0b527bc66719\"," + System.lineSeparator() +
                                "    \"name\": \"" + azureRepository + "\"," + System.lineSeparator() +
                                "    \"url\": \"https://dev.azure.com/fabrikam/_apis/git/repositories/3411ebc1-d5aa-464f-9615-0b527bc66719\"," + System.lineSeparator() +
                                "    \"project\": {" + System.lineSeparator() +
                                "      \"id\": \"a7573007-bbb3-4341-b726-0c4148a07853\"," + System.lineSeparator() +
                                "      \"name\": \"" + azureProject + "\"," + System.lineSeparator() +
                                "      \"description\": \"test project created on Halloween 2016\"," + System.lineSeparator() +
                                "      \"url\": \"https://dev.azure.com/fabrikam/_apis/projects/a7573007-bbb3-4341-b726-0c4148a07853\"," + System.lineSeparator() +
                                "      \"state\": \"wellFormed\"," + System.lineSeparator() +
                                "      \"revision\": 7" + System.lineSeparator() +
                                "    }," + System.lineSeparator() +
                                "    \"remoteUrl\": \"http://localhost:8080/" + azureProject + "/_git/" + azureRepository + "\"" + System.lineSeparator() +
                                "  }," + System.lineSeparator() +
                                "  \"pullRequestId\": " + pullRequestId + "," + System.lineSeparator() +
                                "  \"codeReviewId\": " + pullRequestId + "," + System.lineSeparator() +
                                "  \"status\": \"active\"," + System.lineSeparator() +
                                "  \"createdBy\": {" + System.lineSeparator() +
                                "    \"id\": \"d6245f20-2af8-44f4-9451-8107cb2767db\"," + System.lineSeparator() +
                                "    \"displayName\": \"Normal Paulk\"," + System.lineSeparator() +
                                "    \"uniqueName\": \"fabrikamfiber16@hotmail.com\"," + System.lineSeparator() +
                                "    \"url\": \"https://dev.azure.com/fabrikam/_apis/Identities/d6245f20-2af8-44f4-9451-8107cb2767db\"," + System.lineSeparator() +
                                "    \"imageUrl\": \"https://dev.azure.com/fabrikam/_api/_common/identityImage?id=d6245f20-2af8-44f4-9451-8107cb2767db\"" + System.lineSeparator() +
                                "  }," + System.lineSeparator() +
                                "  \"creationDate\": \"2016-11-01T16:30:31.6655471Z\"," + System.lineSeparator() +
                                "  \"title\": \"A new feature\"," + System.lineSeparator() +
                                "  \"description\": \"Adding a new feature\"," + System.lineSeparator() +
                                "  \"sourceRefName\": \"refs/heads/npaulk/my_work\"," + System.lineSeparator() +
                                "  \"targetRefName\": \"refs/heads/new_feature\"," + System.lineSeparator() +
                                "  \"mergeStatus\": \"succeeded\"," + System.lineSeparator() +
                                "  \"mergeId\": \"f5fc8381-3fb2-49fe-8a0d-27dcc2d6ef82\"," + System.lineSeparator() +
                                "  \"lastMergeSourceCommit\": {" + System.lineSeparator() +
                                "    \"commitId\": \"b60280bc6e62e2f880f1b63c1e24987664d3bda3\"," + System.lineSeparator() +
                                "    \"url\": \"https://dev.azure.com/fabrikam/_apis/git/repositories/3411ebc1-d5aa-464f-9615-0b527bc66719/commits/b60280bc6e62e2f880f1b63c1e24987664d3bda3\"" + System.lineSeparator() +
                                "  }," + System.lineSeparator() +
                                "  \"lastMergeTargetCommit\": {" + System.lineSeparator() +
                                "    \"commitId\": \"f47bbc106853afe3c1b07a81754bce5f4b8dbf62\"," + System.lineSeparator() +
                                "    \"url\": \"https://dev.azure.com/fabrikam/_apis/git/repositories/3411ebc1-d5aa-464f-9615-0b527bc66719/commits/f47bbc106853afe3c1b07a81754bce5f4b8dbf62\"" + System.lineSeparator() +
                                "  }," + System.lineSeparator() +
                                "  \"lastMergeCommit\": {" + System.lineSeparator() +
                                "    \"commitId\": \"39f52d24533cc712fc845ed9fd1b6c06b3942588\"," + System.lineSeparator() +
                                "    \"author\": {" + System.lineSeparator() +
                                "      \"name\": \"Normal Paulk\"," + System.lineSeparator() +
                                "      \"email\": \"fabrikamfiber16@hotmail.com\"," + System.lineSeparator() +
                                "      \"date\": \"2016-11-01T16:30:32Z\"" + System.lineSeparator() +
                                "    }," + System.lineSeparator() +
                                "    \"committer\": {" + System.lineSeparator() +
                                "      \"name\": \"Normal Paulk\"," + System.lineSeparator() +
                                "      \"email\": \"fabrikamfiber16@hotmail.com\"," + System.lineSeparator() +
                                "      \"date\": \"2016-11-01T16:30:32Z\"" + System.lineSeparator() +
                                "    }," + System.lineSeparator() +
                                "    \"comment\": \"Merge pull request 22 from npaulk/my_work into new_feature\"," + System.lineSeparator() +
                                "    \"url\": \"https://dev.azure.com/fabrikam/_apis/git/repositories/3411ebc1-d5aa-464f-9615-0b527bc66719/commits/39f52d24533cc712fc845ed9fd1b6c06b3942588\"" + System.lineSeparator() +
                                "  }," + System.lineSeparator() +
                                "  \"reviewers\": [" + System.lineSeparator() +
                                "    {" + System.lineSeparator() +
                                "      \"reviewerUrl\": \"https://dev.azure.com/fabrikam/_apis/git/repositories/3411ebc1-d5aa-464f-9615-0b527bc66719/pullRequests/22/reviewers/d6245f20-2af8-44f4-9451-8107cb2767db\"," + System.lineSeparator() +
                                "      \"vote\": 0," + System.lineSeparator() +
                                "      \"id\": \"d6245f20-2af8-44f4-9451-8107cb2767db\"," + System.lineSeparator() +
                                "      \"displayName\": \"Normal Paulk\"," + System.lineSeparator() +
                                "      \"uniqueName\": \"fabrikamfiber16@hotmail.com\"," + System.lineSeparator() +
                                "      \"url\": \"https://dev.azure.com/fabrikam/_apis/Identities/d6245f20-2af8-44f4-9451-8107cb2767db\"," + System.lineSeparator() +
                                "      \"imageUrl\": \"https://dev.azure.com/fabrikam/_api/_common/identityImage?id=d6245f20-2af8-44f4-9451-8107cb2767db\"" + System.lineSeparator() +
                                "    }" + System.lineSeparator() +
                                "  ]," + System.lineSeparator() +
                                "  \"url\": \"https://dev.azure.com/fabrikam/_apis/git/repositories/3411ebc1-d5aa-464f-9615-0b527bc66719/pullRequests/22\"," + System.lineSeparator() +
                                "  \"_links\": {" + System.lineSeparator() +
                                "    \"self\": {" + System.lineSeparator() +
                                "      \"href\": \"https://dev.azure.com/fabrikam/_apis/git/repositories/3411ebc1-d5aa-464f-9615-0b527bc66719/pullRequests/22\"" + System.lineSeparator() +
                                "    }," + System.lineSeparator() +
                                "    \"repository\": {" + System.lineSeparator() +
                                "      \"href\": \"https://dev.azure.com/fabrikam/_apis/git/repositories/3411ebc1-d5aa-464f-9615-0b527bc66719\"" + System.lineSeparator() +
                                "    }," + System.lineSeparator() +
                                "    \"workItems\": {" + System.lineSeparator() +
                                "      \"href\": \"https://dev.azure.com/fabrikam/_apis/git/repositories/3411ebc1-d5aa-464f-9615-0b527bc66719/pullRequests/22/workitems\"" + System.lineSeparator() +
                                "    }," + System.lineSeparator() +
                                "    \"sourceBranch\": {" + System.lineSeparator() +
                                "      \"href\": \"https://dev.azure.com/fabrikam/_apis/git/repositories/3411ebc1-d5aa-464f-9615-0b527bc66719/refs\"" + System.lineSeparator() +
                                "    }," + System.lineSeparator() +
                                "    \"targetBranch\": {" + System.lineSeparator() +
                                "      \"href\": \"https://dev.azure.com/fabrikam/_apis/git/repositories/3411ebc1-d5aa-464f-9615-0b527bc66719/refs\"" + System.lineSeparator() +
                                "    }," + System.lineSeparator() +
                                "    \"sourceCommit\": {" + System.lineSeparator() +
                                "      \"href\": \"https://dev.azure.com/fabrikam/_apis/git/repositories/3411ebc1-d5aa-464f-9615-0b527bc66719/commits/b60280bc6e62e2f880f1b63c1e24987664d3bda3\"" + System.lineSeparator() +
                                "    }," + System.lineSeparator() +
                                "    \"targetCommit\": {" + System.lineSeparator() +
                                "      \"href\": \"https://dev.azure.com/fabrikam/_apis/git/repositories/3411ebc1-d5aa-464f-9615-0b527bc66719/commits/f47bbc106853afe3c1b07a81754bce5f4b8dbf62\"" + System.lineSeparator() +
                                "    }," + System.lineSeparator() +
                                "    \"createdBy\": {" + System.lineSeparator() +
                                "      \"href\": \"https://dev.azure.com/fabrikam/_apis/Identities/d6245f20-2af8-44f4-9451-8107cb2767db\"" + System.lineSeparator() +
                                "    }," + System.lineSeparator() +
                                "    \"iterations\": {" + System.lineSeparator() +
                                "      \"href\": \"https://dev.azure.com/fabrikam/_apis/git/repositories/3411ebc1-d5aa-464f-9615-0b527bc66719/pullRequests/22/iterations\"" + System.lineSeparator() +
                                "    }" + System.lineSeparator() +
                                "  }," + System.lineSeparator() +
                                "  \"supportsIterations\": true," + System.lineSeparator() +
                                "  \"artifactId\": \"vstfs:///Git/PullRequestId/a7573007-bbb3-4341-b726-0c4148a07853%2f3411ebc1-d5aa-464f-9615-0b527bc66719%2f22\"" + System.lineSeparator() +
                                "}")));

        wireMockRule.stubFor(post(urlEqualTo("/azure+Project/_apis/git/repositories/my+Repository/pullRequests/" + pullRequestId + "/threads/" + threadId + "/comments?api-version=4.1"))
                .withHeader("Accept", equalTo("application/json"))
                .withHeader("Content-Type", equalTo("application/json; charset=UTF-8"))
                .withHeader("Authorization", equalTo(authHeader))
                .withRequestBody(equalTo("{\"content\":\"Issue has been closed in SonarQube\"}")
                )
                .willReturn(ok()));


        wireMockRule.stubFor(post(urlEqualTo("/azure+Project/_apis/git/repositories/my+Repository/pullRequests/"+ pullRequestId +"/statuses?api-version=4.1"))
                .withHeader("Accept", equalTo("application/json"))
                .withHeader("Content-Type", equalTo("application/json; charset=UTF-8"))
                .withHeader("Authorization", equalTo(authHeader))
                .withRequestBody(equalTo("{" +
                        "\"state\":\"SUCCEEDED\"," +
                        "\"description\":\"SonarQube Gate\"," +
                        "\"context\":{\"genre\":\"SonarQube\",\"name\":\"QualityGate\"}," +
                        "\"targetUrl\":\"" + sonarRootUrl + "/dashboard?id=" + sonarProject + "&pullRequest=" + pullRequestId + "\"" +
                        "}")
                )
                .willReturn(ok()));

        wireMockRule.stubFor(patch(urlEqualTo("/azure+Project/_apis/git/repositories/my+Repository/pullRequests/" + pullRequestId + "/threads/" + threadId + "?api-version=4.1"))
                .withHeader("Accept", equalTo("application/json"))
                .withHeader("Content-Type", equalTo("application/json; charset=UTF-8"))
                .withHeader("Authorization", equalTo(authHeader))
                .withRequestBody(equalTo("{" +
                        "\"status\":\"closed\"" +
                        "}")
                )
                .willReturn(ok()));
    }

    @Test
    public void testName() {
        assertThat(new AzureDevOpsPullRequestDecorator(mock(Server.class), mock(AzureDevopsClientFactory.class)).alm()).isEqualTo(Collections.singletonList(ALM.AZURE_DEVOPS));
    }

    @Test
    public void testDecorateQualityGateRepoNameException() {
        when(almSettingDto.getUrl()).thenReturn("almUrl");
        when(almSettingDto.getPersonalAccessToken()).thenReturn("personalAccessToken");
        when(analysisDetails.getBranchName()).thenReturn("123");
        when(projectAlmSettingDto.getAlmSlug()).thenReturn("prj");

        assertThatThrownBy(() -> pullRequestDecorator.decorateQualityGateStatus(analysisDetails, almSettingDto, projectAlmSettingDto))
                .hasMessage("Repository name must be provided")
                .isExactlyInstanceOf(IllegalStateException.class);
    }

    @Test
    public void testDecorateQualityGateRepoSlugException() {
        when(almSettingDto.getUrl()).thenReturn("almUrl");
        when(almSettingDto.getPersonalAccessToken()).thenReturn("personalAccessToken");
        when(analysisDetails.getBranchName()).thenReturn("123");
        when(projectAlmSettingDto.getAlmRepo()).thenReturn("repo");

        assertThatThrownBy(() -> pullRequestDecorator.decorateQualityGateStatus(analysisDetails, almSettingDto, projectAlmSettingDto))
                .hasMessage("Repository slug must be provided")
                .isExactlyInstanceOf(IllegalStateException.class);
    }

    @Test
    public void testDecorateQualityGateProjectIDException() {
        when(almSettingDto.getUrl()).thenReturn("almUrl");
        when(almSettingDto.getPersonalAccessToken()).thenReturn("personalAccessToken");
        when(projectAlmSettingDto.getAlmRepo()).thenReturn("repo");
        when(projectAlmSettingDto.getAlmSlug()).thenReturn("slug");

        assertThatThrownBy(() -> pullRequestDecorator.decorateQualityGateStatus(analysisDetails, almSettingDto, projectAlmSettingDto))
                .hasMessage("Could not parse Pull Request Key")
                .isExactlyInstanceOf(IllegalStateException.class);
    }

    @Test
    public void testDecorateQualityGatePRBranchException() {
        when(almSettingDto.getUrl()).thenReturn("almUrl");
        when(almSettingDto.getPersonalAccessToken()).thenReturn("personalAccessToken");
        when(analysisDetails.getBranchName()).thenReturn("NON-NUMERIC");
        when(projectAlmSettingDto.getAlmSlug()).thenReturn("prj");
        when(projectAlmSettingDto.getAlmRepo()).thenReturn("repo");

        assertThatThrownBy(() -> pullRequestDecorator.decorateQualityGateStatus(analysisDetails, almSettingDto, projectAlmSettingDto))
                .hasMessage("Could not parse Pull Request Key")
                .isExactlyInstanceOf(IllegalStateException.class);
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
