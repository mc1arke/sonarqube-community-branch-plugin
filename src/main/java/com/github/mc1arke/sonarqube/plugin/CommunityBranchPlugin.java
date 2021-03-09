/*
 * Copyright (C) 2020-2021 Michael Clarke
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
import com.github.mc1arke.sonarqube.plugin.scanner.ScannerPullRequestPropertySensor;
import com.github.mc1arke.sonarqube.plugin.server.CommunityBranchFeatureExtension;
import com.github.mc1arke.sonarqube.plugin.server.CommunityBranchSupportDelegate;
import com.github.mc1arke.sonarqube.plugin.server.pullrequest.ws.action.CountBindingAction;
import com.github.mc1arke.sonarqube.plugin.server.pullrequest.ws.action.DeleteBindingAction;
import com.github.mc1arke.sonarqube.plugin.server.pullrequest.ws.action.SetAzureBindingAction;
import com.github.mc1arke.sonarqube.plugin.server.pullrequest.ws.action.SetBitbucketBindingAction;
import com.github.mc1arke.sonarqube.plugin.server.pullrequest.ws.action.SetBitbucketCloudBindingAction;
import com.github.mc1arke.sonarqube.plugin.server.pullrequest.ws.action.SetGithubBindingAction;
import com.github.mc1arke.sonarqube.plugin.server.pullrequest.ws.action.SetGitlabBindingAction;
import com.github.mc1arke.sonarqube.plugin.server.pullrequest.ws.action.CreateBitbucketCloudAction;
import com.github.mc1arke.sonarqube.plugin.server.pullrequest.ws.action.UpdateBitbucketCloudAction;
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

    public static final String IMAGE_URL_BASE = "com.github.mc1arke.sonarqube.plugin.branch.image-url-base";

    @Override
    public String getName() {
        return "Community Branch Plugin";
    }

    @Override
    public void load(CoreExtension.Context context) {
        if (SonarQubeSide.COMPUTE_ENGINE == context.getRuntime().getSonarQubeSide()) {
            context.addExtensions(CommunityReportAnalysisComponentProvider.class, CommunityBranchEditionProvider.class);
        } else if (SonarQubeSide.SERVER == context.getRuntime().getSonarQubeSide()) {
            context.addExtensions(CommunityBranchFeatureExtension.class, CommunityBranchSupportDelegate.class,

                                  CountBindingAction.class,
                                  DeleteBindingAction.class,
                                  SetGithubBindingAction.class,
                                  SetAzureBindingAction.class,
                                  SetBitbucketBindingAction.class,
                                  SetBitbucketCloudBindingAction.class,
                                  SetGitlabBindingAction.class, CreateBitbucketCloudAction.class, UpdateBitbucketCloudAction.class,


                /* org.sonar.db.purge.PurgeConfiguration uses the value for the this property if it's configured, so it only
                needs to be specified here, but doesn't need any additional classes to perform the relevant purge/cleanup
                */
                                  PropertyDefinition
                                          .builder(PurgeConstants.DAYS_BEFORE_DELETING_INACTIVE_BRANCHES_AND_PRS)
                                          .name("Number of days before purging inactive short living branches")
                                          .description(
                                                  "Short living branches are permanently deleted when there are no analysis for the configured number of days.")
                                          .category(CoreProperties.CATEGORY_HOUSEKEEPING)
                                          .subCategory(CoreProperties.SUBCATEGORY_GENERAL).defaultValue("30")
                                          .type(PropertyType.INTEGER).build()


                                 );

        }

        if (SonarQubeSide.COMPUTE_ENGINE == context.getRuntime().getSonarQubeSide() ||
            SonarQubeSide.SERVER == context.getRuntime().getSonarQubeSide()) {
            context.addExtensions(PropertyDefinition.builder(IMAGE_URL_BASE)
                                          .category(CoreProperties.CATEGORY_GENERAL)
                                          .subCategory(CoreProperties.SUBCATEGORY_GENERAL)
                                          .onQualifiers(Qualifiers.APP)
                                          .name("Images base URL")
                                          .description("Base URL used to load the images for the PR comments (please use this only if images are not displayed properly).")
                                          .type(PropertyType.STRING)
                                          .build());

        }
    }

    @Override
    public void define(Plugin.Context context) {
        if (SonarQubeSide.SCANNER == context.getRuntime().getSonarQubeSide()) {
            context.addExtensions(CommunityProjectBranchesLoader.class, CommunityProjectPullRequestsLoader.class,
                                  CommunityBranchConfigurationLoader.class, CommunityBranchParamsValidator.class,
                                  ScannerPullRequestPropertySensor.class);
        }
    }
}
