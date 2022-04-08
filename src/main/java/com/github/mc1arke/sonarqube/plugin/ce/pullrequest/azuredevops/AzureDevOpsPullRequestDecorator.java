/*
 * Copyright (C) 2020-2022 Markus Heberling, Michael Clarke
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
package com.github.mc1arke.sonarqube.plugin.ce.pullrequest.azuredevops;

import com.github.mc1arke.sonarqube.plugin.almclient.azuredevops.AzureDevopsClient;
import com.github.mc1arke.sonarqube.plugin.almclient.azuredevops.AzureDevopsClientFactory;
import com.github.mc1arke.sonarqube.plugin.almclient.azuredevops.model.Comment;
import com.github.mc1arke.sonarqube.plugin.almclient.azuredevops.model.CommentPosition;
import com.github.mc1arke.sonarqube.plugin.almclient.azuredevops.model.CommentThread;
import com.github.mc1arke.sonarqube.plugin.almclient.azuredevops.model.CommentThreadContext;
import com.github.mc1arke.sonarqube.plugin.almclient.azuredevops.model.Commit;
import com.github.mc1arke.sonarqube.plugin.almclient.azuredevops.model.CreateCommentRequest;
import com.github.mc1arke.sonarqube.plugin.almclient.azuredevops.model.CreateCommentThreadRequest;
import com.github.mc1arke.sonarqube.plugin.almclient.azuredevops.model.GitPullRequestStatus;
import com.github.mc1arke.sonarqube.plugin.almclient.azuredevops.model.GitStatusContext;
import com.github.mc1arke.sonarqube.plugin.almclient.azuredevops.model.PullRequest;
import com.github.mc1arke.sonarqube.plugin.almclient.azuredevops.model.enums.CommentThreadStatus;
import com.github.mc1arke.sonarqube.plugin.almclient.azuredevops.model.enums.CommentType;
import com.github.mc1arke.sonarqube.plugin.almclient.azuredevops.model.mappers.GitStatusStateMapper;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.AnalysisDetails;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.DiscussionAwarePullRequestDecorator;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.PostAnalysisIssueVisitor;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.PullRequestBuildStatusDecorator;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.markup.MarkdownFormatterFactory;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.ce.posttask.QualityGate;
import org.sonar.api.platform.Server;
import org.sonar.ce.task.projectanalysis.scm.ScmInfoRepository;
import org.sonar.db.alm.setting.ALM;
import org.sonar.db.alm.setting.AlmSettingDto;
import org.sonar.db.alm.setting.ProjectAlmSettingDto;
import org.sonar.db.protobuf.DbIssues;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class AzureDevOpsPullRequestDecorator extends DiscussionAwarePullRequestDecorator<AzureDevopsClient, PullRequest, Void, CommentThread, Comment> implements PullRequestBuildStatusDecorator {

    private static final Pattern NOTE_MARKDOWN_LEGACY_SEE_LINK_PATTERN = Pattern.compile("^\\[See in SonarQube]\\((.*?)\\)$");
    private final AzureDevopsClientFactory azureDevopsClientFactory;
    private final MarkdownFormatterFactory markdownFormatterFactory;

    public AzureDevOpsPullRequestDecorator(Server server, ScmInfoRepository scmInfoRepository, AzureDevopsClientFactory azureDevopsClientFactory, MarkdownFormatterFactory markdownFormatterFactory) {
        super(server, scmInfoRepository);
        this.azureDevopsClientFactory = azureDevopsClientFactory;
        this.markdownFormatterFactory = markdownFormatterFactory;
    }

    @Override
    public List<ALM> alm() {
        return Collections.singletonList(ALM.AZURE_DEVOPS);
    }

    @Override
    protected AzureDevopsClient createClient(AlmSettingDto almSettingDto, ProjectAlmSettingDto projectAlmSettingDto) {
        return azureDevopsClientFactory.createClient(projectAlmSettingDto, almSettingDto);
    }

    @Override
    protected Optional<String> createFrontEndUrl(PullRequest pullRequest, AnalysisDetails analysisDetails) {
        return Optional.of(pullRequest.getRepository().getRemoteUrl() + "/pullRequest/" + pullRequest.getId());
    }

    @Override
    protected PullRequest getPullRequest(AzureDevopsClient client, AlmSettingDto almSettingDto, ProjectAlmSettingDto projectAlmSettingDto, AnalysisDetails analysis) {
        int pullRequestId;
        try {
            pullRequestId = Integer.parseInt(analysis.getBranchName());
        } catch (NumberFormatException ex) {
            throw new IllegalStateException("Could not parse Pull Request Key", ex);
        }
        String projectName = Optional.ofNullable(StringUtils.trimToNull(projectAlmSettingDto.getAlmSlug())).orElseThrow(() -> new IllegalStateException("Repository slug must be provided"));
        String repositoryName = Optional.ofNullable(StringUtils.trimToNull(projectAlmSettingDto.getAlmRepo())).orElseThrow(() -> new IllegalStateException("Repository name must be provided"));
        try {
            return client.retrievePullRequest(projectName, repositoryName, pullRequestId);
        } catch (IOException ex) {
            throw new IllegalStateException("Could not retrieve Pull Request details", ex);
        }
    }

    @Override
    protected Void getCurrentUser(AzureDevopsClient client) {
        return null;
    }

    @Override
    protected List<String> getCommitIdsForPullRequest(AzureDevopsClient client, PullRequest pullRequest) {
        try {
            return client.getPullRequestCommits(pullRequest.getRepository().getProject().getName(), pullRequest.getRepository().getName(), pullRequest.getId()).stream()
                    .map(Commit::getCommitId)
                    .collect(Collectors.toList());
        } catch (IOException ex) {
            throw new IllegalStateException("Could not retrieve commit details for Pull Request", ex);
        }
    }

    @Override
    protected void submitPipelineStatus(AzureDevopsClient client, PullRequest pullRequest, AnalysisDetails analysis, String sonarqubeRootUrl) {
        try {
            GitPullRequestStatus gitPullRequestStatus = new GitPullRequestStatus(
                    GitStatusStateMapper.toGitStatusState(analysis.getQualityGateStatus()),
                    String.format("SonarQube Quality Gate - %s (%s)", analysis.getAnalysisProjectName(), analysis.getAnalysisProjectKey()),
                    new GitStatusContext("sonarqube/qualitygate", analysis.getAnalysisProjectKey()),
                    String.format("%s/dashboard?id=%s&pullRequest=%s",
                            sonarqubeRootUrl,
                            URLEncoder.encode(analysis.getAnalysisProjectKey(), StandardCharsets.UTF_8.name()),
                            URLEncoder.encode(analysis.getBranchName(), StandardCharsets.UTF_8.name())
                    )
            );

            client.submitPullRequestStatus(pullRequest.getRepository().getProject().getName(), pullRequest.getRepository().getName(), pullRequest.getId(), gitPullRequestStatus);
        } catch (IOException ex) {
            throw new IllegalStateException("Could not update pipeline status in Gitlab", ex);
        }
    }

    @Override
    protected void submitCommitNoteForIssue(AzureDevopsClient client, PullRequest pullRequest, PostAnalysisIssueVisitor.ComponentIssue issue, String filePath,
                                            AnalysisDetails analysis) {
        String issueSummary = analysis.createAnalysisIssueSummary(issue, markdownFormatterFactory);
        DbIssues.Locations location = issue.getIssue().getLocations();

        try {
            CreateCommentRequest comment = new CreateCommentRequest(issueSummary);
            CommentPosition fileStart = new CommentPosition(
                    location.getTextRange().getEndLine(),
                    location.getTextRange().getEndOffset() + 1
            );
            CommentPosition fileEnd = new CommentPosition(
                    location.getTextRange().getStartLine(),
                    location.getTextRange().getStartOffset() + 1
            );
            String file = filePath.startsWith("/") ? filePath : "/" + filePath;
            CommentThreadContext commentThreadContext = new CommentThreadContext(file, fileStart, fileEnd);
            CreateCommentThreadRequest thread = new CreateCommentThreadRequest(commentThreadContext, Collections.singletonList(comment), CommentThreadStatus.ACTIVE);
            client.createThread(pullRequest.getRepository().getProject().getName(), pullRequest.getRepository().getName(), pullRequest.getId(), thread);
        } catch (IOException ex) {
            throw new IllegalStateException("Could not submit commit comment to Azure Devops", ex);
        }
    }


    @Override
    protected void submitSummaryNote(AzureDevopsClient client, PullRequest pullRequest, AnalysisDetails analysis) {
        try {
            String summaryCommentBody = analysis.createAnalysisSummary(markdownFormatterFactory);
            CreateCommentRequest comment = new CreateCommentRequest(summaryCommentBody);
            CreateCommentThreadRequest commentThread = new CreateCommentThreadRequest(null, Collections.singletonList(comment), CommentThreadStatus.ACTIVE);
            CommentThread summaryComment = client.createThread(pullRequest.getRepository().getProject().getName(), pullRequest.getRepository().getName(), pullRequest.getId(), commentThread);
            if (analysis.getQualityGateStatus() == QualityGate.Status.OK) {
                client.resolvePullRequestThread(pullRequest.getRepository().getProject().getName(), pullRequest.getRepository().getName(), pullRequest.getId(), summaryComment.getId());
            }
        } catch (IOException ex) {
            throw new IllegalStateException("Could not submit summary comment to Azure Devops", ex);
        }
    }

    protected List<CommentThread> getDiscussions(AzureDevopsClient client, PullRequest pullRequest) {
        try {
            return client.retrieveThreads(pullRequest.getRepository().getProject().getName(), pullRequest.getRepository().getName(), pullRequest.getId());
        } catch (IOException ex) {
            throw new IllegalStateException("Could not retrieve discussions from Azure Devops", ex);
        }
    }

    @Override
    protected boolean isNoteFromCurrentUser(Comment note, Void user) {
        return true;
    }

    @Override
    protected String getNoteContent(AzureDevopsClient client, Comment note) {
        return note.getContent();
    }

    @Override
    protected List<Comment> getNotesForDiscussion(AzureDevopsClient client, CommentThread discussion) {
        return discussion.getComments();
    }

    @Override
    protected boolean isClosed(CommentThread discussion, List<Comment> notesInDiscussion) {
        return discussion.isDeleted() || discussion.getStatus() == CommentThreadStatus.CLOSED;
    }

    @Override
    protected boolean isUserNote(Comment note) {
        return CommentType.TEXT == note.getCommentType();
    }

    @Override
    protected void addNoteToDiscussion(AzureDevopsClient client, CommentThread discussion, PullRequest pullRequest, String note) {
        try {
            client.addCommentToThread(pullRequest.getRepository().getProject().getName(), pullRequest.getRepository().getName(), pullRequest.getId(), discussion.getId(), new CreateCommentRequest(note));
        } catch (IOException ex) {
            throw new IllegalStateException("Could not add note to Pull Request comment thread on Azure Devops", ex);
        }
    }

    @Override
    protected void resolveDiscussion(AzureDevopsClient client, CommentThread discussion, PullRequest pullRequest) {
        try {
            client.resolvePullRequestThread(pullRequest.getRepository().getProject().getName(), pullRequest.getRepository().getName(), pullRequest.getId(), discussion.getId());
        } catch (IOException ex) {
            throw new IllegalStateException("Could not resolve Pull Request comment thread on Azure Devops", ex);
        }
    }

    @Override
    protected Optional<AnalysisDetails.ProjectIssueIdentifier> parseIssueDetails(AzureDevopsClient client, Comment note, AnalysisDetails analysisDetails) {
        Optional<AnalysisDetails.ProjectIssueIdentifier> issueIdentifier = super.parseIssueDetails(client, note, analysisDetails);
        if (issueIdentifier.isPresent()) {
            return issueIdentifier;
        }
        return parseIssueDetails(client, note, analysisDetails, "See in SonarQube", NOTE_MARKDOWN_LEGACY_SEE_LINK_PATTERN);
    }

}
