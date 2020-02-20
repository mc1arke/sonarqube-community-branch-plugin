/*
 * Copyright (C) 2020 Michael Clarke
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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.AnalysisDetails;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.PostAnalysisIssueVisitor;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.github.GithubApplicationAuthenticationProvider;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.github.RepositoryAuthenticationToken;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.github.v4.model.CheckAnnotationLevel;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.github.v4.model.CheckConclusionState;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.github.v4.model.RequestableCheckStatusState;
import io.aexp.nodes.graphql.Arguments;
import io.aexp.nodes.graphql.GraphQLRequestEntity;
import io.aexp.nodes.graphql.GraphQLResponseEntity;
import io.aexp.nodes.graphql.GraphQLTemplate;
import io.aexp.nodes.graphql.InputObject;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.sonar.api.ce.posttask.QualityGate;
import org.sonar.api.platform.Server;
import org.sonar.api.rule.Severity;
import org.sonar.ce.task.projectanalysis.component.Component;
import org.sonar.ce.task.projectanalysis.component.ReportAttributes;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.db.alm.setting.AlmSettingDto;
import org.sonar.db.alm.setting.ProjectAlmSettingDto;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class GraphqlCheckRunProviderTest {

    @Test
    public void createCheckRunExceptionOnErrorResponse() throws IOException, GeneralSecurityException {
        GraphqlProvider graphqlProvider = mock(GraphqlProvider.class, RETURNS_DEEP_STUBS);
        Clock clock = Clock.fixed(Instant.ofEpochSecond(1234567890), ZoneId.of("UTC"));
        GithubApplicationAuthenticationProvider githubApplicationAuthenticationProvider =
                mock(GithubApplicationAuthenticationProvider.class);
        Server server = mock(Server.class);
        when(server.getPublicRootUrl()).thenReturn("http://sonar.server/root");

        PostAnalysisIssueVisitor postAnalysisIssueVisitor = mock(PostAnalysisIssueVisitor.class);
        when(postAnalysisIssueVisitor.getIssues()).thenReturn(new ArrayList<>());

        AnalysisDetails analysisDetails = mock(AnalysisDetails.class);
        when(analysisDetails.getQualityGateStatus()).thenReturn(QualityGate.Status.OK);
        when(analysisDetails.createAnalysisSummary(any())).thenReturn("dummy summary");
        when(analysisDetails.getCommitSha()).thenReturn("commit SHA");
        when(analysisDetails.getAnalysisProjectKey()).thenReturn("projectKey");
        when(analysisDetails.getBranchName()).thenReturn("branchName");
        when(analysisDetails.getAnalysisDate()).thenReturn(new Date(1234567890));
        when(analysisDetails.getAnalysisId()).thenReturn("analysis ID");
        when(analysisDetails.getPostAnalysisIssueVisitor()).thenReturn(postAnalysisIssueVisitor);

        RepositoryAuthenticationToken repositoryAuthenticationToken = mock(RepositoryAuthenticationToken.class);
        when(repositoryAuthenticationToken.getAuthenticationToken()).thenReturn("dummyAuthToken");
        when(repositoryAuthenticationToken.getRepositoryId()).thenReturn("repository ID");
        when(githubApplicationAuthenticationProvider
                     .getInstallationToken(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(repositoryAuthenticationToken);

        when(graphqlProvider.createRequestBuilder()).thenReturn(GraphQLRequestEntity.Builder());

        GraphQLResponseEntity<CreateCheckRun> graphQLResponseEntity =
                new ObjectMapper().readerFor(GraphQLResponseEntity.class)
                        .readValue("{\"errors\": [{\"message\":\"example message\", \"locations\": []}]}");
        GraphQLTemplate graphQLTemplate = mock(GraphQLTemplate.class);
        when(graphQLTemplate.mutate(any(), eq(CreateCheckRun.class))).thenReturn(graphQLResponseEntity);
        when(graphqlProvider.createGraphQLTemplate()).thenReturn(graphQLTemplate);

        ProjectAlmSettingDto projectAlmSettingDto = mock(ProjectAlmSettingDto.class);
        when(projectAlmSettingDto.getAlmRepo()).thenReturn("dummy/repo");
        AlmSettingDto almSettingDto = mock(AlmSettingDto.class);
        when(almSettingDto.getUrl()).thenReturn("http://host.name");
        when(almSettingDto.getAppId()).thenReturn("app id");
        when(almSettingDto.getPrivateKey()).thenReturn("private key");

        GraphqlCheckRunProvider testCase =
                new GraphqlCheckRunProvider(graphqlProvider, clock, githubApplicationAuthenticationProvider, server);
        assertThatThrownBy(() -> testCase.createCheckRun(analysisDetails, almSettingDto, projectAlmSettingDto))
                .hasMessage(
                "An error was returned in the response from the Github API:" + System.lineSeparator() +
                "- Error{message='example message', locations=[]}").isExactlyInstanceOf(IllegalStateException.class);

        verify(githubApplicationAuthenticationProvider)
                .getInstallationToken(eq("http://host.name"), eq("app id"), eq("private key"), eq("dummy/repo"));

    }


    @Test
    public void createCheckRunExceptionOnInvalidIssueSeverity() throws IOException, GeneralSecurityException {
        GraphqlProvider graphqlProvider = mock(GraphqlProvider.class, RETURNS_DEEP_STUBS);
        Clock clock = Clock.fixed(Instant.ofEpochSecond(1234567890), ZoneId.of("UTC"));
        GithubApplicationAuthenticationProvider githubApplicationAuthenticationProvider =
                mock(GithubApplicationAuthenticationProvider.class);
        when(githubApplicationAuthenticationProvider.getInstallationToken(any(), any(), any(), any()))
                .thenReturn(mock(RepositoryAuthenticationToken.class));
        Server server = mock(Server.class);
        when(server.getPublicRootUrl()).thenReturn("http://sonar.server/root");

        ReportAttributes reportAttributes = mock(ReportAttributes.class);
        when(reportAttributes.getScmPath()).thenReturn(Optional.of("path"));

        Component component = mock(Component.class);
        when(component.getType()).thenReturn(Component.Type.FILE);
        when(component.getReportAttributes()).thenReturn(reportAttributes);

        DefaultIssue defaultIssue = mock(DefaultIssue.class);
        when(defaultIssue.severity()).thenReturn("dummy");

        PostAnalysisIssueVisitor.ComponentIssue componentIssue = mock(PostAnalysisIssueVisitor.ComponentIssue.class);
        when(componentIssue.getIssue()).thenReturn(defaultIssue);
        when(componentIssue.getComponent()).thenReturn(component);

        PostAnalysisIssueVisitor postAnalysisIssueVisitor = mock(PostAnalysisIssueVisitor.class);
        when(postAnalysisIssueVisitor.getIssues()).thenReturn(Collections.singletonList(componentIssue));

        AnalysisDetails analysisDetails = mock(AnalysisDetails.class);
        when(analysisDetails.getQualityGateStatus()).thenReturn(QualityGate.Status.OK);
        when(analysisDetails.createAnalysisSummary(any())).thenReturn("dummy summary");
        when(analysisDetails.getCommitSha()).thenReturn("commit SHA");
        when(analysisDetails.getAnalysisProjectKey()).thenReturn("projectKey");
        when(analysisDetails.getBranchName()).thenReturn("branchName");
        when(analysisDetails.getAnalysisDate()).thenReturn(new Date(1234567890));
        when(analysisDetails.getAnalysisId()).thenReturn("analysis ID");
        when(analysisDetails.getPostAnalysisIssueVisitor()).thenReturn(postAnalysisIssueVisitor);

        ProjectAlmSettingDto projectAlmSettingDto = mock(ProjectAlmSettingDto.class);
        AlmSettingDto almSettingDto = mock(AlmSettingDto.class);

        GraphqlCheckRunProvider testCase =
                new GraphqlCheckRunProvider(graphqlProvider, clock, githubApplicationAuthenticationProvider, server);
        assertThatThrownBy(() -> testCase.createCheckRun(analysisDetails, almSettingDto, projectAlmSettingDto))
                .hasMessage("Unknown severity value: dummy")
                .isExactlyInstanceOf(IllegalArgumentException.class);
    }


    @Test
    public void createCheckRunHappyPathOkStatus() throws IOException, GeneralSecurityException {
        createCheckRunHappyPath(QualityGate.Status.OK);
    }

    @Test
    public void createCheckRunHappyPathErrorStatus() throws IOException, GeneralSecurityException {
        createCheckRunHappyPath(QualityGate.Status.ERROR);
    }


    private void createCheckRunHappyPath(QualityGate.Status status) throws IOException, GeneralSecurityException {
        GraphqlProvider graphqlProvider = mock(GraphqlProvider.class);
        Clock clock = Clock.fixed(Instant.ofEpochSecond(1234567890), ZoneId.of("UTC"));
        GithubApplicationAuthenticationProvider githubApplicationAuthenticationProvider =
                mock(GithubApplicationAuthenticationProvider.class);
        Server server = mock(Server.class);
        when(server.getPublicRootUrl()).thenReturn("http://sonar.server/root");

        DefaultIssue issue1 = mock(DefaultIssue.class);
        when(issue1.getLine()).thenReturn(2);
        when(issue1.getMessage()).thenReturn("issue 1");
        when(issue1.severity()).thenReturn(Severity.INFO);

        ReportAttributes reportAttributes = mock(ReportAttributes.class);
        when(reportAttributes.getScmPath()).thenReturn(Optional.of("path/to.file"));
        Component component1 = mock(Component.class);
        when(component1.getReportAttributes()).thenReturn(reportAttributes);
        when(component1.getType()).thenReturn(Component.Type.FILE);

        PostAnalysisIssueVisitor.ComponentIssue componentIssue1 = mock(PostAnalysisIssueVisitor.ComponentIssue.class);
        when(componentIssue1.getComponent()).thenReturn(component1);
        when(componentIssue1.getIssue()).thenReturn(issue1);

        DefaultIssue issue2 = mock(DefaultIssue.class);
        when(issue2.getLine()).thenReturn(null);
        when(issue2.getMessage()).thenReturn("issue 2");

        PostAnalysisIssueVisitor.ComponentIssue componentIssue2 = mock(PostAnalysisIssueVisitor.ComponentIssue.class);
        when(componentIssue2.getComponent()).thenReturn(component1);
        when(componentIssue2.getIssue()).thenReturn(issue2);
        when(issue2.severity()).thenReturn(Severity.BLOCKER);

        ReportAttributes reportAttributes2 = mock(ReportAttributes.class);
        when(reportAttributes2.getScmPath()).thenReturn(Optional.empty());
        Component component2 = mock(Component.class);
        when(component2.getReportAttributes()).thenReturn(reportAttributes);
        when(component2.getType()).thenReturn(Component.Type.FILE);

        DefaultIssue issue3 = mock(DefaultIssue.class);
        when(issue3.getLine()).thenReturn(9);
        when(issue3.severity()).thenReturn(Severity.CRITICAL);
        when(issue3.getMessage()).thenReturn("issue 3");

        PostAnalysisIssueVisitor.ComponentIssue componentIssue3 = mock(PostAnalysisIssueVisitor.ComponentIssue.class);
        when(componentIssue3.getComponent()).thenReturn(component2);
        when(componentIssue3.getIssue()).thenReturn(issue3);

        ReportAttributes reportAttributes3 = mock(ReportAttributes.class);
        when(reportAttributes3.getScmPath()).thenReturn(Optional.empty());
        Component component3 = mock(Component.class);
        when(component3.getReportAttributes()).thenReturn(reportAttributes);
        when(component3.getType()).thenReturn(Component.Type.PROJECT);

        DefaultIssue issue4 = mock(DefaultIssue.class);
        when(issue4.getLine()).thenReturn(2);
        when(issue4.severity()).thenReturn(Severity.CRITICAL);
        when(issue4.getMessage()).thenReturn("issue 4");

        PostAnalysisIssueVisitor.ComponentIssue componentIssue4 = mock(PostAnalysisIssueVisitor.ComponentIssue.class);
        when(componentIssue4.getComponent()).thenReturn(component3);
        when(componentIssue4.getIssue()).thenReturn(issue4);

        DefaultIssue issue5 = mock(DefaultIssue.class);
        when(issue5.getLine()).thenReturn(1999);
        when(issue5.severity()).thenReturn(Severity.MAJOR);
        when(issue5.getMessage()).thenReturn("issue 5");

        PostAnalysisIssueVisitor.ComponentIssue componentIssue5 = mock(PostAnalysisIssueVisitor.ComponentIssue.class);
        when(componentIssue5.getComponent()).thenReturn(component2);
        when(componentIssue5.getIssue()).thenReturn(issue5);

        DefaultIssue issue6 = mock(DefaultIssue.class);
        when(issue6.getLine()).thenReturn(42);
        when(issue6.severity()).thenReturn(Severity.MINOR);
        when(issue6.getMessage()).thenReturn("issue 6");

        PostAnalysisIssueVisitor.ComponentIssue componentIssue6 = mock(PostAnalysisIssueVisitor.ComponentIssue.class);
        when(componentIssue6.getComponent()).thenReturn(component2);
        when(componentIssue6.getIssue()).thenReturn(issue6);

        List<PostAnalysisIssueVisitor.ComponentIssue> issueList =
                Arrays.asList(componentIssue1, componentIssue2, componentIssue3, componentIssue4, componentIssue5,
                              componentIssue6);
        PostAnalysisIssueVisitor postAnalysisIssueVisitor = mock(PostAnalysisIssueVisitor.class);
        when(postAnalysisIssueVisitor.getIssues()).thenReturn(issueList);

        AnalysisDetails analysisDetails = mock(AnalysisDetails.class);
        when(analysisDetails.getQualityGateStatus()).thenReturn(status);
        when(analysisDetails.createAnalysisSummary(any())).thenReturn("dummy summary");
        when(analysisDetails.getCommitSha()).thenReturn("commit SHA");
        when(analysisDetails.getAnalysisProjectKey()).thenReturn("projectKey");
        when(analysisDetails.getBranchName()).thenReturn("branchName");
        when(analysisDetails.getAnalysisDate()).thenReturn(new Date(1234567890));
        when(analysisDetails.getAnalysisId()).thenReturn("analysis ID");
        when(analysisDetails.getPostAnalysisIssueVisitor()).thenReturn(postAnalysisIssueVisitor);

        ArgumentCaptor<String> authenticationProviderArgumentCaptor = ArgumentCaptor.forClass(String.class);
        RepositoryAuthenticationToken repositoryAuthenticationToken = mock(RepositoryAuthenticationToken.class);
        when(repositoryAuthenticationToken.getAuthenticationToken()).thenReturn("dummyAuthToken");
        when(repositoryAuthenticationToken.getRepositoryId()).thenReturn("repository ID");
        when(githubApplicationAuthenticationProvider
                     .getInstallationToken(authenticationProviderArgumentCaptor.capture(),
                                           authenticationProviderArgumentCaptor.capture(),
                                           authenticationProviderArgumentCaptor.capture(),
                                           authenticationProviderArgumentCaptor.capture()))
                .thenReturn(repositoryAuthenticationToken);

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

        GraphQLResponseEntity<CreateCheckRun> graphQLResponseEntity =
                new ObjectMapper().readerFor(GraphQLResponseEntity.class)
                        .readValue(status == QualityGate.Status.ERROR ? "{\"errors\": []}" : "{\"response\": {}}}");

        ArgumentCaptor<GraphQLRequestEntity> requestEntityArgumentCaptor =
                ArgumentCaptor.forClass(GraphQLRequestEntity.class);

        GraphQLTemplate graphQLTemplate = mock(GraphQLTemplate.class);
        when(graphQLTemplate.mutate(requestEntityArgumentCaptor.capture(), eq(CreateCheckRun.class)))
                .thenReturn(graphQLResponseEntity);
        when(graphqlProvider.createGraphQLTemplate()).thenReturn(graphQLTemplate);

        ProjectAlmSettingDto projectAlmSettingDto = mock(ProjectAlmSettingDto.class);
        when(projectAlmSettingDto.getAlmRepo()).thenReturn("dummy/repo");
        AlmSettingDto almSettingDto = mock(AlmSettingDto.class);
        when(almSettingDto.getUrl()).thenReturn("http://host.name");
        when(almSettingDto.getAppId()).thenReturn("app id");
        when(almSettingDto.getPrivateKey()).thenReturn("private key");

        GraphqlCheckRunProvider testCase =
                new GraphqlCheckRunProvider(graphqlProvider, clock, githubApplicationAuthenticationProvider, server);
        testCase.createCheckRun(analysisDetails, almSettingDto, projectAlmSettingDto);

        assertEquals(1, requestBuilders.size());

        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "Bearer dummyAuthToken");
        headers.put("Accept", "application/vnd.github.antiope-preview+json");


        verify(requestBuilders.get(0)).url(eq("http://host.name/graphql"));
        verify(requestBuilders.get(0)).headers(eq(headers));
        verify(requestBuilders.get(0)).requestMethod(eq(GraphQLTemplate.GraphQLMethod.MUTATE));
        verify(requestBuilders.get(0)).build();
        assertEquals(requestEntities.get(0), requestEntityArgumentCaptor.getValue());

        ArgumentCaptor<Arguments> argumentsArgumentCaptor = ArgumentCaptor.forClass(Arguments.class);
        verify(requestBuilders.get(0)).arguments(argumentsArgumentCaptor.capture());
        assertEquals("createCheckRun", argumentsArgumentCaptor.getValue().getDotPath());
        assertEquals(1, argumentsArgumentCaptor.getValue().getArguments().size());
        assertEquals("input", argumentsArgumentCaptor.getValue().getArguments().get(0).getKey());

        assertEquals(Arrays.asList("http://host.name", "app id", "private key", "dummy/repo"),
                     authenticationProviderArgumentCaptor.getAllValues());

        List<InputObject<Object>> expectedAnnotationObjects = new ArrayList<>();
        int position = 0;
        for (int i = 0; i < issueList.size(); i++) {
            if (issueList.get(i).getComponent().getType() != Component.Type.FILE ||
                !issueList.get(i).getComponent().getReportAttributes().getScmPath().isPresent()) {
                continue;
            }
            int line = (null == issueList.get(i).getIssue().getLine() ? 0 : issueList.get(i).getIssue().getLine());
            verify(inputObjectBuilders.get(position)).put(eq("startLine"), eq(line));
            verify(inputObjectBuilders.get(position)).put(eq("endLine"), eq(line + 1));
            verify(inputObjectBuilders.get(position)).build();
            position++;

            String path = issueList.get(i).getComponent().getReportAttributes().getScmPath().get();
            InputObject.Builder<Object> fileBuilder = inputObjectBuilders.get(position);
            verify(fileBuilder).put(eq("path"), eq(path));
            verify(fileBuilder).put(eq("location"), eq(inputObjects.get(position - 1)));
            String sonarQubeSeverity = issueList.get(i).getIssue().severity();
            verify(fileBuilder).put(eq("annotationLevel"),
                                    eq(sonarQubeSeverity.equals(Severity.INFO) ? CheckAnnotationLevel.NOTICE :
                                       sonarQubeSeverity.equals(Severity.MINOR) ||
                                       sonarQubeSeverity.equals(Severity.MAJOR) ? CheckAnnotationLevel.WARNING :
                                       CheckAnnotationLevel.FAILURE));
            verify(fileBuilder).put(eq("message"), eq("issue " + (i + 1)));
            verify(inputObjectBuilders.get(position)).build();

            expectedAnnotationObjects.add(inputObjects.get(position));

            position++;
        }

        assertEquals(2 + position, inputObjectBuilders.size());

        ArgumentCaptor<List<InputObject<Object>>> annotationArgumentCaptor = ArgumentCaptor.forClass(List.class);

        verify(inputObjectBuilders.get(position))
                .put(eq("title"), eq("Quality Gate " + (status == QualityGate.Status.OK ? "success" : "failed")));
        verify(inputObjectBuilders.get(position)).put(eq("summary"), eq("dummy summary"));
        verify(inputObjectBuilders.get(position)).put(eq("annotations"), annotationArgumentCaptor.capture());
        verify(inputObjectBuilders.get(position)).build();

        assertThat(annotationArgumentCaptor.getValue()).isEqualTo(expectedAnnotationObjects);

        verify(inputObjectBuilders.get(position + 1)).put(eq("repositoryId"), eq("repository ID"));
        verify(inputObjectBuilders.get(position + 1)).put(eq("name"), eq("Sonarqube Results"));
        verify(inputObjectBuilders.get(position + 1)).put(eq("headSha"), eq("commit SHA"));
        verify(inputObjectBuilders.get(position + 1)).put(eq("status"), eq(RequestableCheckStatusState.COMPLETED));
        verify(inputObjectBuilders.get(position + 1)).put(eq("conclusion"), eq(status == QualityGate.Status.OK ?
                                                                               CheckConclusionState.SUCCESS :
                                                                               CheckConclusionState.FAILURE));
        verify(inputObjectBuilders.get(position + 1))
                .put(eq("detailsUrl"), eq("http://sonar.server/root/dashboard?id=projectKey&pullRequest=branchName"));
        verify(inputObjectBuilders.get(position + 1)).put(eq("startedAt"), eq("1970-01-15T06:56:07Z"));
        verify(inputObjectBuilders.get(position + 1)).put(eq("completedAt"), eq("2009-02-13T23:31:30Z"));
        verify(inputObjectBuilders.get(position + 1)).put(eq("externalId"), eq("analysis ID"));
        verify(inputObjectBuilders.get(position + 1)).put(eq("output"), eq(inputObjects.get(position)));
        verify(inputObjectBuilders.get(position + 1)).build();
    }

    @Test
    public void checkCorrectDefaultValuesInjected() {
        Clock clock = mock(Clock.class);
        GithubApplicationAuthenticationProvider githubApplicationAuthenticationProvider =
                mock(GithubApplicationAuthenticationProvider.class);
        Server server = mock(Server.class);
        assertThat(new GraphqlCheckRunProvider(clock, githubApplicationAuthenticationProvider, server))
                .usingRecursiveComparison()
                .isEqualTo(new GraphqlCheckRunProvider(new DefaultGraphqlProvider(), clock,
                                                       githubApplicationAuthenticationProvider, server));
    }
}
