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
package com.github.mc1arke.sonarqube.plugin.scanner;

import org.hamcrest.CustomMatcher;
import org.hamcrest.core.IsEqual;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.notifications.AnalysisWarnings;
import org.sonar.api.utils.MessageException;
import org.sonar.api.utils.System2;
import org.sonar.scanner.scan.branch.BranchConfiguration;
import org.sonar.scanner.scan.branch.BranchInfo;
import org.sonar.scanner.scan.branch.BranchType;
import org.sonar.scanner.scan.branch.DefaultBranchConfiguration;
import org.sonar.scanner.scan.branch.ProjectBranches;
import org.sonar.scanner.scan.branch.ProjectPullRequests;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Michael Clarke
 */
public class CommunityBranchConfigurationLoaderTest {

    private final ExpectedException expectedException = ExpectedException.none();

    @Rule
    public ExpectedException expectedException() {
        return expectedException;
    }

    @Test
    public void testExceptionWhenNoExistingBranchAndBranchParamsPresent() {
        AnalysisWarnings analysisWarnings = mock(AnalysisWarnings.class);
        System2 system2 = mock(System2.class);
        CommunityBranchConfigurationLoader testCase = new CommunityBranchConfigurationLoader(system2, analysisWarnings);
        ProjectBranches branchInfo = mock(ProjectBranches.class);
        when(branchInfo.isEmpty()).thenReturn(true);

        Map<String, String> parameters = new HashMap<>();
        parameters.put("sonar.branch.name", "dummy");

        expectedException.expect(MessageException.class);
        expectedException.expectMessage(IsEqual.equalTo(
                "No branches currently exist in this project. Please scan the main branch without passing any branch parameters."));

        testCase.load(parameters, branchInfo, mock(ProjectPullRequests.class));
    }

    @Test
    public void testDefaultConfigWhenNoExistingBranchAndBranchNameParamMaster() {
        AnalysisWarnings analysisWarnings = mock(AnalysisWarnings.class);
        System2 system2 = mock(System2.class);
        CommunityBranchConfigurationLoader testCase = new CommunityBranchConfigurationLoader(system2, analysisWarnings);
        ProjectBranches branchInfo = mock(ProjectBranches.class);
        when(branchInfo.isEmpty()).thenReturn(true);

        Map<String, String> parameters = new HashMap<>();
        parameters.put("sonar.branch.name", "master");

        assertEquals(DefaultBranchConfiguration.class,
                     testCase.load(parameters, branchInfo, mock(ProjectPullRequests.class)).getClass());
    }

    @Test
    public void testErrorWhenNoExistingBranchAndBranchTargetMasterButNoSourceBranch() {
        AnalysisWarnings analysisWarnings = mock(AnalysisWarnings.class);
        System2 system2 = mock(System2.class);
        CommunityBranchConfigurationLoader testCase = new CommunityBranchConfigurationLoader(system2, analysisWarnings);
        ProjectBranches branchInfo = mock(ProjectBranches.class);
        when(branchInfo.isEmpty()).thenReturn(true);

        Map<String, String> parameters = new HashMap<>();
        parameters.put("sonar.branch.name", "dummy");

        expectedException.expect(MessageException.class);
        expectedException.expectMessage(IsEqual.equalTo(
                "No branches currently exist in this project. Please scan the main branch without passing any branch parameters."));

        testCase.load(parameters, branchInfo, mock(ProjectPullRequests.class));
    }

    @Test
    public void testWarningWhenTargetBranchParameterSpecified() {
        AnalysisWarnings analysisWarnings = mock(AnalysisWarnings.class);
        System2 system2 = mock(System2.class);
        CommunityBranchConfigurationLoader testCase = new CommunityBranchConfigurationLoader(system2, analysisWarnings);
        Map<String, String> parameters = new HashMap<>();
        parameters.put("sonar.branch.name", "feature/shortLivedBranch");
        parameters.put("sonar.branch.target", "dummy");

        BranchInfo mockTargetBranchInfo = mock(BranchInfo.class);
        when(mockTargetBranchInfo.name()).thenReturn("defaultBranchInfo");
        when(mockTargetBranchInfo.type()).thenReturn(BranchType.BRANCH);

        ProjectBranches projectBranches = mock(ProjectBranches.class);
        when(projectBranches.get("masterxxx")).thenReturn(mockTargetBranchInfo);
        when(projectBranches.defaultBranchName()).thenReturn("masterxxx");

        BranchConfiguration result = testCase.load(parameters, projectBranches, mock(ProjectPullRequests.class));

        assertNull(result.targetBranchName());
        assertEquals("feature/shortLivedBranch", result.branchName());
        assertEquals("defaultBranchInfo", result.referenceBranchName());
        assertFalse(result.isPullRequest());

        verify(analysisWarnings).addUnique(eq("Property 'sonar.branch.target' is no longer supported"));
    }


    @Test
    public void testDefaultConfigWhenNoExistingBranchAndBranchParamsAllMaster() {
        AnalysisWarnings analysisWarnings = mock(AnalysisWarnings.class);
        System2 system2 = mock(System2.class);
        CommunityBranchConfigurationLoader testCase = new CommunityBranchConfigurationLoader(system2, analysisWarnings);
        ProjectBranches branchInfo = mock(ProjectBranches.class);
        when(branchInfo.isEmpty()).thenReturn(true);
        when(branchInfo.defaultBranchName()).thenReturn("xyz");

        Map<String, String> parameters = new HashMap<>();
        parameters.put("sonar.branch.name", "master");

        assertEquals(DefaultBranchConfiguration.class,
                     testCase.load(parameters, branchInfo, mock(ProjectPullRequests.class)).getClass());
    }

    @Test
    public void testExceptionWhenNoExistingBranchAndPullRequestAndBranchParametersPresent() {
        AnalysisWarnings analysisWarnings = mock(AnalysisWarnings.class);
        System2 system2 = mock(System2.class);
        CommunityBranchConfigurationLoader testCase = new CommunityBranchConfigurationLoader(system2, analysisWarnings);
        ProjectBranches branchInfo = mock(ProjectBranches.class);
        when(branchInfo.isEmpty()).thenReturn(true);


        Map<String, String> parameters = new HashMap<>();
        parameters.put("sonar.branch.name", "dummy");
        parameters.put("sonar.pullrequest.branch", "dummy2");


        expectedException.expect(MessageException.class);
        expectedException.expectMessage(IsEqual.equalTo(
                "No branches currently exist in this project. Please scan the main branch without passing any branch parameters."));

        testCase.load(parameters, branchInfo, mock(ProjectPullRequests.class));
    }

    @Test
    public void testDefaultBranchInfoWhenNoBranchParametersSpecifiedAndNoBranchesExist() {
        AnalysisWarnings analysisWarnings = mock(AnalysisWarnings.class);
        System2 system2 = mock(System2.class);
        CommunityBranchConfigurationLoader testCase = new CommunityBranchConfigurationLoader(system2, analysisWarnings);

        ProjectBranches branchInfo = mock(ProjectBranches.class);
        when(branchInfo.isEmpty()).thenReturn(true);

        Map<String, String> parameters = new HashMap<>();
        parameters.put("dummy", "dummy");


        assertEquals(DefaultBranchConfiguration.class,
                     testCase.load(parameters, branchInfo, mock(ProjectPullRequests.class)).getClass());
    }

    @Test
    public void testDefaultBranchInfoWhenNoParametersSpecified() {
        AnalysisWarnings analysisWarnings = mock(AnalysisWarnings.class);
        System2 system2 = mock(System2.class);
        CommunityBranchConfigurationLoader testCase = new CommunityBranchConfigurationLoader(system2, analysisWarnings);
        assertEquals(DefaultBranchConfiguration.class, testCase.load(new HashMap<>(), mock(ProjectBranches.class),
                                                                     mock(ProjectPullRequests.class)).getClass());
    }

    @Test
    public void testValidBranchInfoWhenAllBranchParametersSpecified() {
        AnalysisWarnings analysisWarnings = mock(AnalysisWarnings.class);
        System2 system2 = mock(System2.class);
        CommunityBranchConfigurationLoader testCase = new CommunityBranchConfigurationLoader(system2, analysisWarnings);
        Map<String, String> parameters = new HashMap<>();
        parameters.put("sonar.branch.name", "feature/shortLivedFeatureBranch");

        BranchInfo mockTargetBranchInfo = mock(BranchInfo.class);
        when(mockTargetBranchInfo.name()).thenReturn("masterBranchInfo");
        when(mockTargetBranchInfo.type()).thenReturn(BranchType.BRANCH);

        ProjectBranches projectBranches = mock(ProjectBranches.class);
        when(projectBranches.get("master")).thenReturn(mockTargetBranchInfo);
        when(projectBranches.defaultBranchName()).thenReturn("master");

        BranchConfiguration result = testCase.load(parameters, projectBranches, mock(ProjectPullRequests.class));

        assertNull(result.targetBranchName());
        assertEquals("feature/shortLivedFeatureBranch", result.branchName());
        assertEquals("masterBranchInfo", result.referenceBranchName());
        assertFalse(result.isPullRequest());

        expectedException
                .expectMessage(IsEqual.equalTo("Only a branch of type PULL_REQUEST can have a Pull Request key"));
        expectedException.expect(IllegalStateException.class);

        result.pullRequestKey();
    }

    @Test
    public void testValidBranchInfoWhenOnlySourceBranchSpecifiedAndMasterExists() {
        AnalysisWarnings analysisWarnings = mock(AnalysisWarnings.class);
        System2 system2 = mock(System2.class);
        CommunityBranchConfigurationLoader testCase = new CommunityBranchConfigurationLoader(system2, analysisWarnings);
        Map<String, String> parameters = new HashMap<>();
        parameters.put("sonar.branch.name", "feature/shortLivedBranch");

        BranchInfo mockTargetBranchInfo = mock(BranchInfo.class);
        when(mockTargetBranchInfo.name()).thenReturn("defaultBranchInfo");
        when(mockTargetBranchInfo.type()).thenReturn(BranchType.BRANCH);

        ProjectBranches projectBranches = mock(ProjectBranches.class);
        when(projectBranches.get("masterxxx")).thenReturn(mockTargetBranchInfo);
        when(projectBranches.defaultBranchName()).thenReturn("masterxxx");

        BranchConfiguration result = testCase.load(parameters, projectBranches, mock(ProjectPullRequests.class));

        assertNull(result.targetBranchName());
        assertEquals("feature/shortLivedBranch", result.branchName());
        assertEquals("defaultBranchInfo", result.referenceBranchName());
        assertFalse(result.isPullRequest());
    }

    @Test
    public void testExceptionWhenOnlySourceBranchSpecifiedAndNoMasterExists() {
        AnalysisWarnings analysisWarnings = mock(AnalysisWarnings.class);
        System2 system2 = mock(System2.class);
        CommunityBranchConfigurationLoader testCase = new CommunityBranchConfigurationLoader(system2, analysisWarnings);
        Map<String, String> parameters = new HashMap<>();
        parameters.put("sonar.branch.name", "feature/shortLivedBranch");

        BranchInfo mockTargetBranchInfo = mock(BranchInfo.class);
        when(mockTargetBranchInfo.name()).thenReturn("defaultBranchInfo");
        when(mockTargetBranchInfo.type()).thenReturn(BranchType.BRANCH);

        ProjectBranches projectBranches = mock(ProjectBranches.class);

        expectedException.expect(MessageException.class);
        expectedException.expectMessage(IsEqual.equalTo("Could not target requested branch"));

        testCase.load(parameters, projectBranches, mock(ProjectPullRequests.class));

    }

    @Test
    public void testExistingBranchOnlySourceParameters() {
        AnalysisWarnings analysisWarnings = mock(AnalysisWarnings.class);
        System2 system2 = mock(System2.class);
        CommunityBranchConfigurationLoader testCase = new CommunityBranchConfigurationLoader(system2, analysisWarnings);
        Map<String, String> parameters = new HashMap<>();
        parameters.put("sonar.branch.name", "longLivedBranch");

        BranchInfo mockTargetBranchInfo = mock(BranchInfo.class);
        when(mockTargetBranchInfo.name()).thenReturn("longLivedBranch");
        when(mockTargetBranchInfo.type()).thenReturn(BranchType.BRANCH);


        ProjectBranches projectBranches = mock(ProjectBranches.class);
        when(projectBranches.get("longLivedBranch")).thenReturn(mockTargetBranchInfo);

        BranchConfiguration result = testCase.load(parameters, projectBranches, mock(ProjectPullRequests.class));

        assertNull(result.targetBranchName());
        assertEquals("longLivedBranch", result.branchName());
        assertNull(result.referenceBranchName());
        assertFalse(result.isPullRequest());
    }

    @Test
    public void testPullRequestAllParameters() {
        AnalysisWarnings analysisWarnings = mock(AnalysisWarnings.class);
        System2 system2 = mock(System2.class);
        CommunityBranchConfigurationLoader testCase = new CommunityBranchConfigurationLoader(system2, analysisWarnings);
        Map<String, String> parameters = new HashMap<>();
        parameters.put("sonar.pullrequest.branch", "feature/sourceBranch");
        parameters.put("sonar.pullrequest.base", "target");
        parameters.put("sonar.pullrequest.key", "pr-key");

        BranchInfo mockTargetBranchInfo = mock(BranchInfo.class);
        when(mockTargetBranchInfo.name()).thenReturn("targetInfo");
        when(mockTargetBranchInfo.type()).thenReturn(BranchType.BRANCH);


        ProjectBranches projectBranches = mock(ProjectBranches.class);
        when(projectBranches.get("target")).thenReturn(mockTargetBranchInfo);

        BranchConfiguration result = testCase.load(parameters, projectBranches, mock(ProjectPullRequests.class));

        assertEquals("target", result.targetBranchName());
        assertEquals("feature/sourceBranch", result.branchName());
        assertEquals("target", result.referenceBranchName());
        assertTrue(result.isPullRequest());
        assertEquals("pr-key", result.pullRequestKey());
    }


    @Test
    public void testPullRequestMandatoryParameters() {
        AnalysisWarnings analysisWarnings = mock(AnalysisWarnings.class);
        System2 system2 = mock(System2.class);
        CommunityBranchConfigurationLoader testCase = new CommunityBranchConfigurationLoader(system2, analysisWarnings);
        Map<String, String> parameters = new HashMap<>();
        parameters.put("sonar.pullrequest.branch", "feature/sourceBranch");
        parameters.put("sonar.pullrequest.key", "pr-key");

        BranchInfo mockTargetBranchInfo = mock(BranchInfo.class);
        when(mockTargetBranchInfo.name()).thenReturn("masterInfo");
        when(mockTargetBranchInfo.type()).thenReturn(BranchType.BRANCH);


        ProjectBranches projectBranches = mock(ProjectBranches.class);
        when(projectBranches.get("master")).thenReturn(mockTargetBranchInfo);
        when(projectBranches.defaultBranchName()).thenReturn("master");

        BranchConfiguration result = testCase.load(parameters, projectBranches, mock(ProjectPullRequests.class));

        assertEquals("master", result.targetBranchName());
        assertEquals("feature/sourceBranch", result.branchName());
        assertEquals("master", result.referenceBranchName());
        assertTrue(result.isPullRequest());
    }

    @Test
    public void testPullRequestMandatoryParameters2() {
        AnalysisWarnings analysisWarnings = mock(AnalysisWarnings.class);
        System2 system2 = mock(System2.class);
        CommunityBranchConfigurationLoader testCase = new CommunityBranchConfigurationLoader(system2, analysisWarnings);
        Map<String, String> parameters = new HashMap<>();
        parameters.put("sonar.pullrequest.branch", "feature/sourceBranch");
        parameters.put("sonar.pullrequest.key", "pr-key");
        parameters.put("sonar.pullrequest.base", "");

        BranchInfo mockTargetBranchInfo = mock(BranchInfo.class);
        when(mockTargetBranchInfo.name()).thenReturn("masterInfo");
        when(mockTargetBranchInfo.type()).thenReturn(BranchType.BRANCH);


        ProjectBranches projectBranches = mock(ProjectBranches.class);
        when(projectBranches.get("master")).thenReturn(mockTargetBranchInfo);
        when(projectBranches.defaultBranchName()).thenReturn("master");

        BranchConfiguration result = testCase.load(parameters, projectBranches, mock(ProjectPullRequests.class));

        assertEquals("master", result.targetBranchName());
        assertEquals("feature/sourceBranch", result.branchName());
        assertEquals("master", result.referenceBranchName());
        assertTrue(result.isPullRequest());
    }


    @Test
    public void testPullRequestNoSuchTarget() {
        AnalysisWarnings analysisWarnings = mock(AnalysisWarnings.class);
        System2 system2 = mock(System2.class);
        CommunityBranchConfigurationLoader testCase = new CommunityBranchConfigurationLoader(system2, analysisWarnings);
        Map<String, String> parameters = new HashMap<>();
        parameters.put("sonar.pullrequest.branch", "feature/sourceBranch");
        parameters.put("sonar.pullrequest.base", "missingTarget");
        parameters.put("sonar.pullrequest.key", "pr-key");


        ProjectBranches projectBranches = mock(ProjectBranches.class);

        expectedException.expect(MessageException.class);
        expectedException.expectMessage(IsEqual.equalTo("Could not target requested branch"));
        expectedException.expectCause(new CustomMatcher<Throwable>("Cause checker") {
            @Override
            public boolean matches(Object item) {
                return item instanceof IllegalStateException && ((IllegalStateException) item).getMessage()
                        .equals("Target branch 'missingTarget' does not exist");
            }
        });

        testCase.load(parameters, projectBranches, mock(ProjectPullRequests.class));
    }

}
