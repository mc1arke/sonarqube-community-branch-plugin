/*
 * Copyright (C) 2020-2023 Michael Clarke
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
package com.github.mc1arke.sonarqube.plugin.server;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.sonar.api.config.Configuration;
import org.sonar.core.util.SequenceUuidFactory;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.BranchDao;
import org.sonar.db.component.BranchDto;
import org.sonar.db.component.BranchType;
import org.sonar.db.component.ComponentDao;
import org.sonar.db.component.ComponentDto;
import org.sonar.server.ce.queue.BranchSupport;
import org.sonar.server.setting.ProjectConfigurationLoader;

/**
 * @author Michael Clarke
 */
class CommunityBranchSupportDelegateTest {

    private final Clock clock = mock(Clock.class);
    private final SequenceUuidFactory sequenceUuidFactory = mock(SequenceUuidFactory.class);
    private final DbClient dbClient = mock(DbClient.class);
    private final ProjectConfigurationLoader projectConfigurationLoader = mock(ProjectConfigurationLoader.class);
    private final CommunityBranchSupportDelegate underTest = new CommunityBranchSupportDelegate(sequenceUuidFactory, dbClient, clock, projectConfigurationLoader);

    @Test
    void shouldReturnValidComponentKeyForBranchParameters() {
        Map<String, String> params = new HashMap<>();
        params.put("branch", "release-1.1");
        params.put("branchType", "BRANCH");

        BranchSupport.ComponentKey componentKey = underTest.createComponentKey("yyy", params);

        assertThat(componentKey).usingRecursiveComparison().isEqualTo(new CommunityComponentKey("yyy", "release-1.1", null));
    }

    @Test
    void shouldReturnValidComponentKeyForPullRequestParameters() {
        Map<String, String> params = new HashMap<>();
        params.put("pullRequest", "pullrequestkey");

        CommunityComponentKey componentKey = underTest.createComponentKey("aaa", params);

        assertThat(componentKey).usingRecursiveComparison().isEqualTo(new CommunityComponentKey("aaa", null, "pullrequestkey"));
    }


    @Test
    void shouldThrowExceptionOnCreateComponentKeyMissingBranchTypeAndPullParameters() {
        Map<String, String> params = new HashMap<>();

        assertThatThrownBy(() -> underTest.createComponentKey("xxx", params)).isInstanceOf(IllegalArgumentException.class)
            .hasMessage("One of 'branchType' or 'pullRequest' parameters must be specified");
    }

    @Test
    void shouldThrowExceptoinOnCreateComponentKeyInvalidBranchTypeParameter() {
        Map<String, String> params = new HashMap<>();
        params.put("branchType", "abc");
        
        assertThatThrownBy(() -> underTest.createComponentKey("xxx", params)).isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Unsupported branch type 'abc'");
    }

    @Test
    void shouldThrowExceptionIfBranchAndComponentKeysMismatch() {
        DbSession dbSession = mock(DbSession.class);

        ComponentDto componentDto = mock(ComponentDto.class);
        when(componentDto.getKey()).thenReturn("otherComponentKey");
        when(componentDto.uuid()).thenReturn("componentUuid");

        ComponentDto copyComponentDto = spy(ComponentDto.class);
        when(componentDto.copy()).thenReturn(copyComponentDto);

        BranchDto branchDto = mock(BranchDto.class);
        when(branchDto.getUuid()).thenReturn("componentUuid");
        when(branchDto.getBranchType()).thenReturn(BranchType.BRANCH);

        when(clock.millis()).thenReturn(12345678901234L);

        BranchSupport.ComponentKey componentKey = mock(BranchSupport.ComponentKey.class);
        when(componentKey.getKey()).thenReturn("componentKey");
        when(componentKey.getBranchName()).thenReturn(Optional.of("dummy"));
        when(componentKey.getPullRequestKey()).thenReturn(Optional.empty());

        ComponentDao componentDao = mock(ComponentDao.class);

        when(dbClient.componentDao()).thenReturn(componentDao);

        assertThatThrownBy(() -> underTest.createBranchComponent(dbSession, componentKey, componentDto, branchDto)).isInstanceOf(IllegalStateException.class)
            .hasMessage("Component Key and Main Component Key do not match");

    }

    static Stream<Arguments> shouldCreateComponentAndBranchDtoIfValidationPassesData() {
       return Stream.of(
           Arguments.of("branchName", null, BranchType.BRANCH, new String[0], false),
           Arguments.of(null, "pullRequestKey", BranchType.PULL_REQUEST, new String[0], false),
           Arguments.of("complex-name", null, BranchType.BRANCH, new String[]{"abc", "def", "comp.*"}, true)
       );
    }

    @MethodSource("shouldCreateComponentAndBranchDtoIfValidationPassesData")
    @ParameterizedTest
    void shouldCreateComponentAndBranchDtoIfValidationPasses(String branchName, String pullRequestKey, BranchType branchType,
                                                             String[] retainBranchesConfiguration, boolean excludedFromPurge) {
        DbSession dbSession = mock(DbSession.class);

        ComponentDto componentDto = mock(ComponentDto.class);
        when(componentDto.getKey()).thenReturn("componentKey");
        when(componentDto.uuid()).thenReturn("componentUuid");

        ComponentDto copyComponentDto = mock(ComponentDto.class);
        when(componentDto.copy()).thenReturn(copyComponentDto);
        when(copyComponentDto.setBranchUuid(any())).thenReturn(copyComponentDto);
        when(copyComponentDto.setKey(any())).thenReturn(copyComponentDto);
        when(copyComponentDto.setUuidPath(any())).thenReturn(copyComponentDto);
        when(copyComponentDto.setUuid(any())).thenReturn(copyComponentDto);
        when(copyComponentDto.setCreatedAt(any())).thenReturn(copyComponentDto);

        BranchDto branchDto = mock(BranchDto.class);
        when(branchDto.getUuid()).thenReturn("componentUuid");
        when(branchDto.getKey()).thenReturn("nonDummy");

        when(clock.millis()).thenReturn(12345678901234L);

        BranchSupport.ComponentKey componentKey = mock(BranchSupport.ComponentKey.class);
        when(componentKey.getKey()).thenReturn("componentKey");
        when(componentKey.getBranchName()).thenReturn(Optional.ofNullable(branchName));
        when(componentKey.getPullRequestKey()).thenReturn(Optional.ofNullable(pullRequestKey));

        BranchDao branchDao = mock(BranchDao.class);
        ComponentDao componentDao = mock(ComponentDao.class);
        when(dbClient.componentDao()).thenReturn(componentDao);
        when(dbClient.branchDao()).thenReturn(branchDao);

        when(sequenceUuidFactory.create()).thenReturn("uuid0");

        Configuration configuration = mock(Configuration.class);
        when(configuration.getStringArray(any())).thenReturn(retainBranchesConfiguration);
        when(projectConfigurationLoader.loadProjectConfiguration(any(), any())).thenReturn(configuration);

        ComponentDto result = underTest.createBranchComponent(dbSession, componentKey, componentDto, branchDto);

        verify(componentDao).insert(dbSession, copyComponentDto, false);
        verify(copyComponentDto).setUuid("uuid0");
        verify(copyComponentDto).setUuidPath(".");
        verify(copyComponentDto).setCreatedAt(new Date(12345678901234L));

        assertThat(result).isSameAs(copyComponentDto);

        ArgumentCaptor<BranchDto> branchDtoArgumentCaptor = ArgumentCaptor.forClass(BranchDto.class);
        verify(branchDao).insert(eq(dbSession), branchDtoArgumentCaptor.capture());

        assertThat(branchDtoArgumentCaptor.getValue()).usingRecursiveComparison().isEqualTo(new BranchDto()
            .setBranchType(branchType)
            .setExcludeFromPurge(excludedFromPurge)
            .setProjectUuid("componentUuid")
            .setKey(branchType == BranchType.BRANCH ? branchName : pullRequestKey)
            .setUuid("uuid0")
            .setIsMain(false));
    }

}
