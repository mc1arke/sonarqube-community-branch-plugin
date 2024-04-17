/*
 * Copyright (C) 2020-2022 Michael Clarke
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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.mc1arke.sonarqube.plugin.almclient.github.RepositoryAuthenticationToken;
import com.github.mc1arke.sonarqube.plugin.almclient.github.model.Annotation;
import com.github.mc1arke.sonarqube.plugin.almclient.github.model.CheckRunDetails;
import com.github.mc1arke.sonarqube.plugin.almclient.github.v4.model.CheckAnnotationLevel;
import com.github.mc1arke.sonarqube.plugin.almclient.github.v4.model.CheckConclusionState;
import com.github.mc1arke.sonarqube.plugin.almclient.github.v4.model.RequestableCheckStatusState;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.AnalysisDetails;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.PostAnalysisIssueVisitor;
import io.aexp.nodes.graphql.Arguments;
import io.aexp.nodes.graphql.GraphQLRequestEntity;
import io.aexp.nodes.graphql.GraphQLResponseEntity;
import io.aexp.nodes.graphql.GraphQLTemplate;
import io.aexp.nodes.graphql.InputObject;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.sonar.api.issue.Issue;
import org.sonar.api.rule.Severity;
import org.sonar.ce.task.projectanalysis.component.Component;
import org.sonar.ce.task.projectanalysis.component.ReportAttributes;

import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GraphqlGithubClientTest {

    private final GraphqlProvider graphqlProvider = mock(GraphqlProvider.class, RETURNS_DEEP_STUBS);
    private final Clock clock = Clock.fixed(Instant.ofEpochSecond(1234567890), ZoneId.of("UTC"));

    @Test
    void createCheckRunExceptionOnErrorResponse() throws IOException {
        RepositoryAuthenticationToken repositoryAuthenticationToken = mock(RepositoryAuthenticationToken.class);
        when(repositoryAuthenticationToken.getAuthenticationToken()).thenReturn("dummyAuthToken");
        when(repositoryAuthenticationToken.getRepositoryId()).thenReturn("repository ID");

        when(graphqlProvider.createRequestBuilder()).thenReturn(GraphQLRequestEntity.Builder());

        GraphQLResponseEntity<CreateCheckRun> graphQLResponseEntity =
                new ObjectMapper().readerFor(GraphQLResponseEntity.class)
                        .readValue("{\"errors\": [{\"message\":\"example message\", \"locations\": []}]}");
        GraphQLTemplate graphQLTemplate = mock(GraphQLTemplate.class);
        when(graphQLTemplate.mutate(any(), eq(CreateCheckRun.class))).thenReturn(graphQLResponseEntity);
        when(graphqlProvider.createGraphQLTemplate()).thenReturn(graphQLTemplate);

        GraphqlGithubClient testCase =
                new GraphqlGithubClient(graphqlProvider, "https://api.url", repositoryAuthenticationToken);
        CheckRunDetails checkRunDetails = CheckRunDetails.builder().withAnnotations(List.of()).withStartTime(ZonedDateTime.now()).withEndTime(ZonedDateTime.now()).build();
        assertThatThrownBy(() -> testCase.createCheckRun(checkRunDetails, true))
                .hasMessage(
                "An error was returned in the response from the Github API:" + System.lineSeparator() +
                "- Error{message='example message', locations=[]}").isExactlyInstanceOf(IllegalStateException.class);
    }

    @Test
    void verifyCheckRunSubmitsCorrectAnnotations() throws IOException {
        RepositoryAuthenticationToken repositoryAuthenticationToken = mock(RepositoryAuthenticationToken.class);
        when(repositoryAuthenticationToken.getAuthenticationToken()).thenReturn("dummyAuthToken");
        when(repositoryAuthenticationToken.getRepositoryId()).thenReturn("repository ID");
        when(repositoryAuthenticationToken.getOwnerName()).thenReturn("owner");
        when(repositoryAuthenticationToken.getRepositoryName()).thenReturn("repository");

        List<InputObject.Builder<Object>> inputObjectBuilders = new ArrayList<>();
        List<InputObject<Object>> inputObjects = new ArrayList<>();
        doAnswer(i -> {
            InputObject.Builder<Object> builder = spy(new InputObject.Builder<>());
            inputObjectBuilders.add(builder);
            doAnswer(i2 -> {
                InputObject<Object> inputObject = (InputObject<Object>) i2.callRealMethod();
                inputObjects.add(inputObject);
                return inputObject;
            }).when(builder).build();
            return builder;
        }).when(graphqlProvider).createInputObject();

        List<GraphQLRequestEntity.RequestBuilder> requestBuilders = new ArrayList<>();
        List<GraphQLRequestEntity> requestEntities = new ArrayList<>();
        doAnswer(i -> {
            GraphQLRequestEntity.RequestBuilder requestBuilder = spy(GraphQLRequestEntity.Builder());
            requestBuilders.add(requestBuilder);
            doAnswer(i2 -> {
                GraphQLRequestEntity graphQLRequestEntity = (GraphQLRequestEntity) i2.callRealMethod();
                requestEntities.add(graphQLRequestEntity);
                return graphQLRequestEntity;
            }).when(requestBuilder).build();
            return requestBuilder;
        }).when(graphqlProvider).createRequestBuilder();

        ObjectMapper objectMapper = new ObjectMapper();
        GraphQLResponseEntity<CreateCheckRun> graphQLResponseEntity =
            objectMapper.readValue("{\"response\": {\"checkRun\": {\"id\": \"ABC\"}}}", objectMapper.getTypeFactory().constructParametricType(GraphQLResponseEntity.class, CreateCheckRun.class));

        ArgumentCaptor<GraphQLRequestEntity> requestEntityArgumentCaptor = ArgumentCaptor.forClass(GraphQLRequestEntity.class);

        GraphQLTemplate graphQLTemplate = mock(GraphQLTemplate.class);
        when(graphQLTemplate.mutate(requestEntityArgumentCaptor.capture(), eq(CreateCheckRun.class))).thenReturn(graphQLResponseEntity);

        GraphQLResponseEntity<Viewer> viewerResponseEntity = objectMapper.readValue("{" +
                "  \"response\": {" +
                "      \"login\": \"test-sonar[bot]\"" +
                "  }" +
                "}",
            objectMapper.getTypeFactory().constructParametricType(GraphQLResponseEntity.class, Viewer.class));
        ArgumentCaptor<GraphQLRequestEntity> getViewer = ArgumentCaptor.forClass(GraphQLRequestEntity.class);
        when(graphQLTemplate.query(getViewer.capture(), eq(Viewer.class))).thenReturn(viewerResponseEntity);

        String bodyString = objectMapper.writeValueAsString("**Project ID:** project-key-test" + System.lineSeparator());
        GraphQLResponseEntity<GetRepository> getPullRequestResponseEntity =
            objectMapper.readValue("{" +
                "\"response\": " +
                "  {" +
                "    \"pullRequest\": {" +
                "      \"id\": \"MDExOlB1bGxSZXF1ZXN0MzUzNDc=\"," +
                "      \"comments\": {" +
                "        \"nodes\": [" +
                "          {" +
                "            \"id\": \"MDEyOklzc3VlQ29tbWVudDE1MDE3\"," +
                "            \"isMinimized\": false," +
                "            \"body\": " + bodyString + "," +
                "            \"author\": {" +
                "              \"__typename\": \"Bot\"," +
                "              \"login\": \"test-sonar\"" +
                "            }" +
                "          }"+
                "        ],"+
                "        \"pageInfo\": {" +
                "          \"hasNextPage\": false" +
                "        } " +
                "      }"+
                "    }" +
                "  }" +
                "}", objectMapper.getTypeFactory().constructParametricType(GraphQLResponseEntity.class, GetRepository.class));

        ArgumentCaptor<GraphQLRequestEntity> getPullRequestRequestEntityArgumentCaptor = ArgumentCaptor.forClass(GraphQLRequestEntity.class);
        when(graphQLTemplate.query(getPullRequestRequestEntityArgumentCaptor.capture(), eq(GetRepository.class))).thenReturn(getPullRequestResponseEntity);

        GraphQLResponseEntity<MinimizeComment> minimizeCommentResponseEntity =
            objectMapper.readValue("{\"response\":{}}", objectMapper.getTypeFactory().constructParametricType(GraphQLResponseEntity.class, MinimizeComment.class));

        ArgumentCaptor<GraphQLRequestEntity> minimizeCommentRequestEntityArgumentCaptor = ArgumentCaptor.forClass(GraphQLRequestEntity.class);
        when(graphQLTemplate.mutate(minimizeCommentRequestEntityArgumentCaptor.capture(), eq(MinimizeComment.class))).thenReturn(minimizeCommentResponseEntity);

        GraphQLResponseEntity<AddComment> addCommentResponseEntity =
            objectMapper.readValue("{\"response\":{}}", objectMapper.getTypeFactory().constructParametricType(GraphQLResponseEntity.class, AddComment.class));

        ArgumentCaptor<GraphQLRequestEntity> addCommentRequestEntityArgumentCaptor = ArgumentCaptor.forClass(GraphQLRequestEntity.class);
        when(graphQLTemplate.mutate(addCommentRequestEntityArgumentCaptor.capture(), eq(AddComment.class))).thenReturn(addCommentResponseEntity);

        when(graphqlProvider.createGraphQLTemplate()).thenReturn(graphQLTemplate);

        CheckRunDetails checkRunDetails = CheckRunDetails.builder()
                .withAnnotations(IntStream.range(0, 30).mapToObj(i -> Annotation.builder()
                            .withLine(i)
                            .withSeverity(CheckAnnotationLevel.WARNING)
                            .withScmPath("scmPath" + i)
                            .withMessage("annotationMessage " + i)
                            .build()).collect(Collectors.toList()))
                .withCheckConclusionState(CheckConclusionState.SUCCESS)
                .withCommitId("commit-id")
                .withSummary("Summary message")
                .withDashboardUrl("dashboard-url")
                .withStartTime(clock.instant().atZone(ZoneId.of("UTC")).minus(1, ChronoUnit.MINUTES))
                .withEndTime(clock.instant().atZone(ZoneId.of("UTC")))
                .withExternalId("external-id")
                .withName("Name")
                .withTitle("Title")
                .withPullRequestId(999)
                .withProjectKey("project-key-test")
                .build();


        GraphqlGithubClient testCase =
                new GraphqlGithubClient(graphqlProvider, "http://api.target.domain/api", repositoryAuthenticationToken);
        testCase.createCheckRun(checkRunDetails, true);

        assertEquals(5, requestBuilders.size());

        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "Bearer dummyAuthToken");
        headers.put("Accept", "application/vnd.github.antiope-preview+json");

        verify(requestBuilders.get(0)).requestMethod(GraphQLTemplate.GraphQLMethod.MUTATE);
        assertEquals(requestEntities.get(0), requestEntityArgumentCaptor.getValue());

        ArgumentCaptor<Arguments> argumentsArgumentCaptor = ArgumentCaptor.forClass(Arguments.class);
        verify(requestBuilders.get(0)).arguments(argumentsArgumentCaptor.capture());
        assertEquals("createCheckRun", argumentsArgumentCaptor.getValue().getDotPath());
        assertEquals(1, argumentsArgumentCaptor.getValue().getArguments().size());
        assertEquals("input", argumentsArgumentCaptor.getValue().getArguments().get(0).getKey());

        List<InputObject<Object>> expectedAnnotationObjects = new ArrayList<>();
        int position = 0;
        for (Annotation annotation : checkRunDetails.getAnnotations()) {
            int line = annotation.getLine();

            assertThat(inputObjectBuilders.get(position).build())
                    .usingRecursiveComparison()
                            .isEqualTo(new InputObject.Builder<>()
                                    .put("startLine", line)
                                    .put("endLine", line)
                                    .build());
            position++;

            String path = annotation.getScmPath();
            InputObject<Object> fileBuilder = inputObjectBuilders.get(position).build();
            assertThat(fileBuilder).usingRecursiveComparison()
                            .isEqualTo(new InputObject.Builder<>()
                                    .put("path", path)
                                    .put("location", inputObjects.get(position - 1))
                                    .put("annotationLevel", annotation.getSeverity())
                                    .put("message", annotation.getMessage())
                                    .build());

            expectedAnnotationObjects.add(inputObjects.get(position));

            position++;
        }

        assertEquals(4 + position, inputObjectBuilders.size());

        assertThat(inputObjectBuilders.get(position).build())
                .usingRecursiveComparison()
                        .isEqualTo(new InputObject.Builder<>()
                                .put("title", "Title")
                                .put("summary", "Summary message")
                                .put("annotations", expectedAnnotationObjects)
                                .build());

        assertThat(inputObjectBuilders.get(position + 1).build())
                .usingRecursiveComparison()
                        .isEqualTo(new InputObject.Builder<>()
                                .put("repositoryId", "repository ID")
                                .put("name", "Name")
                                .put("headSha", "commit-id")
                                .put("status", RequestableCheckStatusState.COMPLETED)
                                .put("conclusion", CheckConclusionState.SUCCESS)
                                .put("detailsUrl", "dashboard-url")
                                .put("startedAt", "2009-02-13T23:30:30Z")
                                .put("completedAt", "2009-02-13T23:31:30Z")
                                .put("externalId", "external-id")
                                .put("output", inputObjects.get(position))
                                .build());

        for (int i = 0; i < 5; i++) {
            verify(requestBuilders.get(i)).url("http://api.target.domain/api/graphql");
            verify(requestBuilders.get(i)).headers(headers);
            verify(requestBuilders.get(i)).build();
        }

        // Verify GetViewer
        assertEquals(requestEntities.get(1), getViewer.getValue());
        assertEquals(
            "query { viewer { login } } ",
            getViewer.getValue().getRequest()
        );

        // Verify GetPullRequest
        assertEquals(requestEntities.get(2), getPullRequestRequestEntityArgumentCaptor.getValue());
        assertEquals(
            "query { repository (owner:\"owner\",name:\"repository\") { url pullRequest : pullRequest (number:999) { comments : comments (first:100) { nodes" +
                " { author { type : __typename login } id minimized : isMinimized body } pageInfo { hasNextPage endCursor } } id } } } ",
            getPullRequestRequestEntityArgumentCaptor.getValue().getRequest()
        );

        // Validate Minimize Comment
        assertEquals(requestEntities.get(3), minimizeCommentRequestEntityArgumentCaptor.getValue());
        assertEquals(
            "mutation { minimizeComment (input:{classifier:OUTDATED,subjectId:\"MDEyOklzc3VlQ29tbWVudDE1MDE3\"}) { clientMutationId } } ",
            minimizeCommentRequestEntityArgumentCaptor.getValue().getRequest()
        );

        // Validate AddComment
        assertEquals(requestEntities.get(4), addCommentRequestEntityArgumentCaptor.getValue());
        assertEquals(
          "mutation { addComment (input:{body:\"Summary message\",subjectId:\"MDExOlB1bGxSZXF1ZXN0MzUzNDc=\"}) { clientMutationId } } ",
            addCommentRequestEntityArgumentCaptor.getValue().getRequest()
        );

    }

    @Test
    void checkExcessIssuesCorrectlyReported() throws IOException {
        ReportAttributes reportAttributes = mock(ReportAttributes.class);
        when(reportAttributes.getScmPath()).thenReturn(Optional.of("abc"));
        Component component = mock(Component.class);
        when(component.getType()).thenReturn(Component.Type.FILE);
        when(component.getReportAttributes()).thenReturn(reportAttributes);
        List<PostAnalysisIssueVisitor.ComponentIssue> issues = IntStream.range(0, 120)
                .mapToObj(i -> {
                    PostAnalysisIssueVisitor.LightIssue defaultIssue = mock(PostAnalysisIssueVisitor.LightIssue.class);
                    when(defaultIssue.severity()).thenReturn(Severity.INFO);
                    when(defaultIssue.getMessage()).thenReturn("message");
                    when(defaultIssue.status()).thenReturn(Issue.STATUS_OPEN);
                    when(defaultIssue.resolution()).thenReturn(null);
                    return defaultIssue;
                })
                .map(i -> {
                    PostAnalysisIssueVisitor.ComponentIssue componentIssue = mock(
                            PostAnalysisIssueVisitor.ComponentIssue.class);
                    when(componentIssue.getComponent()).thenReturn(component);
                    when(componentIssue.getIssue()).thenReturn(i);
                    return componentIssue;
                }).collect(Collectors.toList());

        AnalysisDetails analysisDetails = mock(AnalysisDetails.class);
        when(analysisDetails.getScmReportableIssues()).thenReturn(issues);
        when(analysisDetails.getPullRequestId()).thenReturn("13579");
        when(analysisDetails.getAnalysisProjectKey()).thenReturn("projectKey");
        when(analysisDetails.getAnalysisDate()).thenReturn(new Date());

        List<InputObject.Builder> builders = new ArrayList<>();

        GraphqlProvider graphqlProvider = mock(GraphqlProvider.class);
        when(graphqlProvider.createInputObject()).thenAnswer(i -> {
            InputObject.Builder builder = spy(new InputObject.Builder<>());
            builders.add(builder);
            return builder;
        });

        GraphQLRequestEntity.RequestBuilder requestBuilder = GraphQLRequestEntity.Builder();
        ObjectMapper objectMapper = new ObjectMapper();

        when(graphqlProvider.createRequestBuilder()).thenReturn(requestBuilder);

        GraphQLTemplate graphQLTemplate = mock(GraphQLTemplate.class);
        GraphQLResponseEntity<CreateCheckRun> graphQLResponseEntity =
                new ObjectMapper().readValue("{\"response\": {\"checkRun\": {\"id\": \"ABC\"}}}", objectMapper.getTypeFactory().constructParametricType(GraphQLResponseEntity.class, CreateCheckRun.class));
        when(graphQLTemplate.mutate(any(), eq(CreateCheckRun.class))).thenReturn(graphQLResponseEntity);
        GraphQLResponseEntity<UpdateCheckRun> graphQLResponseEntity2 =
                new ObjectMapper().readValue("{\"response\": {\"checkRun\": {\"id\": \"ABC\"}}}", objectMapper.getTypeFactory().constructParametricType(GraphQLResponseEntity.class, UpdateCheckRun.class));
        when(graphQLTemplate.mutate(any(), eq(UpdateCheckRun.class))).thenReturn(graphQLResponseEntity2);
        when(graphqlProvider.createGraphQLTemplate()).thenReturn(graphQLTemplate);

        Clock clock = Clock.fixed(Instant.now(), ZoneId.of("UTC"));

        RepositoryAuthenticationToken repositoryAuthenticationToken = mock(RepositoryAuthenticationToken.class);
        when(repositoryAuthenticationToken.getAuthenticationToken()).thenReturn("dummy");

        CheckRunDetails checkRunDetails = mock(CheckRunDetails.class);
        when(checkRunDetails.getAnnotations()).thenReturn(IntStream.range(0, 120).mapToObj(i -> Annotation.builder()
                .withLine(i).withMessage("message " + i)
                .withSeverity(CheckAnnotationLevel.NOTICE)
                .withScmPath("path " + i)
                .build())
                .collect(Collectors.toList()));
        when(checkRunDetails.getStartTime()).thenReturn(clock.instant().atZone(ZoneId.of("UTC")));
        when(checkRunDetails.getEndTime()).thenReturn(clock.instant().atZone(ZoneId.of("UTC")));

        GraphqlGithubClient testCase = new GraphqlGithubClient(graphqlProvider, "https://api.url/path", repositoryAuthenticationToken);
        testCase.createCheckRun(checkRunDetails, false);

        ArgumentCaptor<Class<?>> classArgumentCaptor = ArgumentCaptor.forClass(Class.class);
        verify(graphQLTemplate, times(3)).mutate(any(GraphQLRequestEntity.class), classArgumentCaptor.capture());

        assertThat(classArgumentCaptor.getAllValues()).containsExactly(CreateCheckRun.class, UpdateCheckRun.class, UpdateCheckRun.class);

        ArgumentCaptor<List<InputObject>> annotationsArgumentCaptor = ArgumentCaptor.forClass(List.class);
        verify(builders.get(100), times(3)).put(eq("annotations"), annotationsArgumentCaptor.capture());
        assertThat(annotationsArgumentCaptor.getAllValues().get(0)).hasSize(50);
        assertThat(annotationsArgumentCaptor.getAllValues().get(1)).hasSize(50);
        assertThat(annotationsArgumentCaptor.getAllValues().get(2)).hasSize(20);
    }

}
