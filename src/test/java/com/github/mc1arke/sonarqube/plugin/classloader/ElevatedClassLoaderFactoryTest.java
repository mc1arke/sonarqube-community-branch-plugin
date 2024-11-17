/*
 * Copyright (C) 2019-2024 Michael Clarke
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

import org.junit.jupiter.api.Test;
import org.sonar.api.Plugin;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;


class ElevatedClassLoaderFactoryTest {

    @Test
    void shouldThrowExceptionOnInvalidDelegateLoader() {
        ElevatedClassLoaderFactory testCase = new ElevatedClassLoaderFactoryImpl();

        ClassLoader classLoaderImpl = new ClassLoader() {
        };

        assertThatThrownBy(() -> testCase.createClassLoader(classLoaderImpl, classLoaderImpl))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Incorrect ClassLoader type. Expected 'java.net.URLClassLoader' but got '" + classLoaderImpl.getClass().getName() + "'")
            .hasNoCause();
    }

    @Test
    void shouldDelegateToParentIfTargetClassNotFound() throws IOException, ClassNotFoundException {
        ElevatedClassLoaderFactory testCase = new ElevatedClassLoaderFactoryImpl();

        try (URLClassLoader classLoader1 = new URLClassLoader(new URL[]{}) {
            @Override
            public Class<?> loadClass(String name) throws ClassNotFoundException {
                if ("1".equals(name)) {
                    throw new ClassNotFoundException("Not here");
                }
                return this.getClass();
            }
        }; URLClassLoader classLoader2 = new URLClassLoader(new URL[]{}) {
            @Override
            public Class<?> loadClass(String name) throws ClassNotFoundException {
                if ("2".equals(name)) {
                    throw new ClassNotFoundException("Whoops");
                }
                return this.getClass();
            }
        }) {
            ClassLoader createdClassLoader = testCase.createClassLoader(classLoader2, classLoader1);
            assertThat(createdClassLoader.loadClass("1")).isEqualTo(classLoader2.getClass());

            assertThatThrownBy(() -> createdClassLoader.loadClass("2"))
                .isInstanceOf(ClassNotFoundException.class)
                .hasMessage("Whoops")
                .hasNoCause();
        }
    }


    private static class ElevatedClassLoaderFactoryImpl implements ElevatedClassLoaderFactory {

        @Override
        public ClassLoader createClassLoader(Class<? extends Plugin> pluginClass) {
            return null;
        }
    }
}
