/*
 * Copyright (C) 2020 Marvin Wichmann
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
package com.github.mc1arke.sonarqube.plugin.ce.pullrequest.bitbucket.client.model;

import java.util.Locale;

public class BitbucketConfiguration {
    private final String url;
    private final String token;
    private final String repository;
    private final String project;

    public BitbucketConfiguration(String url, String token, String repository, String project) {
        this.url = url;
        this.token = token;
        this.repository = repository;
        this.project = project;
    }

    public String getRepository() {
        return repository;
    }

    public String getToken() {
        return token;
    }

    public String getUrl() {
        return url;
    }

    public String getProject() {
        return project;
    }

    public boolean isCloud() {
        return url.toLowerCase(Locale.ENGLISH).startsWith("https://api.bitbucket.org");
    }
}
