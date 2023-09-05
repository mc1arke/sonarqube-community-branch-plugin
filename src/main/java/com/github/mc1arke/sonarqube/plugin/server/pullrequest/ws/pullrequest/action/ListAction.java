/*
 * Copyright (C) 2009-2022 SonarSource SA (mailto:info AT sonarsource DOT com), Michael Clarke
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
 */
package com.github.mc1arke.sonarqube.plugin.server.pullrequest.ws.pullrequest.action;

import static org.sonar.server.user.AbstractUserSession.insufficientPrivilegesException;

import com.github.mc1arke.sonarqube.plugin.util.CommunityMoreCollectors;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import org.apache.commons.lang.StringUtils;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.DateUtils;
import org.sonar.api.web.UserRole;
import org.sonar.core.util.stream.MoreCollectors;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.BranchDao;
import org.sonar.db.component.BranchDto;
import org.sonar.db.component.BranchType;
import org.sonar.db.component.SnapshotDto;
import org.sonar.db.measure.LiveMeasureDto;
import org.sonar.db.permission.GlobalPermission;
import org.sonar.db.project.ProjectDto;
import org.sonar.db.protobuf.DbProjectBranches;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.user.UserSession;
import org.sonar.server.ws.WsUtils;
import org.sonarqube.ws.ProjectPullRequests;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.base.Strings;

public class ListAction extends ProjectWsAction {

    private final UserSession userSession;
    private final ProtoBufWriter protoBufWriter;

    @Autowired
    public ListAction(DbClient dbClient, ComponentFinder componentFinder, UserSession userSession) {
        this(dbClient, componentFinder, userSession, WsUtils::writeProtobuf);
    }

    ListAction(DbClient dbClient, ComponentFinder componentFinder, UserSession userSession, ProtoBufWriter protoBufWriter) {
        super("list", dbClient, componentFinder);
        this.userSession = userSession;
        this.protoBufWriter = protoBufWriter;
    }

    @Override
    protected void configureAction(WebService.NewAction action) {
        //no-op
    }

    @Override
    public void handleProjectRequest(ProjectDto project, Request request, Response response, DbSession dbSession) {
         checkPermission(project, userSession);

        BranchDao branchDao = getDbClient().branchDao();
        List<BranchDto> pullRequests = branchDao.selectByProject(dbSession, project).stream()
            .filter(b -> b.getBranchType() == BranchType.PULL_REQUEST)
            .collect(Collectors.toList());
        List<String> pullRequestUuids = pullRequests.stream().map(BranchDto::getUuid).collect(Collectors.toList());

        Map<String, BranchDto> mergeBranchesByUuid = branchDao
            .selectByUuids(dbSession, pullRequests.stream()
                .map(BranchDto::getMergeBranchUuid)
                .filter(Objects::nonNull)
                .collect(Collectors.toList()))
            .stream().collect(CommunityMoreCollectors.uniqueIndex(BranchDto::getUuid));

        Map<String, LiveMeasureDto> qualityGateMeasuresByComponentUuids = getDbClient().liveMeasureDao()
            .selectByComponentUuidsAndMetricKeys(dbSession, pullRequestUuids, List.of(CoreMetrics.ALERT_STATUS_KEY)).stream()
            .collect(CommunityMoreCollectors.uniqueIndex(LiveMeasureDto::getComponentUuid));
        Map<String, String> analysisDateByBranchUuid = getDbClient().snapshotDao().selectLastAnalysesByRootComponentUuids(dbSession, pullRequestUuids).stream()
            .collect(CommunityMoreCollectors.uniqueIndex(SnapshotDto::getUuid, s -> DateUtils.formatDateTime(s.getCreatedAt())));

        ProjectPullRequests.ListWsResponse.Builder protobufResponse = ProjectPullRequests.ListWsResponse.newBuilder();
        pullRequests
            .forEach(b -> addPullRequest(protobufResponse, b, mergeBranchesByUuid, qualityGateMeasuresByComponentUuids.get(b.getUuid()),
                analysisDateByBranchUuid.get(b.getUuid())));
        protoBufWriter.write(protobufResponse.build(), request, response);
    }

    private static void checkPermission(ProjectDto project, UserSession userSession) {
        if (userSession.hasEntityPermission(UserRole.USER, project) ||
            userSession.hasEntityPermission(UserRole.SCAN, project) ||
            userSession.hasPermission(GlobalPermission.SCAN)) {
            return;
        }
        throw insufficientPrivilegesException();
    }

    private static void addPullRequest(ProjectPullRequests.ListWsResponse.Builder response, BranchDto branch, Map<String, BranchDto> mergeBranchesByUuid,
                                       @Nullable LiveMeasureDto qualityGateMeasure, @Nullable String analysisDate) {
        Optional<BranchDto> mergeBranch = Optional.ofNullable(mergeBranchesByUuid.get(branch.getMergeBranchUuid()));

        ProjectPullRequests.PullRequest.Builder builder = ProjectPullRequests.PullRequest.newBuilder();
        builder.setKey(branch.getKey());

        DbProjectBranches.PullRequestData pullRequestData = Objects.requireNonNull(branch.getPullRequestData(), "Pull request data should be available for branch type PULL_REQUEST");
        builder.setBranch(pullRequestData.getBranch());
        Optional.ofNullable(Strings.emptyToNull(pullRequestData.getUrl())).ifPresent(builder::setUrl);
        Optional.ofNullable(Strings.emptyToNull(pullRequestData.getTitle())).ifPresent(builder::setTitle);

        if (mergeBranch.isPresent()) {
            String mergeBranchKey = mergeBranch.get().getKey();
            builder.setBase(mergeBranchKey);
        } else {
            builder.setIsOrphan(true);
        }

        if (StringUtils.isNotEmpty(pullRequestData.getTarget())) {
            builder.setTarget(pullRequestData.getTarget());
        } else {
            mergeBranch.ifPresent(branchDto -> builder.setTarget(branchDto.getKey()));
        }

        Optional.ofNullable(analysisDate).ifPresent(builder::setAnalysisDate);
        setQualityGate(builder, qualityGateMeasure);
        response.addPullRequests(builder);
    }

    private static void setQualityGate(ProjectPullRequests.PullRequest.Builder builder, @Nullable LiveMeasureDto qualityGateMeasure) {
        ProjectPullRequests.Status.Builder statusBuilder = ProjectPullRequests.Status.newBuilder();
        if (qualityGateMeasure != null) {
            Optional.ofNullable(qualityGateMeasure.getDataAsString()).ifPresent(statusBuilder::setQualityGateStatus);
        }
        builder.setStatus(statusBuilder);
    }
}
