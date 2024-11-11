/*
 * Copyright (C) 2022-2024 Michael Clarke
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import org.assertj.core.api.Condition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.kohsuke.github.GHApp;
import org.kohsuke.github.GHAppCreateTokenBuilder;
import org.kohsuke.github.GHAppInstallation;
import org.kohsuke.github.GHAppInstallationToken;
import org.kohsuke.github.GitHub;
import org.kohsuke.github.GitHubBuilder;
import org.kohsuke.github.connector.GitHubConnector;
import org.sonar.api.config.internal.Encryption;
import org.sonar.api.config.internal.Settings;
import org.sonar.db.alm.setting.AlmSettingDto;
import org.sonar.db.alm.setting.ProjectAlmSettingDto;

import com.github.mc1arke.sonarqube.plugin.InvalidConfigurationException;

class GithubClientFactoryTest {

    private static final String PRIVATE_KEY = "-----BEGIN RSA PRIVATE KEY-----\n" +
        "MIIEpAIBAAKCAQEAoA3dIxtKx7qpUAhfoJeYAYZzVdRvOW9s3GtxJVuBI6GOob2V\n" +
        "J7ia1i/h3za591NggqsW+QjjknmhPmqlENCKsyU5FQMkM8eV04xwHxFwjIk6SACB\n" +
        "JigRSgw95b+EKN2p0caTeOiL/+LQ3pauAf+52iFqeWAKvgjxx3+rlSXzgWCBb9uk\n" +
        "1XcDoeRzeXkXvOj9ZQ8wKn8FAounmyjKaIy+u7bJCgVWHRfXwjFWsIWZJ9UY1eAR\n" +
        "ADoPHIHj4WrNmAvE/kOBXNSqmkY+Yu170tQA5iVeHgezrlkM08Qj4+4pqYOPHWZ2\n" +
        "0TNsRn82fuGSyejLhKATbqiOxierHzECIGGUQQIDAQABAoIBADrJjNFRu3BL89dl\n" +
        "E/aw55Cb2S4L1oSClDoLrqXZi7/SHcjzkN7jk9+a+7wYZkrdEYQ9IjV7WdcZnKuH\n" +
        "0TQxXNh7EhHRMxFfu/zVRvNqXOwJlWIP6V/h9KO9hlimNP0rma3m4ZDV3WIx5aT0\n" +
        "NFqgmptvjaOiLp/pOiEcGCIyq9N3UZgGTeJTRdvNiEJDiFPJaJvkJuspbCAIrqFT\n" +
        "hzua3aJyi47GD0nebfAfJyC8Cq9VgKzvVYoKejU51cw9+ikCVSZZiOCVWOkZTPjU\n" +
        "LCuhIAtPppG5s5DxvXtHdWUlpRKRbI1tIVaTk05kBm1SOH0a3H2GiEv4jQvsyZMz\n" +
        "UQQQe78CgYEAw0Ixmsgy8meCKMxH8oWO/ceyZ4uVQCGy4C/q2JmGHL0DkHF6QrZi\n" +
        "DuUrqDoI88lhBL/0JyGlRTy3Ddy2hZdjUdvd6HEyLJbQvy6BmUI5AGXU0+ICHfZ2\n" +
        "YDP+3zY1m4SctM1duJ6XcPPefDapbl4WMxVLs1DTHCAK4dvI9HW9WYcCgYEA0dgY\n" +
        "oihENXtew/s3vlBk+ZN+5xjnRwThUouxZFnzHBeCdE1237y6lUMK6UfjTmstGxWI\n" +
        "g5KoawjZJScH8UDO7OiGTuI93TUzrR1Xi63dTynfE5z5BvKKGKjM4Y3aTvAsbk4q\n" +
        "b+U65srTgvtZG4ASv/4vCxGL8wTJbd8oLWhu9fcCgYEArvUKA5ntZJzw2OOqeBnK\n" +
        "dYVRS0ycMHnBkPX+pZRywh9vKSc1GL/Zf2VDSBqwWNkR0LK677FLKI3trEMfXPa4\n" +
        "bOnondWH0sJUS2o9f/kBoGSeXji+EuD7UtpkPteRE0exLqRxnPKl2fT6XyyPhrBR\n" +
        "jfY//W2nrCTd+2D3YGx7fNMCgYEAyGK6i0c2c3f/M9lXDvcIpcfyvE5stMX1QXVC\n" +
        "jdjTrfTJT7SVmuxHpLej2MccSktQhHeYqERJbgTCD5dpHznLIDKf5v5nIzFlyp+l\n" +
        "dS4vkyQh8UHKEJdVxlyTYaSrXww88YzVO4tEJxZyyrapDfjMbukVFVXJNeVRUQlz\n" +
        "/YCnzVsCgYBipCwzPZzVlK7WdH8jsPC19ZEFd7UIbXM8kqiWhhQp4zL7kJ81Q+zf\n" +
        "xmiRGsJl9uUjypDDOp1qLejoH5EObg3MlNoOq6aqSu1ZaY0rOnALlJwmZS10G2vC\n" +
        "4eO4MsTrF0fH8PnJLK/nbrGX2Ll+PyY5Zn8rfKeTVvmwjSTMFPSORw==\n" +
        "-----END RSA PRIVATE KEY-----";

    private final AlmSettingDto almSettingDto = mock();
    private final ProjectAlmSettingDto projectAlmSettingDto = mock();
    private final Settings settings = mock();
    private final Clock clock = Clock.fixed(Instant.ofEpochSecond(123456789123L), ZoneId.of("UTC"));
    private final GitHubBuilder githubBuilder = mock();
    private final Encryption encryption = mock();

    @BeforeEach
    void setUp() {
        when(almSettingDto.getUrl()).thenReturn("url");
        when(almSettingDto.getDecryptedPrivateKey(any())).thenReturn("privateKey");
        when(projectAlmSettingDto.getAlmRepo()).thenReturn("owner/repo");
        when(almSettingDto.getAppId()).thenReturn("appId");
        when(settings.getEncryption()).thenReturn(encryption);
        when(almSettingDto.getDecryptedPrivateKey(any())).thenReturn(PRIVATE_KEY);
    }

    @Test
    void shouldThrowExceptionIfUrlMissingInAlmSettings() {
        when(almSettingDto.getUrl()).thenReturn(null);
        GithubClientFactory underTest = new GithubClientFactory(clock, settings, () -> githubBuilder);
        assertThatThrownBy(() -> underTest.createClient(almSettingDto, projectAlmSettingDto))
                .isInstanceOf(InvalidConfigurationException.class)
                .hasMessage("No URL has been set for Github connections")
                .has(new Condition<>(t -> ((InvalidConfigurationException) t).getScope() == InvalidConfigurationException.Scope.GLOBAL, "GLOBAL Scope for exception"));
    }

    @Test
    void shouldThrowExceptionIsPrivateKeyMissingInAlmSettings() {
        when(almSettingDto.getDecryptedPrivateKey(any())).thenReturn(null);
        GithubClientFactory underTest = new GithubClientFactory(clock, settings, () -> githubBuilder);
        assertThatThrownBy(() -> underTest.createClient(almSettingDto, projectAlmSettingDto))
                .isInstanceOf(InvalidConfigurationException.class)
                .hasMessage("No private key has been set for Github connections")
                .has(new Condition<>(t -> ((InvalidConfigurationException) t).getScope() == InvalidConfigurationException.Scope.GLOBAL, "GLOBAL Scope for exception"));
    }

    @Test
    void shouldThrowExceptionIfRepoMissingInAlmSettings() {
        when(projectAlmSettingDto.getAlmRepo()).thenReturn(null);
        GithubClientFactory underTest = new GithubClientFactory(clock, settings, () -> githubBuilder);
        assertThatThrownBy(() -> underTest.createClient(almSettingDto, projectAlmSettingDto))
                .isInstanceOf(InvalidConfigurationException.class)
                .hasMessage("No repository name has been set for Github connections")
                .has(new Condition<>(t -> ((InvalidConfigurationException) t).getScope() == InvalidConfigurationException.Scope.PROJECT, "PROJECT Scope for exception"));
    }

    @Test
    void shouldThrowExceptionIfAppIdMissingInAlmSettings() {
        when(almSettingDto.getAppId()).thenReturn(null);
        GithubClientFactory underTest = new GithubClientFactory(clock, settings, () -> githubBuilder);
        assertThatThrownBy(() -> underTest.createClient(almSettingDto, projectAlmSettingDto))
                .isInstanceOf(InvalidConfigurationException.class)
                .hasMessage("No App ID has been set for Github connections")
                .has(new Condition<>(t -> ((InvalidConfigurationException) t).getScope() == InvalidConfigurationException.Scope.GLOBAL, "GLOBAL Scope for exception"));
    }

    @Test
    void shouldThrowExceptionIfGithubCallFails() throws IOException {
        GithubClientFactory underTest = new GithubClientFactory(clock, settings, () -> githubBuilder);
        GitHub github = mock();
        when(github.getApp()).thenThrow(new IOException("dummy"));
        when(githubBuilder.build()).thenReturn(github);
        when(githubBuilder.withEndpoint(any())).thenReturn(githubBuilder);
        when(githubBuilder.withJwtToken(any())).thenReturn(githubBuilder);
        when(githubBuilder.withConnector(any(GitHubConnector.class))).thenReturn(githubBuilder);
        assertThatThrownBy(() -> underTest.createClient(almSettingDto, projectAlmSettingDto))
                .isInstanceOf(InvalidConfigurationException.class)
                .hasMessage("Could not create Github client - dummy")
                .has(new Condition<>(t -> ((InvalidConfigurationException) t).getScope() == InvalidConfigurationException.Scope.PROJECT, "PROJECT Scope for exception"));
    }

    @Test
    void shouldThrowExceptionIfRepoNameDoesNotContainSlash() {
        when(projectAlmSettingDto.getAlmRepo()).thenReturn("repo");
        GithubClientFactory underTest = new GithubClientFactory(clock, settings, () -> githubBuilder);
        assertThatThrownBy(() -> underTest.createClient(almSettingDto, projectAlmSettingDto))
            .usingRecursiveComparison()
            .isEqualTo(new InvalidConfigurationException(InvalidConfigurationException.Scope.PROJECT, "Repository name must be in the format owner/repo"));
    }

    @Test
    void shouldThrowExceptionIfRsaKeyIsNotParseable() {
        when(almSettingDto.getDecryptedPrivateKey(any())).thenReturn("invalid");
        GithubClientFactory underTest = new GithubClientFactory(clock, settings, () -> githubBuilder);
        assertThatThrownBy(() -> underTest.createClient(almSettingDto, projectAlmSettingDto))
            .usingRecursiveComparison()
            .isEqualTo(new InvalidConfigurationException(InvalidConfigurationException.Scope.GLOBAL, "Private key could not be parsed"));
    }

    @Test
    void shouldReturnValidGithubTokenWhenCalledWithCorrectParameters() throws IOException {
        GithubClientFactory underTest = new GithubClientFactory(clock, settings, () -> githubBuilder);
        when(projectAlmSettingDto.getAlmRepo()).thenReturn("alm/slug");

        GitHub github = mock();
        when(githubBuilder.withEndpoint(any())).thenReturn(githubBuilder);
        when(githubBuilder.withJwtToken(any())).thenReturn(githubBuilder);
        when(githubBuilder.withConnector(any(GitHubConnector.class))).thenReturn(githubBuilder);
        when(githubBuilder.withAppInstallationToken(any())).thenReturn(githubBuilder);
        when(githubBuilder.build()).thenReturn(github);
        GHApp ghApp = mock();
        when(github.getApp()).thenReturn(ghApp);
        GHAppInstallation ghAppInstallation = mock();
        when(ghApp.getInstallationByRepository(any(), any())).thenReturn(ghAppInstallation);
        GHAppCreateTokenBuilder tokenBuilder = mock();
        when(ghAppInstallation.createToken()).thenReturn(tokenBuilder);
        GHAppInstallationToken ghAppInstallationToken = mock();
        when(ghAppInstallationToken.getToken()).thenReturn("token");
        when(tokenBuilder.create()).thenReturn(ghAppInstallationToken);
        when(tokenBuilder.repositories(any())).thenReturn(tokenBuilder);

        assertThat(underTest.createClient(almSettingDto, projectAlmSettingDto)).isSameAs(github);
        verify(tokenBuilder).create();
        verify(githubBuilder, times(2)).withEndpoint("url");
        verify(githubBuilder).withJwtToken("eyJhbGciOiJSUzI1NiJ9.eyJpYXQiOjEyMzQ1Njc4OTExMywiZXhwIjoxMjM0NTY3ODkyMzMsImlzcyI6ImFwcElkIn0.Ut0Cf_eKvDNHLIzFx1q-8yweUKOPhA1eSKALpUVtmlY7LHK3G_btuRUnIH4sfKsjEIS-3urq0FxXW7EcmGSxz8ARUNFe26mhnVtY7odMEJip2GhbC397WkIgf-_HV0M8JnMRQ0InFiGMiXKpMM08w02QgHdIV0kPa41HfMiZLYS3VdmbYU4NX8gvVN0mrAHV1brqcdUGJLwSYJBsz3yd8vTtb_M0Kl4TY9jH5Wvh4wRbSpjGmo2vTID3ioHL9JVSBi6El0gKL3AGk5n5BXfVlYvPEIwbtQp0GB-E2azEnQYxyshNid9K1d2Y032AtF_AQ0cXCssoLClUUcke6yiW8Q");
        verify(githubBuilder, times(2)).withConnector(any(GitHubConnector.class));
        verify(githubBuilder).withAppInstallationToken("token");
        verify(githubBuilder, times(2)).build();
        verifyNoMoreInteractions(githubBuilder);

    }
}
