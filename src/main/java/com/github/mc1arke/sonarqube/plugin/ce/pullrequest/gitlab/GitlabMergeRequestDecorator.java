/*
 * Copyright (C) 2020-2024 Markus Heberling, Michael Clarke
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

import com.github.mc1arke.sonarqube.plugin.almclient.gitlab.GitlabClient;
import com.github.mc1arke.sonarqube.plugin.almclient.gitlab.GitlabClientFactory;
import com.github.mc1arke.sonarqube.plugin.almclient.gitlab.model.Commit;
import com.github.mc1arke.sonarqube.plugin.almclient.gitlab.model.CommitNote;
import com.github.mc1arke.sonarqube.plugin.almclient.gitlab.model.Discussion;
import com.github.mc1arke.sonarqube.plugin.almclient.gitlab.model.MergeRequest;
import com.github.mc1arke.sonarqube.plugin.almclient.gitlab.model.MergeRequestNote;
import com.github.mc1arke.sonarqube.plugin.almclient.gitlab.model.Note;
import com.github.mc1arke.sonarqube.plugin.almclient.gitlab.model.PipelineStatus;
import com.github.mc1arke.sonarqube.plugin.almclient.gitlab.model.User;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.AnalysisDetails;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.DiscussionAwarePullRequestDecorator;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.PostAnalysisIssueVisitor;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.markup.MarkdownFormatterFactory;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.report.AnalysisIssueSummary;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.report.AnalysisSummary;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.report.ReportGenerator;
import org.sonar.api.ce.posttask.QualityGate;
import org.sonar.ce.task.projectanalysis.scm.ScmInfoRepository;
import org.sonar.db.alm.setting.ALM;
import org.sonar.db.alm.setting.AlmSettingDto;
import org.sonar.db.alm.setting.ProjectAlmSettingDto;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class GitlabMergeRequestDecorator extends DiscussionAwarePullRequestDecorator<GitlabClient, MergeRequest, User, Discussion, Note> {

    public static final String PULLREQUEST_GITLAB_PROJECT_URL = "sonar.pullrequest.gitlab.projectUrl";
    public static final String PULLREQUEST_GITLAB_PIPELINE_ID =
            "com.github.mc1arke.sonarqube.plugin.branch.pullrequest.gitlab.pipelineId";

    private final GitlabClientFactory gitlabClientFactory;
    private final MarkdownFormatterFactory formatterFactory;

    public GitlabMergeRequestDecorator(ScmInfoRepository scmInfoRepository, GitlabClientFactory gitlabClientFactory, ReportGenerator reportGenerator, MarkdownFormatterFactory formatterFactory) {
        super(scmInfoRepository, reportGenerator);
        this.gitlabClientFactory = gitlabClientFactory;
        this.formatterFactory = formatterFactory;
    }

    @Override
    public List<ALM> alm() {
        return Collections.singletonList(ALM.GITLAB);
    }

    @Override
    protected GitlabClient createClient(AlmSettingDto almSettingDto, ProjectAlmSettingDto projectAlmSettingDto) {
        return gitlabClientFactory.createClient(projectAlmSettingDto, almSettingDto);
    }

    @Override
    protected Optional<String> createFrontEndUrl(MergeRequest mergeRequest, AnalysisDetails analysis) {
        return Optional.of(analysis.getScannerProperty(PULLREQUEST_GITLAB_PROJECT_URL)
                .map(url -> String.format("%s/merge_requests/%s", url, mergeRequest.getIid()))
                .orElse(mergeRequest.getWebUrl()));
    }

    @Override
    protected MergeRequest getPullRequest(GitlabClient client, AlmSettingDto almSettingDto, ProjectAlmSettingDto projectAlmSettingDto, AnalysisDetails analysis) {
        String projectId = projectAlmSettingDto.getAlmRepo();
        long mergeRequestIid;
        try {
            mergeRequestIid = Long.parseLong(analysis.getPullRequestId());
        } catch (NumberFormatException ex) {
            throw new IllegalStateException("Could not parse Merge Request ID", ex);
        }
        try {
            return client.getMergeRequest(projectId, mergeRequestIid);
        } catch (IOException ex) {
            throw new IllegalStateException("Could not retrieve Merge Request details", ex);
        }
    }

    @Override
    protected User getCurrentUser(GitlabClient gitlabClient) {
        try {
            return gitlabClient.getCurrentUser();
        } catch (IOException ex) {
            throw new IllegalStateException("Could not retrieve current user details", ex);
        }
    }

    @Override
    protected List<String> getCommitIdsForPullRequest(GitlabClient gitlabClient, MergeRequest mergeRequest) {
        try {
            return gitlabClient.getMergeRequestCommits(mergeRequest.getTargetProjectId(), mergeRequest.getIid()).stream()
                    .map(Commit::getId)
                    .collect(Collectors.toList());
        } catch (IOException ex) {
            throw new IllegalStateException("Could not retrieve commit details for Merge Request", ex);
        }
    }

    @Override
    protected void submitPipelineStatus(GitlabClient gitlabClient, MergeRequest mergeRequest, AnalysisDetails analysis,
                                        AnalysisSummary analysisSummary) {
        Long pipelineId = analysis.getScannerProperty(PULLREQUEST_GITLAB_PIPELINE_ID)
                .map(Long::parseLong)
                .orElse(null);

        try {
            PipelineStatus pipelineStatus = new PipelineStatus("SonarQube",
                    "SonarQube Status",
                    analysis.getQualityGateStatus() == QualityGate.Status.OK ? PipelineStatus.State.SUCCESS : PipelineStatus.State.FAILED,
                    analysisSummary.getDashboardUrl(),
                    analysisSummary.getNewCoverage(),
                    pipelineId);

            gitlabClient.setMergeRequestPipelineStatus(mergeRequest.getTargetProjectId(), analysis.getCommitSha(), pipelineStatus);
        } catch (IOException ex) {
            throw new IllegalStateException("Could not update pipeline status in Gitlab", ex);
        }
    }

    @Override
    protected void submitCommitNoteForIssue(GitlabClient client, MergeRequest mergeRequest, PostAnalysisIssueVisitor.ComponentIssue issue, String path, AnalysisDetails analysis, AnalysisIssueSummary analysisIssueSummary) {
        Integer line = Optional.ofNullable(issue.getIssue().getLine()).orElseThrow(() -> new IllegalStateException("No line is associated with this issue"));

        try {
            client.addMergeRequestDiscussion(mergeRequest.getTargetProjectId(), mergeRequest.getIid(),
                    new CommitNote(analysisIssueSummary.format(formatterFactory),
                    mergeRequest.getDiffRefs().getBaseSha(),
                    mergeRequest.getDiffRefs().getStartSha(),
                    mergeRequest.getDiffRefs().getHeadSha(),
                    path,
                    path,
                    line));
        } catch (IOException ex) {
            throw new IllegalStateException("Could not submit commit comment to Gitlab", ex);
        }
    }

    @Override
    protected void submitSummaryNote(GitlabClient client, MergeRequest mergeRequest, AnalysisDetails analysis, AnalysisSummary analysisSummary) {
        try {
            Discussion summaryComment = client.addMergeRequestDiscussion(mergeRequest.getTargetProjectId(),
                    mergeRequest.getIid(),
                    new MergeRequestNote(analysisSummary.format(formatterFactory)));
            if (analysis.getQualityGateStatus() == QualityGate.Status.OK) {
                client.resolveMergeRequestDiscussion(mergeRequest.getTargetProjectId(), mergeRequest.getIid(), summaryComment.getId());
            }
        } catch (IOException ex) {
            throw new IllegalStateException("Could not submit summary comment to Gitlab", ex);
        }

    }

    @Override
    protected List<Discussion> getDiscussions(GitlabClient client, MergeRequest pullRequest) {
        try {
            return client.getMergeRequestDiscussions(pullRequest.getTargetProjectId(), pullRequest.getIid());
        } catch (IOException ex) {
            throw new IllegalStateException("Could not retrieve Merge Request discussions", ex);
        }
    }

    @Override
    protected boolean isNoteFromCurrentUser(Note note, User user) {
        return user.getUsername().equals(note.getAuthor().getUsername());
    }

    @Override
    protected String getNoteContent(GitlabClient client, Note note) {
        return note.getBody();
    }

    @Override
    protected List<Note> getNotesForDiscussion(GitlabClient client, Discussion discussion) {
        return discussion.getNotes();
    }

    @Override
    protected boolean isClosed(Discussion discussion, List<Note> notesInDiscussion) {
        return notesInDiscussion.stream()
                .findFirst()
                .map(Note::isResolved)
                .orElse(true);
    }

    @Override
    protected boolean isUserNote(Note note) {
        return !note.isSystem();
    }

    @Override
    protected void addNoteToDiscussion(GitlabClient client, Discussion discussion, MergeRequest pullRequest, String note) {
        try {
            client.addMergeRequestDiscussionNote(pullRequest.getTargetProjectId(), pullRequest.getIid(), discussion.getId(), note);
        } catch (IOException ex) {
            throw new IllegalStateException("Could not add note to Merge Request discussion", ex);
        }
    }

    @Override
    protected void resolveDiscussion(GitlabClient client, Discussion discussion, MergeRequest pullRequest) {
        try {
            client.resolveMergeRequestDiscussion(pullRequest.getTargetProjectId(), pullRequest.getIid(), discussion.getId());
        } catch (IOException ex) {
            throw new IllegalStateException("Could not resolve Merge Request discussion", ex);
        }
    }

    @Override
    protected void deleteDiscussion(GitlabClient client, Discussion discussion, MergeRequest pullRequest, List<Note> notesForDiscussion) {
        try {
            for (Note note : notesForDiscussion) {
                client.deleteMergeRequestDiscussionNote(pullRequest.getTargetProjectId(), pullRequest.getIid(), discussion.getId(), note.getId());
            }
        } catch (IOException ex) {
            throw new IllegalStateException("Could not delete Merge Request discussion", ex);
        }
    }

}
