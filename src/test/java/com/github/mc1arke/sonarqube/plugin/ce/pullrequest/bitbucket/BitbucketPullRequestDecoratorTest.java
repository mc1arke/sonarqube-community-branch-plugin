package com.github.mc1arke.sonarqube.plugin.ce.pullrequest.bitbucket;

import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.AnalysisDetails;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.PostAnalysisIssueVisitor;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.bitbucket.client.BitbucketClient;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.bitbucket.client.model.Annotation;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.bitbucket.client.model.CreateAnnotationsRequest;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.bitbucket.client.model.CreateReportRequest;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.MockitoJUnitRunner;
import org.sonar.api.ce.posttask.QualityGate;
import org.sonar.api.issue.Issue;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.rule.Severity;
import org.sonar.api.rules.RuleType;
import org.sonar.ce.task.projectanalysis.component.Component;
import org.sonar.ce.task.projectanalysis.component.ReportAttributes;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.db.alm.setting.AlmSettingDto;
import org.sonar.db.alm.setting.ProjectAlmSettingDto;

import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class BitbucketPullRequestDecoratorTest {

    private static final String PROJECT = "project";
    private static final String REPO = "repo";
    private static final String COMMIT = "commit";

    private static final String ISSUE_KEY = "issue-key";
    private static final int ISSUE_LINE = 1;
    private static final String ISSUE_LINK = "https://issue-link";
    private static final String ISSUE_MESSAGE = "issue message";
    private static final String ISSUE_PATH = "/issue/path";
    private static final String DASHBOARD_URL = "https://dashboard-url";
    private static final String IMAGE_URL = "https://image-url";

    private AnalysisDetails analysisDetails = mock(AnalysisDetails.class);

    private BitbucketClient client = mock(BitbucketClient.class);

    private BitbucketServerPullRequestDecorator underTest = new BitbucketServerPullRequestDecorator(client);

    private final AlmSettingDto almSettingDto = mock(AlmSettingDto.class);
    private final ProjectAlmSettingDto projectAlmSettingDto = mock(ProjectAlmSettingDto.class);

    @Before
    public void setUp() {
        when(projectAlmSettingDto.getAlmRepo()).thenReturn(PROJECT);
        when(projectAlmSettingDto.getAlmSlug()).thenReturn(REPO);
    }

    @Test
    public void testValidAnalysis() throws IOException {
        when(client.supportsCodeInsights(eq(almSettingDto))).thenReturn(true);

        mockValidAnalysis();
        final ArgumentCaptor<CreateReportRequest> reportCaptor = ArgumentCaptor.forClass(CreateReportRequest.class);
        final ArgumentCaptor<CreateAnnotationsRequest> annotationsCaptor = ArgumentCaptor.forClass(CreateAnnotationsRequest.class);

        underTest.decorateQualityGateStatus(analysisDetails, almSettingDto, projectAlmSettingDto);

        verify(client).createReport(eq(PROJECT), eq(REPO), eq(COMMIT), reportCaptor.capture(), eq(almSettingDto));
        verifyExpectedReport(reportCaptor.getValue());

        verify(client).deleteAnnotations(PROJECT, REPO, COMMIT, almSettingDto);
        verify(client).createAnnotations(eq(PROJECT), eq(REPO), eq(COMMIT), annotationsCaptor.capture(), eq(almSettingDto));

        CreateAnnotationsRequest actualAnnotations = annotationsCaptor.getValue();
        assertThat(actualAnnotations.getAnnotations()).size().isEqualTo(1);
        verifyExpectedAnnotation(actualAnnotations.getAnnotations().iterator().next());
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

        DefaultIssue defaultIssue = mock(DefaultIssue.class);
        when(defaultIssue.status()).thenReturn(Issue.STATUS_OPEN);
        when(defaultIssue.severity()).thenReturn(Severity.CRITICAL);
        when(defaultIssue.getLine()).thenReturn(ISSUE_LINE);
        when(defaultIssue.key()).thenReturn(ISSUE_KEY);
        when(defaultIssue.type()).thenReturn(RuleType.BUG);
        when(defaultIssue.getMessage()).thenReturn(ISSUE_MESSAGE);
        when(analysisDetails.getIssueUrl(ISSUE_KEY)).thenReturn(ISSUE_LINK);
        when(analysisDetails.getBaseImageUrl()).thenReturn(IMAGE_URL);

        PostAnalysisIssueVisitor.ComponentIssue componentIssue = mock(PostAnalysisIssueVisitor.ComponentIssue.class);
        when(componentIssue.getIssue()).thenReturn(defaultIssue);
        when(componentIssue.getComponent()).thenReturn(component);

        PostAnalysisIssueVisitor postAnalysisIssueVisitor = mock(PostAnalysisIssueVisitor.class);
        when(postAnalysisIssueVisitor.getIssues()).thenReturn(Collections.singletonList(componentIssue));

        when(analysisDetails.getPostAnalysisIssueVisitor()).thenReturn(postAnalysisIssueVisitor);
    }

    private void verifyExpectedReport(CreateReportRequest actual) {
        assertThat(actual.getTitle()).isEqualTo("SonarQube");
        assertThat(actual.getResult()).isEqualTo("PASS");
        assertThat(actual.getReporter()).isEqualTo("SonarQube");
        assertThat(actual.getCreatedDate()).isBetween(Instant.now().minus(1, ChronoUnit.MINUTES), Instant.now());
        assertThat(actual.getDetails()).isEqualTo("Quality Gate passed" + System.lineSeparator());
        assertThat(actual.getLink()).isEqualTo(DASHBOARD_URL);
        assertThat(actual.getLogoUrl()).isEqualTo(String.format("%s/common/icon.png", IMAGE_URL));

        assertThat(actual.getData()).size().isEqualTo(6);
    }

    private void verifyExpectedAnnotation(Annotation actual) {
        assertThat(actual.getExternalId()).isEqualTo(ISSUE_KEY);
        assertThat(actual.getLine()).isEqualTo(ISSUE_LINE);
        assertThat(actual.getLink()).isEqualTo(ISSUE_LINK);
        assertThat(actual.getMessage()).isEqualTo(ISSUE_MESSAGE);
        assertThat(actual.getPath()).isEqualTo(ISSUE_PATH);
        assertThat(actual.getSeverity()).isEqualTo("HIGH");
        assertThat(actual.getType()).isEqualTo("BUG");
    }
}
