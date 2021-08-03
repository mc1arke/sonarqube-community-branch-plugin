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
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.core.IsEqual;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;
import org.sonar.api.Plugin;
import org.sonar.api.SonarQubeSide;
import org.sonar.api.SonarRuntime;
import org.sonar.api.config.Configuration;

import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Michael Clarke
 */
public class CommunityBranchPluginBootstrapTest {

    private final ExpectedException expectedException = ExpectedException.none();

    @Rule
    public ExpectedException expectedException() {
        return expectedException;
    }

    @Before
    public void setup() {
        MockPlugin.invokedContext = null;
    }

    @Test
    public void testDefineInvokedOnSuccessLoad() {
        Plugin.Context context = mock(Plugin.Context.class);
        Configuration configuration = mock(Configuration.class);
        when(context.getBootConfiguration()).thenReturn(configuration);
        when(configuration.get(any())).thenReturn(Optional.empty());
        SonarRuntime sonarRuntime = mock(SonarRuntime.class);
        when(context.getRuntime()).thenReturn(sonarRuntime);
        when(sonarRuntime.getSonarQubeSide()).thenReturn(SonarQubeSide.SCANNER);

        ClassLoader classLoader = new ClassLoader() {
            @Override
            public Class<?> loadClass(String name) {
                return MockPlugin.class;
            }
        };

        ElevatedClassLoaderFactory elevatedClassLoaderFactory = mock(ElevatedClassLoaderFactory.class);
        when(elevatedClassLoaderFactory.createClassLoader(any())).thenReturn(classLoader);

        ElevatedClassLoaderFactoryProvider elevatedClassLoaderFactoryProvider =
                mock(ElevatedClassLoaderFactoryProvider.class);
        when(elevatedClassLoaderFactoryProvider.createFactory(any())).thenReturn(elevatedClassLoaderFactory);

        CommunityBranchPluginBootstrap testCase =
                new CommunityBranchPluginBootstrap(elevatedClassLoaderFactoryProvider);
        testCase.define(context);

        assertEquals(context, MockPlugin.invokedContext);
    }

    @Test
    public void testDefineNotInvokedForNonScanner() {
        Plugin.Context context = mock(Plugin.Context.class);
        Configuration configuration = mock(Configuration.class);
        when(context.getBootConfiguration()).thenReturn(configuration);
        when(configuration.get(any())).thenReturn(Optional.empty());
        SonarRuntime sonarRuntime = mock(SonarRuntime.class);
        when(context.getRuntime()).thenReturn(sonarRuntime);
        when(sonarRuntime.getSonarQubeSide()).thenReturn(SonarQubeSide.SERVER);

        ClassLoader classLoader = new ClassLoader() {
            @Override
            public Class<?> loadClass(String name) {
                return MockPlugin.class;
            }
        };

        ElevatedClassLoaderFactory elevatedClassLoaderFactory = mock(ElevatedClassLoaderFactory.class);
        when(elevatedClassLoaderFactory.createClassLoader(any())).thenReturn(classLoader);

        ElevatedClassLoaderFactoryProvider elevatedClassLoaderFactoryProvider =
                mock(ElevatedClassLoaderFactoryProvider.class);
        when(elevatedClassLoaderFactoryProvider.createFactory(any())).thenReturn(elevatedClassLoaderFactory);

        CommunityBranchPluginBootstrap testCase =
                new CommunityBranchPluginBootstrap(elevatedClassLoaderFactoryProvider);
        testCase.define(context);

        assertNull(MockPlugin.invokedContext);
    }


    @Test
    public void testFailureOnIncorrectClassTypeReturned() {
        ClassLoader classLoader = new ClassLoader() {
            @Override
            public Class<?> loadClass(String name) {
                return CommunityBranchPluginBootstrapTest.class;
            }
        };

        ElevatedClassLoaderFactory classLoaderFactory = mock(ElevatedClassLoaderFactory.class);
        when(classLoaderFactory.createClassLoader(any())).thenReturn(classLoader);

        Plugin.Context context = mock(Plugin.Context.class, Mockito.RETURNS_DEEP_STUBS);
        SonarRuntime sonarRuntime = mock(SonarRuntime.class);
        when(context.getRuntime()).thenReturn(sonarRuntime);
        when(sonarRuntime.getSonarQubeSide()).thenReturn(SonarQubeSide.SCANNER);

        expectedException.expect(IllegalStateException.class);
        expectedException.expectMessage(IsEqual.equalTo(
                "Expected loaded class to be instance of 'org.sonar.api.Plugin' but was '" + getClass().getName() +
                "'"));

        ElevatedClassLoaderFactory elevatedClassLoaderFactory = mock(ElevatedClassLoaderFactory.class);
        when(elevatedClassLoaderFactory.createClassLoader(any())).thenReturn(classLoader);

        ElevatedClassLoaderFactoryProvider elevatedClassLoaderFactoryProvider =
                mock(ElevatedClassLoaderFactoryProvider.class);
        when(elevatedClassLoaderFactoryProvider.createFactory(any())).thenReturn(elevatedClassLoaderFactory);


        CommunityBranchPluginBootstrap testCase =
                new CommunityBranchPluginBootstrap(elevatedClassLoaderFactoryProvider);
        testCase.define(context);
    }

    @Test
    public void testFailureOnReflectiveOperationException() {
        ClassLoader classLoader = new ClassLoader() {
            @Override
            public Class<?> loadClass(String name) throws ClassNotFoundException {
                throw new ClassNotFoundException("Whoops");
            }
        };
        Plugin.Context context = mock(Plugin.Context.class, Mockito.RETURNS_DEEP_STUBS);
        SonarRuntime sonarRuntime = mock(SonarRuntime.class);
        when(context.getRuntime()).thenReturn(sonarRuntime);
        when(sonarRuntime.getSonarQubeSide()).thenReturn(SonarQubeSide.SCANNER);

        expectedException.expectCause(new BaseMatcher<ClassNotFoundException>() {

            @Override
            public void describeTo(Description description) {
                description.appendText("Cause matcher");
            }

            @Override
            public boolean matches(Object item) {
                if (item instanceof ClassNotFoundException) {
                    return "Whoops".equals(((ClassNotFoundException) item).getMessage());
                }
                return false;
            }
        });

        expectedException.expect(IllegalStateException.class);
        expectedException.expectMessage(IsEqual.equalTo("Could not create CommunityBranchPlugin instance"));


        ElevatedClassLoaderFactory elevatedClassLoaderFactory = mock(ElevatedClassLoaderFactory.class);
        when(elevatedClassLoaderFactory.createClassLoader(any())).thenReturn(classLoader);

        ElevatedClassLoaderFactoryProvider elevatedClassLoaderFactoryProvider =
                mock(ElevatedClassLoaderFactoryProvider.class);
        when(elevatedClassLoaderFactoryProvider.createFactory(any())).thenReturn(elevatedClassLoaderFactory);


        CommunityBranchPluginBootstrap testCase =
                new CommunityBranchPluginBootstrap(elevatedClassLoaderFactoryProvider);
        testCase.define(context);
    }

    @Test
    public void testNoArgsConstructor() {
        assertEquals(new CommunityBranchPluginBootstrap(DefaultElevatedClassLoaderFactoryProvider.getInstance()),
                     new CommunityBranchPluginBootstrap());
        assertEquals(
                new CommunityBranchPluginBootstrap(DefaultElevatedClassLoaderFactoryProvider.getInstance()).hashCode(),
                new CommunityBranchPluginBootstrap().hashCode());

    }

    @Test
    public void testEqualsSameInstance() {
        CommunityBranchPluginBootstrap testCase = new CommunityBranchPluginBootstrap();
        assertEquals(testCase, testCase);
    }

    @Test
    public void testNotEqualsUnrelatedClasses() {
        assertNotEquals(new CommunityBranchPluginBootstrap(), "");
        assertNotEquals(new CommunityBranchPluginBootstrap(), null);
    }

    @Test
    public void testDifferentHashCode() {
        assertNotEquals(new CommunityBranchPluginBootstrap(mock(ElevatedClassLoaderFactoryProvider.class)).hashCode(),
                        new CommunityBranchPluginBootstrap(mock(ElevatedClassLoaderFactoryProvider.class)).hashCode());
    }


    public static class MockPlugin implements Plugin {

        private static Context invokedContext;

        @Override
        public void define(Context context) {
            invokedContext = context;
        }
    }
}
