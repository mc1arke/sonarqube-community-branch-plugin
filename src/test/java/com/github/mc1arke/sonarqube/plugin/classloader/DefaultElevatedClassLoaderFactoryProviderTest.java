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

import org.junit.jupiter.api.Test;
import org.sonar.api.Plugin;
import org.sonar.api.config.Configuration;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DefaultElevatedClassLoaderFactoryProviderTest {

    @Test
    void shouldCreateValidFactoryWhenNoPropertiesSet() {
        Plugin.Context context = mock();
        Configuration configuration = mock();
        when(context.getBootConfiguration()).thenReturn(configuration);
        when(configuration.get(any())).thenReturn(Optional.empty());

        assertThat(DefaultElevatedClassLoaderFactoryProvider.getInstance()
                           .createFactory(context)).isInstanceOf(ClassReferenceElevatedClassLoaderFactory.class);
    }

}
