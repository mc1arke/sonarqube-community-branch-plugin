package com.github.mc1arke.sonarqube.plugin.ce.pullrequest.bitbucket;

import com.github.mc1arke.sonarqube.plugin.almclient.bitbucket.BitbucketClient;
import com.github.mc1arke.sonarqube.plugin.almclient.bitbucket.BitbucketClientFactory;
import com.github.mc1arke.sonarqube.plugin.almclient.bitbucket.model.AnnotationUploadLimit;
import com.github.mc1arke.sonarqube.plugin.almclient.bitbucket.model.DataValue;
import com.github.mc1arke.sonarqube.plugin.almclient.bitbucket.model.ReportData;
import com.github.mc1arke.sonarqube.plugin.almclient.bitbucket.model.ReportStatus;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.AnalysisDetails;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.PostAnalysisIssueVisitor;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.report.AnalysisIssueSummary;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.report.AnalysisSummary;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.report.ReportGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.sonar.api.ce.posttask.QualityGate;
import org.sonar.api.issue.Issue;
import org.sonar.api.rule.Severity;
import org.sonar.api.rules.RuleType;
import org.sonar.ce.task.projectanalysis.component.Component;
import org.sonar.ce.task.projectanalysis.component.ReportAttributes;
import org.sonar.db.alm.setting.AlmSettingDto;
import org.sonar.db.alm.setting.ProjectAlmSettingDto;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BitbucketPullRequestDecoratorTest {

    private static final String COMMIT = "commit";
    private static final String REPORT_KEY = "report-key";

    private static final String ISSUE_KEY = "issue-key";
    private static final int ISSUE_LINE = 1;
    private static final String ISSUE_LINK = "https://issue-link";
    private static final String ISSUE_MESSAGE = "issue message";
    private static final String ISSUE_PATH = "/issue/path";
    private static final String DASHBOARD_URL = "https://dashboard-url";
    private static final String IMAGE_URL = "https://image-url";

    private final AnalysisDetails analysisDetails = mock(AnalysisDetails.class);
    private final ReportGenerator reportGenerator = mock(ReportGenerator.class);
    private final BitbucketClient client = mock(BitbucketClient.class);
    private final BitbucketClientFactory bitbucketClientFactory = mock(BitbucketClientFactory.class);
    private final BitbucketPullRequestDecorator underTest = new BitbucketPullRequestDecorator(bitbucketClientFactory, reportGenerator);

    private final AlmSettingDto almSettingDto = mock(AlmSettingDto.class);
    private final ProjectAlmSettingDto projectAlmSettingDto = mock(ProjectAlmSettingDto.class);
    private final AnalysisSummary analysisSummary = mock(AnalysisSummary.class);

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
        underTest.decorateQualityGateStatus(analysisDetails, almSettingDto, projectAlmSettingDto);

        ArgumentCaptor<List<ReportData>> reportDataArgumentCaptor = ArgumentCaptor.forClass(List.class);
        verify(client).createCodeInsightsAnnotation(ISSUE_KEY, ISSUE_LINE, ISSUE_LINK, ISSUE_MESSAGE, ISSUE_PATH, "HIGH", "BUG");
        verify(client).createLinkDataValue(DASHBOARD_URL);
        verify(client).createCodeInsightsReport(reportDataArgumentCaptor.capture(), eq("Quality Gate passed" + System.lineSeparator()), any(), eq(DASHBOARD_URL), eq(String.format("%s/common/icon.png", IMAGE_URL)), eq(ReportStatus.PASSED));
        verify(client).deleteAnnotations(COMMIT, REPORT_KEY);

        assertThat(reportDataArgumentCaptor.getValue())
                .usingRecursiveComparison()
                .isEqualTo(List.of(new ReportData("Reliability", new DataValue.Text("0 Bugs")),
                        new ReportData("Code coverage", new DataValue.Percentage(BigDecimal.ONE)),
                        new ReportData("Security", new DataValue.Text("0 Vulnerabilities (and 0 Hotspots)")),
                        new ReportData("Duplication", new DataValue.Percentage(BigDecimal.TEN)),
                        new ReportData("Maintainability", new DataValue.Text("0 Code Smells")),
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
        underTest.decorateQualityGateStatus(analysisDetails, almSettingDto, projectAlmSettingDto);

        ArgumentCaptor<List<ReportData>> reportDataArgumentCaptor = ArgumentCaptor.forClass(List.class);
        verify(client).createCodeInsightsAnnotation(ISSUE_KEY, ISSUE_LINE, ISSUE_LINK, ISSUE_MESSAGE, ISSUE_PATH, "HIGH", "BUG");
        verify(client).createLinkDataValue(DASHBOARD_URL);
        verify(client).createCodeInsightsReport(reportDataArgumentCaptor.capture(), eq("Quality Gate passed" + System.lineSeparator()), any(), eq(DASHBOARD_URL), eq(String.format("%s/common/icon.png", IMAGE_URL)), eq(ReportStatus.PASSED));
        verify(client).deleteAnnotations(COMMIT, REPORT_KEY);

        assertThat(reportDataArgumentCaptor.getValue())
                .usingRecursiveComparison()
                .isEqualTo(List.of(new ReportData("Reliability", new DataValue.Text("0 Bugs")),
                        new ReportData("Code coverage", new DataValue.Percentage(BigDecimal.ZERO)),
                        new ReportData("Security", new DataValue.Text("0 Vulnerabilities (and 0 Hotspots)")),
                        new ReportData("Duplication", new DataValue.Percentage(BigDecimal.ZERO)),
                        new ReportData("Maintainability", new DataValue.Text("0 Code Smells")),
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

        ReportAttributes reportAttributes = mock(ReportAttributes.class);
        when(reportAttributes.getScmPath()).thenReturn(Optional.of(ISSUE_PATH));

        Component component = mock(Component.class);
        when(component.getType()).thenReturn(Component.Type.FILE);
        when(component.getReportAttributes()).thenReturn(reportAttributes);

        PostAnalysisIssueVisitor.LightIssue defaultIssue = mock(PostAnalysisIssueVisitor.LightIssue.class);
        when(defaultIssue.status()).thenReturn(Issue.STATUS_OPEN);
        when(defaultIssue.severity()).thenReturn(Severity.CRITICAL);
        when(defaultIssue.getLine()).thenReturn(ISSUE_LINE);
        when(defaultIssue.key()).thenReturn(ISSUE_KEY);
        when(defaultIssue.type()).thenReturn(RuleType.BUG);
        when(defaultIssue.getMessage()).thenReturn(ISSUE_MESSAGE);

        PostAnalysisIssueVisitor.ComponentIssue componentIssue = mock(PostAnalysisIssueVisitor.ComponentIssue.class);
        when(componentIssue.getIssue()).thenReturn(defaultIssue);
        when(componentIssue.getComponent()).thenReturn(component);

        AnalysisIssueSummary analysisIssueSummary = mock(AnalysisIssueSummary.class);
        when(analysisIssueSummary.getIssueUrl()).thenReturn("https://issue-link");
        when(reportGenerator.createAnalysisIssueSummary(any(), any())).thenReturn(analysisIssueSummary);

        when(analysisSummary.getDashboardUrl()).thenReturn("https://dashboard-url");
        when(analysisSummary.getSummaryImageUrl()).thenReturn("https://image-url/common/icon.png");
        when(reportGenerator.createAnalysisSummary(any())).thenReturn(analysisSummary);

        when(analysisDetails.getScmReportableIssues()).thenReturn(List.of(componentIssue));
    }

}
