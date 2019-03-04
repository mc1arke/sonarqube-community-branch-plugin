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
import org.sonar.api.rules.ActiveRule;

import java.util.Arrays;

/*package*/ enum ProviderType {
    CLASS_REFERENCE {
        @Override
        ElevatedClassLoaderFactory createFactory(Plugin.Context context) {
            return new ClassReferenceElevatedClassLoaderFactory(context.getBootConfiguration()
                                                                        .get(ElevatedClassLoaderFactoryProvider.class
                                                                                     .getName() + ".targetType").orElse(ActiveRule.class.getName()));
        }
    },

    REFLECTIVE {
        @Override
        ElevatedClassLoaderFactory createFactory(Plugin.Context context) {
            return new ReflectiveElevatedClassLoaderFactory();
        }
    };

    abstract ElevatedClassLoaderFactory createFactory(Plugin.Context context);

    /*package*/
    static ProviderType fromName(String name) {
        return Arrays.stream(values()).filter(v -> v.name().equals(name)).findFirst().orElseThrow(
                () -> new IllegalStateException(String.format("No provider with type '%s' could be found", name)));
    }

}
