/*
 * Copyright (C) 2020-2021 Michael Clarke
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
package com.github.mc1arke.sonarqube.plugin.almclient.bitbucket;

import com.github.mc1arke.sonarqube.plugin.almclient.bitbucket.model.server.ErrorResponse;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class BitbucketExceptionTest {

    @Test
    public void verifyMessageReturnedWhenErrorResponseContainsNoErrors() {
        BitbucketException testCase = new BitbucketException(401, new ErrorResponse(null));
        assertThat(testCase.getMessage()).isEqualTo("Bitbucket responded with an error status (401)");
    }
}
