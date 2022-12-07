package com.github.mc1arke.sonarqube.plugin.ce;

import org.jetbrains.annotations.NotNull;
import org.sonar.ce.task.projectanalysis.analysis.AnalysisMetadataHolder;
import org.sonar.ce.task.projectanalysis.analysis.Branch;
import org.sonar.ce.task.projectanalysis.batch.BatchReportReader;
import org.sonar.ce.task.projectanalysis.component.*;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.*;
import org.sonar.db.protobuf.DbProjectBranches;
import org.springframework.context.annotation.Primary;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Optional;
import java.util.regex.Pattern;


@Primary
public class CommunityBranchPersister implements BranchPersister {
    private final DbClient dbClient;
    private final TreeRootHolder treeRootHolder;
    private final AnalysisMetadataHolder analysisMetadataHolder;
    private final ConfigurationRepository configurationRepository;

    public CommunityBranchPersister(DbClient dbClient, TreeRootHolder treeRootHolder, AnalysisMetadataHolder analysisMetadataHolder, ConfigurationRepository configurationRepository) {
        this.dbClient = dbClient;
        this.treeRootHolder = treeRootHolder;
        this.analysisMetadataHolder = analysisMetadataHolder;
        this.configurationRepository = configurationRepository;
    }

    @Override
    public void persist(DbSession dbSession) {
        Branch branch = this.analysisMetadataHolder.getBranch();
        Component root = this.treeRootHolder.getRoot();
        String branchUuid = root.getUuid();
        Optional<ComponentDto> optBanchComponentDto = this.dbClient.componentDao().selectByUuid(dbSession, branchUuid);

        ComponentDto branchComponentDto;
        if (!optBanchComponentDto.isPresent()) {
            if (branch.getType() == BranchType.PULL_REQUEST || !branch.isMain()) {
                ComponentDto componentDto = createPullRequestComponentDto(branch, root);

                this.dbClient.componentDao().insert(dbSession, componentDto);
                branchComponentDto = componentDto;
            } else {
                throw new IllegalStateException("Component has been deleted by end-user during analysis");
            }
        } else {
            branchComponentDto = optBanchComponentDto.get();
        }
        this.dbClient.branchDao().upsert(dbSession, this.toBranchDto(dbSession, branchComponentDto, branch, this.checkIfExcludedFromPurge()));
    }

    @NotNull
    private static ComponentDto createPullRequestComponentDto(Branch branch, Component root) {
        ComponentDto componentDto = new ComponentDto();
        componentDto.setUuid(root.getUuid());
        componentDto.setRootUuid(root.getUuid());
        componentDto.setBranchUuid(root.getUuid());
        componentDto.setKey(root.getKey());
        componentDto.setScope("PRJ");
        componentDto.setQualifier("TRK");
        componentDto.setName(root.getName());
        componentDto.setLongName(root.getName());
        componentDto.setUuidPath(ComponentDto.UUID_PATH_OF_ROOT);
        componentDto.setPrivate(false);
        componentDto.setMainBranchProjectUuid(branch.getReferenceBranchUuid());
        return componentDto;
    }

    private boolean checkIfExcludedFromPurge() {
        if (this.analysisMetadataHolder.getBranch().isMain()) {
            return true;
        } else if (BranchType.PULL_REQUEST.equals(this.analysisMetadataHolder.getBranch().getType())) {
            return false;
        } else {
            String[] branchesToKeep = this.configurationRepository.getConfiguration().getStringArray("sonar.dbcleaner.branchesToKeepWhenInactive");
            return Arrays.stream(branchesToKeep).map(Pattern::compile).anyMatch((excludePattern) -> excludePattern.matcher(this.analysisMetadataHolder.getBranch().getName()).matches());
        }
    }

    // REF - https://github.com/SonarSource/sonarqube/blob/7fe987ca5622f7732223496787ac13e6bb592d74/server/sonar-ce-task-projectanalysis/src/main/java/org/sonar/ce/task/projectanalysis/component/BranchPersisterImpl.java#L79
    protected BranchDto toBranchDto(DbSession dbSession, ComponentDto componentDto, Branch branch, boolean excludeFromPurge) {
        BranchDto dto = new BranchDto();
        dto.setUuid(componentDto.uuid());

        // MainBranchProjectUuid will be null if it's a main branch
        String projectUuid = firstNonNull(componentDto.getMainBranchProjectUuid(), componentDto.branchUuid());
        dto.setProjectUuid(projectUuid);
        dto.setBranchType(branch.getType());
        dto.setExcludeFromPurge(excludeFromPurge);

        // merge branch is only present if it's not a main branch and not an application
        if (!branch.isMain() && !"APP".equals(componentDto.qualifier())) {
            dto.setMergeBranchUuid(branch.getReferenceBranchUuid());
        }

        if (branch.getType() == BranchType.PULL_REQUEST) {
            String pullRequestKey = this.analysisMetadataHolder.getPullRequestKey();
            dto.setKey(pullRequestKey);
            DbProjectBranches.PullRequestData pullRequestData = this.getBuilder(dbSession, projectUuid, pullRequestKey)
                    .setBranch(branch.getName())
                    .setTitle(branch.getName())
                    .setTarget(branch.getTargetBranchName())
                    .build();
            dto.setPullRequestData(pullRequestData);
        } else {
            dto.setKey(branch.getName());
        }

        return dto;
    }

    private DbProjectBranches.PullRequestData.Builder getBuilder(DbSession dbSession, String projectUuid, String pullRequestKey) {
        return this.dbClient.branchDao().selectByPullRequestKey(dbSession, projectUuid, pullRequestKey).map(BranchDto::getPullRequestData).map(DbProjectBranches.PullRequestData::toBuilder).orElse(DbProjectBranches.PullRequestData.newBuilder());
    }

    private static <T> T firstNonNull(@Nullable T first, T second) {
        return first != null ? first : second;
    }
}
