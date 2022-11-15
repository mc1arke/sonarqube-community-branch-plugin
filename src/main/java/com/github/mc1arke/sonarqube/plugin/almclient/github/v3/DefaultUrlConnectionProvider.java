/*
 * Copyright (C) 2019-2022 Michael Clarke
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
package com.github.mc1arke.sonarqube.plugin.almclient.github.v3;

import org.sonar.api.ce.ComputeEngineSide;
import org.sonar.api.server.ServerSide;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;

@ComputeEngineSide
@ServerSide
public final class DefaultUrlConnectionProvider implements UrlConnectionProvider {

    @Override
    public URLConnection createUrlConnection(String url) throws IOException {
        return new URL(url).openConnection();
    }

}
