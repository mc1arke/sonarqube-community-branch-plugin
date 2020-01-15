/*
 * Copyright (C) 2020 Artemy Osipov
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
package com.github.mc1arke.sonarqube.plugin.scanner;

import com.google.common.collect.Sets;
import org.junit.Before;
import org.junit.Test;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.config.Configuration;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;


public class ScannerConfigurationLoaderSensorTest {

    private static final String PROPERTY_1 = "property1";
    private static final String PROPERTY_2 = "property2";

    private ScannerConfigurationLoaderSensor sensor = new ScannerConfigurationLoaderSensor(Sets.newHashSet(PROPERTY_1, PROPERTY_2));

    private SensorContext context = mock(SensorContext.class);
    private Configuration configuration = mock(Configuration.class);

    @Before
    public void init() {
        doReturn(configuration)
                .when(context)
                .config();
    }

    @Test
    public void shouldSaveToContextOnlySpecifiedParameters() {
        String propValue = "someValue";
        doReturn(Optional.of(propValue))
                .when(configuration)
                .get(PROPERTY_1);

        sensor.execute(context);

        verify(context).addContextProperty(PROPERTY_1, propValue);
        verify(context, never()).addContextProperty(eq(PROPERTY_2), any());
    }
}