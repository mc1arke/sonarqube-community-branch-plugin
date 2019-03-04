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
package com.github.mc1arke.sonarqube.plugin.classloader;

import org.hamcrest.core.IsEqual;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.ArgumentCaptor;
import org.sonar.api.Plugin;
import org.sonar.api.batch.rule.ActiveRule;
import org.sonar.classloader.ClassloaderBuilder;
import org.sonar.classloader.Mask;

import java.io.File;
import java.net.MalformedURLException;
import java.util.Arrays;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

/**
 * @author Michael Clarke
 */
public class ClassReferenceElevatedClassLoaderFactoryTest {

    private final ExpectedException expectedException = ExpectedException.none();

    @Rule
    public ExpectedException expectedException() {
        return expectedException;
    }

    @Test
    public void testExceptionOnNoSuchClass() {
        ClassReferenceElevatedClassLoaderFactory testCase = new ClassReferenceElevatedClassLoaderFactory("1");

        expectedException.expect(IllegalStateException.class);
        expectedException.expectMessage(IsEqual.equalTo("Could not load class '1' from Plugin Classloader"));

        testCase.createClassLoader(((Plugin) context -> {
        }).getClass());
    }

    @Test
    public void testClassloaderReturnedOnHappyPath() {
        ClassReferenceElevatedClassLoaderFactory testCase =
                spy(new ClassReferenceElevatedClassLoaderFactory(getClass().getName()));
        testCase.createClassLoader(((Plugin) context -> {
        }).getClass());

        ArgumentCaptor<ClassLoader> argumentCaptor = ArgumentCaptor.forClass(ClassLoader.class);
        verify(testCase).createClassLoader(argumentCaptor.capture(), argumentCaptor.capture());

        assertEquals(Arrays.asList(getClass().getClassLoader(), getClass().getClassLoader()),
                     argumentCaptor.getAllValues());
    }

    @Test
    public void testLoadClass() throws ClassNotFoundException, MalformedURLException {
        ClassloaderBuilder builder = new ClassloaderBuilder();
        builder.newClassloader("_api_", getClass().getClassLoader());
        builder.setMask("_api_", new Mask().addInclusion("java/").addInclusion("org/sonar/api/"));

        builder.newClassloader("_customPlugin");
        builder.setParent("_customPlugin", "_api_", new Mask());
        builder.setLoadingOrder("_customPlugin", ClassloaderBuilder.LoadingOrder.SELF_FIRST);

        File[] sonarQubeDistributions = new File("sonarqube-lib/").listFiles();

        for (File pluginJar : new File(sonarQubeDistributions[0], "extensions/plugins/").listFiles()) {
            builder.addURL("_customPlugin", pluginJar.toURI().toURL());
        }

        Map<String, ClassLoader> loaders = builder.build();
        ClassLoader classLoader = loaders.get("_customPlugin");

        Class<? extends Plugin> loadedClass =
                (Class<? extends Plugin>) classLoader.loadClass("org.sonar.plugins.scm.svn.SvnPlugin");

        ClassReferenceElevatedClassLoaderFactory testCase =
                new ClassReferenceElevatedClassLoaderFactory(ActiveRule.class.getName());
        ClassLoader elevatedLoader = testCase.createClassLoader(loadedClass);
        Class<?> elevatedClass = elevatedLoader.loadClass(loadedClass.getName());
        // Getting closer than this is going to be difficult since the URLClassLoader that actually loads is an inner class of evelvatedClassLoader
        assertNotSame(elevatedLoader, elevatedClass.getClassLoader());
    }

}
