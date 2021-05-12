/*
 * Copyright (C) 2021 Michael Clarke
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
package com.github.mc1arke.sonarqube.plugin;

import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.sonar.core.platform.EditionProvider;
import org.sonar.core.platform.PlatformEditionProvider;

import java.io.IOException;
import java.io.InputStream;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

public class CommunityBranchAgentTest {

    @Test
    public void checkErrorThrownIfAgentArgsNotValid() {
        assertThatThrownBy(() -> CommunityBranchAgent.premain("badarg", mock(Instrumentation.class)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid/missing agent argument");
    }


    @Test
    public void checkNoRedefineForWebLaunch() throws UnmodifiableClassException, ClassNotFoundException {
        Instrumentation instrumentation = mock(Instrumentation.class);

        CommunityBranchAgent.premain("web", instrumentation);

        verifyNoInteractions(instrumentation);
    }

    @Test
    public void checkRedefineForCeLaunchSkipsNonTargetClass() throws UnmodifiableClassException, ClassNotFoundException, IllegalClassFormatException {
        Instrumentation instrumentation = mock(Instrumentation.class);

        CommunityBranchAgent.premain("ce", instrumentation);

        ArgumentCaptor<ClassFileTransformer> classFileTransformerArgumentCaptor = ArgumentCaptor.forClass(ClassFileTransformer.class);
        verify(instrumentation).retransformClasses(PlatformEditionProvider.class);
        verify(instrumentation).addTransformer(classFileTransformerArgumentCaptor.capture());

        byte[] input = new byte[]{1, 2, 3, 4, 5, 6};
        byte[] result = classFileTransformerArgumentCaptor.getValue().transform(getClass().getClassLoader(), "com/github/mc1arke/Dummy", getClass(), getClass().getProtectionDomain(), input);

        assertThat(result).isEqualTo(input);
    }


    @Test
    public void checkRedefineForCeLaunchRedefinesTargetClass() throws ReflectiveOperationException, IOException, UnmodifiableClassException, IllegalClassFormatException {
        Instrumentation instrumentation = mock(Instrumentation.class);

        CommunityBranchAgent.premain("ce", instrumentation);

        ArgumentCaptor<ClassFileTransformer> classFileTransformerArgumentCaptor = ArgumentCaptor.forClass(ClassFileTransformer.class);
        verify(instrumentation).retransformClasses(PlatformEditionProvider.class);
        verify(instrumentation).addTransformer(classFileTransformerArgumentCaptor.capture());

        try (InputStream inputStream = PlatformEditionProvider.class.getResourceAsStream(PlatformEditionProvider.class.getSimpleName())) {
            byte[] input = IOUtils.toByteArray(inputStream);
            byte[] result = classFileTransformerArgumentCaptor.getValue().transform(getClass().getClassLoader(), PlatformEditionProvider.class.getName().replaceAll("\\.", "/"), getClass(), getClass().getProtectionDomain(), input);

            CustomClassloader classLoader = new CustomClassloader();

            Class<EditionProvider> redefined = (Class<EditionProvider>) classLoader.loadClass(PlatformEditionProvider.class.getName(), result);
            assertThat(redefined.getConstructor().newInstance().get()).isEqualTo(Optional.of(EditionProvider.Edition.DEVELOPER));
        }
    }

    private static class CustomClassloader extends ClassLoader {

        public Class<?> loadClass(String name, byte[] value) {
            return defineClass(name, value, 0, value.length);
        }

    };

}
