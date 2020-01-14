/*
 * Copyright (C) 2019 Markus Heberling
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
package com.github.mc1arke.sonarqube.plugin.ce.pullrequest.gitlab;

import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.AnalysisDetails;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.PostAnalysisIssueVisitor;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mockito;
import org.sonar.api.ce.posttask.QualityGate;
import org.sonar.api.issue.Issue;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.platform.Server;
import org.sonar.ce.task.projectanalysis.component.Component;
import org.sonar.ce.task.projectanalysis.scm.Changeset;
import org.sonar.ce.task.projectanalysis.scm.ScmInfo;
import org.sonar.ce.task.projectanalysis.scm.ScmInfoRepository;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.db.alm.setting.AlmSettingDto;
import org.sonar.db.alm.setting.ProjectAlmSettingDto;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Optional;

import static com.github.tomakehurst.wiremock.client.WireMock.created;
import static com.github.tomakehurst.wiremock.client.WireMock.delete;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.noContent;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class GitlabServerPullRequestDecoratorTest {

    @Rule
    public final WireMockRule wireMockRule = new WireMockRule(wireMockConfig());

    @Test
    public void decorateQualityGateStatus() {
        String user = "sonar_user";
        String repositorySlug = "repo/slug";
        String commitSHA = "commitSHA";
        String branchName = "1";
        String projectKey = "projectKey";
        String sonarRootUrl = "http://sonar:9000/sonar";
        String discussionId = "6a9c1750b37d513a43987b574953fceb50b03ce7";
        String noteId = "1126";
        String filePath = "/path/to/file";
        int lineNumber = 5;

        QualityGate.Condition coverage = mock(QualityGate.Condition.class);
        when(coverage.getStatus()).thenReturn(QualityGate.EvaluationStatus.OK);
        when(coverage.getValue()).thenReturn("10");

        ProjectAlmSettingDto projectAlmSettingDto = mock(ProjectAlmSettingDto.class);
        AlmSettingDto almSettingDto = mock(AlmSettingDto.class);
        when(almSettingDto.getPersonalAccessToken()).thenReturn("token");

        AnalysisDetails analysisDetails = mock(AnalysisDetails.class);
        when(analysisDetails.getScannerProperty(eq(GitlabServerPullRequestDecorator.PULLREQUEST_GITLAB_INSTANCE_URL)))
                .thenReturn(Optional.of(wireMockRule.baseUrl()+"/api/v4"));
        when(analysisDetails
                     .getScannerProperty(eq(GitlabServerPullRequestDecorator.PULLREQUEST_GITLAB_PROJECT_ID)))
                .thenReturn(Optional.of(repositorySlug));
        when(analysisDetails.getAnalysisProjectKey()).thenReturn(projectKey);
        when(analysisDetails.getBranchName()).thenReturn(branchName);
        when(analysisDetails.getCommitSha()).thenReturn(commitSHA);
        when(analysisDetails.findQualityGateCondition(CoreMetrics.NEW_COVERAGE_KEY)).thenReturn(Optional.of(coverage));
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
        when(analysisDetails.createAnalysisSummary(Mockito.any())).thenReturn("summary");
        when(analysisDetails.createAnalysisIssueSummary(Mockito.any(), Mockito.any())).thenReturn("issue");
        when(analysisDetails.getSCMPathForIssue(componentIssue)).thenReturn(Optional.of(filePath));

        ScmInfoRepository scmInfoRepository = mock(ScmInfoRepository.class);
        ScmInfo scmInfo = mock(ScmInfo.class);
        when(scmInfo.hasChangesetForLine(anyInt())).thenReturn(true);
        when(scmInfo.getChangesetForLine(anyInt())).thenReturn(Changeset.newChangesetBuilder().setDate(0L).setRevision(commitSHA).build());
        when(scmInfoRepository.getScmInfo(component)).thenReturn(Optional.of(scmInfo));
        wireMockRule.stubFor(get(urlPathEqualTo("/api/v4/user")).withHeader("PRIVATE-TOKEN", equalTo("token")).willReturn(okJson("{\n" +
                "  \"id\": 1,\n" +
                "  \"username\": \"" + user + "\"}")));

        wireMockRule.stubFor(get(urlPathEqualTo("/api/v4/projects/" + urlEncode(repositorySlug) + "/merge_requests/" + branchName)).willReturn(okJson("{\n" +
                "  \"id\": 15235,\n" +
                "  \"iid\": " + branchName + ",\n" +
                "  \"diff_refs\": {\n" +
                "    \"base_sha\":\"d6a420d043dfe85e7c240fd136fc6e197998b10a\",\n" +
                "    \"head_sha\":\"" + commitSHA + "\",\n" +
                "    \"start_sha\":\"d6a420d043dfe85e7c240fd136fc6e197998b10a\"}\n" +
                "}")));

        wireMockRule.stubFor(get(urlPathEqualTo("/api/v4/projects/" + urlEncode(repositorySlug) + "/merge_requests/" + branchName + "/commits")).willReturn(okJson("[\n" +
                "  {\n" +
                "    \"id\": \"" + commitSHA + "\"\n" +
                "  }]")));

        wireMockRule.stubFor(get(urlPathEqualTo("/api/v4/projects/" + urlEncode(repositorySlug) + "/merge_requests/" + branchName + "/discussions")).willReturn(okJson("[\n" +
                "  {\n" +
                "    \"id\": \"" + discussionId + "\",\n" +
                "    \"individual_note\": false,\n" +
                "    \"notes\": [\n" +
                "      {\n" +
                "        \"id\": " + noteId + ",\n" +
                "        \"type\": \"DiscussionNote\",\n" +
                "        \"body\": \"discussion text\",\n" +
                "        \"attachment\": null,\n" +
                "        \"author\": {\n" +
                "          \"id\": 1,\n" +
                "          \"username\": \"" + user + "\"\n" +
                "        }}]}]")));

        wireMockRule.stubFor(delete(urlPathEqualTo("/api/v4/projects/" + urlEncode(repositorySlug) + "/merge_requests/" + branchName + "/discussions/" + discussionId + "/notes/" + noteId)).willReturn(noContent()));

        wireMockRule.stubFor(post(urlPathEqualTo("/api/v4/projects/" + urlEncode(repositorySlug) + "/statuses/" + commitSHA))
                .withQueryParam("name", equalTo("SonarQube"))
                .withQueryParam("state", equalTo("failed"))
                .withQueryParam("target_url", equalTo(sonarRootUrl + "/dashboard?id=" + projectKey + "&pullRequest=" + branchName))
                .withQueryParam("coverage", equalTo(coverage.getValue()))
                .willReturn(created()));

        wireMockRule.stubFor(post(urlPathEqualTo("/api/v4/projects/" + urlEncode(repositorySlug) + "/merge_requests/" + branchName + "/discussions"))
                .withRequestBody(equalTo("body=summary"))
                .willReturn(created()));

        wireMockRule.stubFor(post(urlPathEqualTo("/api/v4/projects/" + urlEncode(repositorySlug) + "/merge_requests/" + branchName + "/discussions"))
                .withRequestBody(equalTo("body=issue&" +
                        urlEncode("position[base_sha]") + "=d6a420d043dfe85e7c240fd136fc6e197998b10a&" +
                        urlEncode("position[start_sha]") + "=d6a420d043dfe85e7c240fd136fc6e197998b10a&" +
                        urlEncode("position[head_sha]") + "=" + commitSHA + "&" +
                        urlEncode("position[old_path]") + "=" + urlEncode(filePath) + "&" +
                        urlEncode("position[new_path]") + "=" + urlEncode(filePath) + "&" +
                        urlEncode("position[new_line]") + "=" + lineNumber + "&" +
                        urlEncode("position[position_type]") + "=text"))
                .willReturn(created()));

        Server server = mock(Server.class);
        when(server.getPublicRootUrl()).thenReturn(sonarRootUrl);
        GitlabServerPullRequestDecorator pullRequestDecorator =
                new GitlabServerPullRequestDecorator(server, scmInfoRepository);


        pullRequestDecorator.decorateQualityGateStatus(analysisDetails, almSettingDto, projectAlmSettingDto);
    }

    private static String urlEncode(String value) {
        try {
            return URLEncoder.encode(value, StandardCharsets.UTF_8.name());
        } catch (UnsupportedEncodingException e) {
            throw new Error("No support for UTF-8!", e);
        }
    }
}
