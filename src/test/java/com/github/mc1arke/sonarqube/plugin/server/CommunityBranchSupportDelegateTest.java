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
package com.github.mc1arke.sonarqube.plugin.server;

import org.hamcrest.core.IsEqual;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.sonar.core.util.SequenceUuidFactory;
import org.sonar.core.util.UuidFactory;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.BranchDto;
import org.sonar.db.component.BranchType;
import org.sonar.db.component.ComponentDao;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.server.ce.queue.BranchSupport;

import java.time.Clock;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Michael Clarke
 */
public class CommunityBranchSupportDelegateTest {

    private final ExpectedException expectedException = ExpectedException.none();

    @Rule
    public ExpectedException expectedException() {
        return expectedException;
    }

    @Test
    public void testCreateComponentKeyBranchType() {
        Map<String, String> params = new HashMap<>();
        params.put("branch", "release-1.1");
        params.put("branchType", "BRANCH");

        BranchSupport.ComponentKey componentKey =
                new CommunityBranchSupportDelegate(new SequenceUuidFactory(), mock(DbClient.class), mock(Clock.class))
                        .createComponentKey("yyy", params);

        assertEquals("yyy:BRANCH:release-1.1", componentKey.getDbKey());
        assertEquals("yyy", componentKey.getKey());
        assertFalse(componentKey.getPullRequestKey().isPresent());
        assertFalse(componentKey.isMainBranch());
        assertTrue(componentKey.getBranch().isPresent());
        assertEquals("release-1.1", componentKey.getBranch().get().getName());
        assertTrue(componentKey.getMainBranchComponentKey().isMainBranch());
        assertEquals(BranchType.BRANCH, componentKey.getBranch().get().getType());
    }

    @Test
    public void testCreateComponentKeyPullRequest() {
        Map<String, String> params = new HashMap<>();
        params.put("pullRequest", "pullrequestkey");

        CommunityComponentKey componentKey =
                new CommunityBranchSupportDelegate(new SequenceUuidFactory(), mock(DbClient.class), mock(Clock.class))
                        .createComponentKey("yyy", params);
        assertEquals("yyy:PULL_REQUEST:pullrequestkey", componentKey.getDbKey());
        assertEquals("yyy", componentKey.getKey());
        assertTrue(componentKey.getPullRequestKey().isPresent());
        assertEquals("pullrequestkey", componentKey.getPullRequestKey().get());
        assertFalse(componentKey.isMainBranch());
        assertFalse(componentKey.getBranch().isPresent());
        assertTrue(componentKey.getMainBranchComponentKey().isMainBranch());
        CommunityComponentKey mainBranchComponentKey = componentKey.getMainBranchComponentKey();
        assertSame(mainBranchComponentKey, mainBranchComponentKey.getMainBranchComponentKey());
    }


    @Test
    public void testCreateComponentKeyMissingBranchTypeAndPullParameters() {
        Map<String, String> params = new HashMap<>();

        expectedException.expect(IllegalArgumentException.class);
        expectedException
                .expectMessage(IsEqual.equalTo("One of 'branchType' or 'pullRequest' parameters must be specified"));

        new CommunityBranchSupportDelegate(new SequenceUuidFactory(), mock(DbClient.class), mock(Clock.class))
                .createComponentKey("xxx", params);
    }

    @Test
    public void testCreateComponentKeyInvalidBranchTypeParameter() {
        Map<String, String> params = new HashMap<>();
        params.put("branchType", "abc");

        expectedException.expect(IllegalArgumentException.class);
        expectedException.expectMessage(IsEqual.equalTo("Unsupported branch type 'abc'"));

        new CommunityBranchSupportDelegate(new SequenceUuidFactory(), mock(DbClient.class), mock(Clock.class))
                .createComponentKey("xxx", params);
    }

    @Test
    public void testCreateBranchComponentComponentKeyComponentDtoKeyMismatch() {
        DbSession dbSession = mock(DbSession.class);
        OrganizationDto organizationDto = mock(OrganizationDto.class);

        ComponentDto componentDto = mock(ComponentDto.class);
        when(componentDto.getKey()).thenReturn("otherComponentKey");
        when(componentDto.uuid()).thenReturn("componentUuid");

        ComponentDto copyComponentDto = spy(ComponentDto.class);
        when(componentDto.copy()).thenReturn(copyComponentDto);

        BranchDto branchDto = mock(BranchDto.class);
        when(branchDto.getUuid()).thenReturn("componentUuid");
        when(branchDto.getBranchType()).thenReturn(BranchType.BRANCH);

        Clock clock = mock(Clock.class);
        when(clock.millis()).thenReturn(12345678901234L);

        BranchSupport.ComponentKey componentKey = mock(BranchSupport.ComponentKey.class);
        when(componentKey.getKey()).thenReturn("componentKey");
        when(componentKey.getDbKey()).thenReturn("dbKey");
        when(componentKey.getBranch()).thenReturn(Optional.of(new BranchSupport.Branch("dummy", BranchType.BRANCH)));
        when(componentKey.getPullRequestKey()).thenReturn(Optional.empty());

        ComponentDao componentDao = spy(mock(ComponentDao.class));

        DbClient dbClient = mock(DbClient.class);
        when(dbClient.componentDao()).thenReturn(componentDao);

        UuidFactory uuidFactory = new SequenceUuidFactory();

        expectedException.expect(IllegalStateException.class);
        expectedException.expectMessage(IsEqual.equalTo("Component Key and Main Component Key do not match"));

        new CommunityBranchSupportDelegate(uuidFactory, dbClient, clock)
                .createBranchComponent(dbSession, componentKey, organizationDto, componentDto, branchDto);

    }

    @Test
    public void testCreateBranchComponent() {
        DbSession dbSession = mock(DbSession.class);
        OrganizationDto organizationDto = mock(OrganizationDto.class);

        ComponentDto componentDto = mock(ComponentDto.class);
        when(componentDto.getKey()).thenReturn("componentKey");
        when(componentDto.uuid()).thenReturn("componentUuid");

        ComponentDto copyComponentDto = spy(ComponentDto.class);
        when(componentDto.copy()).thenReturn(copyComponentDto);

        BranchDto branchDto = mock(BranchDto.class);
        when(branchDto.getUuid()).thenReturn("componentUuid");
        when(branchDto.getKey()).thenReturn("dummy");

        Clock clock = mock(Clock.class);
        when(clock.millis()).thenReturn(12345678901234L);

        BranchSupport.ComponentKey componentKey = mock(BranchSupport.ComponentKey.class);
        when(componentKey.getKey()).thenReturn("componentKey");
        when(componentKey.getDbKey()).thenReturn("dbKey");
        when(componentKey.getBranch()).thenReturn(Optional.of(new BranchSupport.Branch("dummy", BranchType.BRANCH)));
        when(componentKey.getPullRequestKey()).thenReturn(Optional.empty());

        ComponentDao componentDao = spy(mock(ComponentDao.class));

        DbClient dbClient = mock(DbClient.class);
        when(dbClient.componentDao()).thenReturn(componentDao);

        UuidFactory uuidFactory = mock(UuidFactory.class);
        when(uuidFactory.create()).then(new Answer<String>() {
            private int i = 0;

            @Override
            public String answer(InvocationOnMock invocationOnMock) {
                return "uuid" + (i++);
            }
        });

        ComponentDto result = new CommunityBranchSupportDelegate(uuidFactory, dbClient, clock)
                .createBranchComponent(dbSession, componentKey, organizationDto, componentDto, branchDto);

        verify(componentDao).insert(dbSession, copyComponentDto);
        verify(copyComponentDto).setUuid("uuid0");
        verify(copyComponentDto).setProjectUuid("uuid0");
        verify(copyComponentDto).setRootUuid("uuid0");
        verify(copyComponentDto).setUuidPath(".");
        verify(copyComponentDto).setModuleUuidPath(".uuid0.");
        verify(copyComponentDto).setMainBranchProjectUuid("componentUuid");
        verify(copyComponentDto).setDbKey(componentKey.getDbKey());
        verify(copyComponentDto).setCreatedAt(new Date(12345678901234L));

        assertSame(copyComponentDto, result);


    }

    @Test
    public void testCreateBranchComponentUseExistingDto() {
        DbSession dbSession = mock(DbSession.class);
        OrganizationDto organizationDto = mock(OrganizationDto.class);

        ComponentDto componentDto = mock(ComponentDto.class);
        when(componentDto.getKey()).thenReturn("componentKey");
        when(componentDto.uuid()).thenReturn("componentUuid");

        ComponentDto copyComponentDto = spy(ComponentDto.class);
        when(componentDto.copy()).thenReturn(copyComponentDto);

        BranchDto branchDto = mock(BranchDto.class);
        when(branchDto.getUuid()).thenReturn("componentUuid");
        when(branchDto.getKey()).thenReturn("dummy");
        when(branchDto.getBranchType()).thenReturn(BranchType.BRANCH);

        Clock clock = mock(Clock.class);
        when(clock.millis()).thenReturn(1234567890123L);

        BranchSupport.ComponentKey componentKey = mock(BranchSupport.ComponentKey.class);
        when(componentKey.getKey()).thenReturn("componentKey");
        when(componentKey.getDbKey()).thenReturn("dbKey");
        when(componentKey.getBranch()).thenReturn(Optional.of(new BranchSupport.Branch("dummy", BranchType.BRANCH)));
        when(componentKey.getPullRequestKey()).thenReturn(Optional.empty());

        ComponentDao componentDao = spy(mock(ComponentDao.class));

        DbClient dbClient = mock(DbClient.class);
        when(dbClient.componentDao()).thenReturn(componentDao);

        UuidFactory uuidFactory = new SequenceUuidFactory();

        ComponentDto result = new CommunityBranchSupportDelegate(uuidFactory, dbClient, clock)
                .createBranchComponent(dbSession, componentKey, organizationDto, componentDto, branchDto);

        assertSame(componentDto, result);

    }

    @Test
    public void testCreateBranchComponentUseExistingDto2() {
        DbSession dbSession = mock(DbSession.class);
        OrganizationDto organizationDto = mock(OrganizationDto.class);

        ComponentDto componentDto = mock(ComponentDto.class);
        when(componentDto.getKey()).thenReturn("componentKey");
        when(componentDto.uuid()).thenReturn("componentUuid");

        ComponentDto copyComponentDto = spy(ComponentDto.class);
        when(componentDto.copy()).thenReturn(copyComponentDto);

        BranchDto branchDto = mock(BranchDto.class);
        when(branchDto.getUuid()).thenReturn("componentUuid");
        when(branchDto.getKey()).thenReturn("dummy");
        when(branchDto.getBranchType()).thenReturn(BranchType.BRANCH);

        Clock clock = mock(Clock.class);
        when(clock.millis()).thenReturn(1234567890123L);

        BranchSupport.ComponentKey componentKey = mock(BranchSupport.ComponentKey.class);
        when(componentKey.getKey()).thenReturn("componentKey");
        when(componentKey.getDbKey()).thenReturn("dbKey");
        when(componentKey.getBranch()).thenReturn(Optional.empty());
        when(componentKey.getPullRequestKey()).thenReturn(Optional.empty());

        ComponentDao componentDao = spy(mock(ComponentDao.class));

        DbClient dbClient = mock(DbClient.class);
        when(dbClient.componentDao()).thenReturn(componentDao);

        UuidFactory uuidFactory = new SequenceUuidFactory();

        ComponentDto result = new CommunityBranchSupportDelegate(uuidFactory, dbClient, clock)
                .createBranchComponent(dbSession, componentKey, organizationDto, componentDto, branchDto);

        verify(componentDao).insert(dbSession, copyComponentDto);
        verify(copyComponentDto).setUuid("1");
        verify(copyComponentDto).setProjectUuid("1");
        verify(copyComponentDto).setRootUuid("1");
        verify(copyComponentDto).setUuidPath(".");
        verify(copyComponentDto).setModuleUuidPath(".1.");
        verify(copyComponentDto).setMainBranchProjectUuid("componentUuid");
        verify(copyComponentDto).setDbKey(componentKey.getDbKey());
        verify(copyComponentDto).setCreatedAt(new Date(1234567890123L));

        assertSame(copyComponentDto, result);

    }


}
