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
package com.github.mc1arke.sonarqube.plugin.classloader;

import org.hamcrest.core.IsEqual;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.Plugin;
import org.sonar.classloader.ClassloaderBuilder;
import org.sonar.classloader.Mask;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Map;

import static org.junit.Assert.assertNotSame;

/**
 * @author Michael Clarke
 */
public class ReflectiveElevatedClassLoaderFactoryTest {

    private static final String TARGET_PLUGIN_CLASS = "org.sonar.plugins.java.JavaPlugin";
    private static final String BUNDLED_PLUGINS_DIRECTORY = "lib/extensions";
    private static final String SONARQUBE_LIB_DIRECTORY = "sonarqube-lib/";

    private final ExpectedException expectedException = ExpectedException.none();

    @Rule
    public ExpectedException expectedException() {
        return expectedException;
    }

    @Test
    public void testLoadClass() throws ClassNotFoundException, MalformedURLException {
        ClassloaderBuilder builder = new ClassloaderBuilder();
        builder.newClassloader("_api_", getClass().getClassLoader());
        builder.setMask("_api_", Mask.builder().include("java/", "org/sonar/api/").build());

        builder.newClassloader("_customPlugin");
        builder.setParent("_customPlugin", "_api_", Mask.ALL);
        builder.setLoadingOrder("_customPlugin", ClassloaderBuilder.LoadingOrder.SELF_FIRST);

        File[] sonarQubeDistributions = new File(SONARQUBE_LIB_DIRECTORY).listFiles();

        for (File pluginJar : new File(sonarQubeDistributions[0], BUNDLED_PLUGINS_DIRECTORY).listFiles()) {
            builder.addURL("_customPlugin", pluginJar.toURI().toURL());
        }

        Map<String, ClassLoader> loaders = builder.build();
        ClassLoader classLoader = loaders.get("_customPlugin");

        Class<? extends Plugin> loadedClass =
                (Class<? extends Plugin>) classLoader.loadClass(TARGET_PLUGIN_CLASS);

        ReflectiveElevatedClassLoaderFactory testCase = new ReflectiveElevatedClassLoaderFactory();
        ClassLoader elevatedLoader = testCase.createClassLoader(loadedClass);
        Class<?> elevatedClass = elevatedLoader.loadClass(loadedClass.getName());
        // Getting closer than this is going to be difficult since the URLClassLoader that actually loads is an inner class of evelvatedClassLoader
        assertNotSame(elevatedLoader, elevatedClass.getClassLoader());
    }


    @Test
    public void testLoadClassInvalidClassRealmKey() throws ClassNotFoundException, MalformedURLException {
        ClassloaderBuilder builder = new ClassloaderBuilder();
        builder.newClassloader("_xxx_", getClass().getClassLoader());
        builder.setMask("_xxx_", Mask.builder().include("java/", "org/sonar/api/").build());

        builder.newClassloader("_customPlugin");
        builder.setParent("_customPlugin", "_xxx_", Mask.ALL);
        builder.setLoadingOrder("_customPlugin", ClassloaderBuilder.LoadingOrder.SELF_FIRST);

        File[] sonarQubeDistributions = new File(SONARQUBE_LIB_DIRECTORY).listFiles();

        for (File pluginJar : new File(sonarQubeDistributions[0], BUNDLED_PLUGINS_DIRECTORY).listFiles()) {
            builder.addURL("_customPlugin", pluginJar.toURI().toURL());
        }

        Map<String, ClassLoader> loaders = builder.build();
        ClassLoader classLoader = loaders.get("_customPlugin");

        Class<? extends Plugin> loadedClass =
                (Class<? extends Plugin>) classLoader.loadClass(TARGET_PLUGIN_CLASS);

        expectedException.expect(IllegalStateException.class);
        expectedException.expectMessage(IsEqual.equalTo("Expected classloader with key '_api_' but found key '_xxx_'"));

        ReflectiveElevatedClassLoaderFactory testCase = new ReflectiveElevatedClassLoaderFactory();
        testCase.createClassLoader(loadedClass);

    }


    @Test
    public void testLoadClassNoParentRef() throws ClassNotFoundException, MalformedURLException {
        ClassloaderBuilder builder = new ClassloaderBuilder();
        builder.newClassloader("_xxx_", getClass().getClassLoader());
        builder.setMask("_xxx_", Mask.ALL);

        File[] sonarQubeDistributions = new File(SONARQUBE_LIB_DIRECTORY).listFiles();

        for (File pluginJar : new File(sonarQubeDistributions[0], BUNDLED_PLUGINS_DIRECTORY).listFiles()) {
            builder.addURL("_xxx_", pluginJar.toURI().toURL());
        }

        Map<String, ClassLoader> loaders = builder.build();
        ClassLoader classLoader = loaders.get("_xxx_");

        Class<? extends Plugin> loadedClass =
                (Class<? extends Plugin>) classLoader.loadClass(TARGET_PLUGIN_CLASS);

        expectedException.expect(IllegalStateException.class);
        expectedException.expectMessage(IsEqual.equalTo("Could not access ClassLoader chain using reflection"));

        ReflectiveElevatedClassLoaderFactory testCase = new ReflectiveElevatedClassLoaderFactory();
        testCase.createClassLoader(loadedClass);

    }

    @Test
    public void testLoadClassInvalidApiClassloader() throws ClassNotFoundException, MalformedURLException {
        ClassloaderBuilder builder = new ClassloaderBuilder();
        builder.newClassloader("_customPlugin");
        builder.setParent("_customPlugin", new URLClassLoader(new URL[0]), Mask.ALL);
        builder.setLoadingOrder("_customPlugin", ClassloaderBuilder.LoadingOrder.SELF_FIRST);

        File[] sonarQubeDistributions = new File(SONARQUBE_LIB_DIRECTORY).listFiles();

        for (File pluginJar : new File(sonarQubeDistributions[0], BUNDLED_PLUGINS_DIRECTORY).listFiles()) {
            builder.addURL("_customPlugin", pluginJar.toURI().toURL());
        }

        Map<String, ClassLoader> loaders = builder.build();
        ClassLoader classLoader = loaders.get("_customPlugin");

        Class<? extends Plugin> loadedClass =
                (Class<? extends Plugin>) classLoader.loadClass(TARGET_PLUGIN_CLASS);

        expectedException.expect(IllegalStateException.class);
        expectedException.expectMessage(IsEqual.equalTo(
                "Expected classloader of type 'org.sonar.classloader.ClassRealm' but got 'java.net.URLClassLoader'"));

        ReflectiveElevatedClassLoaderFactory testCase = new ReflectiveElevatedClassLoaderFactory();
        testCase.createClassLoader(loadedClass);

    }

    @Test
    public void testLoadClassInvalidClassloader() throws ClassNotFoundException, MalformedURLException {

        File[] sonarQubeDistributions = new File(SONARQUBE_LIB_DIRECTORY).listFiles();
        File[] plugins = new File(sonarQubeDistributions[0], BUNDLED_PLUGINS_DIRECTORY).listFiles();

        URL[] urls = new URL[plugins.length];
        int i = 0;
        for (File pluginJar : plugins) {
            urls[i++] = pluginJar.toURI().toURL();
        }

        ClassLoader classLoader = new URLClassLoader(urls);

        Class<? extends Plugin> loadedClass =
                (Class<? extends Plugin>) classLoader.loadClass(TARGET_PLUGIN_CLASS);

        expectedException.expect(IllegalStateException.class);
        expectedException.expectMessage(IsEqual.equalTo(
                "Expected classloader of type 'org.sonar.classloader.ClassRealm' but got 'java.net.URLClassLoader'"));

        ReflectiveElevatedClassLoaderFactory testCase = new ReflectiveElevatedClassLoaderFactory();
        testCase.createClassLoader(loadedClass);

    }

}
