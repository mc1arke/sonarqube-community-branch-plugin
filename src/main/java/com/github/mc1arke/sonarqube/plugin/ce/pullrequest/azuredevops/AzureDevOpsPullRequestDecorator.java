package com.github.mc1arke.sonarqube.plugin.ce.pullrequest.azuredevops;

import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.AnalysisDetails;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.DecorationResult;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.PostAnalysisIssueVisitor;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.PullRequestBuildStatusDecorator;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.azuredevops.model.Comment;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.azuredevops.model.CommentPosition;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.azuredevops.model.CommentThread;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.azuredevops.model.CommentThreadContext;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.azuredevops.model.GitPullRequestStatus;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.azuredevops.model.GitStatusContext;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.azuredevops.model.PullRequest;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.azuredevops.model.enums.CommentThreadStatus;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.azuredevops.model.mappers.GitStatusStateMapper;
import org.apache.commons.lang3.StringUtils;
import org.sonar.api.issue.Issue;
import org.sonar.api.platform.Server;
import org.sonar.db.alm.setting.ALM;
import org.sonar.db.alm.setting.AlmSettingDto;
import org.sonar.db.alm.setting.ProjectAlmSettingDto;
import org.sonar.db.protobuf.DbIssues;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public class AzureDevOpsPullRequestDecorator implements PullRequestBuildStatusDecorator {

    private final Server server;
    private final AzureDevopsClientFactory azureDevopsClientFactory;

    public AzureDevOpsPullRequestDecorator(Server server, AzureDevopsClientFactory azureDevopsClientFactory) {
        super();
        this.server = server;
        this.azureDevopsClientFactory = azureDevopsClientFactory;
    }

    @Override
    public DecorationResult decorateQualityGateStatus(AnalysisDetails analysisDetails, AlmSettingDto almSettingDto, ProjectAlmSettingDto projectAlmSettingDto) {
        AzureDevopsClient client = azureDevopsClientFactory.createClient(projectAlmSettingDto, almSettingDto);

        long pullRequestId;
        try {
            pullRequestId = Long.parseLong(analysisDetails.getScannerProperty(AZUREDEVOPS_ENV_PULLREQUEST_ID)
                    .orElse(analysisDetails.getBranchName()));
        } catch (NumberFormatException ex) {
            throw new IllegalStateException("Could not parse Pull Request Key", ex);
        }

        String azureRepositoryName = Optional.ofNullable(projectAlmSettingDto.getAlmRepo()).orElseThrow(() -> new IllegalStateException("Repository name must be provided"));
        String azureProjectId = Optional.ofNullable(projectAlmSettingDto.getAlmSlug()).orElseThrow(() -> new IllegalStateException("Repository slug must be provided"));

        PullRequest pullRequest = getPullRequest(client, azureProjectId, azureRepositoryName, pullRequestId);
        updatePullRequestStatus(client, pullRequest, analysisDetails, server);

        List<CommentThread> azureCommentThreads = getRelevantThreadsForPullRequest(client, pullRequest);

        List<PostAnalysisIssueVisitor.ComponentIssue> openIssues = analysisDetails.getPostAnalysisIssueVisitor().getIssues();
        for (PostAnalysisIssueVisitor.ComponentIssue issue : openIssues) {
            analysisDetails.getSCMPathForIssue(issue).map(AzureDevOpsPullRequestDecorator::normalisePath)
                    .ifPresent(filePath -> handleIssue(analysisDetails, issue, azureCommentThreads, client, filePath, pullRequest));
        }

        return DecorationResult.builder().withPullRequestUrl(pullRequest.getRepository().getRemoteUrl() + "/pullRequest/" + pullRequest.getId()).build();
    }

    private static PullRequest getPullRequest(AzureDevopsClient client, String projectId, String repositoryName, long pullRequestId) {
        try {
            return client.retrievePullRequest(projectId, repositoryName, pullRequestId);
        } catch (IOException ex) {
            throw new IllegalStateException("Could not retrieve Pull Request from Azure Devops", ex);
        }
    }

    private static void updatePullRequestStatus(AzureDevopsClient client, PullRequest pullRequest, AnalysisDetails analysisDetails, Server server) {
        GitPullRequestStatus gitPullRequestStatus = new GitPullRequestStatus(
                GitStatusStateMapper.toGitStatusState(analysisDetails.getQualityGateStatus()),
                "SonarQube Gate",
                new GitStatusContext("SonarQube", "QualityGate"),
                String.format("%s/dashboard?id=%s&pullRequest=%s",
                        server.getPublicRootUrl(),
                        URLEncoder.encode(analysisDetails.getAnalysisProjectKey(), StandardCharsets.UTF_8),
                        URLEncoder.encode(analysisDetails.getBranchName(), StandardCharsets.UTF_8)
                )
        );

        try {
            client.submitPullRequestStatus(pullRequest.getRepository().getProject().getName(), pullRequest.getRepository().getName(), pullRequest.getId(), gitPullRequestStatus);
        } catch (IOException ex) {
            throw new IllegalStateException("Could not submit Pull Request status to Azure DevOps", ex);
        }
    }

    private static List<CommentThread> getRelevantThreadsForPullRequest(AzureDevopsClient client, PullRequest pullRequest) {
        try {
            List<CommentThread> comments = client.retrieveThreads(pullRequest.getRepository().getProject().getName(), pullRequest.getRepository().getName(), pullRequest.getId());

            return comments.stream()
                    .filter(commentThread -> !commentThread.isDeleted())
                    .filter(commentThread -> Optional.of(commentThread)
                            .map(CommentThread::getThreadContext)
                            .filter(context -> Optional.of(context)
                                    .map(CommentThreadContext::getFilePath)
                                    .map(StringUtils::trimToNull)
                                    .isPresent()
                            )
                            .filter(context -> Optional.of(context)
                                    .map(CommentThreadContext::getRightFileStart)
                                    .isPresent()
                            )
                            .isPresent())
                    .collect(Collectors.toList());
        } catch (IOException ex) {
            throw new IllegalStateException("Could not retrieve Pull Request comment threads from Azure DevOps", ex);
        }
    }

    private static void handleIssue(AnalysisDetails analysisDetails, PostAnalysisIssueVisitor.ComponentIssue issue,
                             List<CommentThread> azureCommentThreads, AzureDevopsClient client, String filePath,
                             PullRequest pullRequest) {
        for (CommentThread azureThread : azureCommentThreads) {
            if (isThreadForIssue(azureThread, filePath, issue)) {
                closeCommentThreadIfOutdated(issue, azureThread, client, pullRequest);
                return;
            }
        }

        if (!Issue.STATUS_OPEN.equals(issue.getIssue().getStatus())) {
            return;
        }

        String message = String.format("%s: %s ([rule](%s))%n%n[See in SonarQube](%s)",
                issue.getIssue().type().name(),
                issue.getIssue().getMessage(),
                analysisDetails.getRuleUrlWithRuleKey(issue.getIssue().getRuleKey().toString()),
                analysisDetails.getIssueUrl(issue.getIssue())
        );

        DbIssues.Locations location = Objects.requireNonNull(issue.getIssue().getLocations());

        Comment comment = new Comment(message);
        CommentPosition fileStart = new CommentPosition(
                location.getTextRange().getEndLine(),
                location.getTextRange().getEndOffset() + 1
        );
        CommentPosition fileEnd = new CommentPosition(
                location.getTextRange().getStartLine(),
                location.getTextRange().getStartOffset() + 1
        );
        CommentThreadContext commentThreadContext = new CommentThreadContext(filePath, fileStart, fileEnd);
        CommentThread thread = new CommentThread(Collections.singletonList(comment), commentThreadContext);
        try {
            client.createThread(pullRequest.getRepository().getProject().getName(), pullRequest.getRepository().getName(), pullRequest.getId(), thread);
        } catch (IOException ex) {
            throw new IllegalStateException("Could not create thread on Azure DevOps", ex);
        }
    }

    private static boolean isThreadForIssue(CommentThread commentThread, String filePath, PostAnalysisIssueVisitor.ComponentIssue issue) {
        return commentThread.getThreadContext().getFilePath().equals(filePath)
                && commentThread.getComments().stream()
                .anyMatch(c -> c.getContent().contains(issue.getIssue().key()));
    }

    private static String normalisePath(String path) {
        return path.endsWith("/") ? path : "/" + path;
    }

    private static void closeCommentThreadIfOutdated(PostAnalysisIssueVisitor.ComponentIssue issue, CommentThread azureThread,
                                                  AzureDevopsClient client, PullRequest pullRequest) {
        if (Issue.STATUS_OPEN.equals(issue.getIssue().getStatus()) || azureThread.getStatus() != CommentThreadStatus.ACTIVE) {
            return;
        }

        String projectName = pullRequest.getRepository().getProject().getName();
        String repositoryName = pullRequest.getRepository().getName();
        long pullRequestId = pullRequest.getId();
        int threadId = azureThread.getId();

        try {
            client.addCommentToThread(projectName, repositoryName, pullRequestId, threadId, new Comment("Issue has been closed in SonarQube"));
            client.updateThreadStatus(projectName, repositoryName, pullRequestId, threadId, CommentThreadStatus.CLOSED);
        } catch (IOException ex) {
            throw new IllegalStateException("Could not close Pull Request comment thread on Azure DevOps", ex);
        }
    }

    @Override
    public List<ALM> alm() {
        return Collections.singletonList(ALM.AZURE_DEVOPS);
    }
}
