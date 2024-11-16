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
package com.github.mc1arke.sonarqube.plugin.ce.pullrequest.azuredevops;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.ce.posttask.QualityGate;
import org.sonar.ce.task.projectanalysis.scm.ScmInfoRepository;
import org.sonar.db.alm.setting.ALM;
import org.sonar.db.alm.setting.AlmSettingDto;
import org.sonar.db.alm.setting.ProjectAlmSettingDto;
import org.sonar.db.protobuf.DbIssues;

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
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.report.AnalysisIssueSummary;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.report.AnalysisSummary;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.report.ReportGenerator;

public class AzureDevOpsPullRequestDecorator extends DiscussionAwarePullRequestDecorator<AzureDevopsClient, PullRequest, String, CommentThread, Comment> implements PullRequestBuildStatusDecorator {

    private static final Logger logger = LoggerFactory.getLogger(AzureDevOpsPullRequestDecorator.class);
    private static final Pattern NOTE_MARKDOWN_LEGACY_SEE_LINK_PATTERN = Pattern.compile("^\\[See in SonarQube]\\((.*?)\\)$");
    private final AzureDevopsClientFactory azureDevopsClientFactory;
    private final MarkdownFormatterFactory markdownFormatterFactory;

    public AzureDevOpsPullRequestDecorator(ScmInfoRepository scmInfoRepository,
                                           AzureDevopsClientFactory azureDevopsClientFactory,
                                           ReportGenerator reportGenerator, MarkdownFormatterFactory markdownFormatterFactory) {
        super(scmInfoRepository, reportGenerator);
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
        String targetUri = pullRequest.getRepository().getRemoteUrl();
        try {
            URI uri = new URI(targetUri);
            targetUri = new URI(uri.getScheme(), null, uri.getHost(), uri.getPort(), uri.getPath(), null, null).toString();
        } catch (URISyntaxException ex) {
            logger.warn("Could not construct normalised URI for Pull Request link. Unparsed URL will be used instead", ex);
        }
        return Optional.of(targetUri + "/pullRequest/" + pullRequest.getId());
    }

    @Override
    protected PullRequest getPullRequest(AzureDevopsClient client, AlmSettingDto almSettingDto, ProjectAlmSettingDto projectAlmSettingDto, AnalysisDetails analysis) {
        int pullRequestId;
        try {
            pullRequestId = Integer.parseInt(analysis.getPullRequestId());
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
    protected String getCurrentUser(AzureDevopsClient client) {
        try {
            return client.getConnectionData().getAuthenticatedUser().getId();
        } catch (Exception e) {
            logger.warn("Could not retrieve authenticated user", e);
            // historically we didn't handle users here so always returned null. This is a fallback to that behaviour.
            return null;
        }
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
    protected void submitPipelineStatus(AzureDevopsClient client, PullRequest pullRequest, AnalysisDetails analysis, AnalysisSummary analysisSummary) {
        try {
            GitPullRequestStatus gitPullRequestStatus = new GitPullRequestStatus(
                    GitStatusStateMapper.toGitStatusState(analysis.getQualityGateStatus()),
                    String.format("SonarQube Quality Gate - %s (%s)", analysis.getAnalysisProjectName(), analysis.getAnalysisProjectKey()),
                    new GitStatusContext("sonarqube/qualitygate", analysis.getAnalysisProjectKey()),
                    analysisSummary.getDashboardUrl()
            );

            client.submitPullRequestStatus(pullRequest.getRepository().getProject().getName(), pullRequest.getRepository().getName(), pullRequest.getId(), gitPullRequestStatus);
        } catch (IOException ex) {
            throw new IllegalStateException("Could not update pipeline status in Gitlab", ex);
        }
    }

    @Override
    protected void submitCommitNoteForIssue(AzureDevopsClient client, PullRequest pullRequest, PostAnalysisIssueVisitor.ComponentIssue issue, String filePath,
                                            AnalysisDetails analysis, AnalysisIssueSummary analysisIssueSummary) {
        DbIssues.Locations location = issue.getIssue().getLocations();

        try {
            CreateCommentRequest comment = new CreateCommentRequest(analysisIssueSummary.format(markdownFormatterFactory));
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
    protected void submitSummaryNote(AzureDevopsClient client, PullRequest pullRequest, AnalysisDetails analysis, AnalysisSummary analysisSummary) {
        try {
            CreateCommentRequest comment = new CreateCommentRequest(analysisSummary.format(markdownFormatterFactory));
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
    protected boolean isNoteFromCurrentUser(Comment note, String user) {
        return note.getAuthor().getId().equals(user);
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
    protected void deleteDiscussion(AzureDevopsClient client, CommentThread discussion, PullRequest pullRequest, List<Comment> notesForDiscussion) {
        try {
            for (Comment note : notesForDiscussion) {
                client.deletePullRequestThreadComment(pullRequest.getRepository().getProject().getName(), pullRequest.getRepository().getName(), pullRequest.getId(), discussion.getId(), note.getId());
            }
        } catch (IOException ex) {
            throw new IllegalStateException("Could not delete Pull Request comment thread on Azure Devops", ex);
        }
    }

    @Override
    protected Optional<ProjectIssueIdentifier> parseIssueDetails(AzureDevopsClient client, Comment note) {
        Optional<ProjectIssueIdentifier> issueIdentifier = super.parseIssueDetails(client, note);
        if (issueIdentifier.isPresent()) {
            return issueIdentifier;
        }
        return parseIssueDetails(client, note, "See in SonarQube", NOTE_MARKDOWN_LEGACY_SEE_LINK_PATTERN);
    }

}
