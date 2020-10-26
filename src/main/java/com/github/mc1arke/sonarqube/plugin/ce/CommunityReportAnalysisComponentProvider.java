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
package com.github.mc1arke.sonarqube.plugin.ce;

import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.PostAnalysisIssueVisitor;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.PullRequestPostAnalysisTask;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.azuredevops.AzureDevOpsServerPullRequestDecorator;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.bitbucket.BitbucketPullRequestDecorator;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.github.GithubPullRequestDecorator;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.github.v3.DefaultLinkHeaderReader;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.github.v3.RestApplicationAuthenticationProvider;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.github.v4.GraphqlCheckRunProvider;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.gitlab.GitlabServerPullRequestDecorator;
import org.sonar.ce.task.projectanalysis.container.ReportAnalysisComponentProvider;

import java.util.Arrays;
import java.util.List;

/**
 * @author Michael Clarke
 */
public class CommunityReportAnalysisComponentProvider implements ReportAnalysisComponentProvider {

    @Override
    public List<Object> getComponents() {
        return Arrays.asList(CommunityBranchLoaderDelegate.class, PullRequestPostAnalysisTask.class,
                             PostAnalysisIssueVisitor.class, GithubPullRequestDecorator.class,
                             GraphqlCheckRunProvider.class, DefaultLinkHeaderReader.class, RestApplicationAuthenticationProvider.class,
                             BitbucketPullRequestDecorator.class, GitlabServerPullRequestDecorator.class,
                             AzureDevOpsServerPullRequestDecorator.class);
    }

}
