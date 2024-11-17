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
package com.github.mc1arke.sonarqube.plugin.ce.pullrequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.sonar.api.issue.IssueStatus;
import org.sonar.ce.task.projectanalysis.analysis.AnalysisMetadataHolder;
import org.sonar.ce.task.projectanalysis.component.Component;
import org.sonar.ce.task.projectanalysis.issue.fixedissues.PullRequestFixedIssueRepository;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.core.issue.tracking.Input;
import org.sonar.core.issue.tracking.NonClosedTracking;
import org.sonar.core.issue.tracking.Tracker;

class PullRequestFixedIssuesIssueVisitorTest {

    @Test
    void shouldSkipVisitIfNotAPullRequest() {
        AnalysisMetadataHolder analysisMetadataHolder = mock();
        PullRequestFixedIssueRepository pullRequestFixedIssuesRepository = mock();
        Tracker<DefaultIssue, DefaultIssue> tracker = mock();

        when(analysisMetadataHolder.isPullRequest()).thenReturn(false);

        Component component = mock();
        Input<DefaultIssue> rawIssues = mock();
        Input<DefaultIssue> baseIssues = mock();

        PullRequestFixedIssuesIssueVisitor underTest = new PullRequestFixedIssuesIssueVisitor(pullRequestFixedIssuesRepository, analysisMetadataHolder, tracker);
        underTest.onRawIssues(component, rawIssues, baseIssues);

        verify(analysisMetadataHolder).isPullRequest();

        verifyNoInteractions(pullRequestFixedIssuesRepository, tracker);
    }

    @Test
    void shouldSkipVisitIfPullRequestButNoBaseIssuesToFix() {
        AnalysisMetadataHolder analysisMetadataHolder = mock();
        PullRequestFixedIssueRepository pullRequestFixedIssuesRepository = mock();
        Tracker<DefaultIssue, DefaultIssue> tracker = mock();

        when(analysisMetadataHolder.isPullRequest()).thenReturn(true);

        Component component = mock();
        Input<DefaultIssue> rawIssues = mock();
        Input<DefaultIssue> baseIssues = null;

        PullRequestFixedIssuesIssueVisitor underTest = new PullRequestFixedIssuesIssueVisitor(pullRequestFixedIssuesRepository, analysisMetadataHolder, tracker);
        underTest.onRawIssues(component, rawIssues, baseIssues);

        verify(analysisMetadataHolder).isPullRequest();

        verifyNoInteractions(pullRequestFixedIssuesRepository, tracker);
    }

    @Test
    void shouldPassFixedAndRemovedIssuesToRepository() {
        AnalysisMetadataHolder analysisMetadataHolder = mock();
        PullRequestFixedIssueRepository pullRequestFixedIssuesRepository = mock();
        Tracker<DefaultIssue, DefaultIssue> tracker = mock();

        when(analysisMetadataHolder.isPullRequest()).thenReturn(true);

        Component component = mock();
        Input<DefaultIssue> rawIssues = mock();
        List<DefaultIssue> baseIssueList = List.of(mockIssue(IssueStatus.FALSE_POSITIVE), mockIssue(IssueStatus.OPEN), mockIssue(IssueStatus.FIXED), mockIssue(IssueStatus.OPEN), mockIssue(IssueStatus.FALSE_POSITIVE), mockIssue(null));
        Input<DefaultIssue> baseIssues = mock();
        when(baseIssues.getIssues()).thenReturn(baseIssueList);

        NonClosedTracking<DefaultIssue, DefaultIssue> nonClosedTracking = mock();
        Map<DefaultIssue, DefaultIssue> matchedRaws = Map.of(mockIssue(IssueStatus.OPEN), baseIssueList.get(1), mockIssue(IssueStatus.FIXED), baseIssueList.get(3));
        when(nonClosedTracking.getMatchedRaws()).thenReturn(matchedRaws);
        Stream<DefaultIssue> unmatchedBaseIssues = Stream.of(baseIssueList.get(0), baseIssueList.get(2));
        when(nonClosedTracking.getUnmatchedBases()).thenReturn(unmatchedBaseIssues);
        when(tracker.trackNonClosed(any(), any())).thenReturn(nonClosedTracking);

        PullRequestFixedIssuesIssueVisitor underTest = new PullRequestFixedIssuesIssueVisitor(pullRequestFixedIssuesRepository, analysisMetadataHolder, tracker);
        underTest.onRawIssues(component, rawIssues, baseIssues);

        verify(analysisMetadataHolder).isPullRequest();
        ArgumentCaptor<Input<DefaultIssue>> trackingIssuesCaptor = ArgumentCaptor.captor();
        verify(tracker).trackNonClosed(eq(rawIssues), trackingIssuesCaptor.capture());
        assertThat(trackingIssuesCaptor.getValue().getIssues()).containsExactly(baseIssueList.get(1), baseIssueList.get(3));
        verify(nonClosedTracking).getUnmatchedBases();

        verify(pullRequestFixedIssuesRepository).addFixedIssue(baseIssueList.get(0));
        verify(pullRequestFixedIssuesRepository).addFixedIssue(baseIssueList.get(2));
        verify(pullRequestFixedIssuesRepository).addFixedIssue(baseIssueList.get(3));
    }

    private static DefaultIssue mockIssue(IssueStatus issueStatus) {
        DefaultIssue issue = mock();
        when(issue.issueStatus()).thenReturn(issueStatus);
        return issue;
    }
}
