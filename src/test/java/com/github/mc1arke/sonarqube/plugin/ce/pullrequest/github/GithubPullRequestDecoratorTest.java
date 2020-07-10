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
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.DecorationResult;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.commentfilter.IssueFilterRunner;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.sonar.db.alm.setting.ALM;
import org.sonar.db.alm.setting.AlmSettingDto;
import org.sonar.db.alm.setting.ProjectAlmSettingDto;

import java.io.IOException;
import java.security.GeneralSecurityException;

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

    private CheckRunProvider checkRunProvider = mock(CheckRunProvider.class);
    private AnalysisDetails analysisDetails = mock(AnalysisDetails.class);
    private GithubPullRequestDecorator testCase = new GithubPullRequestDecorator(checkRunProvider);
    private ProjectAlmSettingDto projectAlmSettingDto = mock(ProjectAlmSettingDto.class);
    private AlmSettingDto almSettingDto = mock(AlmSettingDto.class);
    private IssueFilterRunner issueFilterRunner = mock(IssueFilterRunner.class);


    @Test
    public void testName() {
        assertThat(testCase.alm()).isEqualTo(ALM.GITHUB);
    }

    @Test
    public void testDecorateQualityGatePropagateException() throws IOException, GeneralSecurityException {
        Exception dummyException = new IOException("Dummy Exception");
        doThrow(dummyException).when(checkRunProvider).createCheckRun(any(), any(), any(),any());

        assertThatThrownBy(() -> testCase.decorateQualityGateStatus(analysisDetails, almSettingDto, projectAlmSettingDto, null))
                .hasMessage("Could not decorate Pull Request on Github")
                .isExactlyInstanceOf(IllegalStateException.class).hasCause(dummyException);
    }

    @Test
    public void testDecorateQualityGateReturnValue() throws IOException, GeneralSecurityException {
        DecorationResult expectedResult = DecorationResult.builder().build();
        doReturn(expectedResult).when(checkRunProvider).createCheckRun(any(), any(), any(), any());
        DecorationResult decorationResult = testCase.decorateQualityGateStatus(analysisDetails, almSettingDto, projectAlmSettingDto, issueFilterRunner);

        ArgumentCaptor<AnalysisDetails> argumentCaptor = ArgumentCaptor.forClass(AnalysisDetails.class);
        verify(checkRunProvider).createCheckRun(argumentCaptor.capture(), eq(almSettingDto), eq(projectAlmSettingDto), eq(issueFilterRunner));
        assertEquals(analysisDetails, argumentCaptor.getValue());
        assertThat(decorationResult).isSameAs(expectedResult);
    }
}
