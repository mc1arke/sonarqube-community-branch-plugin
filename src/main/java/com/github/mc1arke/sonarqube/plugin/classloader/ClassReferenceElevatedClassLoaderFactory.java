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

import org.sonar.api.Plugin;

/**
 * A {@link ElevatedClassLoaderFactory} that uses a ClassLoader from an exposed SonarQube core class as a delegate for
 * attempting to load any classes that are not found from the plugin's ClassLoader.
 *
 * @author Michael Clarke
 */
public class ClassReferenceElevatedClassLoaderFactory implements ElevatedClassLoaderFactory {

    private final String className;

    /*package*/ ClassReferenceElevatedClassLoaderFactory(String className) {
        super();
        this.className = className;
    }

    @Override
    public ClassLoader createClassLoader(Class<? extends Plugin> pluginClass) {
        Class<?> coreClass;
        try {
            coreClass = Class.forName(className);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException(
                    String.format("Could not load class '%s' from Plugin Classloader", className), e);
        }
        return createClassLoader(pluginClass.getClassLoader(), coreClass.getClassLoader());
    }

}
