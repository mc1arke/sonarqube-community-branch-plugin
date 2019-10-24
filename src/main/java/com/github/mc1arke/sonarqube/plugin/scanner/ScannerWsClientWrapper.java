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
package com.github.mc1arke.sonarqube.plugin.scanner;

import org.sonar.scanner.bootstrap.ScannerWsClient;
import org.sonarqube.ws.client.WsRequest;
import org.sonarqube.ws.client.WsResponse;

import java.lang.reflect.InvocationTargetException;

/**
 * Provides a way of invoking {@link ScannerWsClient} between SonarQube versions where it changed from being a class
 * to being an interface.
 */
// Can be removed when support for SonarQube < 8.0 is removed
/*package*/ class ScannerWsClientWrapper {

    private final Object wsClient;

    /*package*/ ScannerWsClientWrapper(Object wsClient) {
        this.wsClient = wsClient;
    }

    WsResponse call(WsRequest request) {
        try {
            return (WsResponse) wsClient.getClass().getMethod("call", WsRequest.class).invoke(wsClient, request);
        } catch (ReflectiveOperationException ex) {
            handleIfInvocationException(ex);
            throw new IllegalStateException("Could not execute ScannerWsClient", ex);
        }
    }

    private static void handleIfInvocationException(ReflectiveOperationException ex) {
        if (!(ex instanceof InvocationTargetException)) {
            return;
        }
        Throwable cause = ex.getCause();
        if (cause instanceof Error) {
            throw (Error) cause;
        } else if (cause instanceof RuntimeException) {
            throw (RuntimeException) cause;
        }
    }
}
