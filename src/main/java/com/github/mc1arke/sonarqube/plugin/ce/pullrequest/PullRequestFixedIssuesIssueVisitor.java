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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.sonar.api.issue.IssueStatus;
import org.sonar.ce.task.projectanalysis.analysis.AnalysisMetadataHolder;
import org.sonar.ce.task.projectanalysis.component.Component;
import org.sonar.ce.task.projectanalysis.issue.DefaultTrackingInput;
import org.sonar.ce.task.projectanalysis.issue.IssueVisitor;
import org.sonar.ce.task.projectanalysis.issue.fixedissues.PullRequestFixedIssueRepository;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.core.issue.tracking.Input;
import org.sonar.core.issue.tracking.NonClosedTracking;
import org.sonar.core.issue.tracking.Tracker;
import org.sonar.core.issue.tracking.Tracking;

public class PullRequestFixedIssuesIssueVisitor extends IssueVisitor {

    private static final List<IssueStatus> NON_CLOSED_ISSUE_STATUSES = List.of(IssueStatus.OPEN, IssueStatus.ACCEPTED, IssueStatus.CONFIRMED);

    private final PullRequestFixedIssueRepository pullRequestFixedIssueRepository;
    private final AnalysisMetadataHolder analysisMetadataHolder;
    private final Tracker<DefaultIssue, DefaultIssue> tracker;

    public PullRequestFixedIssuesIssueVisitor(PullRequestFixedIssueRepository pullRequestFixedIssueRepository,
                                              AnalysisMetadataHolder analysisMetadataHolder,
                                              Tracker<DefaultIssue, DefaultIssue> tracker) {
        this.pullRequestFixedIssueRepository = pullRequestFixedIssueRepository;
        this.analysisMetadataHolder = analysisMetadataHolder;
        this.tracker = tracker;
    }

    @Override
    public void onRawIssues(Component component, Input<DefaultIssue> rawIssues, Input<DefaultIssue> baseIssues) {
        if (!analysisMetadataHolder.isPullRequest() || baseIssues == null) {
            return;
        }

        for (DefaultIssue fixedIssue : findFixedIssues(rawIssues, baseIssues)) {
            pullRequestFixedIssueRepository.addFixedIssue(fixedIssue);
        }
    }

    private List<DefaultIssue> findFixedIssues(Input<DefaultIssue> rawIssues, Input<DefaultIssue> baseIssues) {
        List<DefaultIssue> nonClosedBaseIssues = baseIssues.getIssues().stream()
            .filter(issue -> Optional.ofNullable(issue.issueStatus())
                .map(NON_CLOSED_ISSUE_STATUSES::contains)
                .orElse(false))
            .collect(Collectors.toList());
        Input<DefaultIssue> trackingIssues = new DefaultTrackingInput(nonClosedBaseIssues, baseIssues.getLineHashSequence(), baseIssues.getBlockHashSequence());
        NonClosedTracking<DefaultIssue, DefaultIssue> nonClosedTrackedBaseIssues = tracker.trackNonClosed(rawIssues, trackingIssues);

        List<DefaultIssue> fixedIssues = new ArrayList<>();
        fixedIssues.addAll(findFixedIssues(nonClosedTrackedBaseIssues));
        fixedIssues.addAll(nonClosedTrackedBaseIssues.getUnmatchedBases().collect(Collectors.toList()));
        return fixedIssues;
    }

    private static List<DefaultIssue> findFixedIssues(Tracking<DefaultIssue, DefaultIssue> nonClosedIssues) {
        return nonClosedIssues.getMatchedRaws().entrySet().stream()
            .filter(issueEntry -> issueEntry.getKey().issueStatus() == IssueStatus.FIXED)
            .map(Map.Entry::getValue)
            .collect(Collectors.toList());
    }

}
