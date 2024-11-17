/*
 * Copyright (C) 2019-2024 Michael Clarke
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.sonar.api.issue.IssueStatus;
import org.sonar.api.issue.impact.Severity;
import org.sonar.api.issue.impact.SoftwareQuality;
import org.sonar.api.rule.RuleKey;
import org.sonar.ce.task.projectanalysis.component.Component;
import org.sonar.ce.task.projectanalysis.component.ReportAttributes;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.db.protobuf.DbIssues;

class PostAnalysisIssueVisitorTest {

    private static final String EXAMPLE_ISSUE_KEY = "key";
    private static final int EXAMPLE_ISSUE_LINE = 1000;
    private static final String EXAMPLE_ISSUE_MESSAGE = "message";
    private static final String EXAMPLE_ISSUE_RESOLUTION = "resolution";
    private static final Map<SoftwareQuality, Severity> EXAMPLE_IMPACTS = Map.of(SoftwareQuality.RELIABILITY, Severity.HIGH);
    private static final IssueStatus EXAMPLE_ISSUE_STATUS = IssueStatus.OPEN;
    private static final RuleKey EXAMPLE_ISSUE_RULEKEY = RuleKey.of("repo", "rule");
    private static final DbIssues.Locations EXAMPLE_ISSUE_LOCATIONS = DbIssues.Locations.getDefaultInstance();

    @Test
    void checkAllIssuesCollected() {
        PostAnalysisIssueVisitor testCase = new PostAnalysisIssueVisitor();

        List<PostAnalysisIssueVisitor.ComponentIssue> expected = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            DefaultIssue issue = mock();
            Component component = mock();
            expected.add(new PostAnalysisIssueVisitor.ComponentIssue(component, new PostAnalysisIssueVisitor.LightIssue(issue)));

            testCase.onIssue(component, issue);
        }


        assertThat(testCase.getIssues()).hasSameSizeAs(expected);
        for (int i = 0; i < expected.size(); i++) {
            assertThat(testCase.getIssues().get(i).getIssue()).isEqualTo(expected.get(i).getIssue());
            assertThat(testCase.getIssues().get(i).getComponent()).isEqualTo(expected.get(i).getComponent());
        }
    }

    private DefaultIssue exampleDefaultIssue() {
        DefaultIssue defaultIssue = mock();
        when(defaultIssue.key()).thenReturn(EXAMPLE_ISSUE_KEY);
        when(defaultIssue.getLine()).thenReturn(EXAMPLE_ISSUE_LINE);
        when(defaultIssue.getMessage()).thenReturn(EXAMPLE_ISSUE_MESSAGE);
        when(defaultIssue.resolution()).thenReturn(EXAMPLE_ISSUE_RESOLUTION);
        when(defaultIssue.issueStatus()).thenReturn(EXAMPLE_ISSUE_STATUS);
        when(defaultIssue.getRuleKey()).thenReturn(EXAMPLE_ISSUE_RULEKEY);
        when(defaultIssue.getLocations()).thenReturn(EXAMPLE_ISSUE_LOCATIONS);
        when(defaultIssue.impacts()).thenReturn(EXAMPLE_IMPACTS);
        return defaultIssue;
    }

    @Test
    void testLightIssueMapping() {
        // mock a DefaultIssue
        DefaultIssue defaultIssue = exampleDefaultIssue();
        Component component = mock();

        // map the DefaultIssue into a LightIssue (using PostAnalysisIssueVisitor to workaround private constructor)
        PostAnalysisIssueVisitor visitor = new PostAnalysisIssueVisitor();
        visitor.onIssue(component, defaultIssue);
        PostAnalysisIssueVisitor.LightIssue lightIssue = visitor.getIssues().get(0).getIssue();

        // check values equality, twice (see below)
        for (int i = 0; i < 2; i++) {
            assertThat(lightIssue.key()).isEqualTo(EXAMPLE_ISSUE_KEY);
            assertThat(lightIssue.getLine()).isEqualTo(EXAMPLE_ISSUE_LINE);
            assertThat(lightIssue.getMessage()).isEqualTo(EXAMPLE_ISSUE_MESSAGE);
            assertThat(lightIssue.resolution()).isEqualTo(EXAMPLE_ISSUE_RESOLUTION);
            assertThat(lightIssue.impacts()).isEqualTo(EXAMPLE_IMPACTS);
            assertThat(lightIssue.issueStatus()).isEqualTo(EXAMPLE_ISSUE_STATUS);
            assertThat(lightIssue.getRuleKey()).isEqualTo(EXAMPLE_ISSUE_RULEKEY);
            assertThat(lightIssue.getLocations()).isEqualTo(EXAMPLE_ISSUE_LOCATIONS);
        }

        // check DefaultIssue getters have been called _exactly once_
        verify(defaultIssue).key();
        verify(defaultIssue).getLine();
        verify(defaultIssue).getMessage();
        verify(defaultIssue).resolution();
        verify(defaultIssue).impacts();
        verify(defaultIssue).issueStatus();
        verify(defaultIssue).getRuleKey();
        verify(defaultIssue).getLocations();
        verifyNoMoreInteractions(defaultIssue);
    }

    @Test
    void shouldReturnEqualsForSameIssueContents() {
        DefaultIssue defaultIssue = exampleDefaultIssue();
        Component component = mock();

        // map the DefaultIssue into two equal LightIssues
        PostAnalysisIssueVisitor visitor = new PostAnalysisIssueVisitor();
        visitor.onIssue(component, defaultIssue);
        visitor.onIssue(component, defaultIssue);
        PostAnalysisIssueVisitor.LightIssue lightIssue1 = visitor.getIssues().get(0).getIssue();
        PostAnalysisIssueVisitor.LightIssue lightIssue2 = visitor.getIssues().get(1).getIssue();

        // assert equality
        assertThat(lightIssue1).isEqualTo(lightIssue2)
            .hasSameHashCodeAs(lightIssue2)

        // also assert equality on identity
            .isEqualTo(lightIssue1)
            .hasSameHashCodeAs(lightIssue1);
    }

    @Test
    void shouldNotReturnEqualsForDifferentIssueContents() {
        DefaultIssue defaultIssue = exampleDefaultIssue();
        Component component = mock();

        // map the DefaultIssue into a first LightIssue
        PostAnalysisIssueVisitor visitor = new PostAnalysisIssueVisitor();
        visitor.onIssue(component, defaultIssue);
        PostAnalysisIssueVisitor.LightIssue lightIssue1 = visitor.getIssues().get(0).getIssue();

        // map a slightly different DefaultIssue into another LightIssue
        doReturn("another message").when(defaultIssue).getMessage();
        visitor.onIssue(component, defaultIssue);
        PostAnalysisIssueVisitor.LightIssue lightIssue2 = visitor.getIssues().get(1).getIssue();

        // assert difference
        assertThat(lightIssue1).isNotEqualTo(lightIssue2)
            .doesNotHaveSameHashCodeAs(lightIssue2)

        // also assert difference with unrelated objects, and null
            .isNotEqualTo(new Object())
            .isNotEqualTo(null);
        
    }

    @Test
    void shouldReturnScmInfoForFileComponent() {
        Component component = mock();
        when(component.getType()).thenReturn(Component.Type.FILE);
        ReportAttributes reportAttributes = mock();
        when(reportAttributes.getScmPath()).thenReturn(Optional.of("path"));
        when(component.getReportAttributes()).thenReturn(reportAttributes);

        PostAnalysisIssueVisitor.LightIssue issue = mock();
        PostAnalysisIssueVisitor.ComponentIssue underTest = new PostAnalysisIssueVisitor.ComponentIssue(component, issue);

        assertThat(underTest.getScmPath()).contains("path");
    }

    @Test
    void shouldReturnNoScmInfoForNonFileComponent() {
        Component component = mock();
        when(component.getType()).thenReturn(Component.Type.PROJECT);
        ReportAttributes reportAttributes = mock();
        when(reportAttributes.getScmPath()).thenReturn(Optional.of("path"));
        when(component.getReportAttributes()).thenReturn(reportAttributes);

        PostAnalysisIssueVisitor.LightIssue issue = mock();
        PostAnalysisIssueVisitor.ComponentIssue underTest = new PostAnalysisIssueVisitor.ComponentIssue(component, issue);

        assertThat(underTest.getScmPath()).isEmpty();
    }

    @Test
    void shouldReturnNoScmInfoForFileComponentWithNoInfo() {
        Component component = mock();
        when(component.getType()).thenReturn(Component.Type.FILE);
        ReportAttributes reportAttributes = mock();
        when(reportAttributes.getScmPath()).thenReturn(Optional.empty());
        when(component.getReportAttributes()).thenReturn(reportAttributes);

        PostAnalysisIssueVisitor.LightIssue issue = mock();
        PostAnalysisIssueVisitor.ComponentIssue underTest = new PostAnalysisIssueVisitor.ComponentIssue(component, issue);

        assertThat(underTest.getScmPath()).isEmpty();
    }


}
