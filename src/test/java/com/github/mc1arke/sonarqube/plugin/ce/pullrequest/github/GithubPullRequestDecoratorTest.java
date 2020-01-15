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
package com.github.mc1arke.sonarqube.plugin.ce.pullrequest.github;

import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.AnalysisDetails;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.UnifyConfiguration;
import org.junit.Test;

import java.io.IOException;
import java.security.GeneralSecurityException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class GithubPullRequestDecoratorTest {

    private CheckRunProvider checkRunProvider = mock(CheckRunProvider.class);
    private AnalysisDetails analysisDetails = mock(AnalysisDetails.class);
    private UnifyConfiguration unifyConfiguration = mock(UnifyConfiguration.class);
    private GithubPullRequestDecorator testCase = new GithubPullRequestDecorator(checkRunProvider);

    @Test
    public void testName() {
        assertThat(testCase.name()).isEqualTo("Github");
    }

    @Test
    public void testDecorateQualityGatePropagateException() throws IOException, GeneralSecurityException {
        Exception dummyException = new IOException("Dummy Exception");
        doThrow(dummyException).when(checkRunProvider).createCheckRun(any(), any());

        assertThatThrownBy(() -> testCase.decorateQualityGateStatus(analysisDetails, unifyConfiguration))
                .hasMessage("Could not decorate Pull Request on Github")
                .isExactlyInstanceOf(IllegalStateException.class).hasCause(dummyException);
    }

    @Test
    public void testDecorateQualityGateReturnValue() throws IOException, GeneralSecurityException {
        testCase.decorateQualityGateStatus(analysisDetails, unifyConfiguration);

        verify(checkRunProvider).createCheckRun(analysisDetails, unifyConfiguration);
    }
}