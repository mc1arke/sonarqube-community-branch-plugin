/*
 * Copyright (C) 2020-2023 Michael Clarke
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

import com.github.mc1arke.sonarqube.plugin.almclient.DefaultLinkHeaderReader;
import com.github.mc1arke.sonarqube.plugin.almclient.azuredevops.DefaultAzureDevopsClientFactory;
import com.github.mc1arke.sonarqube.plugin.almclient.bitbucket.DefaultBitbucketClientFactory;
import com.github.mc1arke.sonarqube.plugin.almclient.bitbucket.HttpClientBuilderFactory;
import com.github.mc1arke.sonarqube.plugin.almclient.github.DefaultGithubClientFactory;
import com.github.mc1arke.sonarqube.plugin.almclient.github.v3.DefaultUrlConnectionProvider;
import com.github.mc1arke.sonarqube.plugin.almclient.github.v3.RestApplicationAuthenticationProvider;
import com.github.mc1arke.sonarqube.plugin.almclient.github.v4.DefaultGraphqlProvider;
import com.github.mc1arke.sonarqube.plugin.almclient.gitlab.DefaultGitlabClientFactory;
import com.github.mc1arke.sonarqube.plugin.ce.CommunityReportAnalysisComponentProvider;
import com.github.mc1arke.sonarqube.plugin.scanner.BranchConfigurationFactory;
import com.github.mc1arke.sonarqube.plugin.scanner.CommunityBranchConfigurationLoader;
import com.github.mc1arke.sonarqube.plugin.scanner.CommunityBranchParamsValidator;
import com.github.mc1arke.sonarqube.plugin.scanner.CommunityProjectBranchesLoader;
import com.github.mc1arke.sonarqube.plugin.scanner.ScannerPullRequestPropertySensor;
import com.github.mc1arke.sonarqube.plugin.scanner.autoconfiguration.AzureDevopsAutoConfigurer;
import com.github.mc1arke.sonarqube.plugin.scanner.autoconfiguration.BitbucketPipelinesAutoConfigurer;
import com.github.mc1arke.sonarqube.plugin.scanner.autoconfiguration.CirrusCiAutoConfigurer;
import com.github.mc1arke.sonarqube.plugin.scanner.autoconfiguration.CodeMagicAutoConfigurer;
import com.github.mc1arke.sonarqube.plugin.scanner.autoconfiguration.GithubActionsAutoConfigurer;
import com.github.mc1arke.sonarqube.plugin.scanner.autoconfiguration.GitlabCiAutoConfigurer;
import com.github.mc1arke.sonarqube.plugin.scanner.autoconfiguration.JenkinsAutoConfigurer;
import com.github.mc1arke.sonarqube.plugin.server.CommunityBranchFeatureExtension;
import com.github.mc1arke.sonarqube.plugin.server.CommunityBranchSupportDelegate;
import com.github.mc1arke.sonarqube.plugin.server.MonoRepoFeature;
import com.github.mc1arke.sonarqube.plugin.server.pullrequest.validator.AzureDevopsValidator;
import com.github.mc1arke.sonarqube.plugin.server.pullrequest.validator.BitbucketValidator;
import com.github.mc1arke.sonarqube.plugin.server.pullrequest.validator.GithubValidator;
import com.github.mc1arke.sonarqube.plugin.server.pullrequest.validator.GitlabValidator;
import com.github.mc1arke.sonarqube.plugin.server.pullrequest.ws.binding.action.DeleteBindingAction;
import com.github.mc1arke.sonarqube.plugin.server.pullrequest.ws.binding.action.SetAzureBindingAction;
import com.github.mc1arke.sonarqube.plugin.server.pullrequest.ws.binding.action.SetBitbucketBindingAction;
import com.github.mc1arke.sonarqube.plugin.server.pullrequest.ws.binding.action.SetBitbucketCloudBindingAction;
import com.github.mc1arke.sonarqube.plugin.server.pullrequest.ws.binding.action.SetGithubBindingAction;
import com.github.mc1arke.sonarqube.plugin.server.pullrequest.ws.binding.action.SetGitlabBindingAction;
import com.github.mc1arke.sonarqube.plugin.server.pullrequest.ws.binding.action.ValidateBindingAction;
import com.github.mc1arke.sonarqube.plugin.server.pullrequest.ws.pullrequest.PullRequestWs;
import com.github.mc1arke.sonarqube.plugin.server.pullrequest.ws.pullrequest.action.DeleteAction;
import com.github.mc1arke.sonarqube.plugin.server.pullrequest.ws.pullrequest.action.ListAction;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.sonar.api.Plugin;
import org.sonar.api.SonarQubeSide;
import org.sonar.api.config.PropertyDefinition;
import org.sonar.core.extension.CoreExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Michael Clarke
 */
class CommunityBranchPluginTest {

    @Test
    void shouldDefineClassesForScannerSide() {
        final CommunityBranchPlugin testCase = new CommunityBranchPlugin();

        final Plugin.Context context = mock(Plugin.Context.class, Mockito.RETURNS_DEEP_STUBS);
        when(context.getRuntime().getSonarQubeSide()).thenReturn(SonarQubeSide.SCANNER);

        testCase.define(context);

        verify(context).addExtensions(CommunityProjectBranchesLoader.class,
                CommunityBranchConfigurationLoader.class, CommunityBranchParamsValidator.class,
                ScannerPullRequestPropertySensor.class, BranchConfigurationFactory.class,
                AzureDevopsAutoConfigurer.class, BitbucketPipelinesAutoConfigurer.class,
                CirrusCiAutoConfigurer.class, CodeMagicAutoConfigurer.class,
                GithubActionsAutoConfigurer.class, GitlabCiAutoConfigurer.class,
                JenkinsAutoConfigurer.class);
    }

    @Test
    void shouldDefineClassesForServerSide() {
        final CommunityBranchPlugin testCase = new CommunityBranchPlugin();

        final Plugin.Context context = mock(Plugin.Context.class, Mockito.RETURNS_DEEP_STUBS);
        when(context.getRuntime().getSonarQubeSide()).thenReturn(SonarQubeSide.SERVER);

        testCase.define(context);

        verify(context, never()).addExtensions(any());
    }

    @Test
    void shouldDefineClassesForComputeEngineSide() {
        final CommunityBranchPlugin testCase = new CommunityBranchPlugin();

        final CoreExtension.Context context = mock(CoreExtension.Context.class, Mockito.RETURNS_DEEP_STUBS);
        when(context.getRuntime().getSonarQubeSide()).thenReturn(SonarQubeSide.COMPUTE_ENGINE);

        testCase.load(context);

        verify(context).addExtensions(CommunityReportAnalysisComponentProvider.class);
        verify(context).addExtensions(any(PropertyDefinition.class), eq(MonoRepoFeature.class));
    }


    @Test
    void shouldAddExtensionsForServerSideLoad() {
        final CommunityBranchPlugin testCase = new CommunityBranchPlugin();

        final CoreExtension.Context context = mock(CoreExtension.Context.class, Mockito.RETURNS_DEEP_STUBS);
        when(context.getRuntime().getSonarQubeSide()).thenReturn(SonarQubeSide.SERVER);

        testCase.load(context);


        verify(context).addExtensions(eq(CommunityBranchFeatureExtension.class),
                eq(CommunityBranchSupportDelegate.class),
                eq(DeleteBindingAction.class),
                eq(SetGithubBindingAction.class),
                eq(SetAzureBindingAction.class),
                eq(SetBitbucketBindingAction.class),
                eq(SetBitbucketCloudBindingAction.class),
                eq(SetGitlabBindingAction.class),
                eq(ValidateBindingAction.class),
                eq(DeleteAction.class),
                eq(ListAction.class),
                eq(PullRequestWs.class),
                eq(GithubValidator.class),
                eq(DefaultGraphqlProvider.class),
                eq(DefaultGithubClientFactory.class),
                eq(DefaultLinkHeaderReader.class),
                eq(DefaultUrlConnectionProvider.class),
                eq(RestApplicationAuthenticationProvider.class),
                eq(HttpClientBuilderFactory.class),
                eq(DefaultBitbucketClientFactory.class),
                eq(BitbucketValidator.class),
                eq(GitlabValidator.class),
                eq(DefaultGitlabClientFactory.class),
                eq(DefaultAzureDevopsClientFactory.class),
                eq(AzureDevopsValidator.class),
                any(PropertyDefinition.class),
                any(PropertyDefinition.class));

        verify(context).addExtensions(any(PropertyDefinition.class), eq(MonoRepoFeature.class));
    }

    @Test
    void shouldNotAddAnyExtensionsForScannerSideLoad() {
        final CommunityBranchPlugin testCase = new CommunityBranchPlugin();

        final CoreExtension.Context context = mock(CoreExtension.Context.class, Mockito.RETURNS_DEEP_STUBS);
        when(context.getRuntime().getSonarQubeSide()).thenReturn(SonarQubeSide.SCANNER);

        testCase.load(context);

        verify(context, never()).addExtensions(any());
    }

    @Test
    void shouldReturnPluginNameForGetName() {
        assertThat(new CommunityBranchPlugin().getName()).isEqualTo("Community Branch Plugin");
    }
}
