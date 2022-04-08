package com.github.mc1arke.sonarqube.plugin.ce.pullrequest.bitbucket;

import com.github.mc1arke.sonarqube.plugin.almclient.bitbucket.BitbucketClient;
import com.github.mc1arke.sonarqube.plugin.almclient.bitbucket.BitbucketClientFactory;
import com.github.mc1arke.sonarqube.plugin.almclient.bitbucket.model.AnnotationUploadLimit;
import com.github.mc1arke.sonarqube.plugin.almclient.bitbucket.model.ReportStatus;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.AnalysisDetails;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.PostAnalysisIssueVisitor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.sonar.api.ce.posttask.QualityGate;
import org.sonar.api.issue.Issue;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.rule.Severity;
import org.sonar.api.rules.RuleType;
import org.sonar.ce.task.projectanalysis.component.Component;
import org.sonar.ce.task.projectanalysis.component.ReportAttributes;
import org.sonar.db.alm.setting.AlmSettingDto;
import org.sonar.db.alm.setting.ProjectAlmSettingDto;

import java.io.IOException;
import java.time.Instant;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BitbucketPullRequestDecoratorTest {

    private static final String COMMIT = "commit";

    private static final String ISSUE_KEY = "issue-key";
    private static final int ISSUE_LINE = 1;
    private static final String ISSUE_LINK = "https://issue-link";
    private static final String ISSUE_MESSAGE = "issue message";
    private static final String ISSUE_PATH = "/issue/path";
    private static final String DASHBOARD_URL = "https://dashboard-url";
    private static final String IMAGE_URL = "https://image-url";

    private final AnalysisDetails analysisDetails = mock(AnalysisDetails.class);
    private final BitbucketClient client = mock(BitbucketClient.class);
    private final BitbucketClientFactory bitbucketClientFactory = mock(BitbucketClientFactory.class);
    private final BitbucketPullRequestDecorator underTest = new BitbucketPullRequestDecorator(bitbucketClientFactory);

    private final AlmSettingDto almSettingDto = mock(AlmSettingDto.class);
    private final ProjectAlmSettingDto projectAlmSettingDto = mock(ProjectAlmSettingDto.class);

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
        underTest.decorateQualityGateStatus(analysisDetails, almSettingDto, projectAlmSettingDto);

        verify(client).createCodeInsightsAnnotation(ISSUE_KEY, ISSUE_LINE, ISSUE_LINK, ISSUE_MESSAGE, ISSUE_PATH, "HIGH", "BUG");
        verify(client).createLinkDataValue(DASHBOARD_URL);
        verify(client).createCodeInsightsReport(any(), eq("Quality Gate passed" + System.lineSeparator()), any(), eq(DASHBOARD_URL), eq(String.format("%s/common/icon.png", IMAGE_URL)), eq(ReportStatus.PASSED));
        verify(client).deleteAnnotations(COMMIT);
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

        Map<RuleType, Long> ruleCount = new HashMap<>();
        ruleCount.put(RuleType.CODE_SMELL, 1L);
        ruleCount.put(RuleType.VULNERABILITY, 2L);
        ruleCount.put(RuleType.SECURITY_HOTSPOT, 3L);
        ruleCount.put(RuleType.BUG, 4L);

        when(analysisDetails.countRuleByType()).thenReturn(ruleCount);
        when(analysisDetails.findQualityGateCondition(CoreMetrics.NEW_COVERAGE_KEY)).thenReturn(Optional.empty());
        when(analysisDetails.findQualityGateCondition(CoreMetrics.NEW_DUPLICATED_LINES_DENSITY_KEY)).thenReturn(Optional.empty());
        when(analysisDetails.getAnalysisDate()).thenReturn(Date.from(Instant.now()));
        when(analysisDetails.getDashboardUrl()).thenReturn(DASHBOARD_URL);

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
        when(analysisDetails.getIssueUrl(defaultIssue)).thenReturn(ISSUE_LINK);
        when(analysisDetails.getBaseImageUrl()).thenReturn(IMAGE_URL);

        PostAnalysisIssueVisitor.ComponentIssue componentIssue = mock(PostAnalysisIssueVisitor.ComponentIssue.class);
        when(componentIssue.getIssue()).thenReturn(defaultIssue);
        when(componentIssue.getComponent()).thenReturn(component);

        when(analysisDetails.getScmReportableIssues()).thenReturn(Collections.singletonList(componentIssue));
    }

}
