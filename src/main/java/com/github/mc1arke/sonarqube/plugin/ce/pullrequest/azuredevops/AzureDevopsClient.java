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
package com.github.mc1arke.sonarqube.plugin.ce.pullrequest.azuredevops;

import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.azuredevops.model.Comment;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.azuredevops.model.CommentThread;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.azuredevops.model.GitPullRequestStatus;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.azuredevops.model.PullRequest;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.azuredevops.model.enums.CommentThreadStatus;

import java.io.IOException;
import java.util.List;

public interface AzureDevopsClient {

    void submitPullRequestStatus(String projectId, String repositoryName, long pullRequestId, GitPullRequestStatus status) throws IOException;

    List<CommentThread> retrieveThreads(String projectId, String repositoryName, long pullRequestId) throws IOException;

    void createThread(String projectId, String repositoryName, long pullRequestId, CommentThread thread) throws IOException;

    void addCommentToThread(String projectId, String repositoryName, long pullRequestId, long threadId, Comment comment) throws IOException;

    void updateThreadStatus(String projectId, String repositoryName, long pullRequestId, long threadId, CommentThreadStatus status) throws IOException;

    PullRequest retrievePullRequest(String projectId, String repositoryName, long pullRequestId) throws IOException;
}
