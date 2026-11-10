/*
 * Copyright (C) 2020-2026 Michael Clarke
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

import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.gitlab.GitlabMergeRequestDecorator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.sonar.api.config.Configuration;
import java.util.Optional;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.utils.System2;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class ScannerPullRequestPropertySensorTest {

    private final System2 system2 = mock();
    private final SensorContext context = mock();
    private final ScannerPullRequestPropertySensor sensor = new ScannerPullRequestPropertySensor(system2);

    private final Configuration configuration = mock();

    @BeforeEach
    void configureContext() {
        when(context.config()).thenReturn(configuration);
    }

    @Test
    void shouldReportBatchModeFromScannerConfiguration() {
        when(configuration.get(GitlabMergeRequestDecorator.PULLREQUEST_GITLAB_BATCH_COMMENTS)).thenReturn(Optional.of("true"));
        sensor.execute(context);
        verify(context).config();
        verify(context).addContextProperty(GitlabMergeRequestDecorator.PULLREQUEST_GITLAB_BATCH_COMMENTS, "true");
        verifyNoMoreInteractions(context);
    }

    @Test
    void shouldReportExplicitlyDisabledBatchMode() {
        when(configuration.get(GitlabMergeRequestDecorator.PULLREQUEST_GITLAB_BATCH_COMMENTS)).thenReturn(Optional.of("false"));
        sensor.execute(context);
        verify(context).config();
        verify(context).addContextProperty(GitlabMergeRequestDecorator.PULLREQUEST_GITLAB_BATCH_COMMENTS, "false");
        verifyNoMoreInteractions(context);
    }

    @Test
    void shouldReportAllGitlabVariablesFromEnvironment() {
        when(system2.envVariable("GITLAB_CI")).thenReturn("true");
        when(system2.envVariable("CI_API_V4_URL")).thenReturn("value");
        when(system2.envVariable("CI_PROJECT_PATH")).thenReturn("value");
        when(system2.envVariable("CI_MERGE_REQUEST_PROJECT_URL")).thenReturn("value");
        when(system2.envVariable("CI_PIPELINE_ID")).thenReturn("value");

        sensor.execute(context);

        verify(context).config();
        verify(context).addContextProperty(GitlabMergeRequestDecorator.PULLREQUEST_GITLAB_PROJECT_URL, "value");
        verify(context).addContextProperty(GitlabMergeRequestDecorator.PULLREQUEST_GITLAB_PIPELINE_ID, "value");
        verifyNoMoreInteractions(context);
    }

    @Test
    void shouldReportGitlabVariablesFromEnvironmentWhenMinimalPropertiesSet() {
        when(system2.envVariable("GITLAB_CI")).thenReturn("true");
        when(system2.envVariable("CI_MERGE_REQUEST_PROJECT_URL")).thenReturn("value");
        when(system2.envVariable("CI_PIPELINE_ID")).thenReturn("value");

        sensor.execute(context);

        verify(context).config();
        verify(context).addContextProperty(GitlabMergeRequestDecorator.PULLREQUEST_GITLAB_PROJECT_URL, "value");
        verify(context).addContextProperty(GitlabMergeRequestDecorator.PULLREQUEST_GITLAB_PIPELINE_ID, "value");
        verifyNoMoreInteractions(context);
    }
}
