/*
 * Copyright (C) 2021 Michael Clarke
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
package com.github.mc1arke.sonarqube.plugin.almclient.azuredevops;

import com.github.mc1arke.sonarqube.plugin.almclient.azuredevops.model.CommentThread;
import com.github.mc1arke.sonarqube.plugin.almclient.azuredevops.model.Commit;
import com.github.mc1arke.sonarqube.plugin.almclient.azuredevops.model.CreateCommentRequest;
import com.github.mc1arke.sonarqube.plugin.almclient.azuredevops.model.CreateCommentThreadRequest;
import com.github.mc1arke.sonarqube.plugin.almclient.azuredevops.model.GitPullRequestStatus;
import com.github.mc1arke.sonarqube.plugin.almclient.azuredevops.model.PullRequest;
import com.github.mc1arke.sonarqube.plugin.almclient.azuredevops.model.Repository;

import java.io.IOException;
import java.util.List;

public interface AzureDevopsClient {

    PullRequest retrievePullRequest(String projectName, String repositoryName, int pullRequestId) throws IOException;

    List<Commit> getPullRequestCommits(String projectName, String repositoryName, int pullRequestId) throws IOException;

    List<CommentThread> retrieveThreads(String projectName, String repositoryName, int pullRequestId) throws IOException;

    CommentThread createThread(String projectName, String repositoryName, int pullRequestId, CreateCommentThreadRequest commentThreadRequest) throws IOException;

    void addCommentToThread(String projectName, String repositoryName, int pullRequestId, int threadId, CreateCommentRequest comment) throws IOException;

    void resolvePullRequestThread(String projectName, String repositoryName, int pullRequestId, int threadId) throws IOException;

    void submitPullRequestStatus(String projectName, String repositoryName, int pullRequestId, GitPullRequestStatus status) throws IOException;

    Repository getRepository(String projectName, String repositoryName) throws IOException;
}
