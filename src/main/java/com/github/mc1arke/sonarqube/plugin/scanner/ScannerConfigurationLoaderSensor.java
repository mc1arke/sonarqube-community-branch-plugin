/*
 * Copyright (C) 2020 Artemy Osipov
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

import com.github.mc1arke.sonarqube.plugin.CommunityBranchPlugin;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.bitbucket.BitbucketServerPullRequestDecorator;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.github.v4.GraphqlCheckRunProvider;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.gitlab.GitlabServerPullRequestDecorator;
import com.google.common.collect.Sets;
import org.sonar.api.batch.sensor.Sensor;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.SensorDescriptor;

import java.util.Optional;
import java.util.Set;

public class ScannerConfigurationLoaderSensor implements Sensor {

    private final Set<String> overridableParameters;

    public ScannerConfigurationLoaderSensor() {
        this(Sets.newHashSet(
                CommunityBranchPlugin.PULL_REQUEST_PROVIDER,
                BitbucketServerPullRequestDecorator.PULL_REQUEST_BITBUCKET_PROJECT_KEY,
                BitbucketServerPullRequestDecorator.PULL_REQUEST_BITBUCKET_REPOSITORY_SLUG,
                GraphqlCheckRunProvider.PULL_REQUEST_GITHUB_REPOSITORY,
                GitlabServerPullRequestDecorator.PULLREQUEST_GITLAB_REPOSITORY_SLUG
        ));
    }

    ScannerConfigurationLoaderSensor(Set<String> overridableParameters) {
        this.overridableParameters = overridableParameters;
    }

    @Override
    public void describe(SensorDescriptor descriptor) {
        descriptor.name(getClass().getName());
    }

    @Override
    public void execute(SensorContext context) {
        overridableParameters.forEach(param -> saveProperty(param, context));
    }

    private void saveProperty(String propertyName, SensorContext context) {
        Optional<String> param = context.config().get(propertyName);

        param.ifPresent(value -> context.addContextProperty(propertyName, value));
    }
}
