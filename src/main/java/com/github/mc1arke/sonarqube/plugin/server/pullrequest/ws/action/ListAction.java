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

import static java.util.Optional.ofNullable;

import java.util.List;
import java.util.stream.Collectors;

import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.alm.setting.AlmSettingDto;
import org.sonar.db.component.ComponentDto;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.user.UserSession;
import org.sonar.server.ws.WsUtils;
import org.sonarqube.ws.AlmSettings.AlmSetting;
import org.sonarqube.ws.AlmSettings.ListWsResponse;

import com.github.mc1arke.sonarqube.plugin.server.pullrequest.ws.AlmTypeMapper;

public class ListAction extends ProjectWsAction {

    private final DbClient dbClient;
    private  final ProtoBufWriter protoBufWriter;

    public ListAction(DbClient dbClient, UserSession userSession, ComponentFinder componentFinder) {
        this(dbClient, userSession, componentFinder, WsUtils::writeProtobuf);
    }

    public ListAction(DbClient dbClient, UserSession userSession, ComponentFinder componentFinder, ProtoBufWriter protoBufWriter) {
        super("list", dbClient, componentFinder, userSession);
        this.dbClient = dbClient;
        this.protoBufWriter = protoBufWriter;
    }

    @Override
    protected void configureAction(WebService.NewAction action) {
        //no-op
    }

    @Override
    protected void handleProjectRequest(ComponentDto project, Request request, Response response, DbSession dbSession) {
        List<AlmSettingDto> settings = dbClient.almSettingDao().selectAll(dbSession);
        List<AlmSetting> wsAlmSettings = settings.stream().map(almSetting -> {
            AlmSetting.Builder almSettingBuilder = AlmSetting.newBuilder().setKey(almSetting.getKey())
                .setAlm(AlmTypeMapper.toAlmWs(almSetting.getAlm()));
            ofNullable(almSetting.getUrl()).ifPresent(almSettingBuilder::setUrl);
            return almSettingBuilder.build();
        }).collect(Collectors.toList());
        ListWsResponse.Builder builder = ListWsResponse.newBuilder().addAllAlmSettings(wsAlmSettings);

        protoBufWriter.write(builder.build(), request, response);
    }


}
