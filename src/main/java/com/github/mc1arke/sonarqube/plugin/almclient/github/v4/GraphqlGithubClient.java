/*
 * Copyright (C) 2020-2021 Michael Clarke
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
package com.github.mc1arke.sonarqube.plugin.almclient.github.v4;

import com.github.mc1arke.sonarqube.plugin.almclient.github.GithubClient;
import com.github.mc1arke.sonarqube.plugin.almclient.github.RepositoryAuthenticationToken;
import com.github.mc1arke.sonarqube.plugin.almclient.github.v4.model.CheckAnnotationLevel;
import com.github.mc1arke.sonarqube.plugin.almclient.github.v4.model.CheckConclusionState;
import com.github.mc1arke.sonarqube.plugin.almclient.github.v4.model.CommentClassifiers;
import com.github.mc1arke.sonarqube.plugin.almclient.github.v4.model.RequestableCheckStatusState;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.AnalysisDetails;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.DecorationResult;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.PostAnalysisIssueVisitor;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.markup.MarkdownFormatterFactory;
import io.aexp.nodes.graphql.Argument;
import io.aexp.nodes.graphql.Arguments;
import io.aexp.nodes.graphql.GraphQLRequestEntity;
import io.aexp.nodes.graphql.GraphQLResponseEntity;
import io.aexp.nodes.graphql.GraphQLTemplate;
import io.aexp.nodes.graphql.InputObject;
import io.aexp.nodes.graphql.internal.Error;
import org.sonar.api.ce.posttask.QualityGate;
import org.sonar.api.issue.Issue;
import org.sonar.api.platform.Server;
import org.sonar.api.rule.Severity;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.ce.task.projectanalysis.component.Component;
import org.sonar.db.alm.setting.AlmSettingDto;
import org.sonar.db.alm.setting.ProjectAlmSettingDto;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.time.Clock;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TimeZone;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import static org.apache.commons.lang.ArrayUtils.isEmpty;

public class GraphqlGithubClient implements GithubClient {

    private static final Logger LOGGER = Loggers.get(GraphqlGithubClient.class);
    private static final String DATE_TIME_PATTERN = "yyyy-MM-dd'T'HH:mm:ssXXX";
    private static final String INPUT = "input";

    private final GraphqlProvider graphqlProvider;
    private final Clock clock;
    private final RepositoryAuthenticationToken repositoryAuthenticationToken;
    private final Server server;

    private static final List<String> OPEN_ISSUE_STATUSES =
            Issue.STATUSES.stream().filter(s -> !Issue.STATUS_CLOSED.equals(s) && !Issue.STATUS_RESOLVED.equals(s))
                    .collect(Collectors.toList());

    public GraphqlGithubClient(RepositoryAuthenticationToken repositoryAuthenticationToken,
                               Server server) {
        this(new DefaultGraphqlProvider(), Clock.systemDefaultZone(), repositoryAuthenticationToken, server);
    }

    GraphqlGithubClient(GraphqlProvider graphqlProvider, Clock clock,
                        RepositoryAuthenticationToken repositoryAuthenticationToken,
                        Server server) {
        super();
        this.graphqlProvider = graphqlProvider;
        this.clock = clock;
        this.repositoryAuthenticationToken = repositoryAuthenticationToken;
        this.server = server;
    }

    @Override
    public DecorationResult createCheckRun(AnalysisDetails analysisDetails, AlmSettingDto almSettingDto,
                               ProjectAlmSettingDto projectAlmSettingDto) throws IOException {
        String apiUrl = Optional.ofNullable(almSettingDto.getUrl()).orElseThrow(() -> new IllegalArgumentException("No URL has been set for Github connections"));
        String projectPath = Optional.ofNullable(projectAlmSettingDto.getAlmRepo()).orElseThrow(() -> new IllegalArgumentException("No repository name has been set for Github connections"));


        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "Bearer " + repositoryAuthenticationToken.getAuthenticationToken());
        headers.put("Accept", "application/vnd.github.antiope-preview+json");


        String summary = analysisDetails.createAnalysisSummary(new MarkdownFormatterFactory());

        List<PostAnalysisIssueVisitor.ComponentIssue> issues = analysisDetails.getPostAnalysisIssueVisitor().getIssues();

        List<InputObject<Object>> annotations = createAnnotations(issues);

        InputObject.Builder<Object> checkRunOutputContentBuilder = graphqlProvider.createInputObject().put("title", "Quality Gate " +
                                                                                                     (analysisDetails
                                                                                                              .getQualityGateStatus() ==
                                                                                                      QualityGate.Status.OK ?
                                                                                                      "success" :
                                                                                                      "failed"))
                .put("summary", summary)
                .put("annotations", annotations);

        SimpleDateFormat startedDateFormat = new SimpleDateFormat(DATE_TIME_PATTERN);
        startedDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));

        Map<String, Object> inputObjectArguments = new HashMap<>();
        inputObjectArguments.put("repositoryId", repositoryAuthenticationToken.getRepositoryId());
        inputObjectArguments.put("name", String.format("%s Sonarqube Results", analysisDetails.getAnalysisProjectName()));
        inputObjectArguments.put("status", RequestableCheckStatusState.COMPLETED);
        inputObjectArguments.put("conclusion", QualityGate.Status.OK == analysisDetails.getQualityGateStatus() ?
                                   CheckConclusionState.SUCCESS : CheckConclusionState.FAILURE);
        inputObjectArguments.put("detailsUrl", String.format("%s/dashboard?id=%s&pullRequest=%s", server.getPublicRootUrl(),
                                                 URLEncoder.encode(analysisDetails.getAnalysisProjectKey(),
                                                                   StandardCharsets.UTF_8), URLEncoder
                                                         .encode(analysisDetails.getBranchName(),
                                                                 StandardCharsets.UTF_8)));
        inputObjectArguments.put("startedAt", startedDateFormat.format(analysisDetails.getAnalysisDate()));
        inputObjectArguments.put("completedAt", DateTimeFormatter.ofPattern(DATE_TIME_PATTERN).withZone(ZoneId.of("UTC"))
                        .format(clock.instant()));
        inputObjectArguments.put("externalId", analysisDetails.getAnalysisId());
        inputObjectArguments.put("output", checkRunOutputContentBuilder.build());

        InputObject.Builder<Object> repositoryInputObjectBuilder = graphqlProvider.createInputObject();
        inputObjectArguments.forEach(repositoryInputObjectBuilder::put);

        String graphqlUrl = getGraphqlUrl(apiUrl);

        GraphQLRequestEntity.RequestBuilder graphQLRequestEntityBuilder =
                graphqlProvider.createRequestBuilder()
                        .url(graphqlUrl)
                        .headers(headers)
                        .request(CreateCheckRun.class)
                        .arguments(new Arguments("createCheckRun", new Argument<>(INPUT, repositoryInputObjectBuilder
                                .put("headSha", analysisDetails.getCommitSha())
                                .build())))
                        .requestMethod(GraphQLTemplate.GraphQLMethod.MUTATE);

        GraphQLRequestEntity graphQLRequestEntity = graphQLRequestEntityBuilder.build();

        GraphQLResponseEntity<CreateCheckRun> graphQLResponseEntity = executeRequest((r, t) -> graphqlProvider.createGraphQLTemplate().mutate(r, t),
                                                                                     graphQLRequestEntity, CreateCheckRun.class);

        reportRemainingIssues(issues, graphQLResponseEntity.getResponse().getCheckRun().getId(),
                              inputObjectArguments, checkRunOutputContentBuilder, graphQLRequestEntityBuilder);


        if (Optional.ofNullable(projectAlmSettingDto.getSummaryCommentEnabled()).orElse(true)) {
            postSummaryComment(graphqlUrl, headers, projectPath, analysisDetails.getBranchName(), summary);
        }

        return DecorationResult.builder()
                .withPullRequestUrl(repositoryAuthenticationToken.getRepositoryUrl() + "/pull/" + analysisDetails.getBranchName())
                .build();

    }

    private void postSummaryComment(String graphqlUrl, Map<String, String> headers, String projectPath, String pullRequestKey, String summary) throws IOException {
        String login = getLogin(graphqlUrl, headers);

        String[] paths = projectPath.split("/", 2);
        String owner = paths[0];
        String projectName = paths[1];

        GetPullRequest.PullRequest pullRequest = getPullRequest(graphqlUrl, headers, projectName, pullRequestKey, owner);
        String pullRequestId = pullRequest.getId();

        getComments(pullRequest, graphqlUrl, headers, projectName, pullRequestKey, owner).stream()
            .filter(c -> "Bot".equalsIgnoreCase(c.getAuthor().getType()) && login.equalsIgnoreCase(c.getAuthor().getLogin()))
            .filter(c -> !c.isMinimized())
            .map(Comments.CommentNode::getId)
            .forEach(commentId -> this.minimizeComment(graphqlUrl, headers, commentId));

        InputObject.Builder<Object> repositoryInputObjectBuilder = graphqlProvider.createInputObject();

        InputObject<Object> input = repositoryInputObjectBuilder
            .put("body", summary)
            .put("subjectId", pullRequestId)
            .build();

        GraphQLRequestEntity graphQLRequestEntity =
            graphqlProvider.createRequestBuilder()
                .url(graphqlUrl)
                .headers(headers)
                .request(AddComment.class)
                .arguments(new Arguments("addComment", new Argument<>(INPUT, input)))
                .requestMethod(GraphQLTemplate.GraphQLMethod.MUTATE)
                .build();

        executeRequest((r, t) -> graphqlProvider.createGraphQLTemplate().mutate(r, t), graphQLRequestEntity, AddComment.class);

    }

    private List<Comments.CommentNode> getComments(GetPullRequest.PullRequest pullRequest, String graphqlUrl, Map<String, String> headers, String projectName, String pullRequestKey, String owner) throws MalformedURLException {
        List<Comments.CommentNode> comments = new ArrayList<>(pullRequest.getComments().getNodes());

        PageInfo currentPageInfo = pullRequest.getComments().getPageInfo();
        if (currentPageInfo.hasNextPage()) {
            GetPullRequest.PullRequest response = getPullRequest(graphqlUrl, headers, projectName, pullRequestKey, owner, currentPageInfo);
            comments.addAll(getComments(response, graphqlUrl, headers, projectName, pullRequestKey, owner));
        }

        return comments;
    }

    private GetPullRequest.PullRequest getPullRequest(String graphqlUrl, Map<String, String> headers, String projectName, String pullRequestKey, String owner) throws MalformedURLException {
        return getPullRequest(graphqlUrl, headers, projectName, pullRequestKey, owner, null);
    }

    private GetPullRequest.PullRequest getPullRequest(String graphqlUrl, Map<String, String> headers, String projectName, String pullRequestKey, String owner, PageInfo pageInfo) throws MalformedURLException {
        GraphQLRequestEntity getPullRequest =
                graphqlProvider.createRequestBuilder()
                        .url(graphqlUrl)
                        .headers(headers)
                        .request(GetPullRequest.class)
                        .arguments(
                                new Arguments("repository", new Argument<>("owner", owner), new Argument<>("name", projectName)),
                                new Arguments("repository.pullRequest", new Argument<>("number", Integer.valueOf(pullRequestKey))),
                                new Arguments("repository.pullRequest.comments", new Argument<>("first", 100), new Argument<>("after", Optional.ofNullable(pageInfo).map(PageInfo::getEndCursor).orElse(null)))
                        )
                        .build();

        return executeRequest((r, t) -> graphqlProvider.createGraphQLTemplate().query(r, t), getPullRequest, GetPullRequest.class).getResponse().getPullRequest();
    }

    private void minimizeComment(String graphqlUrl, Map<String, String> headers, String commentId) {
            InputObject<Object> input = graphqlProvider.createInputObject()
                .put("subjectId", commentId)
                .put("classifier", CommentClassifiers.OUTDATED)
                .build();

        try {

            GraphQLRequestEntity graphQLRequestEntity = graphqlProvider.createRequestBuilder()
                .url(graphqlUrl)
                .headers(headers)
                .request(MinimizeComment.class)
                .arguments(new Arguments("minimizeComment", new Argument<>(INPUT, input)))
                .requestMethod(GraphQLTemplate.GraphQLMethod.MUTATE)
                .build();

            executeRequest((r, t) -> graphqlProvider.createGraphQLTemplate().mutate(r, t), graphQLRequestEntity, MinimizeComment.class);

        } catch (IOException e) {
            LOGGER.error("Error during minimize comment", e);
        }
    }

    private String getLogin(String graphqlUrl, Map<String, String> headers) throws IOException {
        GraphQLRequestEntity viewerQuery = graphqlProvider.createRequestBuilder()
                .url(graphqlUrl)
                .headers(headers)
                .request(Viewer.class)
                .build();

        GraphQLResponseEntity<Viewer> response =
            executeRequest((r, t) -> graphqlProvider.createGraphQLTemplate().query(r, t), viewerQuery, Viewer.class);

        return response.getResponse().getLogin().replace("[bot]", "");
    }

    private static <R> GraphQLResponseEntity<R> executeRequest(
            BiFunction<GraphQLRequestEntity, Class<R>, GraphQLResponseEntity<R>> executor, GraphQLRequestEntity graphQLRequestEntity, Class<R> responseType) {
        LOGGER.debug("Using request: " + graphQLRequestEntity.getRequest());

        GraphQLResponseEntity<R> response = executor.apply(graphQLRequestEntity, responseType);

        LOGGER.debug("Received response: " + response.toString());

        if (!isEmpty(response.getErrors())) {
            List<String> errors = new ArrayList<>();
            for (Error error : response.getErrors()) {
                errors.add("- " + error.toString());
            }
            throw new IllegalStateException(
                    "An error was returned in the response from the Github API:" + System.lineSeparator() +
                    errors.stream().collect(Collectors.joining(System.lineSeparator())));
        }

        return response;
    }

    private void reportRemainingIssues(List<PostAnalysisIssueVisitor.ComponentIssue> outstandingIssues, String checkRunId,
                                       Map<String, Object> repositoryInputArguments, InputObject.Builder<Object> outputObjectBuilder,
                                       GraphQLRequestEntity.RequestBuilder graphQLRequestEntityBuilder) {

        if (outstandingIssues.size() <= 50) {
            return;
        }

        List<PostAnalysisIssueVisitor.ComponentIssue> issues = outstandingIssues.subList(50, outstandingIssues.size());

        InputObject<Object> outputObject = outputObjectBuilder
                .put("annotations", createAnnotations(issues))
                .build();

        InputObject.Builder<Object> repositoryInputObjectBuilder = graphqlProvider.createInputObject();
        repositoryInputArguments.forEach(repositoryInputObjectBuilder::put);

        InputObject<Object> repositoryInputObject = repositoryInputObjectBuilder
                .put("checkRunId", checkRunId)
                .put("output", outputObject)
                .build();

       GraphQLRequestEntity graphQLRequestEntity = graphQLRequestEntityBuilder
               .request(UpdateCheckRun.class)
               .arguments(new Arguments("updateCheckRun", new Argument<>(INPUT, repositoryInputObject)))
                        .requestMethod(GraphQLTemplate.GraphQLMethod.MUTATE)
                        .build();

       executeRequest((r, t) -> graphqlProvider.createGraphQLTemplate().mutate(r, t), graphQLRequestEntity, UpdateCheckRun.class);

       reportRemainingIssues(issues, checkRunId, repositoryInputArguments, outputObjectBuilder, graphQLRequestEntityBuilder);
    }

    private List<InputObject<Object>> createAnnotations(List<PostAnalysisIssueVisitor.ComponentIssue> issues) {
        return issues.stream()
                .limit(50)
                .filter(i -> i.getComponent().getReportAttributes().getScmPath().isPresent())
                .filter(i -> i.getComponent().getType() == Component.Type.FILE)
                .filter(i -> i.getIssue().resolution() == null)
                .filter(i -> OPEN_ISSUE_STATUSES.contains(i.getIssue().status())).map(componentIssue -> {
            InputObject<Object> issueLocation = graphqlProvider.createInputObject()
                    .put("startLine", Optional.ofNullable(componentIssue.getIssue().getLine()).orElse(0))
                    .put("endLine", Optional.ofNullable(componentIssue.getIssue().getLine()).orElse(0))
                    .build();
            return graphqlProvider.createInputObject()
                    .put("path", componentIssue.getComponent().getReportAttributes().getScmPath().get())
                    .put("location", issueLocation)
                    .put("annotationLevel", mapToGithubAnnotationLevel(componentIssue.getIssue().severity()))
                    .put("message", componentIssue.getIssue().getMessage().replace("\\","\\\\").replace("\"", "\\\"")).build();
        }).collect(Collectors.toList());
    }

    private static String getGraphqlUrl(String apiUrl) {
        if (apiUrl.endsWith("/")) {
            apiUrl = apiUrl.substring(0, apiUrl.length() - 1);
        }
        if (apiUrl.endsWith("/v3")) {
            apiUrl = apiUrl.substring(0, apiUrl.length() - 3);
        }
        apiUrl = apiUrl + "/graphql";

        return apiUrl;
    }

    private static CheckAnnotationLevel mapToGithubAnnotationLevel(String sonarqubeSeverity) {
        switch (sonarqubeSeverity) {
            case Severity.INFO:
                return CheckAnnotationLevel.NOTICE;
            case Severity.MINOR:
            case Severity.MAJOR:
                return CheckAnnotationLevel.WARNING;
            case Severity.CRITICAL:
            case Severity.BLOCKER:
                return CheckAnnotationLevel.FAILURE;
            default:
                throw new IllegalArgumentException("Unknown severity value: " + sonarqubeSeverity);
        }
    }

}
