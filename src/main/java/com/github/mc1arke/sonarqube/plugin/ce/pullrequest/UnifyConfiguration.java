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

import org.sonar.api.ce.posttask.ScannerContext;
import org.sonar.api.config.Configuration;

import java.util.Optional;

public class UnifyConfiguration {

    private final Configuration serverConfiguration;
    private final ScannerContext scannerContext;

    public UnifyConfiguration(Configuration serverConfiguration, ScannerContext scannerContext) {
        this.serverConfiguration = serverConfiguration;
        this.scannerContext = scannerContext;
    }

    public Optional<String> getProperty(String propertyName) {
        if (scannerContext.getProperties().containsKey(propertyName)) {
            return Optional.of(scannerContext.getProperties().get(propertyName));
        }

        return getServerProperty(propertyName);
    }

    public String getRequiredProperty(String propertyName) {
        return getProperty(propertyName).orElseThrow(() ->
                new IllegalStateException(propertyName + " must be specified in the project configuration or as scanner parameter")
        );
    }

    public Optional<String> getServerProperty(String propertyName) {
        return serverConfiguration.get(propertyName);
    }

    public String getRequiredServerProperty(String propertyName) {
        return getServerProperty(propertyName).orElseThrow(() ->
                new IllegalStateException(propertyName + " must be specified in the project configuration")
        );
    }
}
