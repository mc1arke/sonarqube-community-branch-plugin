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
package com.github.mc1arke.sonarqube.plugin;

public class CommunityBranchPluginConstants {

    private CommunityBranchPluginConstants(){
        // only used for constants
    }

    /**
     * GENERAL CONSTANTS
     */
    public static final String IMAGE_URL_BASE = "com.github.mc1arke.sonarqube.plugin.branch.image-url-base";

    public static final String PULL_REQUEST_COMMENT_SUMMARY_ENABLED = "com.github.mc1arke.sonarqube.plugin.branch.pullrequest.comment.summary.enabled";

    public static final String PULL_REQUEST_FILE_COMMENT_ENABLED = "com.github.mc1arke.sonarqube.plugin.branch.pullrequest.file.comment.enabled";

    public static final String PULL_REQUEST_DELETE_COMMENTS_ENABLED = "com.github.mc1arke.sonarqube.plugin.branch.pullrequest.delete.comments.enabled";

    /**
     * BITBUCKET CONSTANTS
     */
    public static final String PULL_REQUEST_BITBUCKET_URL = "com.github.mc1arke.sonarqube.plugin.branch.pullrequest.bitbucket.url";

    public static final String PULL_REQUEST_BITBUCKET_TOKEN = "com.github.mc1arke.sonarqube.plugin.branch.pullrequest.bitbucket.token";

    public static final String PULL_REQUEST_BITBUCKET_PROJECT_KEY = "com.github.mc1arke.sonarqube.plugin.branch.pullrequest.bitbucket.projectKey";

    public static final String PULL_REQUEST_BITBUCKET_USER_SLUG = "com.github.mc1arke.sonarqube.plugin.branch.pullrequest.bitbucket.userSlug";

    public static final String PULL_REQUEST_BITBUCKET_REPOSITORY_SLUG = "com.github.mc1arke.sonarqube.plugin.branch.pullrequest.bitbucket.repositorySlug";

    public static final String PULL_REQUEST_BITBUCKET_COMMENT_USER_SLUG = "com.github.mc1arke.sonarqube.plugin.branch.pullrequest.bitbucket.comment.userSlug";

}
