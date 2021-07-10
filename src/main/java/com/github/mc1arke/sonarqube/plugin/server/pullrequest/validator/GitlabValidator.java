/*
 * Copyright (C) 2021 Michael Clarke
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
package com.github.mc1arke.sonarqube.plugin.server.pullrequest.validator;

import com.github.mc1arke.sonarqube.plugin.InvalidConfigurationException;
import com.github.mc1arke.sonarqube.plugin.almclient.gitlab.GitlabClient;
import com.github.mc1arke.sonarqube.plugin.almclient.gitlab.GitlabClientFactory;
import org.sonar.api.server.ServerSide;
import org.sonar.db.alm.setting.ALM;
import org.sonar.db.alm.setting.AlmSettingDto;
import org.sonar.db.alm.setting.ProjectAlmSettingDto;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

@ServerSide
public class GitlabValidator implements Validator {

    private final GitlabClientFactory gitlabClientFactory;

    public GitlabValidator(GitlabClientFactory gitlabClientFactory) {
        this.gitlabClientFactory = gitlabClientFactory;
    }

    @Override
    public void validate(ProjectAlmSettingDto projectAlmSettingDto, AlmSettingDto almSettingDto) {
        GitlabClient gitlabClient;
        try {
            gitlabClient = gitlabClientFactory.createClient(projectAlmSettingDto, almSettingDto);
        } catch (InvalidConfigurationException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            throw new InvalidConfigurationException(InvalidConfigurationException.Scope.PROJECT, "Could not create Gitlab client - " + ex.getMessage(), ex);
        }
        try {
            gitlabClient.getProject(projectAlmSettingDto.getAlmRepo());
        } catch (IOException | RuntimeException ex) {
            throw new InvalidConfigurationException(InvalidConfigurationException.Scope.PROJECT, "Project could not be retrieved from Gitlab - " + ex.getMessage(), ex);
        }
    }

    @Override
    public List<ALM> alm() {
        return Collections.singletonList(ALM.GITLAB);
    }
}
