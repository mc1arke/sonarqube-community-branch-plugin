/*
 * Copyright (C) 2020 Michael Clarke
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
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.sonar.api.Plugin;
import org.sonar.api.SonarQubeSide;
import org.sonar.core.extension.CoreExtension;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Michael Clarke
 */
public class CommunityBranchPluginTest {

    private final ExpectedException expectedException = ExpectedException.none();

    @Rule
    public ExpectedException expectedException() {
        return expectedException;
    }

    @Test
    public void testScannerSideDefine() {
        final CommunityBranchPlugin testCase = new CommunityBranchPlugin();

        final Plugin.Context context = mock(Plugin.Context.class, Mockito.RETURNS_DEEP_STUBS);
        when(context.getRuntime().getSonarQubeSide()).thenReturn(SonarQubeSide.SCANNER);

        testCase.define(context);

        final ArgumentCaptor<Object> argumentCaptor = ArgumentCaptor.forClass(Object.class);
        verify(context, times(1))
                .addExtensions(argumentCaptor.capture(), argumentCaptor.capture(), argumentCaptor.capture());


        assertEquals(Arrays.asList(CommunityProjectBranchesLoader.class, CommunityProjectPullRequestsLoader.class,
                                   CommunityBranchConfigurationLoader.class, CommunityBranchParamsValidator.class),
                     argumentCaptor.getAllValues().subList(0, 4));
    }

    @Test
    public void testNonScannerSideDefine() {
        final CommunityBranchPlugin testCase = new CommunityBranchPlugin();

        final Plugin.Context context = mock(Plugin.Context.class, Mockito.RETURNS_DEEP_STUBS);
        when(context.getRuntime().getSonarQubeSide()).thenReturn(SonarQubeSide.SERVER);

        testCase.define(context);

        verify(context, never()).addExtensions(any());
    }

    @Test
    public void testComputeEngineSideLoad() {
        final CommunityBranchPlugin testCase = new CommunityBranchPlugin();

        final CoreExtension.Context context = mock(CoreExtension.Context.class, Mockito.RETURNS_DEEP_STUBS);
        when(context.getRuntime().getSonarQubeSide()).thenReturn(SonarQubeSide.COMPUTE_ENGINE);

        testCase.load(context);

        final ArgumentCaptor<Class> argumentCaptor = ArgumentCaptor.forClass(Class.class);
        verify(context, times(2)).addExtensions(argumentCaptor.capture(), argumentCaptor.capture());


        assertEquals(Arrays.asList(CommunityReportAnalysisComponentProvider.class, CommunityBranchEditionProvider.class),
                     argumentCaptor.getAllValues().subList(0, 2));
    }


    @Test
    public void testServerSideLoad() {
        final CommunityBranchPlugin testCase = new CommunityBranchPlugin();

        final CoreExtension.Context context = spy(mock(CoreExtension.Context.class, Mockito.RETURNS_DEEP_STUBS));
        when(context.getRuntime().getSonarQubeSide()).thenReturn(SonarQubeSide.SERVER);

        testCase.load(context);

        final ArgumentCaptor<Object> argumentCaptor = ArgumentCaptor.forClass(Object.class);
        verify(context, times(2)).addExtensions(argumentCaptor.capture(), argumentCaptor.capture());

        assertEquals(24, argumentCaptor.getAllValues().size());

        assertEquals(Arrays.asList(CommunityBranchFeatureExtension.class, CommunityBranchSupportDelegate.class),
                     argumentCaptor.getAllValues().subList(0, 2));
    }

    @Test
    public void testLoad() {
        final CommunityBranchPlugin testCase = new CommunityBranchPlugin();

        final CoreExtension.Context context = mock(CoreExtension.Context.class, Mockito.RETURNS_DEEP_STUBS);
        when(context.getRuntime().getSonarQubeSide()).thenReturn(SonarQubeSide.SCANNER);

        testCase.load(context);

        verify(context, never()).addExtensions(any());
    }

    @Test
    public void testGetName() {
        assertEquals("Community Branch Plugin", new CommunityBranchPlugin().getName());
    }
}
