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
package com.github.mc1arke.sonarqube.plugin.almclient.github.v3.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class AppInstallation {

    private final String repositoriesUrl;
    private final String accessTokensUrl;

    @JsonCreator
    public AppInstallation(@JsonProperty("repositories_url") String repositoriesUrl,
                           @JsonProperty("access_tokens_url") String accessTokensUrl) {
        super();
        this.repositoriesUrl = repositoriesUrl;
        this.accessTokensUrl = accessTokensUrl;
    }

    public String getRepositoriesUrl() {
        return repositoriesUrl;
    }

    public String getAccessTokensUrl() {
        return accessTokensUrl;
    }
}
