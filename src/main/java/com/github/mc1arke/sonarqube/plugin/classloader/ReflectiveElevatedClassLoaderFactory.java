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

import java.lang.reflect.Field;
import java.security.AccessController;
import java.security.PrivilegedAction;

/**
 * Provides a facility for creating the relevant ClassLoader by using reflection to access the necessary fields in
 * org.sonar.classloader.ClassRealm and org.sonar.classloader.ClassloaderRef.
 *
 * @author Michael Clarke
 */
public class ReflectiveElevatedClassLoaderFactory implements ElevatedClassLoaderFactory {

    private static final String CLASS_REALM_NAME = "org.sonar.classloader.ClassRealm";

    @Override
    public ClassLoader createClassLoader(Class<? extends Plugin> pluginClass) {
        ClassLoader pluginClassLoader =
                AccessController.doPrivileged((PrivilegedAction<ClassLoader>) pluginClass::getClassLoader);

        if (!CLASS_REALM_NAME.equals(pluginClassLoader.getClass().getName())) {
            throw new IllegalStateException(
                    String.format("Expected classloader of type '%s' but got '%s'", CLASS_REALM_NAME,
                                  pluginClassLoader.getClass().getName()));
        }


        try {
            return createFromPluginClassLoader(pluginClassLoader);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Could not access ClassLoader chain using reflection", e);
        }
    }


    private ClassLoader createFromPluginClassLoader(ClassLoader pluginClassLoader) throws ReflectiveOperationException {
        Field parentRefField = pluginClassLoader.getClass().getDeclaredField("parentRef");
        parentRefField.setAccessible(true);
        Object classLoaderRef = parentRefField.get(pluginClassLoader);

        Field classloaderField = classLoaderRef.getClass().getDeclaredField("classloader");
        classloaderField.setAccessible(true);

        ClassLoader pluginParentClassLoader = (ClassLoader) classloaderField.get(classLoaderRef);

        if (!CLASS_REALM_NAME.equals(pluginParentClassLoader.getClass().getName())) {
            throw new IllegalStateException(
                    String.format("Expected classloader of type '%s' but got '%s'", CLASS_REALM_NAME,
                                  pluginParentClassLoader.getClass().getName()));
        }

        Field keyField = pluginParentClassLoader.getClass().getDeclaredField("key");
        keyField.setAccessible(true);
        String key = (String) keyField.get(pluginParentClassLoader);

        if ("_api_".equals(key)) {
            ClassLoader coreClassLoader = pluginParentClassLoader.getParent();
            return createClassLoader(pluginClassLoader, coreClassLoader);
        } else {
            throw new IllegalStateException(
                    String.format("Expected classloader with key '_api_' but found key '%s'", key));
        }


    }

}
