/*
 * Copyright (C) 2020-2021 Markus Heberling, Michael Clarke
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
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.DecorationResult;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.PostAnalysisIssueVisitor;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.PullRequestBuildStatusDecorator;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.gitlab.model.Commit;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.gitlab.model.CommitNote;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.gitlab.model.Discussion;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.gitlab.model.MergeRequest;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.gitlab.model.MergeRequestNote;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.gitlab.model.Note;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.gitlab.model.PipelineStatus;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.gitlab.model.User;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.markup.FormatterFactory;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.markup.MarkdownFormatterFactory;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.ImmutableTriple;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.sonar.api.ce.posttask.QualityGate;
import org.sonar.api.issue.Issue;
import org.sonar.api.platform.Server;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.ce.task.projectanalysis.scm.Changeset;
import org.sonar.ce.task.projectanalysis.scm.ScmInfoRepository;
import org.sonar.db.alm.setting.ALM;
import org.sonar.db.alm.setting.AlmSettingDto;
import org.sonar.db.alm.setting.ProjectAlmSettingDto;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class GitlabMergeRequestDecorator implements PullRequestBuildStatusDecorator {

    public static final String PULLREQUEST_GITLAB_INSTANCE_URL = "sonar.pullrequest.gitlab.instanceUrl";
    public static final String PULLREQUEST_GITLAB_PROJECT_ID = "sonar.pullrequest.gitlab.projectId";
    public static final String PULLREQUEST_GITLAB_PROJECT_URL = "sonar.pullrequest.gitlab.projectUrl";
    public static final String PULLREQUEST_GITLAB_PIPELINE_ID =
            "com.github.mc1arke.sonarqube.plugin.branch.pullrequest.gitlab.pipelineId";

    private static final Logger LOGGER = Loggers.get(GitlabMergeRequestDecorator.class);

    private static final String RESOLVED_ISSUE_NEEDING_CLOSED_MESSAGE =
            "This issue no longer exists in SonarQube, but due to other comments being present in this discussion, the discussion is not being being closed automatically. " +
                    "Please manually resolve this discussion once the other comments have been reviewed.";

    private static final List<String> OPEN_ISSUE_STATUSES =
            Issue.STATUSES.stream().filter(s -> !Issue.STATUS_CLOSED.equals(s) && !Issue.STATUS_RESOLVED.equals(s))
                    .collect(Collectors.toList());

    private static final Pattern NOTE_VIEW_LINK_PATTERN = Pattern.compile("^\\[View in SonarQube]\\((.*?)\\)$");

    private final Server server;
    private final ScmInfoRepository scmInfoRepository;
    private final GitlabClientFactory gitlabClientFactory;

    public GitlabMergeRequestDecorator(Server server, ScmInfoRepository scmInfoRepository, GitlabClientFactory gitlabClientFactory) {
        super();
        this.server = server;
        this.scmInfoRepository = scmInfoRepository;
        this.gitlabClientFactory = gitlabClientFactory;
    }

    @Override
    public DecorationResult decorateQualityGateStatus(AnalysisDetails analysis, AlmSettingDto almSettingDto,
                                                      ProjectAlmSettingDto projectAlmSettingDto) {
        String apiURL = Optional.ofNullable(StringUtils.stripToNull(almSettingDto.getUrl()))
                .orElseGet(() -> getMandatoryScannerProperty(analysis, PULLREQUEST_GITLAB_INSTANCE_URL));
        String projectId = Optional.ofNullable(StringUtils.stripToNull(projectAlmSettingDto.getAlmRepo()))
                .orElseGet(() -> getMandatoryScannerProperty(analysis, PULLREQUEST_GITLAB_PROJECT_ID));
        long mergeRequestIid;
        try {
            mergeRequestIid = Long.parseLong(analysis.getBranchName());
        } catch (NumberFormatException ex) {
            throw new IllegalStateException("Could not parse Merge Request ID", ex);
        }
        String apiToken = almSettingDto.getPersonalAccessToken();

        GitlabClient gitlabClient = gitlabClientFactory.createClient(apiURL, apiToken);

        MergeRequest mergeRequest = getMergeRequest(gitlabClient, projectId, mergeRequestIid);
        User user = getCurrentGitlabUser(gitlabClient);
        List<String> commitIds = getCommitIdsForMergeRequest(gitlabClient, mergeRequest);
        List<PostAnalysisIssueVisitor.ComponentIssue> openSonarqubeIssues = analysis.getPostAnalysisIssueVisitor().getIssues().stream()
                .filter(i -> OPEN_ISSUE_STATUSES.contains(i.getIssue().getStatus()))
                .collect(Collectors.toList());

        List<Triple<Discussion, Note, Optional<String>>> currentProjectSonarqueComments = findOpenSonarqubeComments(gitlabClient,
                mergeRequest,
                user.getUsername(),
                analysis);

        List<String> commentKeysForOpenComments = closeOldDiscussionsAndExtractRemainingKeys(gitlabClient,
                user.getUsername(),
                currentProjectSonarqueComments,
                openSonarqubeIssues,
                mergeRequest);

        List<Pair<PostAnalysisIssueVisitor.ComponentIssue, String>> uncommentedIssues = findIssuesWithoutComments(openSonarqubeIssues,
                commentKeysForOpenComments)
                .stream()
                .map(issue -> loadScmPathsForIssues(issue, analysis))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .filter(issue -> isIssueFromCommitInCurrentMergeRequest(issue.getLeft(), commitIds, scmInfoRepository))
                .collect(Collectors.toList());

        FormatterFactory formatterFactory = new MarkdownFormatterFactory();

        uncommentedIssues.forEach(issue -> submitCommitCommentForIssue(issue.getLeft(),
                issue.getRight(),
                gitlabClient,
                mergeRequest,
                analysis,
                formatterFactory));
        submitSummaryComment(gitlabClient, analysis, mergeRequest, formatterFactory);
        submitPipelineStatus(gitlabClient, analysis, mergeRequest, server.getPublicRootUrl());

        String mergeRequestHtmlUrl = analysis.getScannerProperty(PULLREQUEST_GITLAB_PROJECT_URL)
                .map(url -> String.format("%s/merge_requests/%s", url, mergeRequestIid))
                .orElse(mergeRequest.getWebUrl());

        return DecorationResult.builder().withPullRequestUrl(mergeRequestHtmlUrl).build();
    }

    @Override
    public List<ALM> alm() {
        return Collections.singletonList(ALM.GITLAB);
    }

    private static MergeRequest getMergeRequest(GitlabClient gitlabClient, String projectId, long mergeRequestIid) {
        try {
            return gitlabClient.getMergeRequest(projectId, mergeRequestIid);
        } catch (IOException ex) {
            throw new IllegalStateException("Could not retrieve Merge Request details", ex);
        }
    }

    private static User getCurrentGitlabUser(GitlabClient gitlabClient) {
        try {
            return gitlabClient.getCurrentUser();
        } catch (IOException ex) {
            throw new IllegalStateException("Could not retrieve current user details", ex);
        }
    }

    private static List<String> getCommitIdsForMergeRequest(GitlabClient gitlabClient, MergeRequest mergeRequest) {
        try {
            return gitlabClient.getMergeRequestCommits(mergeRequest.getSourceProjectId(), mergeRequest.getIid()).stream()
                    .map(Commit::getId)
                    .collect(Collectors.toList());
        } catch (IOException ex) {
            throw new IllegalStateException("Could not retrieve commit details for Merge Request", ex);
        }
    }

    private static void submitPipelineStatus(GitlabClient gitlabClient, AnalysisDetails analysis, MergeRequest mergeRequest, String sonarqubeRootUrl) {
        try {
            Long pipelineId = analysis.getScannerProperty(PULLREQUEST_GITLAB_PIPELINE_ID)
                    .map(Long::parseLong)
                    .orElse(null);

            BigDecimal coverage = analysis.getCoverage().orElse(null);

            String dashboardUrl = String.format(
                    "%s/dashboard?id=%s&pullRequest=%s",
                    sonarqubeRootUrl,
                    URLEncoder.encode(analysis.getAnalysisProjectKey(), StandardCharsets.UTF_8.name()),
                    URLEncoder.encode(analysis.getBranchName(), StandardCharsets.UTF_8.name()));

            PipelineStatus pipelineStatus = new PipelineStatus("SonarQube",
                    "SonarQube Status",
                    analysis.getQualityGateStatus() == QualityGate.Status.OK ? PipelineStatus.State.SUCCESS : PipelineStatus.State.FAILED,
                    dashboardUrl,
                    coverage,
                    pipelineId);

            gitlabClient.setMergeRequestPipelineStatus(mergeRequest.getSourceProjectId(), analysis.getCommitSha(), pipelineStatus);
        } catch (IOException ex) {
            throw new IllegalStateException("Could not update pipeline status in Gitlab", ex);
        }
    }

    private static String getMandatoryScannerProperty(AnalysisDetails analysis, String propertyName) {
        return analysis.getScannerProperty(propertyName)
                .orElseThrow(() -> new IllegalStateException(String.format("'%s' has not been set in scanner properties", propertyName)));
    }

    private static List<PostAnalysisIssueVisitor.ComponentIssue> findIssuesWithoutComments(List<PostAnalysisIssueVisitor.ComponentIssue> openSonarqubeIssues,
                                                                                           List<String> openGitlabIssueIdentifiers) {
        return openSonarqubeIssues.stream()
                .filter(issue -> !openGitlabIssueIdentifiers.contains(issue.getIssue().key()))
                .filter(issue -> issue.getIssue().getLine() != null)
                .collect(Collectors.toList());
    }

    private static Optional<Pair<PostAnalysisIssueVisitor.ComponentIssue, String>> loadScmPathsForIssues(PostAnalysisIssueVisitor.ComponentIssue componentIssue,
                                                                                                         AnalysisDetails analysis) {
        return Optional.of(componentIssue)
                .map(issue -> new ImmutablePair<>(issue, analysis.getSCMPathForIssue(issue)))
                .filter(pair -> pair.getRight().isPresent())
                .map(pair -> new ImmutablePair<>(pair.getLeft(), pair.getRight().get()));
    }

    private static void submitCommitCommentForIssue(PostAnalysisIssueVisitor.ComponentIssue issue, String path,
                                                    GitlabClient gitlabClient, MergeRequest mergeRequest,
                                                    AnalysisDetails analysis, FormatterFactory formatterFactory) {
        String issueSummary = analysis.createAnalysisIssueSummary(issue, formatterFactory);

        Integer line = Optional.ofNullable(issue.getIssue().getLine()).orElseThrow(() -> new IllegalStateException("No line is associated with this issue"));

        try {
            gitlabClient.addMergeRequestDiscussion(mergeRequest.getSourceProjectId(), mergeRequest.getIid(), new CommitNote(issueSummary,
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

    private static boolean isIssueFromCommitInCurrentMergeRequest(PostAnalysisIssueVisitor.ComponentIssue componentIssue, List<String> commitIds, ScmInfoRepository scmInfoRepository) {
        return Optional.of(componentIssue)
                .map(issue -> new ImmutablePair<>(issue.getIssue(), scmInfoRepository.getScmInfo(issue.getComponent())))
                .filter(issuePair -> issuePair.getRight().isPresent())
                .map(issuePair -> new ImmutablePair<>(issuePair.getLeft(), issuePair.getRight().get()))
                .filter(issuePair -> null != issuePair.getLeft().getLine())
                .filter(issuePair -> issuePair.getRight().hasChangesetForLine(issuePair.getLeft().getLine()))
                .map(issuePair -> issuePair.getRight().getChangesetForLine(issuePair.getLeft().getLine()))
                .map(Changeset::getRevision)
                .filter(commitIds::contains)
                .isPresent();
    }

    private static void submitSummaryComment(GitlabClient gitlabClient, AnalysisDetails analysis, MergeRequest mergeRequest,
                                             FormatterFactory formatterFactory) {
        try {
            String summaryCommentBody = analysis.createAnalysisSummary(formatterFactory);
            Discussion summaryComment = gitlabClient.addMergeRequestDiscussion(mergeRequest.getSourceProjectId(),
                    mergeRequest.getIid(),
                    new MergeRequestNote(summaryCommentBody));
            if (analysis.getQualityGateStatus() == QualityGate.Status.OK) {
                gitlabClient.resolveMergeRequestDiscussion(mergeRequest.getSourceProjectId(), mergeRequest.getIid(), summaryComment.getId());
            }
        } catch (IOException ex) {
            throw new IllegalStateException("Could not submit summary comment to Gitlab", ex);
        }

    }

    private static List<Triple<Discussion, Note, Optional<String>>> findOpenSonarqubeComments(GitlabClient gitlabClient,
                                                                                              MergeRequest mergeRequest,
                                                                                              String currentUsername,
                                                                                              AnalysisDetails analysisDetails) {
        try {
            return gitlabClient.getMergeRequestDiscussions(mergeRequest.getSourceProjectId(), mergeRequest.getIid()).stream()
                    .map(discussion -> discussion.getNotes().stream()
                            .findFirst()
                            .filter(note -> currentUsername.equals(note.getAuthor().getUsername()))
                            .filter(note -> !isResolved(discussion, note, currentUsername))
                            .filter(Note::isResolvable)
                            .map(note -> new ImmutableTriple<>(discussion, note, parseIssueDetails(note, analysisDetails))))
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .collect(Collectors.toList());
        } catch (IOException ex) {
            throw new IllegalStateException("Could not retrieve Merge Request discussions", ex);
        }
    }

    private static List<String> closeOldDiscussionsAndExtractRemainingKeys(GitlabClient gitlabClient, String currentUsername,
                                                                               List<Triple<Discussion, Note, Optional<String>>> openSonarqubeComments,
                                                                               List<PostAnalysisIssueVisitor.ComponentIssue> openIssues,
                                                                               MergeRequest mergeRequest) {
        List<String> openIssueKeys = openIssues.stream()
                .map(issue -> issue.getIssue().key())
                .collect(Collectors.toList());

        List<String> remainingCommentKeys = new ArrayList<>();

        for (Triple<Discussion, Note, Optional<String>> openSonarqubeComment : openSonarqubeComments) {
            Optional<String> issueKey = openSonarqubeComment.getRight();
            Discussion discussion = openSonarqubeComment.getLeft();
            if (!issueKey.isPresent()) {
                LOGGER.warn("Note {} was found on discussion {} in Merge Request {} posted by Sonarqube user {}, " +
                        "but Sonarqube issue details could not be parsed from it. " +
                        "This discussion will therefore will not be cleaned up.",
                        openSonarqubeComment.getMiddle().getId(),
                        openSonarqubeComment.getLeft().getId(),
                        mergeRequest.getIid(),
                        currentUsername);
            } else if (!openIssueKeys.contains(issueKey.get())) {
                resolveOrPlaceFinalCommentOnDiscussion(gitlabClient, currentUsername, discussion, mergeRequest);
            } else {
                remainingCommentKeys.add(issueKey.get());
            }
        }

        return remainingCommentKeys;
    }

    private static boolean isResolved(Discussion discussion, Note note, String currentUsername) {
        return note.isResolved() || discussion.getNotes().stream()
                .filter(message -> currentUsername.equals(note.getAuthor().getUsername()))
                .anyMatch(message -> RESOLVED_ISSUE_NEEDING_CLOSED_MESSAGE.equals(message.getBody()));
    }

    private static void resolveOrPlaceFinalCommentOnDiscussion(GitlabClient gitlabClient, String currentUsername,
                                                               Discussion discussion, MergeRequest mergeRequest) {
        try {
            if (discussion.getNotes().stream()
                    .filter(note -> !note.isSystem())
                    .anyMatch(note -> !currentUsername.equals(note.getAuthor().getUsername()))) {
                gitlabClient.addMergeRequestDiscussionNote(mergeRequest.getSourceProjectId(), mergeRequest.getIid(), discussion.getId(), RESOLVED_ISSUE_NEEDING_CLOSED_MESSAGE);
            } else {
                gitlabClient.resolveMergeRequestDiscussion(mergeRequest.getSourceProjectId(), mergeRequest.getIid(), discussion.getId());
            }
        } catch (IOException ex) {
            throw new IllegalStateException("Could not clean-up discussion on Gitlab", ex);
        }
    }

    private static Optional<String> parseIssueDetails(Note note, AnalysisDetails analysisDetails) {
        try (BufferedReader reader = new BufferedReader(new StringReader(note.getBody()))) {
            return reader.lines()
                    .filter(line -> line.contains("View in SonarQube"))
                    .map(line -> parseIssueLineDetails(line, analysisDetails))
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .findFirst();
        } catch (IOException ex) {
            throw new IllegalStateException("Could not parse details from note", ex);
        }
    }

    private static Optional<String> parseIssueLineDetails(String noteLine, AnalysisDetails analysisDetails) {
        Matcher identifierMatcher = NOTE_VIEW_LINK_PATTERN.matcher(noteLine);

        if (identifierMatcher.matches()) {
            return analysisDetails.parseIssueIdFromUrl(identifierMatcher.group(1));
        } else {
            return Optional.empty();
        }
    }

}
