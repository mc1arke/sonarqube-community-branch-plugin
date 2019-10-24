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
package com.github.mc1arke.sonarqube.plugin.scanner;

import org.junit.Test;
import org.sonar.scanner.bootstrap.GlobalConfiguration;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Michael Clarke
 */
public class CommunityBranchParamsValidatorTest {

    @Test
    public void testNoMessagesOnNoParams() {
        List<String> messages = new ArrayList<>();

        GlobalConfiguration globalConfiguration = mock(GlobalConfiguration.class);
        new CommunityBranchParamsValidator(globalConfiguration).validate(messages, null);
        assertTrue(messages.isEmpty());
    }

    @Test
    public void testNoMessagesOnlyLegacyBranch() {
        List<String> messages = new ArrayList<>();

        GlobalConfiguration globalConfiguration = mock(GlobalConfiguration.class);
        new CommunityBranchParamsValidator(globalConfiguration).validate(messages, "legacy");
        assertTrue(messages.isEmpty());
    }

    @Test
    public void testMessagesBranchParamsAndLegacyBranch() {
        List<String> messages = new ArrayList<>();

        GlobalConfiguration globalConfiguration = mock(GlobalConfiguration.class);
        when(globalConfiguration.hasKey(any())).thenReturn(true);
        new CommunityBranchParamsValidator(globalConfiguration).validate(messages, "legacy");
        assertEquals(1, messages.size());
        assertEquals(
                "The legacy 'sonar.branch' parameter cannot be used at the same time as 'sonar.branch.name' or 'sonar.branch.target'",
                messages.get(0));
    }

    @Test
    public void testMessagesBranchParamsAndLegacyBranch2() {
        List<String> messages = new ArrayList<>();

        GlobalConfiguration globalConfiguration = mock(GlobalConfiguration.class);
        when(globalConfiguration.hasKey(eq("sonar.branch.target"))).thenReturn(true);
        when(globalConfiguration.hasKey(eq("sonar.branch"))).thenReturn(true);
        new CommunityBranchParamsValidator(globalConfiguration).validate(messages, "legacy");
        assertEquals(1, messages.size());
        assertEquals(
                "The legacy 'sonar.branch' parameter cannot be used at the same time as 'sonar.branch.name' or 'sonar.branch.target'",
                messages.get(0));
    }

    @Test
    public void testNoMessagesOnValidate() {
        List<String> messages = new ArrayList<>();

        GlobalConfiguration globalConfiguration = mock(GlobalConfiguration.class);
        when(globalConfiguration.hasKey(eq("sonar.branch.target"))).thenReturn(true);
        when(globalConfiguration.hasKey(eq("sonar.branch"))).thenReturn(true);
        new CommunityBranchParamsValidator(globalConfiguration).validate(messages);
        assertEquals(0, messages.size());
    }
}
