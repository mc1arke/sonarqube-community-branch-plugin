/*
 * Copyright (C) 2021-2024 Michael Clarke
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

import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.report.AnalysisIssueSummary;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.report.AnalysisSummary;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.report.ReportGenerator;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.ImmutableTriple;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.sonar.ce.task.projectanalysis.scm.Changeset;
import org.sonar.ce.task.projectanalysis.scm.ScmInfoRepository;
import org.sonar.db.alm.setting.AlmSettingDto;
import org.sonar.db.alm.setting.ProjectAlmSettingDto;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public abstract class DiscussionAwarePullRequestDecorator<C, P, U, D, N> implements PullRequestBuildStatusDecorator {

    private static final String RESOLVED_ISSUE_NEEDING_CLOSED_MESSAGE =
            "This issue no longer exists in SonarQube, but due to other comments being present in this discussion, the discussion is not being being closed automatically. " +
                    "Please manually resolve this discussion once the other comments have been reviewed.";
    private static final String RESOLVED_SUMMARY_NEEDING_CLOSED_MESSAGE =
            "This summary note is outdated, but due to other comments being present in this discussion, the discussion is not being being removed. " +
                    "Please manually resolve this discussion once the other comments have been reviewed.";

    private static final String VIEW_IN_SONARQUBE_LABEL = "View in SonarQube";
    private static final Pattern NOTE_MARKDOWN_VIEW_LINK_PATTERN = Pattern.compile("^\\[" + VIEW_IN_SONARQUBE_LABEL + "]\\((.*?)\\)$");
    private static final String DECORATOR_SUMMARY_COMMENT = "decorator-summary-comment";

    private final ScmInfoRepository scmInfoRepository;
    private final ReportGenerator reportGenerator;

    protected DiscussionAwarePullRequestDecorator(ScmInfoRepository scmInfoRepository, ReportGenerator reportGenerator) {
        super();
        this.scmInfoRepository = scmInfoRepository;
        this.reportGenerator = reportGenerator;
    }

    @Override
    public DecorationResult decorateQualityGateStatus(AnalysisDetails analysis, AlmSettingDto almSettingDto,
                                                      ProjectAlmSettingDto projectAlmSettingDto) {
        C client = createClient(almSettingDto, projectAlmSettingDto);
        
        P pullRequest = getPullRequest(client, almSettingDto, projectAlmSettingDto, analysis);
        U user = getCurrentUser(client);
        List<PostAnalysisIssueVisitor.ComponentIssue> openSonarqubeIssues = analysis.getScmReportableIssues();

        List<Triple<D, N, Optional<ProjectIssueIdentifier>>> currentProjectSonarqubeComments = findOpenSonarqubeComments(client,
                pullRequest,
                user)
                .stream()
                .filter(comment -> !projectAlmSettingDto.getMonorepo() || isCommentFromCurrentProject(comment, analysis.getAnalysisProjectKey()))
                .collect(Collectors.toList());

        List<String> commentKeysForOpenComments = closeOldDiscussionsAndExtractRemainingKeys(client,
                user,
                currentProjectSonarqubeComments,
                openSonarqubeIssues,
                pullRequest);

        List<String> commitIds = getCommitIdsForPullRequest(client, pullRequest);
        List<Pair<PostAnalysisIssueVisitor.ComponentIssue, String>> uncommentedIssues = findIssuesWithoutComments(openSonarqubeIssues,
                commentKeysForOpenComments)
                .stream()
                .map(DiscussionAwarePullRequestDecorator::loadScmPathsForIssues)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .filter(issue -> isIssueFromCommitInCurrentRequest(issue.getLeft(), commitIds, scmInfoRepository))
                .collect(Collectors.toList());

        uncommentedIssues.forEach(issue -> submitCommitNoteForIssue(client,
                pullRequest,
                issue.getLeft(),
                issue.getRight(),
                analysis,
                reportGenerator.createAnalysisIssueSummary(issue.getLeft(), analysis)));

        AnalysisSummary analysisSummary = reportGenerator.createAnalysisSummary(analysis);
        submitSummaryNote(client, pullRequest, analysis, analysisSummary);
        submitPipelineStatus(client, pullRequest, analysis, analysisSummary);

        DecorationResult.Builder builder = DecorationResult.builder();
        createFrontEndUrl(pullRequest, analysis).ifPresent(builder::withPullRequestUrl);
        return builder.build();
    }

    protected abstract C createClient(AlmSettingDto almSettingDto, ProjectAlmSettingDto projectAlmSettingDto);

    protected abstract Optional<String> createFrontEndUrl(P pullRequest, AnalysisDetails analysisDetails);

    protected abstract P getPullRequest(C client, AlmSettingDto almSettingDto, ProjectAlmSettingDto projectAlmSettingDto, AnalysisDetails analysis);

    protected abstract U getCurrentUser(C client);

    protected abstract List<String> getCommitIdsForPullRequest(C client, P pullRequest);

    protected abstract void submitPipelineStatus(C client, P pullRequest, AnalysisDetails analysis, AnalysisSummary analysisSummary);

    protected abstract void submitCommitNoteForIssue(C client, P pullRequest, PostAnalysisIssueVisitor.ComponentIssue issue, String filePath,
                                                     AnalysisDetails analysis, AnalysisIssueSummary analysisIssueSummary);

    protected abstract String getNoteContent(C client, N note);

    protected abstract List<N> getNotesForDiscussion(C client, D discussion);

    protected abstract boolean isClosed(D discussion, List<N> notesInDiscussion);

    protected abstract boolean isUserNote(N note);

    protected abstract void addNoteToDiscussion(C client, D discussion, P pullRequest, String note);

    protected abstract void resolveDiscussion(C client, D discussion, P pullRequest);

    protected abstract void deleteDiscussion(C client, D discussion, P pullRequest, List<N> notesForDiscussion);

    protected abstract void submitSummaryNote(C client, P pullRequest, AnalysisDetails analysis, AnalysisSummary analysisSummary);

    protected abstract List<D> getDiscussions(C client, P pullRequest);

    protected abstract boolean isNoteFromCurrentUser(N note, U user);

    private static List<PostAnalysisIssueVisitor.ComponentIssue> findIssuesWithoutComments(List<PostAnalysisIssueVisitor.ComponentIssue> openSonarqubeIssues,
                                                                                           List<String> openGitlabIssueIdentifiers) {
        return openSonarqubeIssues.stream()
                .filter(issue -> !openGitlabIssueIdentifiers.contains(issue.getIssue().key()))
                .filter(issue -> issue.getIssue().getLine() != null)
                .collect(Collectors.toList());
    }

    private static Optional<Pair<PostAnalysisIssueVisitor.ComponentIssue, String>> loadScmPathsForIssues(PostAnalysisIssueVisitor.ComponentIssue componentIssue) {
        return Optional.of(componentIssue)
                .map(issue -> new ImmutablePair<>(issue, issue.getScmPath()))
                .filter(pair -> pair.getRight().isPresent())
                .map(pair -> new ImmutablePair<>(pair.getLeft(), pair.getRight().get()));
    }

    private static boolean isIssueFromCommitInCurrentRequest(PostAnalysisIssueVisitor.ComponentIssue componentIssue, List<String> commitIds, ScmInfoRepository scmInfoRepository) {
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

    private List<Triple<D, N, Optional<ProjectIssueIdentifier>>> findOpenSonarqubeComments(C client, P pullRequest,
                                                                                           U currentUser) {
        return getDiscussions(client, pullRequest).stream()
                .map(discussion -> {
                    List<N> commentsForDiscussion = getNotesForDiscussion(client, discussion);
                    return commentsForDiscussion.stream()
                        .findFirst()
                        .filter(note -> isNoteFromCurrentUser(note, currentUser))
                        .filter(note -> !isResolved(client, discussion, commentsForDiscussion, currentUser) || isSummaryComment(client, commentsForDiscussion.stream().findFirst().orElse(null)))
                        .map(note -> new ImmutableTriple<>(discussion, note, parseIssueDetails(client, note)));
                })
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());
    }

    private boolean isSummaryComment(C client, N note) {
        return Optional.of(note)
            .flatMap(message -> parseIssueDetails(client, message))
            .filter(projectIssueIdentifier -> DECORATOR_SUMMARY_COMMENT.equals(projectIssueIdentifier.getIssueKey()))
            .isPresent();
    }

    private List<String> closeOldDiscussionsAndExtractRemainingKeys(C client, U currentUser,
                                                                    List<Triple<D, N, Optional<ProjectIssueIdentifier>>> openSonarqubeComments,
                                                                    List<PostAnalysisIssueVisitor.ComponentIssue> openIssues,
                                                                    P pullRequest) {
        List<String> openIssueKeys = openIssues.stream()
                .map(issue -> issue.getIssue().key())
                .collect(Collectors.toList());

        List<String> remainingCommentKeys = new ArrayList<>();

        for (Triple<D, N, Optional<ProjectIssueIdentifier>> openSonarqubeComment : openSonarqubeComments) {
            Optional<ProjectIssueIdentifier> noteIdentifier = openSonarqubeComment.getRight();
            D discussion = openSonarqubeComment.getLeft();
            if (noteIdentifier.isEmpty()) {
                continue;
            }

            String issueKey = noteIdentifier.get().getIssueKey();
            if (DECORATOR_SUMMARY_COMMENT.equals(issueKey)) {
                deleteOrPlaceFinalCommentOnDiscussion(client, currentUser, discussion, pullRequest);
            } else if (!openIssueKeys.contains(issueKey)) {
                resolveOrPlaceFinalCommentOnDiscussion(client, currentUser, discussion, pullRequest);
            } else {
                remainingCommentKeys.add(issueKey);
            }
        }

        return remainingCommentKeys;
    }

    private boolean isResolved(C client, D discussion, List<N> notesInDiscussion, U currentUser) {
        return isClosed(discussion, notesInDiscussion) || notesInDiscussion.stream()
                .filter(message -> isNoteFromCurrentUser(message, currentUser))
                .map(message -> getNoteContent(client, message))
                .anyMatch(content -> RESOLVED_ISSUE_NEEDING_CLOSED_MESSAGE.equals(content) || RESOLVED_SUMMARY_NEEDING_CLOSED_MESSAGE.equals(content));
    }

    private void resolveOrPlaceFinalCommentOnDiscussion(C client, U currentUser, D discussion, P pullRequest) {
        if (getNotesForDiscussion(client, discussion).stream()
                .filter(this::isUserNote)
                .anyMatch(note -> !isNoteFromCurrentUser(note, currentUser))) {
            addNoteToDiscussion(client, discussion, pullRequest, RESOLVED_ISSUE_NEEDING_CLOSED_MESSAGE);
        } else {
            resolveDiscussion(client, discussion, pullRequest);
        }

    }

    private void deleteOrPlaceFinalCommentOnDiscussion(C client, U currentUser, D discussion, P pullRequest) {
        List<N> notesForDiscussion = getNotesForDiscussion(client, discussion);
        if (notesForDiscussion.stream()
            .filter(this::isUserNote)
            .anyMatch(note -> !isNoteFromCurrentUser(note, currentUser))) {
            addNoteToDiscussion(client, discussion, pullRequest, RESOLVED_SUMMARY_NEEDING_CLOSED_MESSAGE);
        } else {
            deleteDiscussion(client, discussion, pullRequest, notesForDiscussion);
        }

    }

    protected Optional<ProjectIssueIdentifier> parseIssueDetails(C client, N note) {
        return parseIssueDetails(client, note, VIEW_IN_SONARQUBE_LABEL, NOTE_MARKDOWN_VIEW_LINK_PATTERN);
    }

    protected Optional<ProjectIssueIdentifier> parseIssueDetails(C client, N note, String label, Pattern pattern) {
        String noteContent = getNoteContent(client, note);
        if (noteContent == null) {
            return Optional.empty();
        }
        try (BufferedReader reader = new BufferedReader(new StringReader(noteContent))) {
            return reader.lines()
                    .filter(line -> line.contains(label))
                    .map(line -> parseIssueLineDetails(line, pattern))
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .findFirst();
        } catch (IOException ex) {
            throw new IllegalStateException("Could not parse details from note", ex);
        }
    }

    private static Optional<ProjectIssueIdentifier> parseIssueLineDetails(String noteLine, Pattern pattern) {
        Matcher identifierMatcher = pattern.matcher(noteLine);

        if (identifierMatcher.matches()) {
            return parseIssueIdFromUrl(identifierMatcher.group(1));
        } else {
            return Optional.empty();
        }
    }

    private static Optional<ProjectIssueIdentifier> parseIssueIdFromUrl(String issueUrl) {
        URI url = URI.create(issueUrl);
        List<NameValuePair> parameters = URLEncodedUtils.parse(url, StandardCharsets.UTF_8);
        Optional<String> optionalProjectId = parameters.stream()
                .filter(parameter -> "id".equals(parameter.getName()))
                .map(NameValuePair::getValue)
                .findFirst();

        if (optionalProjectId.isEmpty()) {
            return Optional.empty();
        }

        String projectId = optionalProjectId.get();

        if (url.getPath().endsWith("/dashboard")) {
            return Optional.of(new ProjectIssueIdentifier(projectId, DECORATOR_SUMMARY_COMMENT));
        } else if (url.getPath().endsWith("security_hotspots")) {
            return parameters.stream()
                    .filter(parameter -> "hotspots".equals(parameter.getName()))
                    .map(NameValuePair::getValue)
                    .findFirst()
                    .map(issueId -> new ProjectIssueIdentifier(projectId, issueId));
        } else {
            return parameters.stream()
                    .filter(parameter -> "issues".equals(parameter.getName()))
                    .map(NameValuePair::getValue)
                    .findFirst()
                    .map(issueId -> new ProjectIssueIdentifier(projectId, issueId));
        }
    }

    private static boolean isCommentFromCurrentProject(Triple<?, ?, Optional<ProjectIssueIdentifier>> comment, String projectId) {
        return comment.getRight().filter(projectIssueIdentifier -> projectId.equals(projectIssueIdentifier.getProjectKey())).isPresent();
    }

    protected static class ProjectIssueIdentifier {

        private final String projectKey;
        private final String issueKey;

        private ProjectIssueIdentifier(String projectKey, String issueKey) {
            this.projectKey = projectKey;
            this.issueKey = issueKey;
        }

        public String getProjectKey() {
            return projectKey;
        }

        public String getIssueKey() {
            return issueKey;
        }
    }
}
