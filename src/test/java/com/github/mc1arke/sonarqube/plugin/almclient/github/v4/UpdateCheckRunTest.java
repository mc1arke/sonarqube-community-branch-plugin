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
package com.github.mc1arke.sonarqube.plugin.almclient.github.v4;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.mc1arke.sonarqube.plugin.almclient.github.v4.model.CheckRun;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;

public class UpdateCheckRunTest {

    @Test
    public void deserialiseReturnsSerialiseInput() throws IOException {
        UpdateCheckRun testCase = new UpdateCheckRun(new CheckRun("check run ID"));

        ObjectMapper objectMapper = new ObjectMapper();
        String serialised = objectMapper.writeValueAsString(testCase);

        UpdateCheckRun deserialised = objectMapper.readerFor(UpdateCheckRun.class).readValue(serialised);

        assertEquals("check run ID", deserialised.getCheckRun().getId());
    }
}
