/*
 * Copyright (C) 2022-2024 Michael Clarke
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

import org.junit.jupiter.api.Test;
import org.sonar.api.utils.MessageException;
import org.sonar.scanner.scan.branch.BranchConfiguration;
import org.sonar.scanner.scan.branch.BranchInfo;
import org.sonar.scanner.scan.branch.BranchType;
import org.sonar.scanner.scan.branch.ProjectBranches;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BranchConfigurationFactoryTest {

    @Test
    void shouldReturnBranchWithNoTargetIfNoProjectBranchesExist() {
        ProjectBranches projectBranches = mock(ProjectBranches.class);
        when(projectBranches.isEmpty()).thenReturn(true);

        BranchConfigurationFactory underTest = new BranchConfigurationFactory();
        BranchConfiguration actual = underTest.createBranchConfiguration("branch", projectBranches);

        assertThat(actual).usingRecursiveComparison().isEqualTo(new CommunityBranchConfiguration("branch", BranchType.BRANCH, null, null, null));
    }

    @Test
    void shouldReturnBranchWithDefaultReferenceIfSpecifiedBranchDoesNotExist() {
        ProjectBranches projectBranches = mock(ProjectBranches.class);
        when(projectBranches.isEmpty()).thenReturn(false);
        when(projectBranches.defaultBranchName()).thenReturn("default");
        when(projectBranches.get(any())).thenReturn(null);

        BranchConfigurationFactory underTest = new BranchConfigurationFactory();
        BranchConfiguration actual = underTest.createBranchConfiguration("branch", projectBranches);

        assertThat(actual).usingRecursiveComparison().isEqualTo(new CommunityBranchConfiguration("branch", BranchType.BRANCH, "default", null, null));
    }

    @Test
    void shouldReturnBranchWithSelfReferenceIfSpecifiedBranchDoesExist() {
        ProjectBranches projectBranches = mock(ProjectBranches.class);
        when(projectBranches.isEmpty()).thenReturn(false);
        when(projectBranches.defaultBranchName()).thenReturn("default");
        when(projectBranches.get(any())).thenReturn(mock(BranchInfo.class));

        BranchConfigurationFactory underTest = new BranchConfigurationFactory();
        BranchConfiguration actual = underTest.createBranchConfiguration("branch", projectBranches);

        assertThat(actual).usingRecursiveComparison().isEqualTo(new CommunityBranchConfiguration("branch", BranchType.BRANCH, "branch", null, null));
    }

    @Test
    void shouldThrowErrorIfAttemptingToCreatePullRequestWithNoTargetIfNoProjectBranchesExist() {
        ProjectBranches projectBranches = mock(ProjectBranches.class);
        when(projectBranches.isEmpty()).thenReturn(true);
        when(projectBranches.defaultBranchName()).thenReturn("default-branch-name");

        BranchConfigurationFactory underTest = new BranchConfigurationFactory();
        assertThatThrownBy(() -> underTest.createPullRequestConfiguration("key", "source", null, projectBranches))
                .usingRecursiveComparison()
                .isEqualTo(MessageException.of("No branch exists in Sonarqube with the name default-branch-name"));
    }

    @Test
    void shouldThrowErrorIfAttemptingToCreatePullRequestWithTargetIfNoProjectBranchesExist() {
        ProjectBranches projectBranches = mock(ProjectBranches.class);
        when(projectBranches.isEmpty()).thenReturn(true);

        BranchConfigurationFactory underTest = new BranchConfigurationFactory();
        assertThatThrownBy(() -> underTest.createPullRequestConfiguration("key", "source", "target", projectBranches))
                .usingRecursiveComparison()
                .isEqualTo(MessageException.of("No branch exists in Sonarqube with the name target"));
    }

    @Test
    void shouldThrowErrorIfAttemptingToCreatePullRequestWithTargetBranchThatDoesNotExist() {
        ProjectBranches projectBranches = mock(ProjectBranches.class);
        when(projectBranches.isEmpty()).thenReturn(false);

        BranchConfigurationFactory underTest = new BranchConfigurationFactory();
        assertThatThrownBy(() -> underTest.createPullRequestConfiguration("key", "source", "target-branch", projectBranches))
                .usingRecursiveComparison()
                .isEqualTo(MessageException.of("No branch exists in Sonarqube with the name target-branch"));
    }

    @Test
    void shouldReturnPullRequestWithTargetOfDefaultBranchIfTargetNotSpecifiedAndDefaultExists() {
        ProjectBranches projectBranches = mock(ProjectBranches.class);
        when(projectBranches.isEmpty()).thenReturn(false);
        when(projectBranches.defaultBranchName()).thenReturn("defaultBranch");
        BranchInfo branchInfo = new BranchInfo("defaultBranch", BranchType.BRANCH, true, null);
        when(projectBranches.get("defaultBranch")).thenReturn(branchInfo);

        BranchConfigurationFactory underTest = new BranchConfigurationFactory();
        BranchConfiguration actual = underTest.createPullRequestConfiguration("key", "source", null, projectBranches);

        assertThat(actual).usingRecursiveComparison().isEqualTo(new CommunityBranchConfiguration("source", BranchType.PULL_REQUEST, "defaultBranch", "defaultBranch", "key"));
    }

    @Test
    void shouldReturnPullRequestWithTargetOfTargetAsReferenceIfTargetBranchExists() {
        ProjectBranches projectBranches = mock(ProjectBranches.class);
        when(projectBranches.isEmpty()).thenReturn(false);
        BranchInfo branchInfo = new BranchInfo("target", BranchType.PULL_REQUEST, false, "target2");
        when(projectBranches.get("target")).thenReturn(branchInfo);
        BranchInfo branchInfo2 = new BranchInfo("target2", BranchType.BRANCH, false, "target3");
        when(projectBranches.get("target2")).thenReturn(branchInfo2);

        BranchConfigurationFactory underTest = new BranchConfigurationFactory();
        BranchConfiguration actual = underTest.createPullRequestConfiguration("key", "source", "target", projectBranches);

        assertThat(actual).usingRecursiveComparison().isEqualTo(new CommunityBranchConfiguration("source", BranchType.PULL_REQUEST, "target2", "target", "key"));
    }

    @Test
    void shouldThrowExceptionIfPullRequestTargetsOtherPullRequestWithoutATarget() {
        ProjectBranches projectBranches = mock(ProjectBranches.class);
        when(projectBranches.isEmpty()).thenReturn(false);
        BranchInfo branchInfo = new BranchInfo("target", BranchType.PULL_REQUEST, false, null);
        when(projectBranches.get("target")).thenReturn(branchInfo);

        BranchConfigurationFactory underTest = new BranchConfigurationFactory();
        assertThatThrownBy(() -> underTest.createPullRequestConfiguration("key", "source", "target", projectBranches)).hasMessage("The branch 'target' of type PULL_REQUEST does not have a target");
    }

}