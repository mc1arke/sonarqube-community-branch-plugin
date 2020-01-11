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
package com.github.mc1arke.sonarqube.plugin.ce.pullrequest.github;

import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.AnalysisDetails;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.PullRequestBuildStatusDecorator;

public class GithubPullRequestDecorator implements PullRequestBuildStatusDecorator {

    private final CheckRunProvider checkRunProvider;

    public GithubPullRequestDecorator(CheckRunProvider checkRunProvider) {
        this.checkRunProvider = checkRunProvider;
    }

    @Override
    public void decorateQualityGateStatus(AnalysisDetails analysisDetails) {
        try {
            checkRunProvider.createCheckRun(analysisDetails);
        } catch (Exception ex) {
            throw new IllegalStateException("Could not decorate Pull Request on Github", ex);
        }

    }

    @Override
    public String name() {
        return "Github";
    }

}
