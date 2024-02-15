/*
 * Copyright (C) 2020-2023 Michael Clarke
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

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.mc1arke.sonarqube.plugin.InvalidConfigurationException;
import com.github.mc1arke.sonarqube.plugin.almclient.LinkHeaderReader;
import com.github.mc1arke.sonarqube.plugin.almclient.github.GithubApplicationAuthenticationProvider;
import com.github.mc1arke.sonarqube.plugin.almclient.github.RepositoryAuthenticationToken;
import com.github.mc1arke.sonarqube.plugin.almclient.github.v3.model.AppInstallation;
import com.github.mc1arke.sonarqube.plugin.almclient.github.v3.model.AppToken;
import com.github.mc1arke.sonarqube.plugin.almclient.github.v3.model.InstallationRepositories;
import com.github.mc1arke.sonarqube.plugin.almclient.github.v3.model.Repository;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.impl.DefaultJwtBuilder;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.sonar.api.ce.ComputeEngineSide;
import org.sonar.api.server.ServerSide;

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
import java.util.Optional;

@ServerSide
@ComputeEngineSide
public class RestApplicationAuthenticationProvider implements GithubApplicationAuthenticationProvider {

    private static final String ACCEPT_HEADER = "Accept";
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_AUTHORIZATION_HEADER_PREFIX = "Bearer ";

    private static final String APP_PREVIEW_ACCEPT_HEADER = "application/vnd.github.machine-man-preview+json";

    private final Clock clock;
    private final LinkHeaderReader linkHeaderReader;
    private final UrlConnectionProvider urlProvider;
    private final ObjectMapper objectMapper;

    public RestApplicationAuthenticationProvider(Clock clock, LinkHeaderReader linkHeaderReader, UrlConnectionProvider urlProvider) {
        super();
        this.clock = clock;
        this.urlProvider = urlProvider;
        this.linkHeaderReader = linkHeaderReader;
        this.objectMapper = new ObjectMapper().disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    }

    @Override
    public RepositoryAuthenticationToken getInstallationToken(String apiUrl, String appId, String apiPrivateKey,
                                                              String projectPath) throws IOException {

        Instant issued = clock.instant().minus(10, ChronoUnit.SECONDS);
        Instant expiry = issued.plus(2, ChronoUnit.MINUTES);
        String jwtToken = new DefaultJwtBuilder().issuedAt(Date.from(issued)).expiration(Date.from(expiry))
                .claim("iss", appId).signWith(createPrivateKey(apiPrivateKey), Jwts.SIG.RS256).compact();

        Optional<RepositoryAuthenticationToken> repositoryAuthenticationToken = findTokenFromAppInstallationList(getV3Url(apiUrl) + "/app/installations", jwtToken, projectPath);

        return repositoryAuthenticationToken.orElseThrow(() -> new InvalidConfigurationException(InvalidConfigurationException.Scope.PROJECT,
            "No token could be found with access to the requested repository using the given application ID and key"));
    }

    private Optional<RepositoryAuthenticationToken> findTokenFromAppInstallationList(String apiUrl, String jwtToken, String projectPath) throws IOException {
        URLConnection appConnection = urlProvider.createUrlConnection(apiUrl);
        appConnection.setRequestProperty(ACCEPT_HEADER, APP_PREVIEW_ACCEPT_HEADER);
        appConnection.setRequestProperty(AUTHORIZATION_HEADER, BEARER_AUTHORIZATION_HEADER_PREFIX + jwtToken);

        try (Reader reader = new InputStreamReader(appConnection.getInputStream())) {
            AppInstallation[] appInstallations = objectMapper.readerFor(AppInstallation[].class).readValue(reader);
            for (AppInstallation appInstallation : appInstallations) {
                Optional<RepositoryAuthenticationToken> repositoryAuthenticationToken = findAppTokenFromAppInstallation(appInstallation, jwtToken, projectPath);

                if (repositoryAuthenticationToken.isPresent()) {
                    return repositoryAuthenticationToken;
                }
            }
        }

        Optional<String> nextLink = linkHeaderReader.findNextLink(appConnection.getHeaderField("Link"));
        if (nextLink.isEmpty()) {
            return Optional.empty();
        }

        return findTokenFromAppInstallationList(nextLink.get(), jwtToken, projectPath);
    }

    private Optional<RepositoryAuthenticationToken> findAppTokenFromAppInstallation(AppInstallation installation, String jwtToken, String projectPath) throws IOException {
        URLConnection accessTokenConnection = urlProvider.createUrlConnection(installation.getAccessTokensUrl());
        ((HttpURLConnection) accessTokenConnection).setRequestMethod("POST");
        accessTokenConnection.setRequestProperty(ACCEPT_HEADER, APP_PREVIEW_ACCEPT_HEADER);
        accessTokenConnection
                .setRequestProperty(AUTHORIZATION_HEADER, BEARER_AUTHORIZATION_HEADER_PREFIX + jwtToken);

        try (Reader reader = new InputStreamReader(accessTokenConnection.getInputStream())) {
            AppToken appToken = objectMapper.readerFor(AppToken.class).readValue(reader);

            String targetUrl = installation.getRepositoriesUrl();

            Optional<RepositoryAuthenticationToken> potentialRepositoryAuthenticationToken = findRepositoryAuthenticationToken(appToken, targetUrl, projectPath);

            if (potentialRepositoryAuthenticationToken.isPresent()) {
                return potentialRepositoryAuthenticationToken;
            }

        }

        return Optional.empty();
    }

    private Optional<RepositoryAuthenticationToken> findRepositoryAuthenticationToken(AppToken appToken, String targetUrl,
                                                                                      String projectPath) throws IOException {
        URLConnection installationRepositoriesConnection = urlProvider.createUrlConnection(targetUrl);
        ((HttpURLConnection) installationRepositoriesConnection).setRequestMethod("GET");
        installationRepositoriesConnection.setRequestProperty(ACCEPT_HEADER, APP_PREVIEW_ACCEPT_HEADER);
        installationRepositoriesConnection.setRequestProperty(AUTHORIZATION_HEADER,
                                                              BEARER_AUTHORIZATION_HEADER_PREFIX + appToken.getToken());

        try (Reader installationRepositoriesReader = new InputStreamReader(
                installationRepositoriesConnection.getInputStream())) {
            InstallationRepositories installationRepositories =
                    objectMapper.readerFor(InstallationRepositories.class).readValue(installationRepositoriesReader);
            for (Repository repository : installationRepositories.getRepositories()) {
                if (projectPath.equals(repository.getFullName())) {
                    return Optional.of(new RepositoryAuthenticationToken(repository.getNodeId(), appToken.getToken(), repository.getHtmlUrl(), repository.getName(), repository.getOwner().getLogin()));
                }
            }

        }

        Optional<String> nextLink = linkHeaderReader.findNextLink(installationRepositoriesConnection.getHeaderField("Link"));

        if (nextLink.isEmpty()) {
            return Optional.empty();
        }

        return findRepositoryAuthenticationToken(appToken, nextLink.get(), projectPath);
    }

    private static String getV3Url(String apiUrl) {
        if (apiUrl.endsWith("/")) {
            apiUrl = apiUrl.substring(0, apiUrl.length() - 1);
        }
        if (apiUrl.endsWith("/api")) {
            apiUrl = apiUrl + "/v3";
        }
        return apiUrl;
    }

    private static PrivateKey createPrivateKey(String apiPrivateKey) throws IOException {
        try (PEMParser pemParser = new PEMParser(new StringReader(apiPrivateKey))) {
            return new JcaPEMKeyConverter().getPrivateKey(((PEMKeyPair) pemParser.readObject()).getPrivateKeyInfo());
        }
    }
}
