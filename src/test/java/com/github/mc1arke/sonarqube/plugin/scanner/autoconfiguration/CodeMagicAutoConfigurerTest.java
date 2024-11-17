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
package com.github.mc1arke.sonarqube.plugin.scanner.autoconfiguration;

import com.github.mc1arke.sonarqube.plugin.scanner.BranchConfigurationFactory;
import org.junit.jupiter.api.Test;
import org.sonar.api.utils.System2;
import org.sonar.scanner.scan.branch.BranchConfiguration;
import org.sonar.scanner.scan.branch.ProjectBranches;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CodeMagicAutoConfigurerTest {

    @Test
    void shouldReturnOptionalEmptyIfNotCi() {
        System2 system2 = mock();
        BranchConfigurationFactory branchConfigurationFactory = mock();
        ProjectBranches projectBranches = mock();

        CodeMagicAutoConfigurer underTest = new CodeMagicAutoConfigurer(branchConfigurationFactory);
        assertThat(underTest.detectConfiguration(system2, projectBranches)).isEmpty();
    }

    @Test
    void shouldReturnOptionalEmptyIfCiWithNoFciBranchProperty() {
        System2 system2 = mock();
        when(system2.envVariable("CI")).thenReturn("true");
        BranchConfigurationFactory branchConfigurationFactory = mock();
        ProjectBranches projectBranches = mock();

        CodeMagicAutoConfigurer underTest = new CodeMagicAutoConfigurer(branchConfigurationFactory);
        assertThat(underTest.detectConfiguration(system2, projectBranches)).isEmpty();
    }

    @Test
    void shouldReturnBranchConfigurationBasedOnNoPrIdInEnvironmentParameters() {
        System2 system2 = mock();
        when(system2.envVariable("CI")).thenReturn("true");
        when(system2.envVariable("FCI_BRANCH")).thenReturn("branch");
        BranchConfigurationFactory branchConfigurationFactory = mock();
        BranchConfiguration branchConfiguration = mock();
        when(branchConfigurationFactory.createBranchConfiguration(any(), any())).thenReturn(branchConfiguration);
        ProjectBranches projectBranches = mock();

        CodeMagicAutoConfigurer underTest = new CodeMagicAutoConfigurer(branchConfigurationFactory);
        assertThat(underTest.detectConfiguration(system2, projectBranches)).contains(branchConfiguration);
        verify(branchConfigurationFactory).createBranchConfiguration("branch", projectBranches);
    }

    @Test
    void shouldReturnPullRequestConfigurationBasedOnPrIdInEnvironmentParameters() {
        System2 system2 = mock();
        when(system2.envVariable("CI")).thenReturn("true");
        when(system2.envVariable("FCI_BRANCH")).thenReturn("source");
        when(system2.envVariable("FCI_PULL_REQUEST")).thenReturn("true");
        when(system2.envVariable("FCI_PULL_REQUEST_NUMBER")).thenReturn("id");
        when(system2.envVariable("FCI_PULL_REQUEST_DEST")).thenReturn("target");
        BranchConfigurationFactory branchConfigurationFactory = mock();
        BranchConfiguration branchConfiguration = mock();
        when(branchConfigurationFactory.createPullRequestConfiguration(any(), any(), any(), any())).thenReturn(branchConfiguration);
        ProjectBranches projectBranches = mock();

        CodeMagicAutoConfigurer underTest = new CodeMagicAutoConfigurer(branchConfigurationFactory);
        assertThat(underTest.detectConfiguration(system2, projectBranches)).contains(branchConfiguration);
        verify(branchConfigurationFactory).createPullRequestConfiguration("id", "source", "target", projectBranches);
    }
}