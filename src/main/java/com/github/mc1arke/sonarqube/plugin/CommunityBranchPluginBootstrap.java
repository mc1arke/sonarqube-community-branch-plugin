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
import org.sonar.api.Plugin;
import org.sonar.api.SonarQubeSide;

import java.util.Objects;


/**
 * The entry-point class used by SonarQube to launch this plugin. Since SonarQube runs all its plugin in isolated
 * ClassLoaders with limited access to SonarQube's core classes, this class works its way through the ClassLoader
 * chain used to load this class, and finds the 'API' ClassLoader used by SonarQube to filter out 'non-core' classes.
 * {@link DefaultElevatedClassLoaderFactoryProvider} is used to create a {@link ElevatedClassLoaderFactory} which generates a
 * suitable ClassLoader with access to this plugin's classes and SonarQube's core classes. This resulting ClassLoader is
 * used to load the class {@link CommunityBranchPlugin}, thereby allowing any instance generated from this class, and
 * any dependencies of this class to have access to classes from SonarQube core.
 *
 * @author Michael Clarke
 * @see DefaultElevatedClassLoaderFactoryProvider
 * @see ElevatedClassLoaderFactory
 * @see CommunityBranchPlugin
 */
public class CommunityBranchPluginBootstrap implements Plugin {

    private final ElevatedClassLoaderFactoryProvider elevatedClassLoaderFactoryProvider;

    public CommunityBranchPluginBootstrap() {
        this(DefaultElevatedClassLoaderFactoryProvider.getInstance());
    }

    /*package*/ CommunityBranchPluginBootstrap(ElevatedClassLoaderFactoryProvider elevatedClassLoaderFactoryProvider) {
        super();
        this.elevatedClassLoaderFactoryProvider = elevatedClassLoaderFactoryProvider;
    }

    @Override
    public void define(Context context) {
        if (SonarQubeSide.SCANNER != context.getRuntime().getSonarQubeSide()) {
            return;
        }
        try {
            ClassLoader classLoader =
                    elevatedClassLoaderFactoryProvider.createFactory(context).createClassLoader(getClass());
            Class<?> targetClass = classLoader.loadClass(getClass().getName().replace("Bootstrap", ""));
            Object instance = targetClass.getDeclaredConstructor().newInstance();
            if (!(instance instanceof Plugin)) {
                throw new IllegalStateException(
                        String.format("Expected loaded class to be instance of '%s' but was '%s'",
                                      Plugin.class.getName(), instance.getClass().getName()));
            }
            ((Plugin) instance).define(context);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException("Could not create CommunityBranchPlugin instance", ex);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        CommunityBranchPluginBootstrap that = (CommunityBranchPluginBootstrap) o;
        return Objects.equals(elevatedClassLoaderFactoryProvider, that.elevatedClassLoaderFactoryProvider);
    }

    @Override
    public int hashCode() {
        return Objects.hash(elevatedClassLoaderFactoryProvider);
    }
}
