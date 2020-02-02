/*
 * Copyright (C) 2020 Michael Clarke
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
package com.github.mc1arke.sonarqube.plugin.server.pullrequest.ws;

import org.sonar.db.alm.setting.ALM;
import org.sonarqube.ws.AlmSettings;

public final class AlmTypeMapper {

    private AlmTypeMapper() {
        super();
    }

    public static AlmSettings.Alm toAlmWs(ALM alm) {
        switch (alm) {
            case AZURE_DEVOPS:
                return AlmSettings.Alm.azure;
            case BITBUCKET:
                return AlmSettings.Alm.bitbucket;
            case GITHUB:
                return AlmSettings.Alm.github;
            case GITLAB:
                return AlmSettings.Alm.gitlab;
            default:
                throw new IllegalStateException(String.format("Unknown ALM '%s'", alm.name()));
        }
    }
}
