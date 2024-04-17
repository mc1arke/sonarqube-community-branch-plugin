/*
 * Copyright (C) 2020-2023 Michael Clarke
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
import com.github.mc1arke.sonarqube.plugin.almclient.github.model.Annotation;
import com.github.mc1arke.sonarqube.plugin.almclient.github.model.CheckRunDetails;
import com.github.mc1arke.sonarqube.plugin.almclient.github.v4.model.CommentClassifiers;
import com.github.mc1arke.sonarqube.plugin.almclient.github.v4.model.RequestableCheckStatusState;
import io.aexp.nodes.graphql.Argument;
import io.aexp.nodes.graphql.Arguments;
import io.aexp.nodes.graphql.GraphQLRequestEntity;
import io.aexp.nodes.graphql.GraphQLResponseEntity;
import io.aexp.nodes.graphql.GraphQLTemplate;
import io.aexp.nodes.graphql.InputObject;
import io.aexp.nodes.graphql.internal.Error;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.MalformedURLException;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import static org.apache.commons.lang.ArrayUtils.isEmpty;

public class GraphqlGithubClient implements GithubClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(GraphqlGithubClient.class);
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX")
            .withZone(ZoneId.of("UTC"));
    private static final String INPUT = "input";

    private final GraphqlProvider graphqlProvider;
    private final RepositoryAuthenticationToken repositoryAuthenticationToken;
    private final String apiUrl;


    public GraphqlGithubClient(GraphqlProvider graphqlProvider, String apiUrl,
                               RepositoryAuthenticationToken repositoryAuthenticationToken) {
        super();
        this.graphqlProvider = graphqlProvider;
        this.apiUrl = apiUrl;
        this.repositoryAuthenticationToken = repositoryAuthenticationToken;
    }

    @Override
    public String createCheckRun(CheckRunDetails checkRunDetails, boolean postSummaryComment) throws IOException {
        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "Bearer " + repositoryAuthenticationToken.getAuthenticationToken());
        headers.put("Accept", "application/vnd.github.antiope-preview+json");

        List<InputObject<Object>> annotations = createAnnotations(checkRunDetails.getAnnotations());

        InputObject.Builder<Object> checkRunOutputContentBuilder = graphqlProvider.createInputObject().put("title", checkRunDetails.getTitle())
                .put("summary", checkRunDetails.getSummary())
                .put("annotations", annotations);

        Map<String, Object> inputObjectArguments = new HashMap<>();
        inputObjectArguments.put("repositoryId", repositoryAuthenticationToken.getRepositoryId());
        inputObjectArguments.put("name", checkRunDetails.getName());
        inputObjectArguments.put("status", RequestableCheckStatusState.COMPLETED);
        inputObjectArguments.put("conclusion", checkRunDetails.getCheckConclusionState());
        inputObjectArguments.put("detailsUrl", checkRunDetails.getDashboardUrl());
        inputObjectArguments.put("startedAt", DATE_TIME_FORMATTER.format(checkRunDetails.getStartTime()));
        inputObjectArguments.put("completedAt", DATE_TIME_FORMATTER.format(checkRunDetails.getEndTime()));
        inputObjectArguments.put("externalId", checkRunDetails.getExternalId());
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
                                .put("headSha", checkRunDetails.getCommitId())
                                .build())))
                        .requestMethod(GraphQLTemplate.GraphQLMethod.MUTATE);

        GraphQLRequestEntity graphQLRequestEntity = graphQLRequestEntityBuilder.build();

        GraphQLResponseEntity<CreateCheckRun> graphQLResponseEntity = executeRequest((r, t) -> graphqlProvider.createGraphQLTemplate().mutate(r, t),
                                                                                     graphQLRequestEntity, CreateCheckRun.class);

        reportRemainingAnnotations(checkRunDetails.getAnnotations(), graphQLResponseEntity.getResponse().getCheckRun().getId(),
                              inputObjectArguments, checkRunOutputContentBuilder, graphQLRequestEntityBuilder);


        if (postSummaryComment) {
            postSummaryComment(graphqlUrl, headers, checkRunDetails.getPullRequestId(), checkRunDetails.getSummary(), checkRunDetails.getProjectKey());
        }

        return graphQLResponseEntity.getResponse().getCheckRun().getId();

    }

    @Override
    public String getRepositoryUrl() {
        return repositoryAuthenticationToken.getRepositoryUrl();
    }

    private void postSummaryComment(String graphqlUrl, Map<String, String> headers, int pullRequestKey, String summary, String projectId) throws IOException {
        String login = getLogin(graphqlUrl, headers);

        GetRepository.PullRequest pullRequest = getPullRequest(graphqlUrl, headers, pullRequestKey);
        String pullRequestId = pullRequest.getId();
        String projectCommentMarker = String.format("**Project ID:** %s%n", projectId);

        getComments(pullRequest, graphqlUrl, headers, pullRequestKey).stream()
            .filter(c -> "Bot".equalsIgnoreCase(c.getAuthor().getType()) && login.equalsIgnoreCase(c.getAuthor().getLogin()))
            .filter(c -> !c.isMinimized())
            .filter(c -> c.getBody().contains(projectCommentMarker))
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

    private List<Comments.CommentNode> getComments(GetRepository.PullRequest pullRequest, String graphqlUrl, Map<String, String> headers, int pullRequestKey) throws MalformedURLException {
        List<Comments.CommentNode> comments = new ArrayList<>(pullRequest.getComments().getNodes());

        PageInfo currentPageInfo = pullRequest.getComments().getPageInfo();
        if (currentPageInfo.hasNextPage()) {
            GetRepository.PullRequest response = getPullRequest(graphqlUrl, headers, pullRequestKey, currentPageInfo);
            comments.addAll(getComments(response, graphqlUrl, headers, pullRequestKey));
        }

        return comments;
    }

    private GetRepository.PullRequest getPullRequest(String graphqlUrl, Map<String, String> headers, int pullRequestKey) throws MalformedURLException {
        return getPullRequest(graphqlUrl, headers, pullRequestKey, null);
    }

    private GetRepository.PullRequest getPullRequest(String graphqlUrl, Map<String, String> headers, int pullRequestKey, PageInfo pageInfo) throws MalformedURLException {
        GraphQLRequestEntity getPullRequest =
                graphqlProvider.createRequestBuilder()
                        .url(graphqlUrl)
                        .headers(headers)
                        .request(GetRepository.class)
                        .arguments(
                                new Arguments("repository", new Argument<>("owner", repositoryAuthenticationToken.getOwnerName()), new Argument<>("name", repositoryAuthenticationToken.getRepositoryName())),
                                new Arguments("repository.pullRequest", new Argument<>("number", pullRequestKey)),
                                new Arguments("repository.pullRequest.comments", new Argument<>("first", 100), new Argument<>("after", Optional.ofNullable(pageInfo).map(PageInfo::getEndCursor).orElse(null)))
                        )
                        .build();

        return executeRequest((r, t) -> graphqlProvider.createGraphQLTemplate().query(r, t), getPullRequest, GetRepository.class).getResponse().getPullRequest();
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
        LOGGER.atDebug().setMessage("Using request: {}").addArgument(graphQLRequestEntity::getRequest).log();

        GraphQLResponseEntity<R> response = executor.apply(graphQLRequestEntity, responseType);

        LOGGER.debug("Received response: {}", response);

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

    private void reportRemainingAnnotations(List<Annotation> outstandingAnnotations, String checkRunId,
                                            Map<String, Object> repositoryInputArguments, InputObject.Builder<Object> outputObjectBuilder,
                                            GraphQLRequestEntity.RequestBuilder graphQLRequestEntityBuilder) {

        if (outstandingAnnotations.size() <= 50) {
            return;
        }

        List<Annotation> annotations = outstandingAnnotations.subList(50, outstandingAnnotations.size());

        InputObject<Object> outputObject = outputObjectBuilder
                .put("annotations", createAnnotations(annotations))
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

       reportRemainingAnnotations(annotations, checkRunId, repositoryInputArguments, outputObjectBuilder, graphQLRequestEntityBuilder);
    }

    private List<InputObject<Object>> createAnnotations(List<Annotation> annotations) {
        return annotations.stream()
                .limit(50)
                .map(annotation -> {
            InputObject<Object> issueLocation = graphqlProvider.createInputObject()
                    .put("startLine", Optional.ofNullable(annotation.getLine()).orElse(0))
                    .put("endLine", Optional.ofNullable(annotation.getLine()).orElse(0))
                    .build();
            return graphqlProvider.createInputObject()
                    .put("path", annotation.getScmPath())
                    .put("location", issueLocation)
                    .put("annotationLevel", annotation.getSeverity())
                    .put("message", annotation.getMessage())
                    .build();
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

}
