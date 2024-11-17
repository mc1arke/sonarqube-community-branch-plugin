/*
 * Copyright (C) 2020-2024 Michael Clarke
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
package com.github.mc1arke.sonarqube.plugin.classloader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.sonar.api.Plugin;
import org.sonar.api.batch.rule.ActiveRule;
import org.sonar.classloader.ClassloaderBuilder;
import org.sonar.classloader.Mask;

class ClassReferenceElevatedClassLoaderFactoryTest {

    private static final String TARGET_PLUGIN_CLASS = "org.sonar.plugins.java.JavaPlugin";

    @Test
    void shouldThrowExceptionIfNoSuchClassExists() {
        ClassReferenceElevatedClassLoaderFactory testCase = new ClassReferenceElevatedClassLoaderFactory("1");
        Class<? extends Plugin> pluginClass = ((Plugin) context -> {}).getClass();
        assertThatThrownBy(() -> testCase.createClassLoader(pluginClass))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Could not load class '1' from Plugin Classloader")
            .hasCause(new ClassNotFoundException("1"));

    }

    @Test
    void shouldCreateClassloaderWhenProvidedValidArguments() throws ReflectiveOperationException, MalformedURLException {
        URLClassLoader mockClassLoader = new URLClassLoader(findSonarqubePluginJars());
        ElevatedClassLoaderFactory testCase = spy(new ClassReferenceElevatedClassLoaderFactory(getClass().getName()));
        testCase.createClassLoader((Class<? extends Plugin>) mockClassLoader.loadClass(TARGET_PLUGIN_CLASS));

        ArgumentCaptor<ClassLoader> argumentCaptor = ArgumentCaptor.captor();
        verify(testCase).createClassLoader(argumentCaptor.capture(), argumentCaptor.capture());

        assertThat(argumentCaptor.getAllValues()).containsExactly(mockClassLoader, getClass().getClassLoader());
    }

    @Test
    void shouldCreateClassLoaderThatCanLoadInterceptedClasses() throws ClassNotFoundException, MalformedURLException {
        ClassloaderBuilder builder = new ClassloaderBuilder();
        builder.newClassloader("_api_", getClass().getClassLoader());
        builder.setMask("_api_", Mask.builder().include("java/", "org/sonar/api/").build());

        builder.newClassloader("_customPlugin");
        builder.setParent("_customPlugin", "_api_", Mask.ALL);
        builder.setLoadingOrder("_customPlugin", ClassloaderBuilder.LoadingOrder.SELF_FIRST);

        for (URL pluginUrl : findSonarqubePluginJars()) {
            builder.addURL("_customPlugin", pluginUrl);
        }

        Map<String, ClassLoader> loaders = builder.build();
        ClassLoader classLoader = loaders.get("_customPlugin");

        Class<? extends Plugin> loadedClass = (Class<? extends Plugin>) classLoader.loadClass(TARGET_PLUGIN_CLASS);

        ClassReferenceElevatedClassLoaderFactory testCase =
                new ClassReferenceElevatedClassLoaderFactory(ActiveRule.class.getName());
        ClassLoader elevatedLoader = testCase.createClassLoader(loadedClass);
        Class<?> elevatedClass = elevatedLoader.loadClass(loadedClass.getName());
        // Getting closer than this is going to be difficult since the URLClassLoader that actually loads is an inner class of elevatedClassLoader
        assertThat(elevatedClass.getClassLoader()).isNotSameAs(elevatedLoader);
    }

    private static URL[] findSonarqubePluginJars() throws MalformedURLException {
        List<URL> pluginUrls = new ArrayList<>();
        File[] sonarQubeDistributions = new File("sonarqube-lib/").listFiles();

        for (File pluginJar : new File(sonarQubeDistributions[0], "lib/extensions/").listFiles()) {
            pluginUrls.add(pluginJar.toURI().toURL());
        }
        return pluginUrls.toArray(new URL[0]);
    }

}
