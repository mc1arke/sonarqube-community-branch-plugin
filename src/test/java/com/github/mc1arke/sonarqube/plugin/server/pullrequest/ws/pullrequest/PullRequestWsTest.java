/*
 * Copyright (C) 2022 Michael Clarke
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
package com.github.mc1arke.sonarqube.plugin.server.pullrequest.ws.pullrequest;


import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.sonar.api.server.ws.WebService;

import com.github.mc1arke.sonarqube.plugin.server.pullrequest.ws.pullrequest.action.PullRequestWsAction;

class PullRequestWsTest {

    @Test
    void shouldCallDefineOnEachAction() {
        PullRequestWsAction[] pullRequestWsActions = new PullRequestWsAction[]{mock(PullRequestWsAction.class), mock(PullRequestWsAction.class), mock(PullRequestWsAction.class)};

        WebService.Context context = mock(WebService.Context.class);
        WebService.NewController controller = mock(WebService.NewController.class);
        when(context.createController(any())).thenReturn(controller);

        new PullRequestWs(pullRequestWsActions).define(context);

        for (PullRequestWsAction pullRequestWsAction : pullRequestWsActions) {
            verify(pullRequestWsAction).define(controller);
        }
        verify(context).createController("api/project_pull_requests");
    }

}