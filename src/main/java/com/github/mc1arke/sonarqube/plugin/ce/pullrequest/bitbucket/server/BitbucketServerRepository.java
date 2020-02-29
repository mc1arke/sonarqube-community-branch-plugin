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
package com.github.mc1arke.sonarqube.plugin.ce.pullrequest.bitbucket.server;

public class BitbucketServerRepository {

    enum ProjectType {
        PROJECT, USER
    }

    private final ProjectType projectType;
    private final String projectKey;
    private final String repositorySlug;

    public static BitbucketServerRepository projectRepository(String projectKey, String repositorySlug) {
        return new BitbucketServerRepository(ProjectType.PROJECT, projectKey, repositorySlug);
    }

    public static BitbucketServerRepository userRepository(String userSlug, String repositorySlug) {
        return new BitbucketServerRepository(ProjectType.USER, userSlug, repositorySlug);
    }

    private BitbucketServerRepository(ProjectType projectType, String projectKey, String repositorySlug) {
        this.projectType = projectType;
        this.projectKey = projectKey;
        this.repositorySlug = repositorySlug;
    }

    public ProjectType getProjectType() {
        return projectType;
    }

    public String getProjectKey() {
        return projectKey;
    }

    public String getRepositorySlug() {
        return repositorySlug;
    }
}
