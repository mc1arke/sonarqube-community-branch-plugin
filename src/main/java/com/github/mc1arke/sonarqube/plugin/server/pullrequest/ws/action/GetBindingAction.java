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

import com.github.mc1arke.sonarqube.plugin.server.pullrequest.ws.AlmTypeMapper;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.alm.setting.AlmSettingDto;
import org.sonar.db.alm.setting.ProjectAlmSettingDto;
import org.sonar.db.project.ProjectDto;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.user.UserSession;
import org.sonar.server.ws.WsUtils;
import org.sonarqube.ws.AlmSettings.GetBindingWsResponse;

import java.util.Optional;

import static java.lang.String.format;

public class GetBindingAction extends ProjectWsAction {

    private final DbClient dbClient;
    private final ProtoBufWriter protoBufWriter;

    public GetBindingAction(DbClient dbClient, ComponentFinder componentFinder, UserSession userSession) {
        this(dbClient, componentFinder, userSession, WsUtils::writeProtobuf);
    }

    GetBindingAction(DbClient dbClient, ComponentFinder componentFinder, UserSession userSession, ProtoBufWriter protoBufWriter) {
        super("get_binding", dbClient, componentFinder, userSession, true);
        this.dbClient = dbClient;
        this.protoBufWriter = protoBufWriter;
    }

    @Override
    protected void configureAction(WebService.NewAction action) {
        //no-op
    }

    @Override
    protected void handleProjectRequest(ProjectDto project, Request request, Response response, DbSession dbSession) {
        ProjectAlmSettingDto projectAlmSetting = dbClient.projectAlmSettingDao().selectByProject(dbSession, project)
            .orElseThrow(() -> new NotFoundException(
                format("Project '%s' is not bound to any ALM", project.getKey())));
        AlmSettingDto almSetting =
            dbClient.almSettingDao().selectByUuid(dbSession, projectAlmSetting.getAlmSettingUuid()).orElseThrow(
                () -> new IllegalStateException(
                    format("ALM setting '%s' cannot be found", projectAlmSetting.getAlmSettingUuid())));

        GetBindingWsResponse.Builder builder =
            GetBindingWsResponse.newBuilder().setAlm(AlmTypeMapper.toAlmWs(almSetting.getAlm()))
                .setKey(almSetting.getKey());
        Optional.ofNullable(projectAlmSetting.getAlmRepo()).ifPresent(builder::setRepository);
        Optional.ofNullable(almSetting.getUrl()).ifPresent(builder::setUrl);
        Optional.ofNullable(projectAlmSetting.getAlmSlug()).ifPresent(builder::setSlug);
        protoBufWriter.write(builder.build(), request, response);
    }

}
