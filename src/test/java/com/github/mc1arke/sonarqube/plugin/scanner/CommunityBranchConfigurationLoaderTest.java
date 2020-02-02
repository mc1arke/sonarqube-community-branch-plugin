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

import org.hamcrest.core.IsEqual;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.notifications.AnalysisWarnings;
import org.sonar.scanner.scan.branch.BranchConfiguration;
import org.sonar.scanner.scan.branch.BranchConfigurationLoader;
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

    private final AnalysisWarnings analysisWarnings = mock(AnalysisWarnings.class);
    private final BranchConfigurationLoader testCase = new CommunityBranchConfigurationLoader(analysisWarnings);

    @Rule
    public ExpectedException expectedException() {
        return expectedException;
    }

    @Test
    public void testExceptionWhenNoExistingBranchAndBranchParamsPresent() {
        ProjectBranches branchInfo = mock(ProjectBranches.class);
        when(branchInfo.isEmpty()).thenReturn(true);

        Map<String, String> parameters = new HashMap<>();
        parameters.put("sonar.branch.name", "dummy");

        BranchConfiguration branchConfiguration = testCase.load(parameters, branchInfo, mock(ProjectPullRequests.class));

        assertEquals("dummy", branchConfiguration.branchName());
        assertNull(branchConfiguration.referenceBranchName());
        assertEquals(BranchType.BRANCH, branchConfiguration.branchType());
    }

    @Test
    public void testDefaultConfigWhenNoExistingBranchAndBranchNameParamMaster() {
        ProjectBranches branchInfo = mock(ProjectBranches.class);
        when(branchInfo.isEmpty()).thenReturn(true);

        Map<String, String> parameters = new HashMap<>();
        parameters.put("sonar.branch.name", "master");

        BranchConfiguration branchConfiguration = testCase.load(parameters, mock(ProjectBranches.class), mock(ProjectPullRequests.class));
        assertEquals("master", branchConfiguration.branchName());
        assertNull(branchConfiguration.targetBranchName());
        assertNull(branchConfiguration.referenceBranchName());
        assertEquals(BranchType.BRANCH, branchConfiguration.branchType());    }

    @Test
    public void testErrorWhenNoExistingBranchAndBranchTargetMasterButNoSourceBranch() {
        ProjectBranches branchInfo = mock(ProjectBranches.class);
        when(branchInfo.isEmpty()).thenReturn(true);

        Map<String, String> parameters = new HashMap<>();
        parameters.put("sonar.branch.name", "dummy");

        BranchConfiguration branchConfiguration = testCase.load(parameters, branchInfo, mock(ProjectPullRequests.class));

        assertEquals("dummy", branchConfiguration.branchName());
        assertNull(branchConfiguration.referenceBranchName());
        assertNull(branchConfiguration.targetBranchName());
        assertEquals(BranchType.BRANCH, branchConfiguration.branchType());
    }

    @Test
    public void testWarningWhenTargetBranchParameterSpecified() {
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
        assertEquals("masterxxx", result.referenceBranchName());
        assertFalse(result.isPullRequest());

        verify(analysisWarnings).addUnique(eq("Property 'sonar.branch.target' is no longer supported"));
    }


    @Test
    public void testDefaultConfigWhenNoExistingBranchAndBranchParamsAllMaster() {
        ProjectBranches branchInfo = mock(ProjectBranches.class);
        when(branchInfo.isEmpty()).thenReturn(true);

        Map<String, String> parameters = new HashMap<>();
        parameters.put("sonar.branch.name", "master");

        BranchConfiguration branchConfiguration = testCase.load(parameters, branchInfo, mock(ProjectPullRequests.class));

        assertEquals("master", branchConfiguration.branchName());
        assertEquals(BranchType.BRANCH, branchConfiguration.branchType());
        assertNull(branchConfiguration.referenceBranchName());
        assertNull(branchConfiguration.targetBranchName());
    }

    @Test
    public void testDefaultBranchInfoWhenNoBranchParametersSpecifiedAndNoBranchesExist() {
        ProjectBranches branchInfo = mock(ProjectBranches.class);
        when(branchInfo.isEmpty()).thenReturn(true);

        Map<String, String> parameters = new HashMap<>();
        parameters.put("dummy", "dummy");


        assertEquals(DefaultBranchConfiguration.class,
                     testCase.load(parameters, branchInfo, mock(ProjectPullRequests.class)).getClass());
    }

    @Test
    public void testDefaultBranchInfoWhenNoParametersSpecified() {
        assertEquals(DefaultBranchConfiguration.class, testCase.load(new HashMap<>(), mock(ProjectBranches.class),
                                                                     mock(ProjectPullRequests.class)).getClass());
    }

    @Test
    public void testValidBranchInfoWhenAllBranchParametersSpecified() {
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
        assertEquals("master", result.referenceBranchName());
        assertFalse(result.isPullRequest());

        expectedException
                .expectMessage(IsEqual.equalTo("Only a branch of type PULL_REQUEST can have a Pull Request key"));
        expectedException.expect(IllegalStateException.class);

        result.pullRequestKey();
    }

    @Test
    public void testValidBranchInfoWhenOnlySourceBranchSpecifiedAndMasterExists() {
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
        assertEquals("masterxxx", result.referenceBranchName());
        assertFalse(result.isPullRequest());
    }

    @Test
    public void testExceptionWhenOnlySourceBranchSpecifiedAndNoMasterExists() {
        Map<String, String> parameters = new HashMap<>();
        parameters.put("sonar.branch.name", "feature/shortLivedBranch");

        BranchInfo mockTargetBranchInfo = mock(BranchInfo.class);
        when(mockTargetBranchInfo.name()).thenReturn("defaultBranchInfo");
        when(mockTargetBranchInfo.type()).thenReturn(BranchType.BRANCH);

        ProjectBranches projectBranches = mock(ProjectBranches.class);

        BranchConfiguration branchConfiguration = testCase.load(parameters, projectBranches, mock(ProjectPullRequests.class));

        assertEquals("feature/shortLivedBranch", branchConfiguration.branchName());
        assertNull(branchConfiguration.referenceBranchName());
        assertNull(branchConfiguration.targetBranchName());
        assertEquals(BranchType.BRANCH, branchConfiguration.branchType());
    }

    @Test
    public void testExistingBranchOnlySourceParameters() {
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
        assertEquals("longLivedBranch", result.referenceBranchName());
        assertFalse(result.isPullRequest());
    }

    @Test
    public void testPullRequestAllParameters() {
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
        Map<String, String> parameters = new HashMap<>();
        parameters.put("sonar.pullrequest.branch", "feature/sourceBranch");
        parameters.put("sonar.pullrequest.base", "missingTarget");
        parameters.put("sonar.pullrequest.key", "pr-key");


        ProjectBranches projectBranches = mock(ProjectBranches.class);

        BranchConfiguration branchConfiguration = testCase.load(parameters, projectBranches, mock(ProjectPullRequests.class));
        assertEquals("feature/sourceBranch", branchConfiguration.branchName());
        assertEquals("missingTarget", branchConfiguration.targetBranchName());
        assertNull(branchConfiguration.referenceBranchName());
        assertEquals(BranchType.PULL_REQUEST, branchConfiguration.branchType());
    }

}
