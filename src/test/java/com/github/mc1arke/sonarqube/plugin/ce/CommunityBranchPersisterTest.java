package com.github.mc1arke.sonarqube.plugin.ce;

import org.junit.Test;
import org.sonar.ce.task.projectanalysis.analysis.AnalysisMetadataHolder;
import org.sonar.ce.task.projectanalysis.analysis.Branch;
import org.sonar.ce.task.projectanalysis.component.Component;
import org.sonar.ce.task.projectanalysis.component.ConfigurationRepository;
import org.sonar.ce.task.projectanalysis.component.TreeRootHolder;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.*;
import org.sonar.db.protobuf.DbProjectBranches;

import java.util.Optional;

import static org.mockito.Mockito.*;

public class CommunityBranchPersisterTest {
    @Test
    public void testPersist() {
        DbClient dbClient = mock(DbClient.class);
        TreeRootHolder treeRootHolder = mock(TreeRootHolder.class);
        AnalysisMetadataHolder analysisMetadataHolder = mock(AnalysisMetadataHolder.class);
        ConfigurationRepository configurationRepository = mock(ConfigurationRepository.class);
        DbSession dbSession = mock(DbSession.class);
        Branch branch = mock(Branch.class);
        Component component = mock(Component.class);
        ComponentDao componentDao = mock(ComponentDao.class);
        BranchDao branchDao = mock(BranchDao.class);
        BranchDto branchDto = mock(BranchDto.class);

        when(analysisMetadataHolder.getBranch()).thenReturn(branch);
        when(treeRootHolder.getRoot()).thenReturn(component);
        when(component.getUuid()).thenReturn("fake");
        when(component.getKey()).thenReturn("fake");
        when(component.getName()).thenReturn("fake");
        when(branch.getReferenceBranchUuid()).thenReturn("fake");
        when(branch.getName()).thenReturn("fake");
        when(branch.getTargetBranchName()).thenReturn("fake");

        when(componentDao.selectByUuid(eq(dbSession), eq("fake"))).thenReturn(Optional.empty());
        when(dbClient.componentDao()).thenReturn(componentDao);

        when(branch.getType()).thenReturn(BranchType.PULL_REQUEST);
        when(branch.isMain()).thenReturn(false);
        when(analysisMetadataHolder.getPullRequestKey()).thenReturn("fake");

        when(dbClient.branchDao()).thenReturn(branchDao);
        when(branchDao.selectByPullRequestKey(eq(dbSession), eq("fake"), eq("fake"))).thenReturn(Optional.of(branchDto));
        when(branchDto.getPullRequestData()).thenReturn(DbProjectBranches.PullRequestData.newBuilder().build());

        CommunityBranchPersister communityBranchPersister = new CommunityBranchPersister(dbClient, treeRootHolder, analysisMetadataHolder, configurationRepository);

        communityBranchPersister.persist(dbSession);

        verify(dbClient.componentDao()).insert(eq(dbSession), any(ComponentDto.class));
        verify(dbClient.branchDao()).upsert(eq(dbSession), any(BranchDto.class));
    }
}
