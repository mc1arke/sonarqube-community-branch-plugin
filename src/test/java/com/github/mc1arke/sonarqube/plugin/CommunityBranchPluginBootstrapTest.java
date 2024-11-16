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

import com.github.mc1arke.sonarqube.plugin.classloader.DefaultElevatedClassLoaderFactoryProvider;
import com.github.mc1arke.sonarqube.plugin.classloader.ElevatedClassLoaderFactory;
import com.github.mc1arke.sonarqube.plugin.classloader.ElevatedClassLoaderFactoryProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mockito;
import org.sonar.api.Plugin;
import org.sonar.api.SonarQubeSide;
import org.sonar.api.SonarRuntime;
import org.sonar.api.config.Configuration;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class CommunityBranchPluginBootstrapTest {

    @BeforeEach
    void setup() {
        MockPlugin.invokedContext = null;
    }

    @Test
    void shouldInvokeDefineOnSuccessfulLoad() {
        Plugin.Context context = mock();
        Configuration configuration = mock();
        when(context.getBootConfiguration()).thenReturn(configuration);
        when(configuration.get(any())).thenReturn(Optional.empty());
        SonarRuntime sonarRuntime = mock();
        when(context.getRuntime()).thenReturn(sonarRuntime);
        when(sonarRuntime.getSonarQubeSide()).thenReturn(SonarQubeSide.SCANNER);

        ClassLoader classLoader = new ClassLoader() {
            @Override
            public Class<?> loadClass(String name) {
                return MockPlugin.class;
            }
        };

        ElevatedClassLoaderFactory elevatedClassLoaderFactory = mock();
        when(elevatedClassLoaderFactory.createClassLoader(any())).thenReturn(classLoader);

        ElevatedClassLoaderFactoryProvider elevatedClassLoaderFactoryProvider = mock();
        when(elevatedClassLoaderFactoryProvider.createFactory(any())).thenReturn(elevatedClassLoaderFactory);

        CommunityBranchPluginBootstrap testCase =
                new CommunityBranchPluginBootstrap(elevatedClassLoaderFactoryProvider, true);
        testCase.define(context);

        assertThat(MockPlugin.invokedContext).isSameAs(context);
    }

    @Test
    void shouldNotInvokeDefineOnNonScannerScope() {
        Plugin.Context context = mock();
        Configuration configuration = mock();
        when(context.getBootConfiguration()).thenReturn(configuration);
        when(configuration.get(any())).thenReturn(Optional.empty());
        SonarRuntime sonarRuntime = mock();
        when(context.getRuntime()).thenReturn(sonarRuntime);
        when(sonarRuntime.getSonarQubeSide()).thenReturn(SonarQubeSide.SERVER);

        ClassLoader classLoader = new ClassLoader() {
            @Override
            public Class<?> loadClass(String name) {
                return MockPlugin.class;
            }
        };

        ElevatedClassLoaderFactory elevatedClassLoaderFactory = mock();
        when(elevatedClassLoaderFactory.createClassLoader(any())).thenReturn(classLoader);

        ElevatedClassLoaderFactoryProvider elevatedClassLoaderFactoryProvider = mock();
        when(elevatedClassLoaderFactoryProvider.createFactory(any())).thenReturn(elevatedClassLoaderFactory);

        CommunityBranchPluginBootstrap testCase =
                new CommunityBranchPluginBootstrap(elevatedClassLoaderFactoryProvider, true);
        testCase.define(context);

        assertThat(MockPlugin.invokedContext).isNull();
    }


    @Test
    void shouldThrowExceptionIfIncorrectClassReturned() {
        ClassLoader classLoader = new ClassLoader() {
            @Override
            public Class<?> loadClass(String name) {
                return CommunityBranchPluginBootstrapTest.class;
            }
        };

        ElevatedClassLoaderFactory classLoaderFactory = mock();
        when(classLoaderFactory.createClassLoader(any())).thenReturn(classLoader);

        Plugin.Context context = mock(Plugin.Context.class, Mockito.RETURNS_DEEP_STUBS);
        SonarRuntime sonarRuntime = mock();
        when(context.getRuntime()).thenReturn(sonarRuntime);
        when(sonarRuntime.getSonarQubeSide()).thenReturn(SonarQubeSide.SCANNER);

        ElevatedClassLoaderFactory elevatedClassLoaderFactory = mock();
        when(elevatedClassLoaderFactory.createClassLoader(any())).thenReturn(classLoader);

        ElevatedClassLoaderFactoryProvider elevatedClassLoaderFactoryProvider =
                mock();
        when(elevatedClassLoaderFactoryProvider.createFactory(any())).thenReturn(elevatedClassLoaderFactory);


        CommunityBranchPluginBootstrap testCase =
                new CommunityBranchPluginBootstrap(elevatedClassLoaderFactoryProvider, true);

        assertThatThrownBy(() -> testCase.define(context))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Expected loaded class to be instance of 'org.sonar.api.Plugin' but was 'com.github.mc1arke.sonarqube.plugin.CommunityBranchPluginBootstrapTest'")
            .hasNoCause();
    }

    @Test
    void shouldThrowExceptionOnReflectionFailure() {
        ClassLoader classLoader = new ClassLoader() {
            @Override
            public Class<?> loadClass(String name) throws ClassNotFoundException {
                throw new ClassNotFoundException("Whoops");
            }
        };
        Plugin.Context context = mock(Plugin.Context.class, Mockito.RETURNS_DEEP_STUBS);
        SonarRuntime sonarRuntime = mock();
        when(context.getRuntime()).thenReturn(sonarRuntime);
        when(sonarRuntime.getSonarQubeSide()).thenReturn(SonarQubeSide.SCANNER);


        ElevatedClassLoaderFactory elevatedClassLoaderFactory = mock();
        when(elevatedClassLoaderFactory.createClassLoader(any())).thenReturn(classLoader);

        ElevatedClassLoaderFactoryProvider elevatedClassLoaderFactoryProvider = mock();
        when(elevatedClassLoaderFactoryProvider.createFactory(any())).thenReturn(elevatedClassLoaderFactory);


        CommunityBranchPluginBootstrap testCase =
                new CommunityBranchPluginBootstrap(elevatedClassLoaderFactoryProvider, true);

        assertThatThrownBy(() -> testCase.define(context))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Could not create CommunityBranchPlugin instance")
            .hasCause(new ClassNotFoundException("Whoops"));
    }

    @EnumSource(value = SonarQubeSide.class, mode = EnumSource.Mode.EXCLUDE, names ="SCANNER")
    @ParameterizedTest
    void shouldThrowExceptionWhenPluginNotMarkedAsAvailableInScope(SonarQubeSide sonarQubeSide) {
        Plugin.Context context = mock();
        SonarRuntime sonarRuntime = mock();
        when(context.getRuntime()).thenReturn(sonarRuntime);
        when(sonarRuntime.getSonarQubeSide()).thenReturn(sonarQubeSide);
        ElevatedClassLoaderFactoryProvider elevatedClassLoaderFactoryProvider = mock();

        CommunityBranchPluginBootstrap underTest = new CommunityBranchPluginBootstrap(elevatedClassLoaderFactoryProvider, false);

        assertThatThrownBy(() -> underTest.define(context))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("The plugin did not detect agent modifications so SonarQube is unlikely to work with Pull Requests or Branches. Please check the Java Agent has been correctly set for the " + sonarQubeSide + " component")
            .hasNoCause();

        verify(context).getRuntime();
        verify(sonarRuntime).getSonarQubeSide();
        verifyNoMoreInteractions(context);
        verifyNoMoreInteractions(sonarRuntime);
        verifyNoInteractions(elevatedClassLoaderFactoryProvider);
    }


    @EnumSource(value = SonarQubeSide.class, mode = EnumSource.Mode.EXCLUDE, names ="SCANNER")
    @ParameterizedTest
    void shouldNotThrowExceptionWhenPluginMarkedAsAvailableInScope(SonarQubeSide sonarQubeSide) {
        Plugin.Context context = mock();
        SonarRuntime sonarRuntime = mock();
        when(context.getRuntime()).thenReturn(sonarRuntime);
        when(sonarRuntime.getSonarQubeSide()).thenReturn(sonarQubeSide);
        ElevatedClassLoaderFactoryProvider elevatedClassLoaderFactoryProvider = mock();

        CommunityBranchPluginBootstrap underTest = new CommunityBranchPluginBootstrap(elevatedClassLoaderFactoryProvider, true);

        underTest.define(context);

        verify(context).getRuntime();
        verify(sonarRuntime).getSonarQubeSide();
        verifyNoMoreInteractions(context);
        verifyNoMoreInteractions(sonarRuntime);
        verifyNoInteractions(elevatedClassLoaderFactoryProvider);
    }

    @Test
    void testNoArgsConstructor() {
        assertEquals(new CommunityBranchPluginBootstrap(DefaultElevatedClassLoaderFactoryProvider.getInstance(), false),
                     new CommunityBranchPluginBootstrap());
        assertEquals(
                new CommunityBranchPluginBootstrap(DefaultElevatedClassLoaderFactoryProvider.getInstance(), false).hashCode(),
                new CommunityBranchPluginBootstrap().hashCode());

    }

    @Test
    void testDifferentHashCode() {
        assertNotEquals(new CommunityBranchPluginBootstrap(mock(), false).hashCode(),
                        new CommunityBranchPluginBootstrap(mock(), true).hashCode());
    }


    public static class MockPlugin implements Plugin {

        private static Context invokedContext;

        @Override
        public void define(Context context) {
            invokedContext = context;
        }
    }
}
