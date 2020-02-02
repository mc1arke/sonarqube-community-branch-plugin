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
package com.github.mc1arke.sonarqube.plugin.server.pullrequest.ws.action;

import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.alm.setting.AlmSettingDto;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.ws.WsAction;

import static java.lang.String.format;

public abstract class AlmSettingsWsAction implements WsAction {

    private final DbClient dbClient;

    protected AlmSettingsWsAction(DbClient dbClient) {
        super();
        this.dbClient = dbClient;
    }

    protected AlmSettingDto getAlmSetting(DbSession dbSession, String almSetting) {
        return dbClient.almSettingDao().selectByKey(dbSession, almSetting)
                .orElseThrow(() -> new NotFoundException(format("ALM setting '%s' could not be found", almSetting)));
    }

    protected void checkAlmSettingDoesNotAlreadyExist(DbSession dbSession, String almSetting) {
        dbClient.almSettingDao().selectByKey(dbSession, almSetting).ifPresent(a -> {
            throw new IllegalArgumentException(format("ALM setting '%s' already exists", a.getKey()));
        });
    }
}
