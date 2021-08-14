/*
 * Copyright (C) 2020-2021 Michael Clarke
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

import com.github.mc1arke.sonarqube.plugin.almclient.github.GithubClient;
import com.github.mc1arke.sonarqube.plugin.almclient.github.GithubClientFactory;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.AnalysisDetails;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.DecorationResult;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.sonar.db.alm.setting.ALM;
import org.sonar.db.alm.setting.AlmSettingDto;
import org.sonar.db.alm.setting.ProjectAlmSettingDto;

import java.io.IOException;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class GithubPullRequestDecoratorTest {

    private final GithubClient githubClient = mock(GithubClient.class);
    private final AnalysisDetails analysisDetails = mock(AnalysisDetails.class);
    private final GithubClientFactory githubClientFactory = mock(GithubClientFactory.class);
    private final GithubPullRequestDecorator testCase = new GithubPullRequestDecorator(githubClientFactory);
    private final ProjectAlmSettingDto projectAlmSettingDto = mock(ProjectAlmSettingDto.class);
    private final AlmSettingDto almSettingDto = mock(AlmSettingDto.class);


    @Test
    public void testName() {
        assertThat(testCase.alm()).isEqualTo(Collections.singletonList(ALM.GITHUB));
    }

    @Test
    public void testDecorateQualityGatePropagateException() throws IOException {
        Exception dummyException = new IOException("Dummy Exception");
        doReturn(githubClient).when(githubClientFactory).createClient(any(), any());
        doThrow(dummyException).when(githubClient).createCheckRun(any(), any(), any());

        assertThatThrownBy(() -> testCase.decorateQualityGateStatus(analysisDetails, almSettingDto, projectAlmSettingDto))
                .hasMessage("Could not decorate Pull Request on Github")
                .isExactlyInstanceOf(IllegalStateException.class).hasCause(dummyException);
    }

    @Test
    public void testDecorateQualityGateReturnValue() throws IOException {
        DecorationResult expectedResult = DecorationResult.builder().build();
        doReturn(githubClient).when(githubClientFactory).createClient(any(), any());
        doReturn(expectedResult).when(githubClient).createCheckRun(any(), any(), any());
        DecorationResult decorationResult = testCase.decorateQualityGateStatus(analysisDetails, almSettingDto, projectAlmSettingDto);

        ArgumentCaptor<AnalysisDetails> argumentCaptor = ArgumentCaptor.forClass(AnalysisDetails.class);
        verify(githubClient).createCheckRun(argumentCaptor.capture(), eq(almSettingDto), eq(projectAlmSettingDto));
        assertEquals(analysisDetails, argumentCaptor.getValue());
        assertThat(decorationResult).isSameAs(expectedResult);
    }
}
