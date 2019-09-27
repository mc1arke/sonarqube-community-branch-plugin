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

import com.github.mc1arke.sonarqube.plugin.ce.CommunityBranchEditionProvider;
import com.github.mc1arke.sonarqube.plugin.ce.CommunityReportAnalysisComponentProvider;
import com.github.mc1arke.sonarqube.plugin.scanner.CommunityBranchConfigurationLoader;
import com.github.mc1arke.sonarqube.plugin.scanner.CommunityBranchParamsValidator;
import com.github.mc1arke.sonarqube.plugin.scanner.CommunityProjectBranchesLoader;
import com.github.mc1arke.sonarqube.plugin.scanner.CommunityProjectPullRequestsLoader;
import com.github.mc1arke.sonarqube.plugin.server.CommunityBranchFeatureExtension;
import com.github.mc1arke.sonarqube.plugin.server.CommunityBranchSupportDelegate;
import org.sonar.api.CoreProperties;
import org.sonar.api.Plugin;
import org.sonar.api.PropertyType;
import org.sonar.api.SonarQubeSide;
import org.sonar.api.config.PropertyDefinition;
import org.sonar.api.resources.Qualifiers;
import org.sonar.core.config.PurgeConstants;
import org.sonar.core.extension.CoreExtension;

/**
 * @author Michael Clarke
 */
public class CommunityBranchPlugin implements Plugin, CoreExtension {

    private static final String PULL_REQUEST_CATEGORY_LABEL = "Pull Request";
    private static final String GENERAL = "General";
    private static final String GITHUB_INTEGRATION_SUBCATEGORY_LABEL = "Integration With Github";
    private static final String BITBUCKET_INTEGRATION_SUBCATEGORY_LABEL = "Integration With Bitbucket";


    @Override
    public String getName() {
        return "Community Branch Plugin";
    }

    @Override
    public void load(CoreExtension.Context context) {
        if (SonarQubeSide.COMPUTE_ENGINE == context.getRuntime().getSonarQubeSide()) {
            context.addExtensions(CommunityReportAnalysisComponentProvider.class, CommunityBranchEditionProvider.class);
        } else if (SonarQubeSide.SERVER == context.getRuntime().getSonarQubeSide()) {
            context.addExtensions(CommunityBranchFeatureExtension.class, CommunityBranchSupportDelegate.class);
        }

        context.addExtensions(
            /* org.sonar.db.purge.PurgeConfiguration uses the value for the this property if it's configured, so it only
            needs to be specified here, but doesn't need any additional classes to perform the relevant purge/cleanup
             */
                PropertyDefinition.builder(PurgeConstants.DAYS_BEFORE_DELETING_INACTIVE_SHORT_LIVING_BRANCHES)
                        .name("Number of days before purging inactive short living branches").description(
                        "Short living branches are permanently deleted when there are no analysis for the configured number of days.")
                        .category(CoreProperties.CATEGORY_GENERAL)
                        .subCategory(CoreProperties.SUBCATEGORY_DATABASE_CLEANER).defaultValue("30")
                        .type(PropertyType.INTEGER).build(),

                //the name and description shown on the UI are automatically loaded from core.properties so don't need to be specified here
                PropertyDefinition.builder(CoreProperties.LONG_LIVED_BRANCHES_REGEX).onQualifiers(Qualifiers.PROJECT)
                        .category(CoreProperties.CATEGORY_GENERAL).subCategory(CoreProperties.SUBCATEGORY_BRANCHES)
                        .defaultValue(CommunityBranchConfigurationLoader.DEFAULT_BRANCH_REGEX).build(),

                PropertyDefinition.builder("sonar.pullrequest.provider").subCategory(PULL_REQUEST_CATEGORY_LABEL)
                        .subCategory(GENERAL)
                        .onlyOnQualifiers(Qualifiers.PROJECT).name("Provider").type(PropertyType.SINGLE_SELECT_LIST)
                        .options("Github", "BitbucketServer", "BitbucketCloud").build(),

                PropertyDefinition.builder("sonar.pullrequest.summary.comment.enabled")
                        .subCategory(PULL_REQUEST_CATEGORY_LABEL)
                        .subCategory(GENERAL)
                        .onlyOnQualifiers(Qualifiers.PROJECT)
                        .name("Enable summary comment")
                        .description("This enables the summary comment (if implemented).")
                        .type(PropertyType.BOOLEAN)
                        .defaultValue("true")
                        .build(),

                PropertyDefinition.builder("sonar.pullrequest.file.comment.enabled")
                        .subCategory(PULL_REQUEST_CATEGORY_LABEL)
                        .subCategory(GENERAL)
                        .onlyOnQualifiers(Qualifiers.PROJECT)
                        .name("Enable file comment")
                        .description("This enables commenting (if implemented).")
                        .type(PropertyType.BOOLEAN)
                        .defaultValue("true")
                        .build(),

                PropertyDefinition.builder("sonar.pullrequest.delete.comments.enabled")
                        .subCategory(PULL_REQUEST_CATEGORY_LABEL)
                        .subCategory(GENERAL)
                        .onlyOnQualifiers(Qualifiers.PROJECT)
                        .name("Enable deleting comments")
                        .description("This cleans up the comments from previous runs (if implemented).")
                        .type(PropertyType.BOOLEAN)
                        .defaultValue("false")
                        .build(),

                PropertyDefinition.builder("sonar.alm.github.app.privateKey.secured")
                        .subCategory(PULL_REQUEST_CATEGORY_LABEL).subCategory(GITHUB_INTEGRATION_SUBCATEGORY_LABEL)
                        .onQualifiers(Qualifiers.APP).name("App Private Key")
                        .type(PropertyType.PASSWORD).build(),

                PropertyDefinition.builder("sonar.alm.github.app.name").subCategory(PULL_REQUEST_CATEGORY_LABEL)
                        .subCategory(GITHUB_INTEGRATION_SUBCATEGORY_LABEL).onQualifiers(Qualifiers.APP).name("App Name")
                        .defaultValue("SonarQube Community Pull Request Analysis").type(PropertyType.STRING).build(),

                PropertyDefinition.builder("sonar.alm.github.app.id").subCategory(PULL_REQUEST_CATEGORY_LABEL)
                        .subCategory(GITHUB_INTEGRATION_SUBCATEGORY_LABEL).onQualifiers(Qualifiers.APP).name("App ID")
                        .type(PropertyType.STRING).build(),

                PropertyDefinition.builder("sonar.pullrequest.github.repository")
                        .subCategory(PULL_REQUEST_CATEGORY_LABEL).subCategory(GITHUB_INTEGRATION_SUBCATEGORY_LABEL)
                        .onlyOnQualifiers(Qualifiers.PROJECT)
                        .name("Repository identifier").description("Example: SonarSource/sonarqube")
                        .type(PropertyType.STRING).build(),

                PropertyDefinition.builder("sonar.pullrequest.github.endpoint").subCategory(PULL_REQUEST_CATEGORY_LABEL)
                        .subCategory(GITHUB_INTEGRATION_SUBCATEGORY_LABEL).onQualifiers(Qualifiers.APP)
                        .name("The API URL for a GitHub instance").description(
                        "The API url for a GitHub instance. https://api.github.com/ for github.com, https://github.company.com/api/ when using GitHub Enterprise")
                        .type(PropertyType.STRING).defaultValue("https://api.github.com").build(),

                PropertyDefinition.builder("sonar.pullrequest.bitbucket.url").subCategory(PULL_REQUEST_CATEGORY_LABEL)
                        .subCategory(BITBUCKET_INTEGRATION_SUBCATEGORY_LABEL).onQualifiers(Qualifiers.PROJECT)
                        .name("URL for Bitbucket (Server or Cloud) instance").description(
                        "Example: http://bitbucket.local")
                        .type(PropertyType.STRING).build(),

                PropertyDefinition.builder("sonar.pullrequest.bitbucket.token")
                        .subCategory(PULL_REQUEST_CATEGORY_LABEL)
                        .subCategory(BITBUCKET_INTEGRATION_SUBCATEGORY_LABEL)
                        .onQualifiers(Qualifiers.PROJECT)
                        .name("The token for the user to comment to the PR on Bitbucket (Server or Cloud) instance")
                        .description("Token used for authentication and commenting to your Bitbucket instance")
                        .type(PropertyType.STRING)
                        .build(),

                PropertyDefinition.builder("sonar.pullrequest.bitbucket.repositorySlug")
                        .subCategory(PULL_REQUEST_CATEGORY_LABEL)
                        .subCategory(BITBUCKET_INTEGRATION_SUBCATEGORY_LABEL)
                        .onlyOnQualifiers(Qualifiers.PROJECT)
                        .name("Repository Slug")
                        .description("Repository Slug see for example https://docs.atlassian.com/bitbucket-server/rest/latest/bitbucket-rest.html")
                        .type(PropertyType.STRING)
                        .build(),

                PropertyDefinition.builder("sonar.pullrequest.bitbucket.userSlug")
                        .subCategory(PULL_REQUEST_CATEGORY_LABEL)
                        .subCategory(BITBUCKET_INTEGRATION_SUBCATEGORY_LABEL)
                        .onlyOnQualifiers(Qualifiers.PROJECT)
                        .name("User Slug")
                        .description("This is used for '/users' repos. Only set one User Slug or ProjectKey!")
                        .type(PropertyType.STRING)
                        .build(),

                PropertyDefinition.builder("sonar.pullrequest.bitbucket.projectKey")
                        .subCategory(PULL_REQUEST_CATEGORY_LABEL)
                        .subCategory(BITBUCKET_INTEGRATION_SUBCATEGORY_LABEL)
                        .onlyOnQualifiers(Qualifiers.PROJECT)
                        .name("ProjectKey")
                        .description("This is used for '/projects' repos. Only set one User Slug or ProjectKey!")
                        .type(PropertyType.STRING)
                        .build()
                             );
    }

    @Override
    public void define(Plugin.Context context) {
        if (SonarQubeSide.SCANNER == context.getRuntime().getSonarQubeSide()) {
            context.addExtensions(CommunityProjectBranchesLoader.class, CommunityProjectPullRequestsLoader.class,
                                  CommunityBranchConfigurationLoader.class, CommunityBranchParamsValidator.class);
        }
    }
}
