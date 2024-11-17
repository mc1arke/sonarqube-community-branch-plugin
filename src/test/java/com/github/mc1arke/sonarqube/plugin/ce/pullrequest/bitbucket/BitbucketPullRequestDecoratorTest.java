/*
 * Copyright (C) 2020-2024 Mathias Åhsberg, Michael Clarke
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
package com.github.mc1arke.sonarqube.plugin.ce.pullrequest.bitbucket;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.sonar.api.ce.posttask.QualityGate;
import org.sonar.api.issue.IssueStatus;
import org.sonar.api.issue.impact.Severity;
import org.sonar.api.issue.impact.SoftwareQuality;
import org.sonar.ce.task.projectanalysis.component.Component;
import org.sonar.ce.task.projectanalysis.component.ReportAttributes;
import org.sonar.db.alm.setting.AlmSettingDto;
import org.sonar.db.alm.setting.ProjectAlmSettingDto;

import com.github.mc1arke.sonarqube.plugin.almclient.bitbucket.BitbucketClient;
import com.github.mc1arke.sonarqube.plugin.almclient.bitbucket.BitbucketClientFactory;
import com.github.mc1arke.sonarqube.plugin.almclient.bitbucket.model.AnnotationUploadLimit;
import com.github.mc1arke.sonarqube.plugin.almclient.bitbucket.model.BuildStatus;
import com.github.mc1arke.sonarqube.plugin.almclient.bitbucket.model.DataValue;
import com.github.mc1arke.sonarqube.plugin.almclient.bitbucket.model.ReportData;
import com.github.mc1arke.sonarqube.plugin.almclient.bitbucket.model.ReportStatus;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.AnalysisDetails;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.PostAnalysisIssueVisitor;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.report.AnalysisIssueSummary;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.report.AnalysisSummary;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.report.ReportGenerator;

class BitbucketPullRequestDecoratorTest {

    private static final String COMMIT = "commit";
    private static final String REPORT_KEY = "com.sonarsource.sonarqube";

    private static final String ISSUE_KEY = "issue-key";
    private static final int ISSUE_LINE = 1;
    private static final String ISSUE_LINK = "https://issue-link";
    private static final String ISSUE_MESSAGE = "issue message";
    private static final String ISSUE_PATH = "/issue/path";
    private static final String DASHBOARD_URL = "https://dashboard-url";
    private static final String IMAGE_URL = "https://image-url";

    private final AnalysisDetails analysisDetails = mock();
    private final ReportGenerator reportGenerator = mock();
    private final BitbucketClient client = mock();
    private final BitbucketClientFactory bitbucketClientFactory = mock();
    private final BitbucketPullRequestDecorator underTest = new BitbucketPullRequestDecorator(bitbucketClientFactory, reportGenerator);

    private final AlmSettingDto almSettingDto = mock();
    private final ProjectAlmSettingDto projectAlmSettingDto = mock();
    private final AnalysisSummary analysisSummary = mock();

    @BeforeEach
    void setUp() {
        when(bitbucketClientFactory.createClient(any(), any())).thenReturn(client);
    }

    @Test
    void testValidAnalysis() throws IOException {
        when(client.supportsCodeInsights()).thenReturn(true);
        AnnotationUploadLimit uploadLimit = new AnnotationUploadLimit(1000, 1000);
        when(client.getAnnotationUploadLimit()).thenReturn(uploadLimit);

        mockValidAnalysis();
        when(analysisSummary.getNewDuplications()).thenReturn(BigDecimal.TEN);
        when(analysisSummary.getNewCoverage()).thenReturn(BigDecimal.ONE);
        when(analysisSummary.getAcceptedIssues()).thenReturn(new AnalysisSummary.UrlIconMetric<>("acceptedIssuesUrl", "acceptedIssuesImageUrl", 0));
        when(analysisSummary.getFixedIssues()).thenReturn(new AnalysisSummary.UrlIconMetric<>("fixedIssuesUrl", "fixedIssuesImageUrl", 12));
        when(analysisSummary.getNewIssues()).thenReturn(new AnalysisSummary.UrlIconMetric<>("newIssuesUrl", "newIssuesImageUrl", 666L));
        when(analysisSummary.getSecurityHotspots()).thenReturn(new AnalysisSummary.UrlIconMetric<>("securityHotspotsUrl", "securityHotspotsImageUrl", 69));
        when(analysisSummary.getSummaryImageUrl()).thenReturn(IMAGE_URL);
        when(analysisSummary.getDashboardUrl()).thenReturn(DASHBOARD_URL);
        when(reportGenerator.createAnalysisSummary(any())).thenReturn(analysisSummary);
        underTest.decorateQualityGateStatus(analysisDetails, almSettingDto, projectAlmSettingDto);

        ArgumentCaptor<List<ReportData>> reportDataArgumentCaptor = ArgumentCaptor.captor();
        verify(client).createCodeInsightsAnnotation(ISSUE_KEY, ISSUE_LINE, ISSUE_LINK, ISSUE_MESSAGE, ISSUE_PATH, "HIGH", "BUG");
        verify(client).createLinkDataValue(DASHBOARD_URL);
        verify(client).createCodeInsightsReport(reportDataArgumentCaptor.capture(), eq("Quality Gate passed" + System.lineSeparator()), any(), eq(DASHBOARD_URL), eq(IMAGE_URL), eq(ReportStatus.PASSED));
        when(analysisSummary.getAcceptedIssues()).thenReturn(new AnalysisSummary.UrlIconMetric<>("acceptedIssuesUrl", "acceptedIssuesImageUrl", 0));
        when(analysisSummary.getFixedIssues()).thenReturn(new AnalysisSummary.UrlIconMetric<>("fixedIssuesUrl", "fixedIssuesImageUrl", 12));
        when(analysisSummary.getNewIssues()).thenReturn(new AnalysisSummary.UrlIconMetric<>("newIssuesUrl", "newIssuesImageUrl", 666L));
        when(analysisSummary.getSecurityHotspots()).thenReturn(new AnalysisSummary.UrlIconMetric<>("securityHotspotsUrl", "securityHotspotsImageUrl", 69));
        verify(client).deleteAnnotations(COMMIT, REPORT_KEY);

        assertThat(reportDataArgumentCaptor.getValue())
                .usingRecursiveComparison()
                .isEqualTo(List.of(new ReportData("New Issues", new DataValue.Text("666 Issues")),
                        new ReportData("Accepted Issues", new DataValue.Text("0 Issues")),
                        new ReportData("Fixed Issues", new DataValue.Text("12 Issues")),
                        new ReportData("Code coverage", new DataValue.Percentage(BigDecimal.ONE)),
                        new ReportData("Duplication", new DataValue.Percentage(BigDecimal.TEN)),
                        new ReportData("Analysis details", null)));
    }

    @Test
    void testNullPercentagesReplacedWithZeroValues() throws IOException {
        when(client.supportsCodeInsights()).thenReturn(true);
        AnnotationUploadLimit uploadLimit = new AnnotationUploadLimit(1000, 1000);
        when(client.getAnnotationUploadLimit()).thenReturn(uploadLimit);

        mockValidAnalysis();
        when(analysisSummary.getNewCoverage()).thenReturn(null);
        when(analysisSummary.getNewDuplications()).thenReturn(null);
        when(analysisSummary.getAcceptedIssues()).thenReturn(new AnalysisSummary.UrlIconMetric<>("acceptedIssuesUrl", "acceptedIssuesImageUrl", 0));
        when(analysisSummary.getFixedIssues()).thenReturn(new AnalysisSummary.UrlIconMetric<>("fixedIssuesUrl", "fixedIssuesImageUrl", 1));
        when(analysisSummary.getNewIssues()).thenReturn(new AnalysisSummary.UrlIconMetric<>("newIssuesUrl", "newIssuesImageUrl", 666L));
        when(analysisSummary.getSecurityHotspots()).thenReturn(new AnalysisSummary.UrlIconMetric<>("securityHotspotsUrl", "securityHotspotsImageUrl", 69));
        underTest.decorateQualityGateStatus(analysisDetails, almSettingDto, projectAlmSettingDto);

        ArgumentCaptor<List<ReportData>> reportDataArgumentCaptor = ArgumentCaptor.captor();
        verify(client).createCodeInsightsAnnotation(ISSUE_KEY, ISSUE_LINE, ISSUE_LINK, ISSUE_MESSAGE, ISSUE_PATH, "HIGH", "BUG");
        verify(client).createLinkDataValue(DASHBOARD_URL);
        verify(client).createCodeInsightsReport(reportDataArgumentCaptor.capture(), eq("Quality Gate passed" + System.lineSeparator()), any(), eq(DASHBOARD_URL), eq(String.format("%s/common/icon.png", IMAGE_URL)), eq(ReportStatus.PASSED));
        verify(client).deleteAnnotations(COMMIT, REPORT_KEY);

        ArgumentCaptor<BuildStatus> buildStatusArgumentCaptor = ArgumentCaptor.captor();
        verify(client).submitBuildStatus(eq(COMMIT), buildStatusArgumentCaptor.capture());
        assertThat(buildStatusArgumentCaptor.getValue()).usingRecursiveComparison().isEqualTo(new BuildStatus(BuildStatus.State.SUCCESSFUL, REPORT_KEY, "SonarQube", DASHBOARD_URL));

        assertThat(reportDataArgumentCaptor.getValue())
                .usingRecursiveComparison()
                .isEqualTo(List.of(new ReportData("New Issues", new DataValue.Text("666 Issues")),
                        new ReportData("Accepted Issues", new DataValue.Text("0 Issues")),
                        new ReportData("Fixed Issues", new DataValue.Text("1 Issue")),
                        new ReportData("Code coverage", new DataValue.Percentage(BigDecimal.ZERO)),
                        new ReportData("Duplication", new DataValue.Percentage(BigDecimal.ZERO)),
                        new ReportData("Analysis details", null)));
    }

    @ParameterizedTest(name = "{arguments}")
    @CsvSource({"100, 1000, 2",
            "1000, 1000, 1",
            "100, 1000, 10"})
    void testExceedsMaximumNumberOfAnnotations(int annotationBatchSize, int totalAllowedAnnotations, int counter) {
        // given
        AnnotationUploadLimit uploadLimit = new AnnotationUploadLimit(annotationBatchSize, totalAllowedAnnotations);

        // when
        boolean result = BitbucketPullRequestDecorator.exceedsMaximumNumberOfAnnotations(counter, uploadLimit);

        // then
        assertFalse(result);
    }

    private void mockValidAnalysis() {
        when(analysisDetails.getCommitSha()).thenReturn(COMMIT);
        when(analysisDetails.getQualityGateStatus()).thenReturn(QualityGate.Status.OK);
        when(analysisDetails.getAnalysisProjectKey()).thenReturn(REPORT_KEY);

        when(analysisDetails.getAnalysisDate()).thenReturn(Date.from(Instant.now()));

        ReportAttributes reportAttributes = mock();
        when(reportAttributes.getScmPath()).thenReturn(Optional.of(ISSUE_PATH));

        Component component = mock();
        when(component.getType()).thenReturn(Component.Type.FILE);
        when(component.getReportAttributes()).thenReturn(reportAttributes);

        PostAnalysisIssueVisitor.LightIssue defaultIssue = mock();
        when(defaultIssue.issueStatus()).thenReturn(IssueStatus.OPEN);
        when(defaultIssue.impacts()).thenReturn(Map.of(SoftwareQuality.RELIABILITY, Severity.HIGH));
        when(defaultIssue.getLine()).thenReturn(ISSUE_LINE);
        when(defaultIssue.key()).thenReturn(ISSUE_KEY);
        when(defaultIssue.getMessage()).thenReturn(ISSUE_MESSAGE);

        PostAnalysisIssueVisitor.ComponentIssue componentIssue = mock();
        when(componentIssue.getIssue()).thenReturn(defaultIssue);
        when(componentIssue.getComponent()).thenReturn(component);

        AnalysisIssueSummary analysisIssueSummary = mock();
        when(analysisIssueSummary.getIssueUrl()).thenReturn("https://issue-link");
        when(reportGenerator.createAnalysisIssueSummary(any(), any())).thenReturn(analysisIssueSummary);

        when(analysisSummary.getDashboardUrl()).thenReturn("https://dashboard-url");
        when(analysisSummary.getSummaryImageUrl()).thenReturn("https://image-url/common/icon.png");
        when(reportGenerator.createAnalysisSummary(any())).thenReturn(analysisSummary);

        when(analysisDetails.getScmReportableIssues()).thenReturn(List.of(componentIssue));
    }

}
