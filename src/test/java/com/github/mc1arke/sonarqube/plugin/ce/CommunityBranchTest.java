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
package com.github.mc1arke.sonarqube.plugin.ce;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.sonar.db.component.BranchType;

/**
 * @author Michael Clarke
 */
class CommunityBranchTest {

    @Test
    void testGenerateKeyMainBranchNullFileOfPath() {
        CommunityBranch testCase = new CommunityBranch("name", BranchType.PULL_REQUEST, true, null, null, null);

        assertThat(testCase.generateKey("projectKey", null)).isEqualTo("projectKey");
    }

    @Test
    void testGenerateKeyMainBranchNonNullFileOfPathHolder() {
        CommunityBranch testCase = new CommunityBranch("name", BranchType.PULL_REQUEST, true, null, null, null);

        assertThat(testCase.generateKey("projectKey", "")).isEqualTo("projectKey");
    }

    @Test
    void testGenerateKeyMainBranchNonNullFileOfPathContent() {
        CommunityBranch testCase = new CommunityBranch("name", BranchType.PULL_REQUEST, true, null, null, null);

        assertThat(testCase.generateKey("projectKey", "path")).isEqualTo("projectKey:path");
    }

    @Test
    void testGetPulRequestKey() {
        assertThat(new CommunityBranch("name", BranchType.PULL_REQUEST, false, null, "prKey", null)
                .getPullRequestKey()).isEqualTo("prKey");
    }

    @Test
    void testGetPulRequestKeyNonPullRequest() {
        CommunityBranch underTest = new CommunityBranch("name", BranchType.BRANCH, false, null, "prKey", null);
        assertThatThrownBy(underTest::getPullRequestKey)
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Only a branch of type PULL_REQUEST can have a pull request ID");
    }

}
