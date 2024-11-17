/*
 * Copyright (C) 2019-2024 Markus Heberling, Michael Clarke
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

import static com.github.tomakehurst.wiremock.client.WireMock.created;
import static com.github.tomakehurst.wiremock.client.WireMock.delete;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.noContent;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.put;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sonar.api.ce.posttask.QualityGate;
import org.sonar.api.config.internal.Encryption;
import org.sonar.api.config.internal.Settings;
import org.sonar.api.issue.IssueStatus;
import org.sonar.ce.task.projectanalysis.component.Component;
import org.sonar.ce.task.projectanalysis.scm.Changeset;
import org.sonar.ce.task.projectanalysis.scm.ScmInfo;
import org.sonar.ce.task.projectanalysis.scm.ScmInfoRepository;
import org.sonar.db.alm.setting.AlmSettingDto;
import org.sonar.db.alm.setting.ProjectAlmSettingDto;

import com.github.mc1arke.sonarqube.plugin.almclient.gitlab.DefaultGitlabClientFactory;
import com.github.mc1arke.sonarqube.plugin.almclient.gitlab.LinkHeaderReader;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.AnalysisDetails;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.PostAnalysisIssueVisitor;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.report.AnalysisIssueSummary;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.report.AnalysisSummary;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.report.ReportGenerator;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;

class GitlabMergeRequestDecoratorIntegrationTest {

    @RegisterExtension
    static WireMockExtension wireMockExtension = WireMockExtension.extensionOptions()
        .failOnUnmatchedRequests(true)
        .build();

    @Test
    void decorateQualityGateStatusOk() {
        decorateQualityGateStatus(QualityGate.Status.OK);
    }

    @Test
    void decorateQualityGateStatusError() {
        decorateQualityGateStatus(QualityGate.Status.ERROR);
    }

    private void decorateQualityGateStatus(QualityGate.Status status) {
        String user = "sonar_user";
        String repositorySlug = "repo/slug";
        String commitSha = "commitSha";
        long mergeRequestIid = 6;
        String projectKey = "projectKey";
        String sonarRootUrl = "http://sonar:9000/sonar";
        String discussionId = "6a9c1750b37d513a43987b574953fceb50b03ce7";
        String noteId = "1126";
        String filePath = "/path/to/file";
        long sourceProjectId = 1234;
        int lineNumber = 5;

        ProjectAlmSettingDto projectAlmSettingDto = mock();
        AlmSettingDto almSettingDto = mock();
        when(almSettingDto.getDecryptedPersonalAccessToken(any())).thenReturn("token");
        when(almSettingDto.getUrl()).thenReturn(wireMockExtension.baseUrl() + "/api/v4");

        AnalysisDetails analysisDetails = mock();
        when(almSettingDto.getUrl()).thenReturn(wireMockExtension.baseUrl() + "/api/v4");
        when(projectAlmSettingDto.getAlmRepo()).thenReturn(repositorySlug);
        when(projectAlmSettingDto.getMonorepo()).thenReturn(true);
        when(analysisDetails.getQualityGateStatus()).thenReturn(status);
        when(analysisDetails.getAnalysisProjectKey()).thenReturn(projectKey);
        when(analysisDetails.getPullRequestId()).thenReturn(Long.toString(mergeRequestIid));
        when(analysisDetails.getCommitSha()).thenReturn(commitSha);

        ScmInfoRepository scmInfoRepository = mock();

        List<PostAnalysisIssueVisitor.ComponentIssue> issues = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            PostAnalysisIssueVisitor.ComponentIssue componentIssue = mock();
            PostAnalysisIssueVisitor.LightIssue defaultIssue = mock();
            when(defaultIssue.issueStatus()).thenReturn(IssueStatus.OPEN);
            when(defaultIssue.getLine()).thenReturn(lineNumber);
            when(defaultIssue.key()).thenReturn("issueKey" + i);
            when(componentIssue.getIssue()).thenReturn(defaultIssue);
            Component component = mock();
            when(componentIssue.getComponent()).thenReturn(component);
            when(componentIssue.getScmPath()).thenReturn(Optional.of(filePath));

            ScmInfo scmInfo = mock();
            when(scmInfo.hasChangesetForLine(anyInt())).thenReturn(true);
            when(scmInfo.getChangesetForLine(anyInt())).thenReturn(Changeset.newChangesetBuilder()
                    .setDate(0L)
                    .setRevision(commitSha)
                    .build());
            when(scmInfoRepository.getScmInfo(component)).thenReturn(Optional.of(scmInfo));

            issues.add(componentIssue);
        }
        when(analysisDetails.getScmReportableIssues()).thenReturn(issues);

        ReportGenerator reportGenerator = mock();
        AnalysisSummary analysisSummary = mock();
        when(analysisSummary.getNewCoverage()).thenReturn(BigDecimal.TEN);
        when(analysisSummary.getDashboardUrl()).thenReturn(sonarRootUrl + "/dashboard?id=" + projectKey + "&pullRequest=" + mergeRequestIid);
        when(analysisSummary.format(any())).thenReturn("summary commént\n\n[link text]");
        when(reportGenerator.createAnalysisSummary(any())).thenReturn(analysisSummary);

        AnalysisIssueSummary analysisIssueSummary = mock();
        when(analysisIssueSummary.format(any())).thenReturn("issué");
        when(reportGenerator.createAnalysisIssueSummary(any(), any())).thenReturn(analysisIssueSummary);

        wireMockExtension.stubFor(get(urlPathEqualTo("/api/v4/user")).withHeader("PRIVATE-TOKEN", equalTo("token")).willReturn(okJson("{\n" +
                "  \"id\": 1,\n" +
                "  \"username\": \"" + user + "\"}")));

        wireMockExtension.stubFor(get(urlPathEqualTo("/api/v4/projects/" + urlEncode(repositorySlug) + "/merge_requests/" + mergeRequestIid)).willReturn(okJson("{\n" +
                "  \"id\": 15235,\n" +
                "  \"iid\": " + mergeRequestIid + ",\n" +
                "  \"target_project_id\": " + sourceProjectId + ",\n" +
                "  \"web_url\": \"http://gitlab.example.com/my-group/my-project/merge_requests/1\",\n" +
                "  \"diff_refs\": {\n" +
                "    \"base_sha\":\"d6a420d043dfe85e7c240fd136fc6e197998b10a\",\n" +
                "    \"head_sha\":\"" + commitSha + "\",\n" +
                "    \"start_sha\":\"d6a420d043dfe85e7c240fd136fc6e197998b10a\"\n" +
                "  }," +
                "  \"source_project_id\": " + sourceProjectId  + "\n" +
                "}")));

        wireMockExtension.stubFor(get(urlPathEqualTo("/api/v4/projects/" + sourceProjectId + "/merge_requests/" + mergeRequestIid + "/commits")).willReturn(okJson("[\n" +
                "  {\n" +
                "    \"id\": \"" + commitSha + "\"\n" +
                "  }]")));

        wireMockExtension.stubFor(get(urlPathEqualTo("/api/v4/projects/" + sourceProjectId + "/merge_requests/" + mergeRequestIid + "/discussions")).willReturn(okJson(
                "[\n" + discussionPostResponseBody(discussionId,
                        discussionNote(noteId, user, "Old sonarqube issue.\\nPlease fix this finding", true, false),
                        discussionNote(noteId + 1, "other", "I have fixed this", true, false)) +
                        ", " +
                        discussionPostResponseBody(discussionId + 1,
                                discussionNote(noteId + 2, user, "Old Sonarqube issue that no longer exists\\n[View in SonarQube](https://sonarqube.dummy/security_hotspots?id=" + projectKey + "&pullRequest=1234&hotspots=randomId)", true, false),
                                discussionNote(noteId + 3, "other", "System message about this being changed in a commit", false, true)) +
                        "," +
                        discussionPostResponseBody(discussionId + 2,
                                discussionNote(noteId + 4, user, "Not a Sonarqube issue, but posted by the same user as Sonarqube, so will be cleaned up", true, false),
                                discussionNote(noteId + 5, "other", "Investigating", true, false)) +
                        "," +
                        discussionPostResponseBody(discussionId + 3,
                                discussionNote(noteId + 6, "other", "not posted by the Sonarqube user", true, false)) +
                        "," +
                        discussionPostResponseBody(discussionId + 4,
                                discussionNote(noteId + 7, user, "Posted by system on behalf of Sonarqube user", false, true)) +
                        "," +
                        discussionPostResponseBody(discussionId + 5,
                                discussionNote(noteId + 8, user, "Ongoing sonarqube issue that should not be closed\\n[View in SonarQube](https://sonarqube.dummy/security_hotspots?id=" + projectKey + "&pullRequest=1234&hotspots=issueKey1)", true, false)) +
                        "," +
                        discussionPostResponseBody(discussionId + 6,
                                discussionNote(noteId + 9, user, "Resolved Sonarqube issue with response comment from other user so discussion can't be closed\\n[View in SonarQube](https://sonarqube.dummy/project/issues?id=" + projectKey + "&pullRequest=1234&issues=oldid&open=oldid)", true, false),
                                discussionNote(noteId + 10, "other", "Comment from other user", true, false)) +
                        "," +
                        discussionPostResponseBody(discussionId + 7,
                                discussionNote(noteId + 11, user, "Sonarqube issue for anther project\\n[View in SonarQube](https://sonarqube.dummy/project/issues?id=abcd-" + projectKey + "&pullRequest=1234&issues=oldid&open=oldid)", true, false)) +
                        "," +
                        discussionPostResponseBody(discussionId + 8,
                            discussionNote(noteId + 12, user, "Old summary note, should be deleted\\n[View in SonarQube](https://sonarqube.dummy/dashboard?id=" + projectKey + "&pullRequest=1234)", true, false)) +
                        "]")));

        wireMockExtension.stubFor(delete(urlPathEqualTo("/api/v4/projects/" + sourceProjectId + "/merge_requests/" + mergeRequestIid + "/discussions/" + discussionId + 8 + "/notes/" + noteId + 12))
                .willReturn(noContent()));

        wireMockExtension.stubFor(post(urlPathEqualTo("/api/v4/projects/" + sourceProjectId + "/merge_requests/" + mergeRequestIid + "/discussions/" + discussionId + "/notes"))
                .withRequestBody(equalTo("body=" + urlEncode("This looks like a comment from an old SonarQube version, but due to other comments being present in this discussion, the discussion is not being being closed automatically. Please manually resolve this discussion once the other comments have been reviewed.")))
                .willReturn(created()));

        wireMockExtension.stubFor(post(urlPathEqualTo("/api/v4/projects/" + sourceProjectId + "/merge_requests/" + mergeRequestIid + "/discussions/" + discussionId + 2 + "/notes"))
                .withRequestBody(equalTo("body=" + urlEncode("This looks like a comment from an old SonarQube version, but due to other comments being present in this discussion, the discussion is not being being closed automatically. Please manually resolve this discussion once the other comments have been reviewed.")))
                .willReturn(created()));

        wireMockExtension.stubFor(post(urlPathEqualTo("/api/v4/projects/" + sourceProjectId + "/merge_requests/" + mergeRequestIid + "/discussions/" + discussionId + 6 + "/notes"))
                .withRequestBody(equalTo("body=" + urlEncode("This issue no longer exists in SonarQube, but due to other comments being present in this discussion, the discussion is not being being closed automatically. Please manually resolve this discussion once the other comments have been reviewed.")))
                .willReturn(created()));

        wireMockExtension.stubFor(post(urlEqualTo("/api/v4/projects/" + sourceProjectId + "/statuses/" + commitSha + "?state=" + (status == QualityGate.Status.OK ? "success" : "failed")))
                .withRequestBody(equalTo("name=SonarQube&target_url=" + urlEncode(sonarRootUrl + "/dashboard?id=" + projectKey + "&pullRequest=" + mergeRequestIid) + "&description=SonarQube+Status&coverage=10"))
                .willReturn(created()));

        wireMockExtension.stubFor(post(urlPathEqualTo("/api/v4/projects/" + sourceProjectId + "/merge_requests/" + mergeRequestIid + "/discussions"))
                .withRequestBody(equalTo("body=summary+comm%C3%A9nt%0A%0A%5Blink+text%5D"))
                .willReturn(created().withBody(discussionPostResponseBody(discussionId, discussionNote(noteId, user, "summary comment", true, false)))));

        wireMockExtension.stubFor(post(urlPathEqualTo("/api/v4/projects/" + sourceProjectId + "/merge_requests/" + mergeRequestIid + "/discussions"))
                .withRequestBody(equalTo("body=issu%C3%A9&" +
                        urlEncode("position[base_sha]") + "=d6a420d043dfe85e7c240fd136fc6e197998b10a&" +
                        urlEncode("position[start_sha]") + "=d6a420d043dfe85e7c240fd136fc6e197998b10a&" +
                        urlEncode("position[head_sha]") + "=" + commitSha + "&" +
                        urlEncode("position[old_path]") + "=" + urlEncode(filePath) + "&" +
                        urlEncode("position[new_path]") + "=" + urlEncode(filePath) + "&" +
                        urlEncode("position[new_line]") + "=" + lineNumber + "&" +
                        urlEncode("position[position_type]") + "=text"))
                .willReturn(created().withBody(discussionPostResponseBody(discussionId, discussionNote(noteId, user, "issue",true, false)))));

        wireMockExtension.stubFor(put(urlPathEqualTo("/api/v4/projects/" + sourceProjectId + "/merge_requests/" + mergeRequestIid + "/discussions/" + discussionId))
                .withQueryParam("resolved", equalTo("true"))
                .willReturn(ok())
        );

        wireMockExtension.stubFor(put(urlPathEqualTo("/api/v4/projects/" + sourceProjectId + "/merge_requests/" + mergeRequestIid + "/discussions/" + discussionId + 1))
                .withQueryParam("resolved", equalTo("true"))
                .willReturn(ok())
        );

        LinkHeaderReader linkHeaderReader = mock();
        Settings settings = mock();
        Encryption encryption = mock();
        when(settings.getEncryption()).thenReturn(encryption);
        GitlabMergeRequestDecorator pullRequestDecorator =
                new GitlabMergeRequestDecorator(scmInfoRepository, new DefaultGitlabClientFactory(linkHeaderReader, settings), reportGenerator, mock());


        assertThat(pullRequestDecorator.decorateQualityGateStatus(analysisDetails, almSettingDto, projectAlmSettingDto).getPullRequestUrl()).isEqualTo(Optional.of("http://gitlab.example.com/my-group/my-project/merge_requests/1"));
    }

    private static String discussionPostResponseBody(String discussionId, String... notes) {
        return "{\n" +
                "    \"id\": \"" + discussionId + "\",\n" +
                "    \"individual_note\": false,\n" +
                "    \"notes\": [\n" + String.join(", ", notes) + "\n]" +
                "}";
    }

    private static String discussionNote(String noteId, String username, String noteBody, boolean resolvable, boolean isSystem) {
        return "{\n" +
                "        \"id\": " + noteId + ",\n" +
                "        \"type\": \"DiscussionNote\",\n" +
                "        \"body\": \"" + noteBody + "\",\n" +
                "        \"attachment\": null,\n" +
                "        \"author\": {\n" +
                "          \"id\": 1,\n" +
                "          \"username\": \"" + username + "\"\n" +
                "        },\n" +
                "        \"resolved\": \"false\",\n" +
                "        \"system\": \"" + isSystem + "\",\n" +
                "        \"resolvable\": \"" + resolvable + "\"\n" +
                "}";
    }

    private static String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
