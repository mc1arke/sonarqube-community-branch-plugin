/*
 * Copyright (C) 2019-2022 Michael Clarke
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
package com.github.mc1arke.sonarqube.plugin.ce.pullrequest;

import org.junit.Test;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rules.RuleType;
import org.sonar.ce.task.projectanalysis.component.Component;
import org.sonar.ce.task.projectanalysis.component.ReportAttributes;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.db.protobuf.DbIssues;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class PostAnalysisIssueVisitorTest {

    private static final long EXAMPLE_ISSUE_EFFORT_IN_MINUTES = 15L;
    private static final String EXAMPLE_ISSUE_KEY = "key";
    private static final int EXAMPLE_ISSUE_LINE = 1000;
    private static final String EXAMPLE_ISSUE_MESSAGE = "message";
    private static final String EXAMPLE_ISSUE_RESOLUTION = "resolution";
    private static final String EXAMPLE_ISSUE_SEVERITY = "severity";
    private static final String EXAMPLE_ISSUE_STATUS = "status";
    private static final RuleType EXAMPLE_ISSUE_TYPE = RuleType.BUG;
    private static final RuleKey EXAMPLE_ISSUE_RULEKEY = RuleKey.of("repo", "rule");
    private static final DbIssues.Locations EXAMPLE_ISSUE_LOCATIONS = DbIssues.Locations.getDefaultInstance();

    @Test
    public void checkAllIssuesCollected() {
        PostAnalysisIssueVisitor testCase = new PostAnalysisIssueVisitor();

        List<PostAnalysisIssueVisitor.ComponentIssue> expected = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            DefaultIssue issue = mock(DefaultIssue.class);
            Component component = mock(Component.class);
            expected.add(new PostAnalysisIssueVisitor.ComponentIssue(component, new PostAnalysisIssueVisitor.LightIssue(issue)));

            testCase.onIssue(component, issue);
        }


        assertThat(testCase.getIssues().size()).isEqualTo(expected.size());
        for (int i = 0; i < expected.size(); i++) {
            assertThat(testCase.getIssues().get(i).getIssue()).isEqualTo(expected.get(i).getIssue());
            assertThat(testCase.getIssues().get(i).getComponent()).isEqualTo(expected.get(i).getComponent());
        }
    }

    private DefaultIssue exampleDefaultIssue() {
        DefaultIssue defaultIssue = mock(DefaultIssue.class);
        doReturn(EXAMPLE_ISSUE_EFFORT_IN_MINUTES).when(defaultIssue).effortInMinutes();
        doReturn(EXAMPLE_ISSUE_KEY).when(defaultIssue).key();
        doReturn(EXAMPLE_ISSUE_LINE).when(defaultIssue).getLine();
        doReturn(EXAMPLE_ISSUE_MESSAGE).when(defaultIssue).getMessage();
        doReturn(EXAMPLE_ISSUE_RESOLUTION).when(defaultIssue).resolution();
        doReturn(EXAMPLE_ISSUE_SEVERITY).when(defaultIssue).severity();
        doReturn(EXAMPLE_ISSUE_STATUS).when(defaultIssue).status();
        doReturn(EXAMPLE_ISSUE_TYPE).when(defaultIssue).type();
        doReturn(EXAMPLE_ISSUE_RULEKEY).when(defaultIssue).getRuleKey();
        doReturn(EXAMPLE_ISSUE_LOCATIONS).when(defaultIssue).getLocations();
        return defaultIssue;
    }

    @Test
    public void testLightIssueMapping() {
        // mock a DefaultIssue
        DefaultIssue defaultIssue = exampleDefaultIssue();
        Component component = mock(Component.class);

        // map the DefaultIssue into a LightIssue (using PostAnalysisIssueVisitor to workaround private constructor)
        PostAnalysisIssueVisitor visitor = new PostAnalysisIssueVisitor();
        visitor.onIssue(component, defaultIssue);
        PostAnalysisIssueVisitor.LightIssue lightIssue = visitor.getIssues().get(0).getIssue();

        // check values equality, twice (see below)
        for (int i = 0; i < 2; i++) {
            assertThat(lightIssue.effortInMinutes()).isEqualTo(EXAMPLE_ISSUE_EFFORT_IN_MINUTES);
            assertThat(lightIssue.key()).isEqualTo(EXAMPLE_ISSUE_KEY);
            assertThat(lightIssue.getLine()).isEqualTo(EXAMPLE_ISSUE_LINE);
            assertThat(lightIssue.getMessage()).isEqualTo(EXAMPLE_ISSUE_MESSAGE);
            assertThat(lightIssue.resolution()).isEqualTo(EXAMPLE_ISSUE_RESOLUTION);
            assertThat(lightIssue.severity()).isEqualTo(EXAMPLE_ISSUE_SEVERITY);
            assertThat(lightIssue.status()).isEqualTo(EXAMPLE_ISSUE_STATUS);
            assertThat(lightIssue.getStatus()).isEqualTo(EXAMPLE_ISSUE_STATUS); // alias getter
            assertThat(lightIssue.type()).isEqualTo(EXAMPLE_ISSUE_TYPE);
            assertThat(lightIssue.getRuleKey()).isEqualTo(EXAMPLE_ISSUE_RULEKEY);
            assertThat(lightIssue.getLocations()).isEqualTo(EXAMPLE_ISSUE_LOCATIONS);
        }

        // check DefaultIssue getters have been called _exactly once_
        verify(defaultIssue).effortInMinutes();
        verify(defaultIssue).key();
        verify(defaultIssue).getLine();
        verify(defaultIssue).getMessage();
        verify(defaultIssue).resolution();
        verify(defaultIssue).severity();
        verify(defaultIssue).status();
        verify(defaultIssue).type();
        verify(defaultIssue).getRuleKey();
        verify(defaultIssue).getLocations();
        verifyNoMoreInteractions(defaultIssue);
    }

    @Test
    public void testEqualLightIssues() {
        DefaultIssue defaultIssue = exampleDefaultIssue();
        Component component = mock(Component.class);

        // map the DefaultIssue into two equal LightIssues
        PostAnalysisIssueVisitor visitor = new PostAnalysisIssueVisitor();
        visitor.onIssue(component, defaultIssue);
        visitor.onIssue(component, defaultIssue);
        PostAnalysisIssueVisitor.LightIssue lightIssue1 = visitor.getIssues().get(0).getIssue();
        PostAnalysisIssueVisitor.LightIssue lightIssue2 = visitor.getIssues().get(1).getIssue();

        // assert equality
        assertEquals(lightIssue1, lightIssue2);
        assertEquals(lightIssue1.hashCode(), lightIssue2.hashCode());

        // also assert equality on identity
        assertEquals(lightIssue1, lightIssue1);
        assertEquals(lightIssue1.hashCode(), lightIssue1.hashCode());
    }

    @Test
    public void testDifferentLightIssues() {
        DefaultIssue defaultIssue = exampleDefaultIssue();
        Component component = mock(Component.class);

        // map the DefaultIssue into a first LightIssue
        PostAnalysisIssueVisitor visitor = new PostAnalysisIssueVisitor();
        visitor.onIssue(component, defaultIssue);
        PostAnalysisIssueVisitor.LightIssue lightIssue1 = visitor.getIssues().get(0).getIssue();

        // map a slightly different DefaultIssue into another LightIssue
        doReturn("another message").when(defaultIssue).getMessage();
        visitor.onIssue(component, defaultIssue);
        PostAnalysisIssueVisitor.LightIssue lightIssue2 = visitor.getIssues().get(1).getIssue();

        // assert difference
        assertNotEquals(lightIssue1, lightIssue2);
        assertNotEquals(lightIssue1.hashCode(), lightIssue2.hashCode());

        // also assert difference with unrelated objects, and null
        assertNotEquals(lightIssue1, new Object());
        assertNotEquals(lightIssue1, null);
        
    }

    @Test
    public void shouldReturnScmInfoForFileComponent() {
        Component component = mock(Component.class);
        when(component.getType()).thenReturn(Component.Type.FILE);
        ReportAttributes reportAttributes = mock(ReportAttributes.class);
        when(reportAttributes.getScmPath()).thenReturn(Optional.of("path"));
        when(component.getReportAttributes()).thenReturn(reportAttributes);

        PostAnalysisIssueVisitor.LightIssue issue = mock(PostAnalysisIssueVisitor.LightIssue.class);
        PostAnalysisIssueVisitor.ComponentIssue underTest = new PostAnalysisIssueVisitor.ComponentIssue(component, issue);

        assertThat(underTest.getScmPath()).contains("path");
    }

    @Test
    public void shouldReturnNoScmInfoForNonFileComponent() {
        Component component = mock(Component.class);
        when(component.getType()).thenReturn(Component.Type.PROJECT);
        ReportAttributes reportAttributes = mock(ReportAttributes.class);
        when(reportAttributes.getScmPath()).thenReturn(Optional.of("path"));
        when(component.getReportAttributes()).thenReturn(reportAttributes);

        PostAnalysisIssueVisitor.LightIssue issue = mock(PostAnalysisIssueVisitor.LightIssue.class);
        PostAnalysisIssueVisitor.ComponentIssue underTest = new PostAnalysisIssueVisitor.ComponentIssue(component, issue);

        assertThat(underTest.getScmPath()).isEmpty();
    }

    @Test
    public void shouldReturnNoScmInfoForFileComponentWithNoInfo() {
        Component component = mock(Component.class);
        when(component.getType()).thenReturn(Component.Type.FILE);
        ReportAttributes reportAttributes = mock(ReportAttributes.class);
        when(reportAttributes.getScmPath()).thenReturn(Optional.empty());
        when(component.getReportAttributes()).thenReturn(reportAttributes);

        PostAnalysisIssueVisitor.LightIssue issue = mock(PostAnalysisIssueVisitor.LightIssue.class);
        PostAnalysisIssueVisitor.ComponentIssue underTest = new PostAnalysisIssueVisitor.ComponentIssue(component, issue);

        assertThat(underTest.getScmPath()).isEmpty();
    }


}
