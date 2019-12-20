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
package com.github.mc1arke.sonarqube.plugin.ce.pullrequest;

import org.junit.Test;
import org.sonar.ce.task.projectanalysis.component.Component;
import org.sonar.core.issue.DefaultIssue;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class PostAnalysisIssueVisitorTest {

    @Test
    public void checkAllIssuesCollected() {
        PostAnalysisIssueVisitor testCase = new PostAnalysisIssueVisitor();

        List<PostAnalysisIssueVisitor.ComponentIssue> expected = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            DefaultIssue issue = (i == 10 ? null : mock(DefaultIssue.class));
            Component component = (i == 5 ? null : mock(Component.class));
            expected.add(new PostAnalysisIssueVisitor.ComponentIssue(component, issue));

            testCase.onIssue(component, issue);
        }


        assertThat(testCase.getIssues().size()).isEqualTo(expected.size());
        for (int i = 0; i < expected.size(); i++) {
            assertThat(testCase.getIssues().get(i).getIssue()).isEqualTo(expected.get(i).getIssue());
            assertThat(testCase.getIssues().get(i).getComponent()).isEqualTo(expected.get(i).getComponent());
        }
    }
}
