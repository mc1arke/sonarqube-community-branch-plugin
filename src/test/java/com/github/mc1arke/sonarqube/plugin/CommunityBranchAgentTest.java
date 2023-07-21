/*
 * Copyright (C) 2021-2023 Michael Clarke
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.io.InputStream;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.lang.reflect.Field;
import java.util.Optional;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.sonar.api.SonarRuntime;
import org.sonar.core.documentation.DefaultDocumentationLinkGenerator;
import org.sonar.core.documentation.DocumentationLinkGenerator;
import org.sonar.core.platform.EditionProvider;
import org.sonar.core.platform.PlatformEditionProvider;
import org.sonar.db.DbClient;
import org.sonar.db.newcodeperiod.NewCodePeriodDao;
import org.sonar.server.almsettings.MultipleAlmFeature;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.feature.SonarQubeFeature;
import org.sonar.server.newcodeperiod.ws.SetAction;
import org.sonar.server.newcodeperiod.ws.UnsetAction;
import org.sonar.server.user.UserSession;

class CommunityBranchAgentTest {

    @Test
    void shouldThrowErrorIfAgentArgsNotValid() {
        assertThatThrownBy(() -> CommunityBranchAgent.premain("badarg", mock(Instrumentation.class)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid/missing agent argument");
    }

    @Test
    void shouldRedefineMultipleAlmFeatureClassForWebLaunch() throws ReflectiveOperationException, IOException, UnmodifiableClassException, IllegalClassFormatException {
        CustomClassloader classLoader = new CustomClassloader();
        Instrumentation instrumentation = mock(Instrumentation.class);

        CommunityBranchAgent.premain("web", instrumentation);

        ArgumentCaptor<ClassFileTransformer> classFileTransformerArgumentCaptor = ArgumentCaptor.forClass(ClassFileTransformer.class);
        verify(instrumentation).retransformClasses(MultipleAlmFeature.class);
        verify(instrumentation, times(3)).addTransformer(classFileTransformerArgumentCaptor.capture());

        try (InputStream inputStream = MultipleAlmFeature.class.getResourceAsStream(MultipleAlmFeature.class.getSimpleName() + ".class")) {
            byte[] input = IOUtils.toByteArray(inputStream);
            byte[] result = classFileTransformerArgumentCaptor.getAllValues().get(0).transform(classLoader, MultipleAlmFeature.class.getName().replaceAll("\\.", "/"), getClass(), getClass().getProtectionDomain(), input);
            Class<SonarQubeFeature> redefined = (Class<SonarQubeFeature>) classLoader.loadClass(MultipleAlmFeature.class.getName(), result);

            SonarRuntime sonarRuntime = mock(SonarRuntime.class);

            SonarQubeFeature multipleAlmFeatureProvider = redefined.getConstructor(SonarRuntime.class).newInstance(sonarRuntime);
            assertThat(multipleAlmFeatureProvider.isAvailable()).isTrue();
        }
    }


    @Test
    void shouldRedefineSetActionClassForWebLaunch() throws ReflectiveOperationException, IOException, UnmodifiableClassException, IllegalClassFormatException {
        CustomClassloader classLoader = new CustomClassloader();
        Instrumentation instrumentation = mock(Instrumentation.class);
        DocumentationLinkGenerator documentationLinkGenerator = mock(DefaultDocumentationLinkGenerator.class);

        CommunityBranchAgent.premain("web", instrumentation);

        ArgumentCaptor<ClassFileTransformer> classFileTransformerArgumentCaptor = ArgumentCaptor.forClass(ClassFileTransformer.class);
        verify(instrumentation).retransformClasses(SetAction.class);
        verify(instrumentation, times(3)).addTransformer(classFileTransformerArgumentCaptor.capture());

        try (InputStream inputStream = SetAction.class.getResourceAsStream(SetAction.class.getSimpleName() + ".class")) {
            byte[] input = IOUtils.toByteArray(inputStream);
            byte[] result = classFileTransformerArgumentCaptor.getAllValues().get(1).transform(classLoader, SetAction.class.getName().replaceAll("\\.", "/"), getClass(), getClass().getProtectionDomain(), input);

            Class<?> setActionClass = classLoader.loadClass(SetAction.class.getName(), result);

            DbClient dbClient = mock(DbClient.class);
            ComponentFinder componentFinder = mock(ComponentFinder.class);
            UserSession userSession = mock(UserSession.class);
            PlatformEditionProvider platformEditionProvider = mock(PlatformEditionProvider.class);
            NewCodePeriodDao newCodePeriodDao = mock(NewCodePeriodDao.class);

      Object setAction = setActionClass.getConstructor(DbClient.class, UserSession.class, ComponentFinder.class, PlatformEditionProvider.class, NewCodePeriodDao.class, DocumentationLinkGenerator.class)
              .newInstance(dbClient, userSession, componentFinder, platformEditionProvider, newCodePeriodDao, documentationLinkGenerator);

            Field editionProviderField = setActionClass.getDeclaredField("editionProvider");
            editionProviderField.setAccessible(true);
            assertThat(((EditionProvider) editionProviderField.get(setAction)).get()).isEqualTo(Optional.of(EditionProvider.Edition.DEVELOPER));
        }
    }

    @Test
    void shouldRedefinesUnsetActionClassForWebLaunch() throws IOException, UnmodifiableClassException, IllegalClassFormatException, ReflectiveOperationException {
        CustomClassloader classLoader = new CustomClassloader();

        Instrumentation instrumentation = mock(Instrumentation.class);
        CommunityBranchAgent.premain("web", instrumentation);
        DocumentationLinkGenerator documentationLinkGenerator = mock(DefaultDocumentationLinkGenerator.class);

        ArgumentCaptor<ClassFileTransformer> classFileTransformerArgumentCaptor = ArgumentCaptor.forClass(ClassFileTransformer.class);
        verify(instrumentation).retransformClasses(UnsetAction.class);
        verify(instrumentation, times(3)).addTransformer(classFileTransformerArgumentCaptor.capture());

        try (InputStream inputStream = SetAction.class.getResourceAsStream(SetAction.class.getSimpleName() + ".class")) {
            byte[] input = IOUtils.toByteArray(inputStream);
            byte[] result = classFileTransformerArgumentCaptor.getAllValues().get(2).transform(classLoader, UnsetAction.class.getName().replaceAll("\\.", "/"), getClass(), getClass().getProtectionDomain(), input);

            Class<?> unsetActionClass = classLoader.loadClass(UnsetAction.class.getName(), result);
            DbClient dbClient = mock(DbClient.class);
            ComponentFinder componentFinder = mock(ComponentFinder.class);
            UserSession userSession = mock(UserSession.class);
            PlatformEditionProvider platformEditionProvider = mock(PlatformEditionProvider.class);
            NewCodePeriodDao newCodePeriodDao = mock(NewCodePeriodDao.class);

            Object setAction = unsetActionClass.getConstructor(DbClient.class, UserSession.class, ComponentFinder.class, PlatformEditionProvider.class, NewCodePeriodDao.class, DocumentationLinkGenerator.class)
                    .newInstance(dbClient, userSession, componentFinder, platformEditionProvider, newCodePeriodDao, documentationLinkGenerator);

            Field editionProviderField = unsetActionClass.getDeclaredField("editionProvider");
            editionProviderField.setAccessible(true);
            assertThat(((EditionProvider) editionProviderField.get(setAction)).get()).isEqualTo(Optional.of(EditionProvider.Edition.DEVELOPER));
        }
    }

    @Test
    void shouldSkipNonTargetClasForWebLaunch() throws UnmodifiableClassException, ClassNotFoundException, IllegalClassFormatException {
        Instrumentation instrumentation = mock(Instrumentation.class);

        CommunityBranchAgent.premain("web", instrumentation);

        ArgumentCaptor<ClassFileTransformer> classFileTransformerArgumentCaptor = ArgumentCaptor.forClass(ClassFileTransformer.class);
        verify(instrumentation).retransformClasses(MultipleAlmFeature.class);
        verify(instrumentation, times(3)).addTransformer(classFileTransformerArgumentCaptor.capture());

        byte[] input = new byte[]{1, 2, 3, 4, 5, 6};
        byte[] result = classFileTransformerArgumentCaptor.getValue().transform(getClass().getClassLoader(), "com/github/mc1arke/Dummy", getClass(), getClass().getProtectionDomain(), input);

        assertThat(result).isEqualTo(input);
    }

    @Test
    void shouldSkipNonTargetClassForCeLunch() throws UnmodifiableClassException, ClassNotFoundException, IllegalClassFormatException {
        Instrumentation instrumentation = mock(Instrumentation.class);

        CommunityBranchAgent.premain("ce", instrumentation);

        ArgumentCaptor<ClassFileTransformer> classFileTransformerArgumentCaptor = ArgumentCaptor.forClass(ClassFileTransformer.class);
        verify(instrumentation).retransformClasses(PlatformEditionProvider.class);
        verify(instrumentation, times(2)).addTransformer(classFileTransformerArgumentCaptor.capture());

        byte[] input = new byte[]{1, 2, 3, 4, 5, 6};
        byte[] result = classFileTransformerArgumentCaptor.getValue().transform(getClass().getClassLoader(), "com/github/mc1arke/Dummy", getClass(), getClass().getProtectionDomain(), input);

        assertThat(result).isEqualTo(input);
    }

    @Test
    void shouldRedefineTargetClassesForCeLaunch() throws ReflectiveOperationException, IOException, UnmodifiableClassException, IllegalClassFormatException {
        Instrumentation instrumentation = mock(Instrumentation.class);

        CommunityBranchAgent.premain("ce", instrumentation);

        ArgumentCaptor<ClassFileTransformer> classFileTransformerArgumentCaptor = ArgumentCaptor.forClass(ClassFileTransformer.class);
        verify(instrumentation).retransformClasses(MultipleAlmFeature.class);
        verify(instrumentation).retransformClasses(PlatformEditionProvider.class);
        verify(instrumentation, times(2)).addTransformer(classFileTransformerArgumentCaptor.capture());

        try (InputStream inputStream = PlatformEditionProvider.class.getResourceAsStream(PlatformEditionProvider.class.getSimpleName() + ".class")) {
            byte[] input = IOUtils.toByteArray(inputStream);
            byte[] result = classFileTransformerArgumentCaptor.getAllValues().get(0).transform(getClass().getClassLoader(), PlatformEditionProvider.class.getName().replaceAll("\\.", "/"), getClass(), getClass().getProtectionDomain(), input);

            CustomClassloader classLoader = new CustomClassloader();

            Class<EditionProvider> redefined = (Class<EditionProvider>) classLoader.loadClass(PlatformEditionProvider.class.getName(), result);
            assertThat(redefined.getConstructor().newInstance().get()).contains(EditionProvider.Edition.DEVELOPER);
        }

        try (InputStream inputStream = MultipleAlmFeature.class.getResourceAsStream(MultipleAlmFeature.class.getSimpleName() + ".class")) {
            byte[] input = IOUtils.toByteArray(inputStream);
            byte[] result = classFileTransformerArgumentCaptor.getAllValues().get(1).transform(getClass().getClassLoader(), MultipleAlmFeature.class.getName().replaceAll("\\.", "/"), getClass(), getClass().getProtectionDomain(), input);

            CustomClassloader classLoader = new CustomClassloader();

            SonarRuntime sonarRuntime = mock(SonarRuntime.class);

            Class<MultipleAlmFeature> redefined = (Class<MultipleAlmFeature>) classLoader.loadClass(MultipleAlmFeature.class.getName(), result);
            SonarQubeFeature multipleAlmFeatureProvider = redefined.getConstructor(SonarRuntime.class).newInstance(sonarRuntime);
            assertThat(multipleAlmFeatureProvider.isAvailable()).isTrue();
        }
    }

    private static class CustomClassloader extends ClassLoader {

        public Class<?> loadClass(String name, byte[] value) {
            return defineClass(name, value, 0, value.length);
        }

    }

}
