/*
 * Copyright (C) 2020-2022 Michael Clarke
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.sonar.api.Plugin;
import org.sonar.api.SonarQubeSide;
import org.sonar.core.extension.CoreExtension;

import com.github.mc1arke.sonarqube.plugin.ce.CommunityReportAnalysisComponentProvider;
import com.github.mc1arke.sonarqube.plugin.scanner.CommunityBranchConfigurationLoader;
import com.github.mc1arke.sonarqube.plugin.scanner.CommunityBranchParamsValidator;
import com.github.mc1arke.sonarqube.plugin.scanner.CommunityProjectBranchesLoader;
import com.github.mc1arke.sonarqube.plugin.scanner.ScannerPullRequestPropertySensor;
import com.github.mc1arke.sonarqube.plugin.server.CommunityBranchFeatureExtension;
import com.github.mc1arke.sonarqube.plugin.server.CommunityBranchSupportDelegate;

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

        final ArgumentCaptor<Object> argumentCaptor = ArgumentCaptor.forClass(Object.class);
        verify(context)
                .addExtensions(argumentCaptor.capture(), argumentCaptor.capture(), argumentCaptor.capture());


        assertThat(argumentCaptor.getAllValues().subList(0, 4)).isEqualTo(Arrays.asList(CommunityProjectBranchesLoader.class,
                                   CommunityBranchConfigurationLoader.class, CommunityBranchParamsValidator.class, ScannerPullRequestPropertySensor.class));
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

        final ArgumentCaptor<Class<?>> argumentCaptor = ArgumentCaptor.forClass(Class.class);
        verify(context, times(2)).addExtensions(argumentCaptor.capture(), argumentCaptor.capture());


        assertThat(argumentCaptor.getAllValues().subList(0, 1)).isEqualTo(List.of(CommunityReportAnalysisComponentProvider.class));
    }


    @Test
    void shouldAddExtensionsForServerSideLoad() {
        final CommunityBranchPlugin testCase = new CommunityBranchPlugin();

        final CoreExtension.Context context = mock(CoreExtension.Context.class, Mockito.RETURNS_DEEP_STUBS);
        when(context.getRuntime().getSonarQubeSide()).thenReturn(SonarQubeSide.SERVER);

        testCase.load(context);

        final ArgumentCaptor<Object> argumentCaptor = ArgumentCaptor.forClass(Object.class);
        verify(context, times(2)).addExtensions(argumentCaptor.capture(), argumentCaptor.capture());

        assertThat(argumentCaptor.getAllValues()).hasSize(29);

        assertThat(argumentCaptor.getAllValues().subList(0, 2)).isEqualTo(List.of(CommunityBranchFeatureExtension.class, CommunityBranchSupportDelegate.class));
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
