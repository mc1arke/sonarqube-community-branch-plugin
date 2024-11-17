/*
 * Copyright (C) 2024 Michael Clarke
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
package com.github.mc1arke.sonarqube.plugin.ce.pullrequest.azuredevops;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.patch;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sonar.api.ce.posttask.QualityGate;
import org.sonar.api.config.internal.Encryption;
import org.sonar.api.config.internal.Settings;
import org.sonar.api.issue.IssueStatus;
import org.sonar.api.rule.RuleKey;
import org.sonar.ce.task.projectanalysis.component.Component;
import org.sonar.ce.task.projectanalysis.scm.Changeset;
import org.sonar.ce.task.projectanalysis.scm.ScmInfo;
import org.sonar.ce.task.projectanalysis.scm.ScmInfoRepository;
import org.sonar.db.alm.setting.AlmSettingDto;
import org.sonar.db.alm.setting.ProjectAlmSettingDto;
import org.sonar.db.protobuf.DbIssues;

import com.github.mc1arke.sonarqube.plugin.almclient.azuredevops.DefaultAzureDevopsClientFactory;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.AnalysisDetails;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.DecorationResult;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.PostAnalysisIssueVisitor;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.markup.MarkdownFormatterFactory;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.report.AnalysisIssueSummary;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.report.AnalysisSummary;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.report.ReportGenerator;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;

class AzureDevOpsPullRequestDecoratorIntegrationTest {

    @RegisterExtension
    static final WireMockExtension wireMockExtension = WireMockExtension.newInstance()
        .failOnUnmatchedRequests(true)
        .build();

    private final String azureProject = "azure Project";
    private final String sonarProject = "sonarProject";
    private final int pullRequestId = 8513;
    private final String azureRepository = "my Repository";
    private final String issueKeyVal = "issueKeyVal";
    private final String projectName = "Project Name";

    private final ProjectAlmSettingDto projectAlmSettingDto = mock();
    private final AlmSettingDto almSettingDto = mock();
    private final ScmInfoRepository scmInfoRepository = mock();
    private final Settings settings = mock();
    private final Encryption encryption = mock();
    private final ReportGenerator reportGenerator = mock();
    private final MarkdownFormatterFactory formatterFactory = mock();
    private final AzureDevOpsPullRequestDecorator pullRequestDecorator = new AzureDevOpsPullRequestDecorator(scmInfoRepository, new DefaultAzureDevopsClientFactory(settings), reportGenerator, formatterFactory);
    private final AnalysisDetails analysisDetails = mock();

    private final PostAnalysisIssueVisitor.ComponentIssue componentIssue = mock();
    private final PostAnalysisIssueVisitor.LightIssue defaultIssue = mock();
    private final Component component = mock();

    @BeforeEach
    void setUp() {
        when(settings.getEncryption()).thenReturn(encryption);
        when(reportGenerator.createAnalysisIssueSummary(any(), any())).thenReturn(mock());
        when(reportGenerator.createAnalysisSummary(any())).thenReturn(mock());
    }

    private void configureTestDefaults() {
        String token = "token";
        when(almSettingDto.getDecryptedPersonalAccessToken(any())).thenReturn(token);
        when(almSettingDto.getUrl()).thenReturn(wireMockExtension.baseUrl());

        when(analysisDetails.getAnalysisProjectName()).thenReturn(projectName);
        when(analysisDetails.getAnalysisProjectKey()).thenReturn(sonarProject);
        when(analysisDetails.getQualityGateStatus()).thenReturn(QualityGate.Status.OK);
        when(analysisDetails.getPullRequestId()).thenReturn(Integer.toString(pullRequestId));
        when(analysisDetails.getIssues()).thenReturn(List.of(componentIssue));

        AnalysisSummary analysisSummary = mock();
        when(analysisSummary.format(any())).thenReturn("analysis summary");
        when(analysisSummary.getDashboardUrl()).thenReturn("http://sonar:9000/sonar/dashboard?id=" + sonarProject + "&pullRequest=" + pullRequestId);
        AnalysisIssueSummary analysisIssueSummary = mock();

        when(reportGenerator.createAnalysisSummary(any())).thenReturn(analysisSummary);
        when(reportGenerator.createAnalysisIssueSummary(any(), any())).thenReturn(analysisIssueSummary);

        DbIssues.Locations locate = DbIssues.Locations.newBuilder().build();
        RuleKey ruleKey = mock();
        when(componentIssue.getIssue()).thenReturn(defaultIssue);
        when(componentIssue.getComponent()).thenReturn(component);
        when(componentIssue.getScmPath()).thenReturn(Optional.of("scmPath"));
        when(defaultIssue.issueStatus()).thenReturn(IssueStatus.OPEN);
        int lineNumber = 5;
        when(defaultIssue.getLine()).thenReturn(lineNumber);
        when(defaultIssue.getLocations()).thenReturn(locate);
        String issueMessage = "issueMessage";
        when(defaultIssue.getMessage()).thenReturn(issueMessage);
        when(defaultIssue.getRuleKey()).thenReturn(ruleKey);
        when(defaultIssue.key()).thenReturn(issueKeyVal);
        Changeset changeset = mock();
        when(changeset.getRevision()).thenReturn("revisionId");
        ScmInfo scmInfo = mock();
        when(scmInfo.hasChangesetForLine(anyInt())).thenReturn(true);
        when(scmInfo.getChangesetForLine(anyInt())).thenReturn(changeset);
        when(scmInfoRepository.getScmInfo(component)).thenReturn(Optional.of(scmInfo));
        String ruleKeyVal = "ruleKeyVal";
        when(ruleKey.toString()).thenReturn(ruleKeyVal);

        when(projectAlmSettingDto.getAlmSlug()).thenReturn(azureProject);
        when(projectAlmSettingDto.getAlmRepo()).thenReturn(azureRepository);

        setupStubs();
    }

    private void setupStubs() {
        String filePath = "path/to/file";
        String threadId = "1468";
        String authorId = "author-id";
        String authHeader = "Basic OnRva2Vu";
        wireMockExtension.stubFor(get(urlEqualTo("/azure%20Project/_apis/git/repositories/my%20Repository/pullRequests/"+ pullRequestId +"/threads?api-version=4.1"))
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
                        "                        \"url\": \"" + wireMockExtension.baseUrl() + "/fabrikam/_apis/Identities/c27db56f-07a0-43ac-9725-d6666e8b66b5\"," + System.lineSeparator() +
                        "                        \"_links\": {" + System.lineSeparator() +
                        "                            \"avatar\": {" + System.lineSeparator() +
                        "                                \"href\": \"" + wireMockExtension.baseUrl() + "/fabrikam/_apis/GraphProfile/MemberAvatars/win.Uy0xLTUtMjEtMzkwNzU4MjE0NC0yNDM3MzcyODg4LTE5Njg5NDAzMjgtMjIxNQ\"" + System.lineSeparator() +
                        "                            }" + System.lineSeparator() +
                        "                        }," + System.lineSeparator() +
                        "                        \"id\": \"" + authorId + "\"," + System.lineSeparator() +
                        "                        \"uniqueName\": \"user@mail.ru\"," + System.lineSeparator() +
                        "                        \"imageUrl\": \"" + wireMockExtension.baseUrl() + "/fabrikam/_api/_common/identityImage?id=c27db56f-07a0-43ac-9725-d6666e8b66b5\"," + System.lineSeparator() +
                        "                        \"descriptor\": \"win.Uy0xLTUtMjEtMzkwNzU4MjE0NC0yNDM3MzcyODg4LTE5Njg5NDAzMjgtMjIxNQ\"" + System.lineSeparator() +
                        "                    }," + System.lineSeparator() +
                        "                    \"content\": \"CODE_SMELL: Remove this unnecessary 'using'. \\n[View in SonarQube](" + wireMockExtension.baseUrl() + "/coding_rules?open=" + issueKeyVal + "&rule_key=" + issueKeyVal + ")\"," + System.lineSeparator() +
                        "                    \"publishedDate\": \"2020-03-10T17:40:09.603Z\"," + System.lineSeparator() +
                        "                    \"lastUpdatedDate\": \"2020-03-10T18:05:06.99Z\"," + System.lineSeparator() +
                        "                    \"lastContentUpdatedDate\": \"2020-03-10T18:05:06.99Z\"," + System.lineSeparator() +
                        "                    \"isDeleted\": false," + System.lineSeparator() +
                        "                    \"commentType\": \"text\"," + System.lineSeparator() +
                        "                    \"usersLiked\": []," + System.lineSeparator() +
                        "                    \"_links\": {" + System.lineSeparator() +
                        "                        \"self\": {" + System.lineSeparator() +
                        "                            \"href\": \"" + wireMockExtension.baseUrl() + "/fabrikam/_apis/git/repositories/28afee9d-4e53-46b8-8deb-99ea20202b2b/pullRequests/8513/threads/80450/comments/1\"" + System.lineSeparator() +
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
                        "                    \"href\": \"" + wireMockExtension.baseUrl() + "/fabrikam/_apis/git/repositories/28afee9d-4e53-46b8-8deb-99ea20202b2b/pullRequests/8513/threads/80450\"" + System.lineSeparator() +
                        "                }" + System.lineSeparator() +
                        "            }" + System.lineSeparator() +
                        "        }" + System.lineSeparator() +
                        "    ]," + System.lineSeparator() +
                        "    \"count\": 2" + System.lineSeparator() +
                        "}")));

        wireMockExtension.stubFor(get(urlEqualTo("/azure%20Project/_apis/git/repositories/my%20Repository/pullRequests/" + pullRequestId +"?api-version=4.1"))
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
                                "    \"remoteUrl\": \"" + wireMockExtension.baseUrl() + "/" + azureProject + "/_git/" + azureRepository + "\"" + System.lineSeparator() +
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

        wireMockExtension.stubFor(post(urlEqualTo("/azure%20Project/_apis/git/repositories/my%20Repository/pullRequests/" + pullRequestId + "/threads/" + threadId + "/comments?api-version=4.1"))
                .withHeader("Accept", equalTo("application/json"))
                .withHeader("Content-Type", equalTo("application/json; charset=UTF-8"))
                .withHeader("Authorization", equalTo(authHeader))
                .withRequestBody(equalTo("{\"content\":\"Issue has been closed in SonarQube\"}")
                )
                .willReturn(ok()));

        wireMockExtension.stubFor(get(urlEqualTo("/azure%20Project/_apis/git/repositories/my%20Repository/pullRequests/" + pullRequestId + "/commits?api-version=4.1"))
                .withHeader("Accept", equalTo("application/json"))
                .withHeader("Authorization", equalTo(authHeader))
                .willReturn(aResponse().withStatus(200).withBody("{\"value\": [{" + System.lineSeparator() +
                        "  \"parents\": []," + System.lineSeparator() +
                        "  \"treeId\": \"7fa1a3523ffef51c525ea476bffff7d648b8cb3d\"," + System.lineSeparator() +
                        "  \"push\": {" + System.lineSeparator() +
                        "    \"pushedBy\": {" + System.lineSeparator() +
                        "      \"id\": \"8c8c7d32-6b1b-47f4-b2e9-30b477b5ab3d\"," + System.lineSeparator() +
                        "      \"displayName\": \"Chuck Reinhart\"," + System.lineSeparator() +
                        "      \"uniqueName\": \"fabrikamfiber3@hotmail.com\"," + System.lineSeparator() +
                        "      \"url\": \"https://vssps.dev.azure.com/fabrikam/_apis/Identities/8c8c7d32-6b1b-47f4-b2e9-30b477b5ab3d\"," + System.lineSeparator() +
                        "      \"imageUrl\": \"https://dev.azure.com/fabrikam/_api/_common/identityImage?id=8c8c7d32-6b1b-47f4-b2e9-30b477b5ab3d\"" + System.lineSeparator() +
                        "    }," + System.lineSeparator() +
                        "    \"pushId\": 1," + System.lineSeparator() +
                        "    \"date\": \"2014-01-29T23:33:15.2434002Z\"" + System.lineSeparator() +
                        "  }," + System.lineSeparator() +
                        "  \"commitId\": \"revisionId\"," + System.lineSeparator() +
                        "  \"author\": {" + System.lineSeparator() +
                        "    \"name\": \"Chuck Reinhart\"," + System.lineSeparator() +
                        "    \"email\": \"fabrikamfiber3@hotmail.com\"," + System.lineSeparator() +
                        "    \"date\": \"2014-01-29T23:32:09Z\"" + System.lineSeparator() +
                        "  }," + System.lineSeparator() +
                        "  \"committer\": {" + System.lineSeparator() +
                        "    \"name\": \"Chuck Reinhart\"," + System.lineSeparator() +
                        "    \"email\": \"fabrikamfiber3@hotmail.com\"," + System.lineSeparator() +
                        "    \"date\": \"2014-01-29T23:32:09Z\"" + System.lineSeparator() +
                        "  }," + System.lineSeparator() +
                        "  \"comment\": \"First cut\\n\"," + System.lineSeparator() +
                        "  \"url\": \"https://dev.azure.com/fabrikam/_apis/git/repositories/278d5cd2-584d-4b63-824a-2ba458937249/commits/be67f8871a4d2c75f13a51c1d3c30ac0d74d4ef4\"," + System.lineSeparator() +
                        "  \"remoteUrl\": \"https://dev.azure.com/fabrikam/_git/Fabrikam-Fiber-Git/commit/be67f8871a4d2c75f13a51c1d3c30ac0d74d4ef4\"," + System.lineSeparator() +
                        "  \"_links\": {" + System.lineSeparator() +
                        "    \"self\": {" + System.lineSeparator() +
                        "      \"href\": \"https://dev.azure.com/fabrikam/_apis/git/repositories/278d5cd2-584d-4b63-824a-2ba458937249/commits/be67f8871a4d2c75f13a51c1d3c30ac0d74d4ef4\"" + System.lineSeparator() +
                        "    }," + System.lineSeparator() +
                        "    \"repository\": {" + System.lineSeparator() +
                        "      \"href\": \"https://dev.azure.com/fabrikam/_apis/git/repositories/278d5cd2-584d-4b63-824a-2ba458937249\"" + System.lineSeparator() +
                        "    }," + System.lineSeparator() +
                        "    \"changes\": {" + System.lineSeparator() +
                        "      \"href\": \"https://dev.azure.com/fabrikam/_apis/git/repositories/278d5cd2-584d-4b63-824a-2ba458937249/commits/be67f8871a4d2c75f13a51c1d3c30ac0d74d4ef4/changes\"" + System.lineSeparator() +
                        "    }," + System.lineSeparator() +
                        "    \"web\": {" + System.lineSeparator() +
                        "      \"href\": \"https://dev.azure.com/fabrikam/_git/Fabrikam-Fiber-Git/commit/be67f8871a4d2c75f13a51c1d3c30ac0d74d4ef4\"" + System.lineSeparator() +
                        "    }," + System.lineSeparator() +
                        "    \"tree\": {" + System.lineSeparator() +
                        "      \"href\": \"https://dev.azure.com/fabrikam/_apis/git/repositories/278d5cd2-584d-4b63-824a-2ba458937249/trees/7fa1a3523ffef51c525ea476bffff7d648b8cb3d\"" + System.lineSeparator() +
                        "    }" + System.lineSeparator() +
                        "  }" + System.lineSeparator() +
                        "}]}")));


        String sonarRootUrl = "http://sonar:9000/sonar";
        wireMockExtension.stubFor(post(urlEqualTo("/azure%20Project/_apis/git/repositories/my%20Repository/pullRequests/"+ pullRequestId +"/statuses?api-version=4.1-preview"))
                .withHeader("Content-Type", equalTo("application/json; charset=UTF-8"))
                .withHeader("Authorization", equalTo(authHeader))
                .withRequestBody(equalTo("{" +
                        "\"state\":\"SUCCEEDED\"," +
                        "\"description\":\"SonarQube Quality Gate - " + projectName + " (" + sonarProject + ")\"," +
                        "\"context\":{\"genre\":\"sonarqube/qualitygate\",\"name\":\"" + sonarProject + "\"}," +
                        "\"targetUrl\":\"" + sonarRootUrl + "/dashboard?id=" + sonarProject + "&pullRequest=" + pullRequestId + "\"" +
                        "}")
                )
                .willReturn(ok()));

        wireMockExtension.stubFor(post(urlEqualTo("/azure%20Project/_apis/git/repositories/my%20Repository/pullRequests/"+ pullRequestId +"/threads?api-version=4.1"))
                .withHeader("Accept", equalTo("application/json"))
                .withHeader("Content-Type", equalTo("application/json; charset=UTF-8"))
                .withHeader("Authorization", equalTo(authHeader))
                .withRequestBody(equalTo("{\"comments\":[{\"content\":\"analysis summary\"}],\"status\":\"active\"}"))
                .willReturn(aResponse().withStatus(200).withBody("{" + System.lineSeparator() +
                        "  \"pullRequestThreadContext\": {" + System.lineSeparator() +
                        "    \"iterationContext\": {" + System.lineSeparator() +
                        "      \"firstComparingIteration\": 1," + System.lineSeparator() +
                        "      \"secondComparingIteration\": 2" + System.lineSeparator() +
                        "    }," + System.lineSeparator() +
                        "    \"changeTrackingId\": 1" + System.lineSeparator() +
                        "  }," + System.lineSeparator() +
                        "  \"id\": " + threadId + "," + System.lineSeparator() +
                        "  \"publishedDate\": \"2016-11-01T16:30:50.083Z\"," + System.lineSeparator() +
                        "  \"lastUpdatedDate\": \"2016-11-01T16:30:50.083Z\"," + System.lineSeparator() +
                        "  \"comments\": [" + System.lineSeparator() +
                        "    {" + System.lineSeparator() +
                        "      \"id\": 1," + System.lineSeparator() +
                        "      \"parentCommentId\": 0," + System.lineSeparator() +
                        "      \"author\": {" + System.lineSeparator() +
                        "        \"id\": \"d6245f20-2af8-44f4-9451-8107cb2767db\"," + System.lineSeparator() +
                        "        \"displayName\": \"Normal Paulk\"," + System.lineSeparator() +
                        "        \"uniqueName\": \"fabrikamfiber16@hotmail.com\"," + System.lineSeparator() +
                        "        \"url\": \"https://dev.azure.com/fabrikam/_apis/Identities/d6245f20-2af8-44f4-9451-8107cb2767db\"," + System.lineSeparator() +
                        "        \"imageUrl\": \"https://dev.azure.com/fabrikam/_api/_common/identityImage?id=d6245f20-2af8-44f4-9451-8107cb2767db\"" + System.lineSeparator() +
                        "      }," + System.lineSeparator() +
                        "      \"content\": \"Should we add a comment about what this value means?\"," + System.lineSeparator() +
                        "      \"publishedDate\": \"2016-11-01T16:30:50.083Z\"," + System.lineSeparator() +
                        "      \"lastUpdatedDate\": \"2016-11-01T16:30:50.083Z\"," + System.lineSeparator() +
                        "      \"commentType\": \"text\"" + System.lineSeparator() +
                        "    }" + System.lineSeparator() +
                        "  ]," + System.lineSeparator() +
                        "  \"status\": \"active\"," + System.lineSeparator() +
                        "  \"threadContext\": {" + System.lineSeparator() +
                        "    \"filePath\": \"/new_feature.cpp\"," + System.lineSeparator() +
                        "    \"rightFileStart\": {" + System.lineSeparator() +
                        "      \"line\": 5," + System.lineSeparator() +
                        "      \"offset\": 1" + System.lineSeparator() +
                        "    }," + System.lineSeparator() +
                        "    \"rightFileEnd\": {" + System.lineSeparator() +
                        "      \"line\": 5," + System.lineSeparator() +
                        "      \"offset\": 13" + System.lineSeparator() +
                        "    }" + System.lineSeparator() +
                        "  }," + System.lineSeparator() +
                        "  \"properties\": {}," + System.lineSeparator() +
                        "  \"isDeleted\": false," + System.lineSeparator() +
                        "  \"_links\": {" + System.lineSeparator() +
                        "    \"self\": {" + System.lineSeparator() +
                        "      \"href\": \"https://dev.azure.com/fabrikam/_apis/git/repositories/3411ebc1-d5aa-464f-9615-0b527bc66719/pullRequests/22/threads/148\"" + System.lineSeparator() +
                        "    }," + System.lineSeparator() +
                        "    \"repository\": {" + System.lineSeparator() +
                        "      \"href\": \"https://dev.azure.com/fabrikam/_apis/git/repositories/3411ebc1-d5aa-464f-9615-0b527bc66719\"" + System.lineSeparator() +
                        "    }" + System.lineSeparator() +
                        "  }" + System.lineSeparator() +
                        "}")));

        wireMockExtension.stubFor(post(urlEqualTo("/azure%20Project/_apis/git/repositories/my%20Repository/pullRequests/"+ pullRequestId +"/threads?api-version=4.1"))
                .withHeader("Accept", equalTo("application/json"))
                .withHeader("Content-Type", equalTo("application/json; charset=UTF-8"))
                .withHeader("Authorization", equalTo(authHeader))
                .withRequestBody(equalTo("{\"threadContext\":{\"filePath\":\"/scmPath\",\"rightFileStart\":{\"line\":0,\"offset\":1},\"rightFileEnd\":{\"line\":0,\"offset\":1}},\"comments\":[{\"content\":\"issue summary\"}],\"status\":\"active\"}"))
                .willReturn(aResponse().withStatus(200).withBody("{" + System.lineSeparator() +
                        "  \"pullRequestThreadContext\": {" + System.lineSeparator() +
                        "    \"iterationContext\": {" + System.lineSeparator() +
                        "      \"firstComparingIteration\": 1," + System.lineSeparator() +
                        "      \"secondComparingIteration\": 2" + System.lineSeparator() +
                        "    }," + System.lineSeparator() +
                        "    \"changeTrackingId\": 1" + System.lineSeparator() +
                        "  }," + System.lineSeparator() +
                        "  \"id\": " + threadId + "," + System.lineSeparator() +
                        "  \"publishedDate\": \"2016-11-01T16:30:50.083Z\"," + System.lineSeparator() +
                        "  \"lastUpdatedDate\": \"2016-11-01T16:30:50.083Z\"," + System.lineSeparator() +
                        "  \"comments\": [" + System.lineSeparator() +
                        "    {" + System.lineSeparator() +
                        "      \"id\": 1," + System.lineSeparator() +
                        "      \"parentCommentId\": 0," + System.lineSeparator() +
                        "      \"author\": {" + System.lineSeparator() +
                        "        \"id\": \"d6245f20-2af8-44f4-9451-8107cb2767db\"," + System.lineSeparator() +
                        "        \"displayName\": \"Normal Paulk\"," + System.lineSeparator() +
                        "        \"uniqueName\": \"fabrikamfiber16@hotmail.com\"," + System.lineSeparator() +
                        "        \"url\": \"https://dev.azure.com/fabrikam/_apis/Identities/d6245f20-2af8-44f4-9451-8107cb2767db\"," + System.lineSeparator() +
                        "        \"imageUrl\": \"https://dev.azure.com/fabrikam/_api/_common/identityImage?id=d6245f20-2af8-44f4-9451-8107cb2767db\"" + System.lineSeparator() +
                        "      }," + System.lineSeparator() +
                        "      \"content\": \"Should we add a comment about what this value means?\"," + System.lineSeparator() +
                        "      \"publishedDate\": \"2016-11-01T16:30:50.083Z\"," + System.lineSeparator() +
                        "      \"lastUpdatedDate\": \"2016-11-01T16:30:50.083Z\"," + System.lineSeparator() +
                        "      \"commentType\": \"text\"" + System.lineSeparator() +
                        "    }" + System.lineSeparator() +
                        "  ]," + System.lineSeparator() +
                        "  \"status\": \"active\"," + System.lineSeparator() +
                        "  \"threadContext\": {" + System.lineSeparator() +
                        "    \"filePath\": \"/new_feature.cpp\"," + System.lineSeparator() +
                        "    \"rightFileStart\": {" + System.lineSeparator() +
                        "      \"line\": 5," + System.lineSeparator() +
                        "      \"offset\": 1" + System.lineSeparator() +
                        "    }," + System.lineSeparator() +
                        "    \"rightFileEnd\": {" + System.lineSeparator() +
                        "      \"line\": 5," + System.lineSeparator() +
                        "      \"offset\": 13" + System.lineSeparator() +
                        "    }" + System.lineSeparator() +
                        "  }," + System.lineSeparator() +
                        "  \"properties\": {}," + System.lineSeparator() +
                        "  \"isDeleted\": false," + System.lineSeparator() +
                        "  \"_links\": {" + System.lineSeparator() +
                        "    \"self\": {" + System.lineSeparator() +
                        "      \"href\": \"https://dev.azure.com/fabrikam/_apis/git/repositories/3411ebc1-d5aa-464f-9615-0b527bc66719/pullRequests/22/threads/148\"" + System.lineSeparator() +
                        "    }," + System.lineSeparator() +
                        "    \"repository\": {" + System.lineSeparator() +
                        "      \"href\": \"https://dev.azure.com/fabrikam/_apis/git/repositories/3411ebc1-d5aa-464f-9615-0b527bc66719\"" + System.lineSeparator() +
                        "    }" + System.lineSeparator() +
                        "  }" + System.lineSeparator() +
                        "}")));

        wireMockExtension.stubFor(patch(urlEqualTo("/azure%20Project/_apis/git/repositories/my%20Repository/pullRequests/" + pullRequestId + "/threads/" + threadId + "?api-version=4.1"))
                .withHeader("Content-Type", equalTo("application/json; charset=UTF-8"))
                .withHeader("Authorization", equalTo(authHeader))
                .withRequestBody(equalTo("{" +
                        "\"status\":\"closed\"" +
                        "}")
                )
                .willReturn(ok()));

        wireMockExtension.stubFor(get(urlEqualTo("/_apis/ConnectionData?api-version=4.1-preview"))
                .withHeader("Accept", equalTo("application/json"))
                .withHeader("Authorization", equalTo(authHeader))
                .willReturn(aResponse().withStatus(200).withBody("{\"authenticatedUser\": {" + System.lineSeparator() +
                        "    \"id\": \"d6245f20-2af8-44f4-9451-8107cb2767db\"," + System.lineSeparator() +
                        "    \"providerDisplayName\": \"Test User\"," + System.lineSeparator() +
                        "    \"customDisplayName\": \"Test User\"," + System.lineSeparator() +
                        "    \"emailAddress\": \"test.user@mail.domain\"" + System.lineSeparator() +
                        "  }" + System.lineSeparator() +
                        "}")));
    }

    @Test
    void decorateQualityGateStatusNewIssue() {
        configureTestDefaults();

        DecorationResult result = pullRequestDecorator.decorateQualityGateStatus(analysisDetails, almSettingDto, projectAlmSettingDto);
        assertThat(result.getPullRequestUrl()).isEqualTo(Optional.of(String.format("%s/%s/_git/%s/pullRequest/%s", wireMockExtension.getRuntimeInfo().getHttpBaseUrl(), azureProject, azureRepository, pullRequestId)));
    }

}
