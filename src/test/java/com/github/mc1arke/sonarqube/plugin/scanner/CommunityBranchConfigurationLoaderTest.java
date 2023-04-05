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
package com.github.mc1arke.sonarqube.plugin.scanner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.sonar.api.utils.System2;
import org.sonar.scanner.scan.branch.BranchConfiguration;
import org.sonar.scanner.scan.branch.BranchConfigurationLoader;
import org.sonar.scanner.scan.branch.DefaultBranchConfiguration;
import org.sonar.scanner.scan.branch.ProjectBranches;

/**
 * @author Michael Clarke
 */
class CommunityBranchConfigurationLoaderTest {

    private final System2 system2 = mock(System2.class);
    private final BranchConfigurationFactory branchConfigurationFactory = mock(BranchConfigurationFactory.class);
    private final BranchAutoConfigurer branchAutoConfigurer = mock(BranchAutoConfigurer.class);
    private final BranchConfigurationLoader testCase = new CommunityBranchConfigurationLoader(system2, branchConfigurationFactory, List.of(branchAutoConfigurer));

    @Test
    void shouldReturnResultFromAutoConfigurerIfPresentAndNoParametersSpecified() {
        BranchConfiguration branchConfiguration = mock(BranchConfiguration.class);
        when(branchAutoConfigurer.detectConfiguration(any(), any())).thenReturn(Optional.of(branchConfiguration));

        ProjectBranches projectBranches = mock(ProjectBranches.class);

        BranchConfiguration actual = testCase.load(Map.of(), projectBranches);

        assertThat(actual).isSameAs(branchConfiguration);
        verify(branchAutoConfigurer).detectConfiguration(system2, projectBranches);
        verifyNoInteractions(branchConfigurationFactory);
    }

    @Test
    void shouldReturnDefaultBranchIfAutoConfigurerNoResultAndNoParametersSpecified() {
        when(branchAutoConfigurer.detectConfiguration(any(), any())).thenReturn(Optional.empty());

        ProjectBranches projectBranches = mock(ProjectBranches.class);

        BranchConfiguration actual = testCase.load(Map.of(), projectBranches);

        assertThat(actual).usingRecursiveComparison().isEqualTo(new DefaultBranchConfiguration());
        verify(branchAutoConfigurer).detectConfiguration(system2, projectBranches);
        verifyNoInteractions(branchConfigurationFactory);
    }

    @Test
    void shouldCreateBranchConfigurationIfAnyBranchPropertiesSet() {
        ProjectBranches projectBranches = mock(ProjectBranches.class);
        BranchConfiguration branchConfiguration = mock(BranchConfiguration.class);
        when(branchConfigurationFactory.createBranchConfiguration(any(), any())).thenReturn(branchConfiguration);

        BranchConfiguration actual = testCase.load(Map.of("sonar.branch.name", "branch", "sonar.branch.target", "target"), projectBranches);

        assertThat(actual).isSameAs(branchConfiguration);
        verify(branchConfigurationFactory).createBranchConfiguration("branch", projectBranches);
        verifyNoInteractions(branchAutoConfigurer);
    }

    @Test
    void shouldCreatePullConfigurationIfAnyPullRequestPropertiesSet() {
        ProjectBranches projectBranches = mock(ProjectBranches.class);
        BranchConfiguration branchConfiguration = mock(BranchConfiguration.class);
        when(branchConfigurationFactory.createPullRequestConfiguration(any(), any(), any(), any())).thenReturn(branchConfiguration);

        BranchConfiguration actual = testCase.load(Map.of("sonar.pullrequest.key", "key", "sonar.pullrequest.branch", "source", "sonar.pullrequest.base", "target"), projectBranches);

        assertThat(actual).isSameAs(branchConfiguration);
        verify(branchConfigurationFactory).createPullRequestConfiguration("key", "source", "target", projectBranches);
        verifyNoInteractions(branchAutoConfigurer);
    }

    @Test
    void shouldThrowErrorIfBothBranchAndPullRequestParametersPresent() {
        assertThatThrownBy(() -> testCase.load(Map.of("sonar.pullrequest.key", "key", "sonar.pullrequest.branch", "source", "sonar.branch.name", "branch"), mock(ProjectBranches.class))).hasMessage("sonar.pullrequest and sonar.branch parameters should not be specified in the same scan");
    }

    @Test
    void shouldThrowErrorIfPullRequestAnalysisWithoutPullRequestKey() {
        assertThatThrownBy(() -> testCase.load(Map.of("sonar.pullrequest.base", "target"), mock(ProjectBranches.class))).hasMessage("sonar.pullrequest.key is required for a pull request analysis");
    }

    @Test
    void shouldThrowErrorIfPullRequestAnalysisWithoutPullRequestBranch() {
        assertThatThrownBy(() -> testCase.load(Map.of("sonar.pullrequest.key", "key"), mock(ProjectBranches.class))).hasMessage("sonar.pullrequest.branch is required for a pull request analysis");
    }

}
