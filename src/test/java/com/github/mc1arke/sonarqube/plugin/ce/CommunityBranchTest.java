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
package com.github.mc1arke.sonarqube.plugin.ce;

import org.hamcrest.core.IsEqual;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.db.component.BranchType;

import static org.junit.Assert.assertEquals;

/**
 * @author Michael Clarke
 */
public class CommunityBranchTest {

    private final ExpectedException expectedException = ExpectedException.none();

    @Rule
    public ExpectedException expectedException() {
        return expectedException;
    }

    @Test
    public void testGenerateKeyMainBranchNullFileOfPath() {
        CommunityBranch testCase = new CommunityBranch("name", BranchType.PULL_REQUEST, true, null, null, null);

        assertEquals("projectKey", testCase.generateKey("projectKey", null));
    }

    @Test
    public void testGenerateKeyMainBranchNonNullFileOfPathHolder() {
        CommunityBranch testCase = new CommunityBranch("name", BranchType.PULL_REQUEST, true, null, null, null);

        assertEquals("projectKey", testCase.generateKey("projectKey", ""));
    }

    @Test
    public void testGenerateKeyMainBranchNonNullFileOfPathContent() {
        CommunityBranch testCase = new CommunityBranch("name", BranchType.PULL_REQUEST, true, null, null, null);

        assertEquals("projectKey:path", testCase.generateKey("projectKey", "path"));
    }

    @Test
    public void testGenerateKeyNonMainBranchNonNullFileOfPathContentPullRequest() {
        CommunityBranch testCase =
                new CommunityBranch("name", BranchType.PULL_REQUEST, false, null, "pullRequestKey", null);

        assertEquals("projectKey:path:PULL_REQUEST:pullRequestKey", testCase.generateKey("projectKey", "path"));
    }

    @Test
    public void testGenerateKeyNonMainBranchNonNullFileOfPathContentBranch() {
        CommunityBranch testCase = new CommunityBranch("name", BranchType.BRANCH, false, null, null, null);

        assertEquals("projectKey:path:BRANCH:name", testCase.generateKey("projectKey", "path"));
    }


    @Test
    public void testGetPulRequestKey() {
        assertEquals("prKey", new CommunityBranch("name", BranchType.PULL_REQUEST, false, null, "prKey", null)
                .getPullRequestKey());
    }

    @Test
    public void testGetPulRequestKeyNonPullRequest() {
        expectedException
                .expectMessage(IsEqual.equalTo("Only a branch of type PULL_REQUEST can have a pull request ID"));
        expectedException.expect(IllegalStateException.class);

        new CommunityBranch("name", BranchType.BRANCH, false, null, "prKey", null).getPullRequestKey();
    }

}
