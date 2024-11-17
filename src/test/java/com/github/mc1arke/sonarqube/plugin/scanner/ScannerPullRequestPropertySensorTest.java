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
package com.github.mc1arke.sonarqube.plugin.scanner;

import org.junit.jupiter.api.Test;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.fs.internal.TestInputFileBuilder;
import org.sonar.api.batch.sensor.internal.SensorContextTester;
import org.sonar.api.utils.System2;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ScannerPullRequestPropertySensorTest {

    private final System2 system2 = mock();
    private final ScannerPullRequestPropertySensor sensor = new ScannerPullRequestPropertySensor(system2);

    @Test
    void testPropertySensorWithGitlabCIEnvValues() throws IOException {
        
        Path temp = Files.createTempDirectory("sensor");

        DefaultInputFile inputFile = new TestInputFileBuilder("foo", "src/Foo.xoo").initMetadata("a\nb\nc\nd\ne\nf\ng\nh\ni\n").build();
        SensorContextTester context = SensorContextTester.create(temp);
        context.fileSystem().add(inputFile);        

        when(system2.envVariable("GITLAB_CI")).thenReturn("true");
        when(system2.envVariable("CI_API_V4_URL")).thenReturn("value");
        when(system2.envVariable("CI_PROJECT_PATH")).thenReturn("value");
        when(system2.envVariable("CI_MERGE_REQUEST_PROJECT_URL")).thenReturn("value");
        when(system2.envVariable("CI_PIPELINE_ID")).thenReturn("value");        

        sensor.execute(context);

        Map<String, String> properties = context.getContextProperties();

        assertThat(properties).hasSize(2);
    }    

    @Test
    void testPropertySensorWithGitlabEnvValues() throws IOException {
        
        Path temp = Files.createTempDirectory("sensor");

        DefaultInputFile inputFile = new TestInputFileBuilder("foo", "src/Foo.xoo").initMetadata("a\nb\nc\nd\ne\nf\ng\nh\ni\n").build();
        SensorContextTester context = SensorContextTester.create(temp);
        context.fileSystem().add(inputFile);        

        when(system2.envVariable("GITLAB_CI")).thenReturn("true");
        when(system2.envVariable("CI_MERGE_REQUEST_PROJECT_URL")).thenReturn("value");
        when(system2.envVariable("CI_PIPELINE_ID")).thenReturn("value");

        sensor.execute(context);

        Map<String, String> properties = context.getContextProperties();

        assertThat(properties).hasSize(2);
    }
}
