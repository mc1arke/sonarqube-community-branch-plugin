/*
 * Copyright (C) 2026 Michael Clarke
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
package com.github.mc1arke.sonarqube.plugin.server.pullrequest.ws.support.action;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.sonar.api.server.authentication.UnauthorizedException;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService.NewAction;
import org.sonar.api.server.ws.WebService.NewController;
import org.sonar.api.utils.DateUtils;
import org.sonar.server.property.InternalProperties;
import org.sonar.server.user.UserSession;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class InfoWsActionTest {

    @Test
    void shouldThrowExceptionIfNotSignedIn() {
        UserSession userSession = mock();
        InternalProperties internalProperties = mock();
        InfoWsAction underTest = new InfoWsAction(userSession, internalProperties);
        Request request = mock();
        Response response = mock();

        when(userSession.checkLoggedIn()).thenThrow(new UnauthorizedException("Not logged in"));

        assertThatThrownBy(() -> underTest.handle(request, response))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("Not logged in");

        verify(userSession).checkLoggedIn();
        verifyNoMoreInteractions(userSession, internalProperties, request, response);
    }

    @Test
    void shouldThrowExceptionIfNotAdmin() {
        UserSession userSession = mock();
        InternalProperties internalProperties = mock();
        InfoWsAction underTest = new InfoWsAction(userSession, internalProperties);
        Request request = mock();
        Response response = mock();

        when(userSession.checkLoggedIn()).thenReturn(userSession);
        when(userSession.checkIsSystemAdministrator()).thenThrow(new UnauthorizedException("Not admin"));

        assertThatThrownBy(() -> underTest.handle(request, response))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("Not admin");

        verify(userSession).checkLoggedIn();
        verify(userSession).checkIsSystemAdministrator();
        verifyNoMoreInteractions(userSession, internalProperties, request, response);
    }

    @Test
    void shouldWriteResponseWithDateIfValueSetAndUserIsAdmin() throws Exception {
        UserSession userSession = mock();
        InternalProperties internalProperties = mock();
        InfoWsAction underTest = new InfoWsAction(userSession, internalProperties);
        Request request = mock();
        Response response = mock();
        Response.Stream stream = mock();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        when(stream.output()).thenReturn(outputStream);

        when(userSession.checkLoggedIn()).thenReturn(userSession);
        when(userSession.checkIsSystemAdministrator()).thenReturn(userSession);
        when(response.stream()).thenReturn(stream);
        when(internalProperties.read(InternalProperties.INSTALLATION_DATE)).thenReturn(Optional.of("1625097600000"));

        underTest.handle(request, response);

        String output = outputStream.toString(StandardCharsets.UTF_8);
        assertThat(output).isEqualTo("{\"statistics\":{\"installationDate\":\"" + DateUtils.formatDateTime(1625097600000L) + "\"}}");

        verify(userSession).checkLoggedIn();
        verify(userSession).checkIsSystemAdministrator();
        verify(response).stream();
        verify(internalProperties).read(InternalProperties.INSTALLATION_DATE);
        verify(stream).setMediaType("application/json");
        verify(stream).setStatus(200);
        verify(stream).output();
        verifyNoMoreInteractions(userSession, internalProperties, request, response, stream);
    }


    @Test
    void shouldWriteResponseWithoutDateIfValueNotSetAndUserIsAdmin() throws Exception {
        UserSession userSession = mock();
        InternalProperties internalProperties = mock();
        InfoWsAction underTest = new InfoWsAction(userSession, internalProperties);
        Request request = mock();
        Response response = mock();
        Response.Stream stream = mock();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        when(stream.output()).thenReturn(outputStream);

        when(userSession.checkLoggedIn()).thenReturn(userSession);
        when(userSession.checkIsSystemAdministrator()).thenReturn(userSession);
        when(response.stream()).thenReturn(stream);
        when(internalProperties.read(InternalProperties.INSTALLATION_DATE)).thenReturn(Optional.empty());

        underTest.handle(request, response);

        String output = outputStream.toString(StandardCharsets.UTF_8);
        assertThat(output).isEqualTo("{\"statistics\":{}}");

        verify(userSession).checkLoggedIn();
        verify(userSession).checkIsSystemAdministrator();
        verify(response).stream();
        verify(internalProperties).read(InternalProperties.INSTALLATION_DATE);
        verify(stream).setMediaType("application/json");
        verify(stream).setStatus(200);
        verify(stream).output();
        verifyNoMoreInteractions(userSession, internalProperties, request, response, stream);
    }

    @Test
    void shouldDefineAction() {
        UserSession userSession = mock();
        InternalProperties internalProperties = mock();
        InfoWsAction underTest = new InfoWsAction(userSession, internalProperties);
        NewController newController = mock();
        NewAction actionDefinition = mock();

        when(newController.createAction(any())).thenReturn(actionDefinition);

        underTest.define(newController);

        verify(newController).createAction("info");
        verify(actionDefinition).setHandler(underTest);
        verifyNoMoreInteractions(newController, actionDefinition, userSession, internalProperties);
    }

}