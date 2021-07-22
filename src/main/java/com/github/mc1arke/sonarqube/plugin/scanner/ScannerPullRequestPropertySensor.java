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
package com.github.mc1arke.sonarqube.plugin.scanner;

import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.gitlab.GitlabMergeRequestDecorator;
import org.sonar.api.batch.sensor.Sensor;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.SensorDescriptor;
import org.sonar.api.utils.System2;

import java.util.Optional;

public class ScannerPullRequestPropertySensor implements Sensor {

    private final System2 system2;

    public ScannerPullRequestPropertySensor(System2 system2) {
        super();
        this.system2 = system2;
    }

    @Override
    public void describe(SensorDescriptor sensorDescriptor) {
        sensorDescriptor.name(getClass().getName());
    }

    @Override
    public void execute(SensorContext sensorContext) {
        if (Boolean.parseBoolean(system2.envVariable("GITLAB_CI"))) {
            Optional.ofNullable(system2.envVariable("CI_MERGE_REQUEST_PROJECT_URL")).ifPresent(v -> sensorContext
                    .addContextProperty(GitlabMergeRequestDecorator.PULLREQUEST_GITLAB_PROJECT_URL, v));
            Optional.ofNullable(system2.envVariable("CI_PIPELINE_ID")).ifPresent(v -> sensorContext
                    .addContextProperty(GitlabMergeRequestDecorator.PULLREQUEST_GITLAB_PIPELINE_ID, v));
        }

        Optional.ofNullable(system2.property(GitlabMergeRequestDecorator.PULLREQUEST_GITLAB_PROJECT_URL)).ifPresent(
                v -> sensorContext.addContextProperty(GitlabMergeRequestDecorator.PULLREQUEST_GITLAB_PROJECT_URL, v));
        Optional.ofNullable(system2.property(GitlabMergeRequestDecorator.PULLREQUEST_GITLAB_PIPELINE_ID)).ifPresent(
                v -> sensorContext.addContextProperty(GitlabMergeRequestDecorator.PULLREQUEST_GITLAB_PIPELINE_ID, v));

    }

}
