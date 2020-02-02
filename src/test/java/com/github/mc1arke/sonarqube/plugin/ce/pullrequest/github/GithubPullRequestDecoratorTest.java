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
package com.github.mc1arke.sonarqube.plugin.ce.pullrequest.github;

import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.AnalysisDetails;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.sonar.db.alm.setting.AlmSettingDto;
import org.sonar.db.alm.setting.ProjectAlmSettingDto;

import java.io.IOException;
import java.security.GeneralSecurityException;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

public class GithubPullRequestDecoratorTest {

    @Test
    public void testName() {
        assertEquals("Github", new GithubPullRequestDecorator(mock(CheckRunProvider.class)).name());
    }

    @Test
    public void testDecorateQualityGatePropagateException() throws IOException, GeneralSecurityException {
        CheckRunProvider checkRunProvider = mock(CheckRunProvider.class);
        Exception dummyException = new IOException("Dummy Exception");
        AnalysisDetails analysisDetails = mock(AnalysisDetails.class);
        GithubPullRequestDecorator testCase = new GithubPullRequestDecorator(checkRunProvider);

        ProjectAlmSettingDto projectAlmSettingDto = mock(ProjectAlmSettingDto.class);
        AlmSettingDto almSettingDto = mock(AlmSettingDto.class);

        doThrow(dummyException).when(checkRunProvider).createCheckRun(any(), any(), any());
        assertThatThrownBy(
                () -> testCase.decorateQualityGateStatus(analysisDetails, almSettingDto, projectAlmSettingDto))
                .hasMessage("Could not decorate Pull Request on Github")
                .isExactlyInstanceOf(IllegalStateException.class).hasCause(dummyException);
    }

    @Test
    public void testDecorateQualityGateReturnValue() throws IOException, GeneralSecurityException {
        CheckRunProvider checkRunProvider = spy(CheckRunProvider.class);
        AnalysisDetails analysisDetails = mock(AnalysisDetails.class);
        GithubPullRequestDecorator testCase = new GithubPullRequestDecorator(checkRunProvider);

        ProjectAlmSettingDto projectAlmSettingDto = mock(ProjectAlmSettingDto.class);
        AlmSettingDto almSettingDto = mock(AlmSettingDto.class);

        ArgumentCaptor<AnalysisDetails> argumentCaptor = ArgumentCaptor.forClass(AnalysisDetails.class);

        testCase.decorateQualityGateStatus(analysisDetails, almSettingDto, projectAlmSettingDto);
        verify(checkRunProvider).createCheckRun(argumentCaptor.capture(), eq(almSettingDto), eq(projectAlmSettingDto));
        assertEquals(analysisDetails, argumentCaptor.getValue());
    }
}