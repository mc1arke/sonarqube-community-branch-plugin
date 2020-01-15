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
package com.github.mc1arke.sonarqube.plugin.ce.pullrequest;

import org.assertj.core.util.Maps;
import org.junit.Test;
import org.sonar.api.ce.posttask.ScannerContext;
import org.sonar.api.config.Configuration;

import java.util.HashMap;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

public class UnifyConfigurationTest {

    private static final String PROPERTY_NAME = "some.sonar.property";
    private static final String PROPERTY_VALUE = "some.sonar.value";

    private Configuration configuration = mock(Configuration.class);
    private ScannerContext scannerContext = mock(ScannerContext.class);

    private UnifyConfiguration unifyConfiguration = new UnifyConfiguration(configuration, scannerContext);

    @Test
    public void shouldReturnPropertyFromScannerContextWhenPropertySetAtScanner() {
        doReturn(Maps.newHashMap(PROPERTY_NAME, PROPERTY_VALUE))
                .when(scannerContext)
                .getProperties();

        Optional<String> property = unifyConfiguration.getProperty(PROPERTY_NAME);

        assertThat(property).hasValue(PROPERTY_VALUE);
    }

    @Test
    public void shouldReturnPropertyFromScannerContextWhenPropertySetAtScannerAndAtConfiguration() {
        doReturn(Optional.of("from configuration"))
                .when(configuration)
                .get(PROPERTY_NAME);
        doReturn(Maps.newHashMap(PROPERTY_NAME, PROPERTY_VALUE))
                .when(scannerContext)
                .getProperties();

        Optional<String> property = unifyConfiguration.getProperty(PROPERTY_NAME);

        assertThat(property).hasValue(PROPERTY_VALUE);
    }

    @Test
    public void shouldReturnPropertyFromConfigurationWhenPropertyNotSetAtScannerButSetAtConfiguration() {
        doReturn(Optional.of(PROPERTY_VALUE))
                .when(configuration)
                .get(PROPERTY_NAME);

        Optional<String> property = unifyConfiguration.getProperty(PROPERTY_NAME);

        assertThat(property).hasValue(PROPERTY_VALUE);
    }

    @Test
    public void shouldReturnPropertyFromScannerContextWhenPropertySetAsBlankStringAtConfigurationButSetAtScanner() {
        doReturn(Optional.of("  "))
                .when(configuration)
                .get(PROPERTY_NAME);
        doReturn(Maps.newHashMap(PROPERTY_NAME, PROPERTY_VALUE))
                .when(scannerContext)
                .getProperties();

        Optional<String> property = unifyConfiguration.getProperty(PROPERTY_NAME);

        assertThat(property).hasValue(PROPERTY_VALUE);
    }

    @Test
    public void shouldReturnEmptyWhenPropertyNotSet() {
        doReturn(new HashMap<>())
                .when(scannerContext)
                .getProperties();

        Optional<String> property = unifyConfiguration.getProperty(PROPERTY_NAME);

        assertThat(property).isEmpty();
    }

    @Test
    public void shouldThrowExceptionWhenRequiredPropertyNotSet() {
        doReturn(new HashMap<>())
                .when(scannerContext)
                .getProperties();

        assertThatThrownBy(
                () -> unifyConfiguration.getRequiredProperty(PROPERTY_NAME)
        ).isInstanceOf(IllegalStateException.class);
    }
}