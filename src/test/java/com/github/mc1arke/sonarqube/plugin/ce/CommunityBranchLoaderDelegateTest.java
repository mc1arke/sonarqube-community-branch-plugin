/*
 * Copyright (C) 2020-2024 Michael Clarke
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
package com.github.mc1arke.sonarqube.plugin.ce;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.sonar.ce.task.projectanalysis.analysis.Branch;
import org.sonar.ce.task.projectanalysis.analysis.MutableAnalysisMetadataHolder;
import org.sonar.ce.task.projectanalysis.component.BranchLoaderDelegate;
import org.sonar.db.DbClient;
import org.sonar.db.component.BranchDao;
import org.sonar.db.component.BranchDto;
import org.sonar.db.component.BranchType;
import org.sonar.scanner.protocol.output.ScannerReport;
import org.sonar.server.project.Project;

import java.util.ArrayList;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class CommunityBranchLoaderDelegateTest {

    private final MutableAnalysisMetadataHolder metadataHolder = mock();
    private final DbClient dbClient = mock();
    private final BranchLoaderDelegate testCase = new CommunityBranchLoaderDelegate(dbClient, metadataHolder);

    @Test
    void testNoBranchDetailsNoExistingBranchThrowsException() {

        BranchDao branchDao = mock();
        when(branchDao.selectByBranchKey(any(), any(), any())).thenReturn(Optional.empty());

        ScannerReport.Metadata metadata = ScannerReport.Metadata.getDefaultInstance();
        when(dbClient.branchDao()).thenReturn(branchDao);
        when(metadataHolder.getProject()).thenReturn(new Project("uuid", "key", "name", "description", new ArrayList<>()));

        assertThatThrownBy(() -> testCase.load(metadata))
            .isExactlyInstanceOf(IllegalStateException.class)
            .hasMessage("Could not find main branch")
            .hasNoCause();
    }

    @Test
    void testNoBranchDetailsExistingBranchMatch() {
        BranchDto branchDto = mock();
        when(branchDto.getBranchType()).thenReturn(BranchType.BRANCH);
        when(branchDto.getKey()).thenReturn("branchKey");
        when(branchDto.getMergeBranchUuid()).thenReturn("mergeBranchUuid");
        when(branchDto.getProjectUuid()).thenReturn("projectUuid");
        when(branchDto.getUuid()).thenReturn("branchUuid");
        when(branchDto.isMain()).thenReturn(false);

        BranchDao branchDao = mock();
        when(branchDao.selectMainBranchByProjectUuid(any(), any())).thenReturn(Optional.of(branchDto));

        ScannerReport.Metadata metadata = ScannerReport.Metadata.getDefaultInstance();
        when(dbClient.branchDao()).thenReturn(branchDao);
        when(metadataHolder.getProject()).thenReturn(new Project("uuid", "key", "name", "description", new ArrayList<>()));

        testCase.load(metadata);

        ArgumentCaptor<Branch> branchArgumentCaptor = ArgumentCaptor.captor();

        verify(metadataHolder).setBranch(branchArgumentCaptor.capture());
        assertThat(branchArgumentCaptor.getValue().getType()).isEqualTo(BranchType.BRANCH);
        assertThat(branchArgumentCaptor.getValue().getTargetBranchName()).isNull();
        assertThat(branchArgumentCaptor.getValue().getName()).isEqualTo("branchKey");
        assertThat(branchArgumentCaptor.getValue().isMain()).isFalse();
        assertThat(branchArgumentCaptor.getValue().supportsCrossProjectCpd()).isFalse();
        assertThat(branchArgumentCaptor.getValue().getReferenceBranchUuid()).isNull();

        verify(metadataHolder).getProject();
        verify(metadataHolder).setPullRequestKey(anyString());

        verifyNoMoreInteractions(metadataHolder);

        verify(dbClient).branchDao();
        verify(dbClient).openSession(anyBoolean());
        verifyNoMoreInteractions(dbClient);

        verify(branchDao).selectMainBranchByProjectUuid(any(), any());
        verifyNoMoreInteractions(branchDao);
    }

    @Test
    void testBranchNameNoMatchingBranch() {

        BranchDao branchDao = mock();
        when(branchDao.selectByBranchKey(any(), any(), any())).thenReturn(Optional.empty());

        ScannerReport.Metadata metadata =
                ScannerReport.Metadata.getDefaultInstance().toBuilder().setBranchName("branch").build();

        when(dbClient.branchDao()).thenReturn(branchDao);

        when(metadataHolder.getProject()).thenReturn(new Project("uuid", "key", "name", "description", new ArrayList<>()));

        assertThatThrownBy(() -> testCase.load(metadata))
            .isExactlyInstanceOf(IllegalStateException.class)
            .hasMessage("Invalid branch type 'UNSET'")
            .hasNoCause();
    }

    @Test
    void testBranchNameMatchingBranch() {
        BranchDto branchDto = mock();
        when(branchDto.getBranchType()).thenReturn(BranchType.BRANCH);
        when(branchDto.getKey()).thenReturn("branchKey");
        when(branchDto.getMergeBranchUuid()).thenReturn("mergeBranchUuid");
        when(branchDto.getProjectUuid()).thenReturn("projectUuid");
        when(branchDto.getUuid()).thenReturn("branchUuid");

        BranchDao branchDao = mock();
        when(branchDao.selectByBranchKey(any(), eq("projectUuid"), eq("branch"))).thenReturn(Optional.of(branchDto));

        ScannerReport.Metadata metadata =
                ScannerReport.Metadata.getDefaultInstance().toBuilder().setBranchName("branch")
                        .setBranchType(ScannerReport.Metadata.BranchType.BRANCH).build();

        when(dbClient.branchDao()).thenReturn(branchDao);

        when(metadataHolder.getProject()).thenReturn(new Project("projectUuid", "key", "name", "description", new ArrayList<>()));

        testCase.load(metadata);

        ArgumentCaptor<Branch> branchArgumentCaptor = ArgumentCaptor.captor();

        verify(metadataHolder).setBranch(branchArgumentCaptor.capture());
        assertThat(branchArgumentCaptor.getValue().getType()).isEqualTo(BranchType.BRANCH);
        assertThat(branchArgumentCaptor.getValue().getReferenceBranchUuid()).isEqualTo("projectUuid");
        assertThat(branchArgumentCaptor.getValue().getName()).isEqualTo("branch");
        assertThat(branchArgumentCaptor.getValue().isMain()).isFalse();
        assertThat(branchArgumentCaptor.getValue().supportsCrossProjectCpd()).isFalse();

        verify(metadataHolder).getProject();
        verify(metadataHolder).setPullRequestKey(anyString());

        verifyNoMoreInteractions(metadataHolder);

        verify(dbClient).branchDao();
        verify(dbClient).openSession(anyBoolean());
        verifyNoMoreInteractions(dbClient);

        verify(branchDao).selectByBranchKey(any(), any(), any());
        verifyNoMoreInteractions(branchDao);
    }

    @Test
    void testBranchNamePullRequest() {
        BranchDto branchDto = mock();
        when(branchDto.getBranchType()).thenReturn(BranchType.BRANCH);
        when(branchDto.getKey()).thenReturn("branchKey");
        when(branchDto.getUuid()).thenReturn("mergeBranchUuid");
        when(branchDto.getProjectUuid()).thenReturn("projectUuid");

        BranchDao branchDao = mock();
        when(branchDao.selectByBranchKey(any(), eq("projectUuid"), eq("branch"))).thenReturn(Optional.of(branchDto));

        ScannerReport.Metadata metadata =
                ScannerReport.Metadata.getDefaultInstance().toBuilder().setBranchName("sourceBranch")
                        .setReferenceBranchName("branch").setBranchType(ScannerReport.Metadata.BranchType.PULL_REQUEST)
                        .setPullRequestKey("pullRequestKey")
                        .setTargetBranchName("branch")
                        .build();

        when(dbClient.branchDao()).thenReturn(branchDao);

        when(metadataHolder.getProject()).thenReturn(new Project("projectUuid", "key", "name", "description", new ArrayList<>()));

        testCase.load(metadata);

        ArgumentCaptor<Branch> branchArgumentCaptor = ArgumentCaptor.captor();

        verify(metadataHolder).setBranch(branchArgumentCaptor.capture());
        assertThat(branchArgumentCaptor.getValue().getType()).isEqualTo(BranchType.PULL_REQUEST);
        assertThat(branchArgumentCaptor.getValue().getReferenceBranchUuid()).isEqualTo("mergeBranchUuid");
        assertThat(branchArgumentCaptor.getValue().getName()).isEqualTo("sourceBranch");
        assertThat(branchArgumentCaptor.getValue().isMain()).isFalse();
        assertThat(branchArgumentCaptor.getValue().supportsCrossProjectCpd()).isFalse();
        assertThat(branchArgumentCaptor.getValue().getTargetBranchName()).isEqualTo("branch");


        verify(metadataHolder).getProject();
        verify(metadataHolder).setPullRequestKey(anyString());

        verifyNoMoreInteractions(metadataHolder);

        verify(dbClient).branchDao();
        verify(dbClient).openSession(anyBoolean());
        verifyNoMoreInteractions(dbClient);

        verify(branchDao).selectByBranchKey(any(), any(), any());
        verifyNoMoreInteractions(branchDao);
    }

    @Test
    void testBranchNamePullRequestNoSuchTarget() {
        BranchDao branchDao = mock();
        when(branchDao.selectByBranchKey(any(), eq("projectUuid"), eq("branch"))).thenReturn(Optional.empty());

        ScannerReport.Metadata metadata =
                ScannerReport.Metadata.getDefaultInstance().toBuilder().setBranchName("sourceBranch")
                        .setReferenceBranchName("branch").setBranchType(ScannerReport.Metadata.BranchType.PULL_REQUEST)
                        .build();

        when(dbClient.branchDao()).thenReturn(branchDao);

        when(metadataHolder.getProject()).thenReturn(new Project("projectUuid", "key", "name", "description", new ArrayList<>()));

        assertThatThrownBy(() -> testCase.load(metadata))
            .isExactlyInstanceOf(IllegalStateException.class)
            .hasMessage("Could not find target branch 'branch' in project")
            .hasNoCause();
    }


    @Test
    void testBranchNameMatchingBranchWithTargetBranch() {
        BranchDto sourceBranchDto = mock();
        when(sourceBranchDto.getBranchType()).thenReturn(BranchType.BRANCH);
        when(sourceBranchDto.getKey()).thenReturn("branchKey");
        when(sourceBranchDto.getMergeBranchUuid()).thenReturn("mergeBranchUuid");
        when(sourceBranchDto.getProjectUuid()).thenReturn("projectUuid");
        when(sourceBranchDto.getUuid()).thenReturn("branchUuid");

        BranchDto targetBranchDto = mock();
        when(targetBranchDto.getBranchType()).thenReturn(BranchType.BRANCH);
        when(targetBranchDto.getKey()).thenReturn("targetBranchKey");
        when(targetBranchDto.getMergeBranchUuid()).thenReturn("targetMergeBranchUuid");
        when(targetBranchDto.getProjectUuid()).thenReturn("projectUuid");
        when(targetBranchDto.getUuid()).thenReturn("targetBranchUuid");

        BranchDao branchDao = mock();
        when(branchDao.selectByBranchKey(any(), eq("projectUuid"), eq("branch")))
                .thenReturn(Optional.of(sourceBranchDto));
        when(branchDao.selectByBranchKey(any(), eq("projectUuid"), eq("mergeBranchName")))
                .thenReturn(Optional.of(targetBranchDto));

        ScannerReport.Metadata metadata =
                ScannerReport.Metadata.getDefaultInstance().toBuilder().setBranchName("branch")
                        .setBranchType(ScannerReport.Metadata.BranchType.BRANCH)
                        .setReferenceBranchName("mergeBranchName")
                        .setTargetBranchName("targetBranchThatDoesNotMatchMergeBranch")
                        .build();

        when(dbClient.branchDao()).thenReturn(branchDao);

        when(metadataHolder.getProject()).thenReturn(new Project("projectUuid", "key", "name", "description", new ArrayList<>()));

        testCase.load(metadata);

        ArgumentCaptor<Branch> branchArgumentCaptor = ArgumentCaptor.captor();

        verify(metadataHolder).setBranch(branchArgumentCaptor.capture());
        assertThat(branchArgumentCaptor.getValue().getType()).isEqualTo(BranchType.BRANCH);
        assertThat(branchArgumentCaptor.getValue().getReferenceBranchUuid()).isEqualTo("targetBranchUuid");
        assertThat(branchArgumentCaptor.getValue().getName()).isEqualTo("branch");
        assertThat(branchArgumentCaptor.getValue().isMain()).isFalse();
        assertThat(branchArgumentCaptor.getValue().supportsCrossProjectCpd()).isFalse();


        verify(metadataHolder).getProject();
        verify(metadataHolder).setPullRequestKey(anyString());

        verifyNoMoreInteractions(metadataHolder);

        verify(dbClient, times(2)).branchDao();
        verify(dbClient, times(2)).openSession(anyBoolean());
        verifyNoMoreInteractions(dbClient);

        verify(branchDao, times(2)).selectByBranchKey(any(), any(), any());
        verifyNoMoreInteractions(branchDao);
    }

    @Test
    void testBranchNameMatchingBranchWithTargetBranchMissingTargetBranch() {
        BranchDto sourceBranchDto = mock();
        when(sourceBranchDto.getBranchType()).thenReturn(BranchType.BRANCH);
        when(sourceBranchDto.getKey()).thenReturn("branchKey");
        when(sourceBranchDto.getMergeBranchUuid()).thenReturn("mergeBranchUuid");
        when(sourceBranchDto.getProjectUuid()).thenReturn("projectUuid");
        when(sourceBranchDto.getUuid()).thenReturn("branchUuid");

        BranchDao branchDao = mock();
        when(branchDao.selectByBranchKey(any(), eq("projectUuid"), eq("branch")))
                .thenReturn(Optional.of(sourceBranchDto));
        when(branchDao.selectByBranchKey(any(), eq("projectUuid"), eq("mergeBranchName"))).thenReturn(Optional.empty());

        ScannerReport.Metadata metadata =
                ScannerReport.Metadata.getDefaultInstance().toBuilder().setBranchName("branch")
                        .setBranchType(ScannerReport.Metadata.BranchType.BRANCH)
                        .setReferenceBranchName("mergeBranchName")
                        .build();

        when(dbClient.branchDao()).thenReturn(branchDao);

        when(metadataHolder.getProject()).thenReturn(new Project("projectUuid", "key", "name", "description", new ArrayList<>()));

        assertThatThrownBy(() -> testCase.load(metadata))
            .isExactlyInstanceOf(IllegalStateException.class)
            .hasMessage("Could not find target branch 'mergeBranchName' in project")
            .hasNoCause();
    }

}
