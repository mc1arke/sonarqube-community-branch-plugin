/*
 * Copyright (C) 2021-2024 Michael Clarke
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
package com.github.mc1arke.sonarqube.plugin.almclient.github;

import java.io.IOException;
import java.io.StringReader;
import java.security.PrivateKey;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Optional;
import java.util.function.Supplier;

import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.kohsuke.github.GHAppInstallationToken;
import org.kohsuke.github.GitHub;
import org.kohsuke.github.GitHubBuilder;
import org.kohsuke.github.extras.okhttp3.OkHttpGitHubConnector;
import org.sonar.api.ce.ComputeEngineSide;
import org.sonar.api.config.internal.Settings;
import org.sonar.api.server.ServerSide;
import org.sonar.db.alm.setting.AlmSettingDto;
import org.sonar.db.alm.setting.ProjectAlmSettingDto;
import org.springframework.beans.factory.annotation.Autowired;

import com.github.mc1arke.sonarqube.plugin.InvalidConfigurationException;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.impl.DefaultJwtBuilder;
import okhttp3.OkHttpClient;

@ServerSide
@ComputeEngineSide
public class GithubClientFactory {

    private final Clock clock;
    private final Settings settings;
    private final Supplier<GitHubBuilder> gitHubBuilderSupplier;

    @Autowired
    public GithubClientFactory(Clock clock, Settings settings) {
        this(clock, settings, GitHubBuilder::new);
    }

    GithubClientFactory(Clock clock, Settings settings, Supplier<GitHubBuilder> gitHubBuilderSupplier) {
        this.clock = clock;
        this.settings = settings;
        this.gitHubBuilderSupplier = gitHubBuilderSupplier;
    }

    public GitHub createClient(AlmSettingDto almSettingDto, ProjectAlmSettingDto projectAlmSettingDto) throws IOException {
            GHAppInstallationToken repositoryAuthenticationToken = authenticate(projectAlmSettingDto, almSettingDto);

            return gitHubBuilderSupplier.get()
                .withConnector(new OkHttpGitHubConnector(new OkHttpClient()))
                .withEndpoint(almSettingDto.getUrl())
                .withAppInstallationToken(repositoryAuthenticationToken.getToken())
                .build();
    }

    private GHAppInstallationToken authenticate(ProjectAlmSettingDto projectAlmSettingDto, AlmSettingDto almSettingDto) {
        String apiUrl = Optional.ofNullable(almSettingDto.getUrl()).orElseThrow(() -> new InvalidConfigurationException(InvalidConfigurationException.Scope.GLOBAL, "No URL has been set for Github connections"));
        String apiPrivateKey = Optional.ofNullable(almSettingDto.getDecryptedPrivateKey(settings.getEncryption())).orElseThrow(() -> new InvalidConfigurationException(InvalidConfigurationException.Scope.GLOBAL, "No private key has been set for Github connections"));
        String projectPath = Optional.ofNullable(projectAlmSettingDto.getAlmRepo()).orElseThrow(() -> new InvalidConfigurationException(InvalidConfigurationException.Scope.PROJECT, "No repository name has been set for Github connections"));
        String appId = Optional.ofNullable(almSettingDto.getAppId()).orElseThrow(() -> new InvalidConfigurationException(InvalidConfigurationException.Scope.GLOBAL, "No App ID has been set for Github connections"));

        try {
            return getInstallationToken(apiUrl, appId, apiPrivateKey, projectPath);
        } catch (IOException ex) {
            throw new InvalidConfigurationException(InvalidConfigurationException.Scope.PROJECT, "Could not create Github client - " + ex.getMessage(), ex);
        }

    }

    private GHAppInstallationToken getInstallationToken(String apiUrl, String appId, String apiPrivateKey, String projectPath) throws IOException {
        Instant issued = clock.instant().minus(10, ChronoUnit.SECONDS);
        Instant expiry = issued.plus(2, ChronoUnit.MINUTES);
        String jwtToken = new DefaultJwtBuilder().issuedAt(Date.from(issued)).expiration(Date.from(expiry))
            .claim("iss", appId).signWith(createPrivateKey(apiPrivateKey), Jwts.SIG.RS256).compact();

        if (!projectPath.contains("/")) {
            throw new InvalidConfigurationException(InvalidConfigurationException.Scope.PROJECT, "Repository name must be in the format 'owner/repo'");
        }
        String owner = projectPath.split("/")[0];
        String repo = projectPath.split("/")[1];
        GitHub github = gitHubBuilderSupplier.get()
            .withEndpoint(apiUrl)
            .withConnector(new OkHttpGitHubConnector(new OkHttpClient()))
            .withJwtToken(jwtToken)
            .build();

        return github.getApp().getInstallationByRepository(owner, repo).createToken().create();
    }

    private static PrivateKey createPrivateKey(String apiPrivateKey) throws IOException {
        try (PEMParser pemParser = new PEMParser(new StringReader(apiPrivateKey))) {
            return new JcaPEMKeyConverter().getPrivateKey(((PEMKeyPair) Optional.ofNullable(pemParser.readObject()).orElseThrow(() -> new InvalidConfigurationException(InvalidConfigurationException.Scope.GLOBAL, "Private key could not be parsed"))).getPrivateKeyInfo());
        }
    }
}
