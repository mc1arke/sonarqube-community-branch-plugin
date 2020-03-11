package com.github.mc1arke.sonarqube.plugin.ce.pullrequest.azuredevops;

import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.AnalysisDetails;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.PostAnalysisIssueVisitor;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.ce.posttask.QualityGate;
import org.sonar.api.issue.Issue;
import org.sonar.api.platform.Server;
import org.sonar.ce.task.projectanalysis.component.Component;
import org.sonar.ce.task.projectanalysis.scm.ScmInfoRepository;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.db.alm.setting.AlmSettingDto;
import org.sonar.db.alm.setting.ProjectAlmSettingDto;

import java.util.Base64;
import java.util.Collections;
import java.util.Optional;


import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AzureDevOpsServerPullRequestDecoratorTest {
    @Rule
    public final WireMockRule wireMockRule = new WireMockRule(wireMockConfig());

    @Test
    public void testName() {
        assertEquals("Azure", new AzureDevOpsServerPullRequestDecorator( mock(Server.class), mock(ScmInfoRepository.class) ).name());
    }

    @Test
    public void decorateQualityGateStatus() {
        String azureProject = "azureProject";
        String sonarProject = "sonarProject";
        String pullRequestId = "8513";
        String baseBranchName = "master";
        String branchName = "feature/some-feature";
        String azureRepository = "myRepository";
        String sonarRootUrl = "http://sonar:9000/sonar";
        String filePath = "/path/to/file";
        int lineNumber = 5;
        String token = "token";
        String authorizationHeader = "Basic " + Base64.getEncoder().encodeToString((":" + token).getBytes());

        ProjectAlmSettingDto projectAlmSettingDto = mock(ProjectAlmSettingDto.class);
        AlmSettingDto almSettingDto = mock(AlmSettingDto.class);
        when(almSettingDto.getPersonalAccessToken()).thenReturn(token);

        AnalysisDetails analysisDetails = mock(AnalysisDetails.class);
        when(analysisDetails.getScannerProperty(eq(AzureDevOpsServerPullRequestDecorator.PULLREQUEST_AZUREDEVOPS_INSTANCE_URL)))
                .thenReturn(Optional.of(wireMockRule.baseUrl()));
        when(analysisDetails.getScannerProperty(eq(AzureDevOpsServerPullRequestDecorator.PULLREQUEST_AZUREDEVOPS_REPOSITORY_NAME)))
                .thenReturn(Optional.of(azureRepository));
        when(analysisDetails.getScannerProperty(eq(AzureDevOpsServerPullRequestDecorator.PULLREQUEST_AZUREDEVOPS_PULLREQUEST_ID)))
                .thenReturn(Optional.of(pullRequestId));
        when(analysisDetails.getScannerProperty(eq(AzureDevOpsServerPullRequestDecorator.PULLREQUEST_AZUREDEVOPS_PROJECT_ID)))
                .thenReturn(Optional.of(azureProject));
        when(analysisDetails.getScannerProperty(eq(AzureDevOpsServerPullRequestDecorator.PULLREQUEST_AZUREDEVOPS_BASE_BRANCH)))
                .thenReturn(Optional.of(baseBranchName));
        when(analysisDetails.getScannerProperty(eq(AzureDevOpsServerPullRequestDecorator.PULLREQUEST_AZUREDEVOPS_BRANCH)))
                .thenReturn(Optional.of(branchName));

        when(analysisDetails.getAnalysisProjectKey()).thenReturn(sonarProject);
        when(analysisDetails.getQualityGateStatus()).thenReturn(QualityGate.Status.OK);
        when(analysisDetails.getBranchName()).thenReturn(pullRequestId);


        PostAnalysisIssueVisitor issueVisitor = mock(PostAnalysisIssueVisitor.class);
        PostAnalysisIssueVisitor.ComponentIssue componentIssue = mock(PostAnalysisIssueVisitor.ComponentIssue.class);
        DefaultIssue defaultIssue = mock(DefaultIssue.class);
        when(defaultIssue.getStatus()).thenReturn(Issue.STATUS_OPEN);
        when(defaultIssue.getLine()).thenReturn(lineNumber);
        when(componentIssue.getIssue()).thenReturn(defaultIssue);

        Component component = mock(Component.class);
        when(componentIssue.getComponent()).thenReturn(component);
        when(issueVisitor.getIssues()).thenReturn(Collections.singletonList(componentIssue));
        when(analysisDetails.getPostAnalysisIssueVisitor()).thenReturn(issueVisitor);
        when(analysisDetails.getSCMPathForIssue(componentIssue)).thenReturn(Optional.of(filePath));

        ScmInfoRepository scmInfoRepository = mock(ScmInfoRepository.class);
        wireMockRule.stubFor(get(urlEqualTo("/_apis/git/repositories/"+ azureRepository +"/pullRequests/"+ pullRequestId +"/threads?api-version=5.0-preview.1"))
                .withHeader("Accept", equalTo("application/json"))
                .withHeader("Content-Type", equalTo("application/json; charset=UTF-8"))
                .withHeader("Authorization", equalTo(authorizationHeader))
                .willReturn(okJson("{\n" +
                        "    \"value\": [\n" +
                        "        {\n" +
                        "            \"pullRequestThreadContext\": {\n" +
                        "                \"iterationContext\": {\n" +
                        "                    \"firstComparingIteration\": 1,\n" +
                        "                    \"secondComparingIteration\": 1\n" +
                        "                },\n" +
                        "                \"changeTrackingId\": 4\n" +
                        "            },\n" +
                        "            \"id\": 80450,\n" +
                        "            \"publishedDate\": \"2020-03-10T17:40:09.603Z\",\n" +
                        "            \"lastUpdatedDate\": \"2020-03-10T18:05:06.99Z\",\n" +
                        "            \"comments\": [\n" +
                        "                {\n" +
                        "                    \"id\": 1,\n" +
                        "                    \"parentCommentId\": 0,\n" +
                        "                    \"author\": {\n" +
                        "                        \"displayName\": \"More text\",\n" +
                        "                        \"url\": \"https://dev.azure.com/fabrikam/_apis/Identities/c27db56f-07a0-43ac-9725-d6666e8b66b5\",\n" +
                        "                        \"_links\": {\n" +
                        "                            \"avatar\": {\n" +
                        "                                \"href\": \"https://dev.azure.com/fabrikam/_apis/GraphProfile/MemberAvatars/win.Uy0xLTUtMjEtMzkwNzU4MjE0NC0yNDM3MzcyODg4LTE5Njg5NDAzMjgtMjIxNQ\"\n" +
                        "                            }\n" +
                        "                        },\n" +
                        "                        \"id\": \"c27db56f-07a0-43ac-9725-d6666e8b66b5\",\n" +
                        "                        \"uniqueName\": \"user@mail.ru\",\n" +
                        "                        \"imageUrl\": \"https://dev.azure.com/fabrikam/_api/_common/identityImage?id=c27db56f-07a0-43ac-9725-d6666e8b66b5\",\n" +
                        "                        \"descriptor\": \"win.Uy0xLTUtMjEtMzkwNzU4MjE0NC0yNDM3MzcyODg4LTE5Njg5NDAzMjgtMjIxNQ\"\n" +
                        "                    },\n" +
                        "                    \"publishedDate\": \"2020-03-10T17:40:09.603Z\",\n" +
                        "                    \"lastUpdatedDate\": \"2020-03-10T18:05:06.99Z\",\n" +
                        "                    \"lastContentUpdatedDate\": \"2020-03-10T18:05:06.99Z\",\n" +
                        "                    \"isDeleted\": true,\n" +
                        "                    \"commentType\": \"text\",\n" +
                        "                    \"usersLiked\": [],\n" +
                        "                    \"_links\": {\n" +
                        "                        \"self\": {\n" +
                        "                            \"href\": \"https://dev.azure.com/fabrikam/_apis/git/repositories/28afee9d-4e53-46b8-8deb-99ea20202b2b/pullRequests/8513/threads/80450/comments/1\"\n" +
                        "                        }\n" +
                        "                    }\n" +
                        "                }\n" +
                        "            ],\n" +
                        "            \"status\": \"active\",\n" +
                        "            \"threadContext\": {\n" +
                        "                \"filePath\": \"/azureProject/myReposytory/Helpers/file.cs\",\n" +
                        "                \"rightFileStart\": {\n" +
                        "                    \"line\": 18,\n" +
                        "                    \"offset\": 11\n" +
                        "                },\n" +
                        "                \"rightFileEnd\": {\n" +
                        "                    \"line\": 18,\n" +
                        "                    \"offset\": 15\n" +
                        "                }\n" +
                        "            },\n" +
                        "            \"properties\": {},\n" +
                        "            \"identities\": null,\n" +
                        "            \"isDeleted\": true,\n" +
                        "            \"_links\": {\n" +
                        "                \"self\": {\n" +
                        "                    \"href\": \"https://dev.azure.com/fabrikam/_apis/git/repositories/28afee9d-4e53-46b8-8deb-99ea20202b2b/pullRequests/8513/threads/80450\"\n" +
                        "                }\n" +
                        "            }\n" +
                        "        },\n" +
                        "        {\n" +
                        "            \"pullRequestThreadContext\": {\n" +
                        "                \"iterationContext\": {\n" +
                        "                    \"firstComparingIteration\": 1,\n" +
                        "                    \"secondComparingIteration\": 1\n" +
                        "                },\n" +
                        "                \"changeTrackingId\": 13\n" +
                        "            },\n" +
                        "            \"id\": 80452,\n" +
                        "            \"publishedDate\": \"2020-03-10T19:06:11.37Z\",\n" +
                        "            \"lastUpdatedDate\": \"2020-03-10T19:06:11.37Z\",\n" +
                        "            \"comments\": [\n" +
                        "                {\n" +
                        "                    \"id\": 1,\n" +
                        "                    \"parentCommentId\": 0,\n" +
                        "                    \"author\": {\n" +
                        "                        \"displayName\": \"text\",\n" +
                        "                        \"url\": \"https://dev.azure.com/fabrikam/_apis/Identities/c27db56f-07a0-43ac-9725-d6666e8b66b5\",\n" +
                        "                        \"_links\": {\n" +
                        "                            \"avatar\": {\n" +
                        "                                \"href\": \"https://dev.azure.com/fabrikam/_apis/GraphProfile/MemberAvatars/win.Uy0xLTUtMjEtMzkwNzU4MjE0NC0yNDM3MzcyODg4LTE5Njg5NDAzMjgtMjIxNQ\"\n" +
                        "                            }\n" +
                        "                        },\n" +
                        "                        \"id\": \"c27db56f-07a0-43ac-9725-d6666e8b66b5\",\n" +
                        "                        \"uniqueName\": \"user@mail.ru\",\n" +
                        "                        \"imageUrl\": \"https://dev.azure.com/fabrikam/_api/_common/identityImage?id=c27db56f-07a0-43ac-9725-d6666e8b66b5\",\n" +
                        "                        \"descriptor\": \"win.Uy0xLTUtMjEtMzkwNzU4MjE0NC0yNDM3MzcyODg4LTE5Njg5NDAzMjgtMjIxNQ\"\n" +
                        "                    },\n" +
                        "                    \"content\": \"Comment\",\n" +
                        "                    \"publishedDate\": \"2020-03-10T19:06:11.37Z\",\n" +
                        "                    \"lastUpdatedDate\": \"2020-03-10T19:06:11.37Z\",\n" +
                        "                    \"lastContentUpdatedDate\": \"2020-03-10T19:06:11.37Z\",\n" +
                        "                    \"commentType\": \"text\",\n" +
                        "                    \"usersLiked\": [],\n" +
                        "                    \"_links\": {\n" +
                        "                        \"self\": {\n" +
                        "                            \"href\": \"https://dev.azure.com/fabrikam/_apis/git/repositories/28afee9d-4e53-46b8-8deb-99ea20202b2b/pullRequests/8513/threads/80452/comments/1\"\n" +
                        "                        }\n" +
                        "                    }\n" +
                        "                }\n" +
                        "            ],\n" +
                        "            \"status\": \"active\",\n" +
                        "            \"threadContext\": {\n" +
                        "                \"filePath\": \"" + filePath + "\",\n" +
                        "                \"rightFileStart\": {\n" +
                        "                    \"line\": 30,\n" +
                        "                    \"offset\": 57\n" +
                        "                },\n" +
                        "                \"rightFileEnd\": {\n" +
                        "                    \"line\": 30,\n" +
                        "                    \"offset\": 65\n" +
                        "                }\n" +
                        "            },\n" +
                        "            \"properties\": {},\n" +
                        "            \"identities\": null,\n" +
                        "            \"isDeleted\": false,\n" +
                        "            \"_links\": {\n" +
                        "                \"self\": {\n" +
                        "                    \"href\": \"https://dev.azure.com/fabrikam/_apis/git/repositories/28afee9d-4e53-46b8-8deb-99ea20202b2b/pullRequests/8513/threads/80452\"\n" +
                        "                }\n" +
                        "            }\n" +
                        "        }\n" +
                        "    ],\n" +
                        "    \"count\": 2\n" +
                        "}")));

        wireMockRule.stubFor(post(urlEqualTo("/_apis/git/repositories/"+ azureRepository +"/pullRequests/"+ pullRequestId +"/threads?api-version=5.0-preview.1"))
                .withHeader("Accept", equalTo("application/json"))
                .withHeader("Content-Type", equalTo("application/json; charset=UTF-8"))
                .withHeader("Authorization", equalTo(authorizationHeader))
                .withRequestBody(equalTo("{\n" +
                        "\t\"status\":\"active\",\n" +
                        "\t\"comments\":[\n" +
                        "\t\t{\n" +
                        "\t\t\t\"content\":\"Message\",\n" +
                        "\t\t\t\"commentType\":\"text\",\n" +
                        "\t\t\t\"parentCommentId\":0,\n" +
                        "\t\t\t\"id\":null,\n" +
                        "\t\t\t\"threadId\":null,\n" +
                        "\t\t\t\"author\":null,\n" +
                        "\t\t\t\"publishedDate\":null,\n" +
                        "\t\t\t\"lastUpdatedDate\":null,\n" +
                        "\t\t\t\"lastContentUpdatedDate\":null,\n" +
                        "\t\t\t\"isDeleted\":null,\n" +
                        "\t\t\t\"usersLiked\":null,\n" +
                        "\t\t\t\"links\":null\n" +
                        "\t\t}\n" +
                        "\t],\n" +
                        "\t\"threadContext\":{\n" +
                        "\t\t\"filePath\":\"" + filePath + "\",\n" +
                        "\t\t\"leftFileStart\":null,\n" +
                        "\t\t\"leftFileEnd\":null,\n" +
                        "\t\t\"rightFileStart\":{\n" +
                        "\t\t\t\"line\":3,\n" +
                        "\t\t\t\"offset\":6\n" +
                        "\t\t},\n" +
                        "\t\t\"rightFileEnd\":{\n" +
                        "\t\t\t\"line\":3,\n" +
                        "\t\t\t\"offset\":10}\n" +
                        "\t\t},\n" +
                        "\t\"id\":0,\n" +
                        "\t\"publishedDate\":null,\n" +
                        "\t\"lastUpdatedDate\":null,\n" +
                        "\t\"identities\":null,\n" +
                        "\t\"isDeleted\":null\n" +
                        "} ")
                )
                .willReturn(ok()));


        wireMockRule.stubFor(post(urlEqualTo("/_apis/git/repositories/"+ azureRepository +"/pullRequests/"+ pullRequestId +"/statuses?api-version=5.0-preview.1"))
                .withHeader("Accept", equalTo("application/json"))
                .withHeader("Content-Type", equalTo("application/json; charset=UTF-8"))
                .withHeader("Authorization", equalTo(authorizationHeader))
                .withRequestBody(equalTo("{" +
                        "\"iterationId\":null," +
                        "\"state\":\"Succeeded\"," +
                        "\"description\":\"SonarQube Gate\"," +
                        "\"context\":{\"name\":\"PullRequestDecoration\",\"genre\":\"SonarQube\"}," +
                        "\"targetUrl\":\"" + sonarRootUrl + "/dashboard?id=" + sonarProject + "&pullRequest=" + pullRequestId + "\"" +
                        "}")
                )
                .willReturn(ok()));

        Server server = mock(Server.class);
        when(server.getPublicRootUrl()).thenReturn(sonarRootUrl);
        AzureDevOpsServerPullRequestDecorator pullRequestDecorator =
                new AzureDevOpsServerPullRequestDecorator(server, scmInfoRepository);

        pullRequestDecorator.decorateQualityGateStatus(analysisDetails, almSettingDto, projectAlmSettingDto);
    }

}
