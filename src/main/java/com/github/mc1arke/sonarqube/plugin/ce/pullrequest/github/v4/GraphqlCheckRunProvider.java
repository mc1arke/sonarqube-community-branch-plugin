/*
 * Copyright (C) 2019 Michael Clarke
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
package com.github.mc1arke.sonarqube.plugin.ce.pullrequest.github.v4;

import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.AnalysisDetails;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.DecorationResult;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.PostAnalysisIssueVisitor;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.UnifyConfiguration;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.github.CheckRunProvider;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.github.GithubApplicationAuthenticationProvider;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.github.RepositoryAuthenticationToken;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.github.v4.model.CheckAnnotationLevel;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.github.v4.model.CheckConclusionState;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.github.v4.model.RequestableCheckStatusState;
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

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
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

public class GraphqlCheckRunProvider implements CheckRunProvider {

    public static final String PULL_REQUEST_GITHUB_URL = "sonar.pullrequest.github.endpoint";
    public static final String PULL_REQUEST_GITHUB_TOKEN = "sonar.alm.github.app.privateKey.secured";
    public static final String PULL_REQUEST_GITHUB_REPOSITORY = "sonar.pullrequest.github.repository";
    public static final String PULL_REQUEST_GITHUB_APP_ID = "sonar.alm.github.app.id";
    public static final String PULL_REQUEST_GITHUB_APP_NAME = "sonar.alm.github.app.name";

    private static final Logger LOGGER = Loggers.get(GraphqlCheckRunProvider.class);
    private static final String DATE_TIME_PATTERN = "yyyy-MM-dd'T'HH:mm:ssXXX";

    private final GraphqlProvider graphqlProvider;
    private final Clock clock;
    private final GithubApplicationAuthenticationProvider githubApplicationAuthenticationProvider;
    private final Server server;

    private static final List<String> OPEN_ISSUE_STATUSES =
            Issue.STATUSES.stream().filter(s -> !Issue.STATUS_CLOSED.equals(s) && !Issue.STATUS_RESOLVED.equals(s))
                    .collect(Collectors.toList());

    public GraphqlCheckRunProvider(Clock clock,
                                   GithubApplicationAuthenticationProvider githubApplicationAuthenticationProvider,
                                   Server server) {
        this(new DefaultGraphqlProvider(), clock, githubApplicationAuthenticationProvider, server);
    }

    GraphqlCheckRunProvider(GraphqlProvider graphqlProvider, Clock clock,
                            GithubApplicationAuthenticationProvider githubApplicationAuthenticationProvider,
                            Server server) {
        super();
        this.graphqlProvider = graphqlProvider;
        this.clock = clock;
        this.githubApplicationAuthenticationProvider = githubApplicationAuthenticationProvider;
        this.server = server;
    }

    @Override
    public DecorationResult createCheckRun(AnalysisDetails analysisDetails, UnifyConfiguration unifyConfiguration) throws IOException, GeneralSecurityException {
        String apiUrl = unifyConfiguration.getRequiredServerProperty(PULL_REQUEST_GITHUB_URL);
        String apiPrivateKey = unifyConfiguration.getRequiredServerProperty(PULL_REQUEST_GITHUB_TOKEN);
        String projectPath = unifyConfiguration.getRequiredProperty(PULL_REQUEST_GITHUB_REPOSITORY);
        String appId = unifyConfiguration.getRequiredServerProperty(PULL_REQUEST_GITHUB_APP_ID);
        String appName = unifyConfiguration.getRequiredServerProperty(PULL_REQUEST_GITHUB_APP_NAME);

        RepositoryAuthenticationToken repositoryAuthenticationToken =
                githubApplicationAuthenticationProvider.getInstallationToken(apiUrl, appId, apiPrivateKey, projectPath);
        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "Bearer " + repositoryAuthenticationToken.getAuthenticationToken());
        headers.put("Accept", "application/vnd.github.antiope-preview+json");


        List<PostAnalysisIssueVisitor.ComponentIssue> issues = analysisDetails.getPostAnalysisIssueVisitor().getIssues();

        List<InputObject<Object>> annotations = createAnnotations(issues);

        InputObject.Builder<Object> checkRunOutputContentBuilder = graphqlProvider.createInputObject().put("title", "Quality Gate " +
                                                                                                     (analysisDetails
                                                                                                              .getQualityGateStatus() ==
                                                                                                      QualityGate.Status.OK ?
                                                                                                      "success" :
                                                                                                      "failed"))
                .put("summary", analysisDetails.createAnalysisSummary(new MarkdownFormatterFactory()))
                .put("annotations", annotations);

        SimpleDateFormat startedDateFormat = new SimpleDateFormat(DATE_TIME_PATTERN);
        startedDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));

        Map<String, Object> inputObjectArguments = new HashMap<>();
        inputObjectArguments.put("repositoryId", repositoryAuthenticationToken.getRepositoryId());
        inputObjectArguments.put("name", appName + " Results");
        inputObjectArguments.put("status", RequestableCheckStatusState.COMPLETED);
        inputObjectArguments.put("conclusion", QualityGate.Status.OK == analysisDetails.getQualityGateStatus() ?
                                   CheckConclusionState.SUCCESS : CheckConclusionState.FAILURE);
        inputObjectArguments.put("detailsUrl", String.format("%s/dashboard?id=%s&pullRequest=%s", server.getPublicRootUrl(),
                                                 URLEncoder.encode(analysisDetails.getAnalysisProjectKey(),
                                                                   StandardCharsets.UTF_8.name()), URLEncoder
                                                         .encode(analysisDetails.getBranchName(),
                                                                 StandardCharsets.UTF_8.name())));
        inputObjectArguments.put("startedAt", startedDateFormat.format(analysisDetails.getAnalysisDate()));
        inputObjectArguments.put("completedAt", DateTimeFormatter.ofPattern(DATE_TIME_PATTERN).withZone(ZoneId.of("UTC"))
                        .format(clock.instant()));
        inputObjectArguments.put("externalId", analysisDetails.getAnalysisId());
        inputObjectArguments.put("output", checkRunOutputContentBuilder.build());


        InputObject.Builder<Object> repositoryInputObjectBuilder = graphqlProvider.createInputObject();
        inputObjectArguments.forEach(repositoryInputObjectBuilder::put);


        GraphQLRequestEntity.RequestBuilder graphQLRequestEntityBuilder =
                graphqlProvider.createRequestBuilder()
                        .url(getGraphqlUrl(apiUrl))
                        .headers(headers)
                        .request(CreateCheckRun.class)
                        .arguments(new Arguments("createCheckRun", new Argument<>("input", repositoryInputObjectBuilder
                                .put("headSha", analysisDetails.getCommitSha())
                                .build())))
                        .requestMethod(GraphQLTemplate.GraphQLMethod.MUTATE);

        GraphQLRequestEntity graphQLRequestEntity = graphQLRequestEntityBuilder.build();

        GraphQLResponseEntity<CreateCheckRun> graphQLResponseEntity = executeRequest((r, t) -> graphqlProvider.createGraphQLTemplate().mutate(r, t),
                                                                                     graphQLRequestEntity, CreateCheckRun.class);

        reportRemainingIssues(issues, graphQLResponseEntity.getResponse().getCheckRun().getId(),
                              inputObjectArguments, checkRunOutputContentBuilder, graphQLRequestEntityBuilder);


        return DecorationResult.builder()
                .withPullRequestUrl(repositoryAuthenticationToken.getRepositoryUrl() + "/pull/" + analysisDetails.getBranchName())
                .build();

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
               .arguments(new Arguments("updateCheckRun", new Argument<>("input", repositoryInputObject)))
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
                    .put("endLine", Optional.ofNullable(componentIssue.getIssue().getLine()).orElse(0) + 1)
                    .build();
            return graphqlProvider.createInputObject()
                    .put("path", componentIssue.getComponent().getReportAttributes().getScmPath().get())
                    .put("location", issueLocation)
                    .put("annotationLevel", mapToGithubAnnotationLevel(componentIssue.getIssue().severity()))
                    .put("message", componentIssue.getIssue().getMessage().replaceAll("\\\\","\\\\\\\\").replaceAll("\"", "\\\\\"")).build();
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
