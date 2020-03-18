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
import java.util.stream.Collectors;

import static org.apache.commons.collections4.ListUtils.partition;

public class GraphqlCheckRunProvider implements CheckRunProvider {

    public static final String PULL_REQUEST_GITHUB_URL = "sonar.pullrequest.github.endpoint";
    public static final String PULL_REQUEST_GITHUB_TOKEN = "sonar.alm.github.app.privateKey.secured";
    public static final String PULL_REQUEST_GITHUB_REPOSITORY = "sonar.pullrequest.github.repository";
    public static final String PULL_REQUEST_GITHUB_APP_ID = "sonar.alm.github.app.id";
    public static final String PULL_REQUEST_GITHUB_APP_NAME = "sonar.alm.github.app.name";

    public static final int GITHUB_CHECKS_ANNOTATIONS_MAX_NUMBER_PER_REQUEST = 50;

    private static final Logger LOGGER = Loggers.get(GraphqlCheckRunProvider.class);
    private static final String DATE_TIME_PATTERN = "yyyy-MM-dd'T'HH:mm:ssXXX";

    private final GraphqlProvider graphqlProvider;
    private final Clock clock;
    private final GithubApplicationAuthenticationProvider githubApplicationAuthenticationProvider;
    private final Server server;

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
    public void createCheckRun(AnalysisDetails analysisDetails, UnifyConfiguration unifyConfiguration) throws IOException, GeneralSecurityException {
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

        List<InputObject<Object>> annotations = analysisDetails.getPostAnalysisIssueVisitor().getIssues().stream()
                .filter(i -> i.getComponent().getReportAttributes().getScmPath().isPresent())
                .filter(i -> i.getComponent().getType() == Component.Type.FILE).map(componentIssue -> {
                    InputObject<Object> issueLocation = graphqlProvider.createInputObject()
                            .put("startLine", Optional.ofNullable(componentIssue.getIssue().getLine()).orElse(0))
                            .put("endLine", Optional.ofNullable(componentIssue.getIssue().getLine()).orElse(0) + 1)
                            .build();
                    return graphqlProvider.createInputObject()
                            .put("path", componentIssue.getComponent().getReportAttributes().getScmPath().get())
                            .put("location", issueLocation)
                            .put("annotationLevel", mapToGithubAnnotationLevel(componentIssue.getIssue().severity()))
                            .put("message", componentIssue.getIssue().getMessage().replaceAll("\"", "\\\\\"")).build();
                }).collect(Collectors.toList());

        List<List<InputObject<Object>>> partitionedAnnotations = partition(annotations, GITHUB_CHECKS_ANNOTATIONS_MAX_NUMBER_PER_REQUEST);

        for (List<InputObject<Object>> annotationsList : partitionedAnnotations) {
            InputObject<Object> checkRunOutputContent = graphqlProvider.createInputObject().put("title", "Quality Gate " +
                    (analysisDetails
                            .getQualityGateStatus() ==
                            QualityGate.Status.OK ?
                            "success" :
                            "failed"))
                    .put("summary", analysisDetails.createAnalysisSummary(new MarkdownFormatterFactory()))
                    .put("annotations", annotationsList).build();

            SimpleDateFormat startedDateFormat = new SimpleDateFormat(DATE_TIME_PATTERN);
            startedDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));

            InputObject<Object> repositoryInputObject =
                    graphqlProvider.createInputObject().put("repositoryId", repositoryAuthenticationToken.getRepositoryId())
                            .put("name", appName + " Results").put("headSha", analysisDetails.getCommitSha())
                            .put("status", RequestableCheckStatusState.COMPLETED).put("conclusion", QualityGate.Status.OK ==
                            analysisDetails
                                    .getQualityGateStatus() ?
                            CheckConclusionState.SUCCESS :
                            CheckConclusionState.FAILURE)
                            .put("detailsUrl", String.format("%s/dashboard?id=%s&pullRequest=%s", server.getPublicRootUrl(),
                                    URLEncoder.encode(analysisDetails.getAnalysisProjectKey(),
                                            StandardCharsets.UTF_8.name()), URLEncoder
                                            .encode(analysisDetails.getBranchName(),
                                                    StandardCharsets.UTF_8.name())))
                            .put("startedAt", startedDateFormat.format(analysisDetails.getAnalysisDate()))
                            .put("completedAt", DateTimeFormatter.ofPattern(DATE_TIME_PATTERN).withZone(ZoneId.of("UTC"))
                                    .format(clock.instant())).put("externalId", analysisDetails.getAnalysisId())
                            .put("output", checkRunOutputContent).build();


            GraphQLRequestEntity graphQLRequestEntity =
                    graphqlProvider.createRequestBuilder().url(apiUrl + "/graphql").headers(headers)
                            .request(CreateCheckRun.class)
                            .arguments(new Arguments("createCheckRun", new Argument<>("input", repositoryInputObject)))
                            .requestMethod(GraphQLTemplate.GraphQLMethod.MUTATE).build();

            LOGGER.debug("Using request: " + graphQLRequestEntity.getRequest());

            GraphQLTemplate graphQLTemplate = graphqlProvider.createGraphQLTemplate();

            GraphQLResponseEntity<CreateCheckRun> response =
                    graphQLTemplate.mutate(graphQLRequestEntity, CreateCheckRun.class);

            LOGGER.debug("Received response: " + response.toString());

            if (null != response.getErrors() && response.getErrors().length > 0) {
                List<String> errors = new ArrayList<>();
                for (Error error : response.getErrors()) {
                    errors.add("- " + error.toString());
                }
                throw new IllegalStateException(
                        "An error was returned in the response from the Github API:" + System.lineSeparator() +
                                errors.stream().collect(Collectors.joining(System.lineSeparator())));
            }
        }
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
