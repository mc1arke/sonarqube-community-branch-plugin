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

/**
 * @author Michael Clarke
 */
public class CommunityBranchPlugin implements Plugin {

    private static final String PULL_REQUEST_CATEGORY_LABEL = "Pull Request";
    private static final String GITHUB_INTEGRATION_SUBCATEGORY_LABEL = "Integration With Github";

    @Override
    public void define(Context context) {
        if (SonarQubeSide.SCANNER == context.getRuntime().getSonarQubeSide()) {
            context.addExtensions(CommunityProjectBranchesLoader.class, CommunityProjectPullRequestsLoader.class,
                                  CommunityBranchConfigurationLoader.class, CommunityBranchParamsValidator.class);
        } else if (SonarQubeSide.COMPUTE_ENGINE == context.getRuntime().getSonarQubeSide()) {
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
                        .subCategory("General")
                        .onlyOnQualifiers(Qualifiers.PROJECT).name("Provider").type(PropertyType.SINGLE_SELECT_LIST)
                        .options("Github").build(),

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
                        .type(PropertyType.STRING).defaultValue("https://api.github.com").build()

                             );

    }

}
