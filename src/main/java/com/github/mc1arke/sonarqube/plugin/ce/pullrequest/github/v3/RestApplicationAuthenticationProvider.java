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
package com.github.mc1arke.sonarqube.plugin.ce.pullrequest.github.v3;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.github.GithubApplicationAuthenticationProvider;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.github.RepositoryAuthenticationToken;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.github.v3.model.AppInstallation;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.github.v3.model.AppToken;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.github.v3.model.InstallationRepositories;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.github.v3.model.Repository;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.impl.DefaultJwtBuilder;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.URLConnection;
import java.security.PrivateKey;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

public class RestApplicationAuthenticationProvider implements GithubApplicationAuthenticationProvider {

    private static final String ACCEPT_HEADER = "Accept";
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_AUTHORIZATION_HEADER_PREFIX = "Bearer ";

    private static final String APP_PREVIEW_ACCEPT_HEADER = "application/vnd.github.machine-man-preview+json";

    private final Clock clock;
    private final UrlConnectionProvider urlProvider;

    public RestApplicationAuthenticationProvider(Clock clock) {
        this(clock, new DefaultUrlConnectionProvider());
    }

    RestApplicationAuthenticationProvider(Clock clock, UrlConnectionProvider urlProvider) {
        super();
        this.clock = clock;
        this.urlProvider = urlProvider;
    }

    @Override
    public RepositoryAuthenticationToken getInstallationToken(String apiUrl, String appId, String apiPrivateKey,
                                                              String projectPath) throws IOException {

        Instant issued = clock.instant().minus(10, ChronoUnit.SECONDS);
        Instant expiry = issued.plus(2, ChronoUnit.MINUTES);
        String jwtToken = new DefaultJwtBuilder().setIssuedAt(Date.from(issued)).setExpiration(Date.from(expiry))
                .claim("iss", appId).signWith(createPrivateKey(apiPrivateKey), SignatureAlgorithm.RS256).compact();

        ObjectMapper objectMapper = new ObjectMapper().disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

        URLConnection appConnection = urlProvider.createUrlConnection(apiUrl + "/app/installations");
        appConnection.setRequestProperty(ACCEPT_HEADER, APP_PREVIEW_ACCEPT_HEADER);
        appConnection.setRequestProperty(AUTHORIZATION_HEADER, BEARER_AUTHORIZATION_HEADER_PREFIX + jwtToken);

        AppInstallation[] appInstallations;
        try (Reader reader = new InputStreamReader(appConnection.getInputStream())) {
            appInstallations = objectMapper.readerFor(AppInstallation[].class).readValue(reader);
        }


        for (AppInstallation installation : appInstallations) {
            URLConnection accessTokenConnection = urlProvider.createUrlConnection(installation.getAccessTokensUrl());
            ((HttpURLConnection) accessTokenConnection).setRequestMethod("POST");
            accessTokenConnection.setRequestProperty(ACCEPT_HEADER, APP_PREVIEW_ACCEPT_HEADER);
            accessTokenConnection
                    .setRequestProperty(AUTHORIZATION_HEADER, BEARER_AUTHORIZATION_HEADER_PREFIX + jwtToken);


            try (Reader reader = new InputStreamReader(accessTokenConnection.getInputStream())) {
                AppToken appToken = objectMapper.readerFor(AppToken.class).readValue(reader);

                URLConnection installationRepositoriesConnection =
                        urlProvider.createUrlConnection(installation.getRepositoriesUrl());
                ((HttpURLConnection) installationRepositoriesConnection).setRequestMethod("GET");
                installationRepositoriesConnection.setRequestProperty(ACCEPT_HEADER, APP_PREVIEW_ACCEPT_HEADER);
                installationRepositoriesConnection.setRequestProperty(AUTHORIZATION_HEADER,
                                                                      BEARER_AUTHORIZATION_HEADER_PREFIX +
                                                                      appToken.getToken());
                String repositoryNodeId = null;
                try (Reader installationRepositoriesReader = new InputStreamReader(
                        installationRepositoriesConnection.getInputStream())) {
                    InstallationRepositories installationRepositories =
                            objectMapper.readerFor(InstallationRepositories.class)
                                    .readValue(installationRepositoriesReader);
                    for (Repository repository : installationRepositories.getRepositories()) {
                        if (projectPath.equals(repository.getFullName())) {
                            repositoryNodeId = repository.getNodeId();
                            break;
                        }
                    }
                    if (null == repositoryNodeId) {
                        continue;
                    }

                }

                return new RepositoryAuthenticationToken(repositoryNodeId, appToken.getToken());

            }
        }

        throw new IllegalStateException(
                "No token could be found with access to the requested repository with the given application ID and key");
    }


    private static PrivateKey createPrivateKey(String apiPrivateKey) throws IOException {
        try (PEMParser pemParser = new PEMParser(new StringReader(apiPrivateKey))) {
            return new JcaPEMKeyConverter().getPrivateKey(((PEMKeyPair) pemParser.readObject()).getPrivateKeyInfo());
        }
    }
}
