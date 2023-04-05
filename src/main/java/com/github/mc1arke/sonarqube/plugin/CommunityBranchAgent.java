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

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtConstructor;
import javassist.CtMethod;
import javassist.NotFoundException;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.core.platform.EditionProvider;

import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.security.ProtectionDomain;
import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

public final class CommunityBranchAgent {

    private static final Logger LOGGER = Loggers.get(CommunityBranchAgent.class);

    private CommunityBranchAgent() {
        super();
    }

    public static void premain(String args, Instrumentation instrumentation) throws UnmodifiableClassException, ClassNotFoundException {
        LOGGER.info("Loading agent");

        Component component = Component.fromString(args).orElseThrow(() -> new IllegalArgumentException("Invalid/missing agent argument"));

        if (component == Component.CE) {
            redefineEdition(instrumentation, "org.sonar.core.platform.PlatformEditionProvider", redefineOptionalEditionGetMethod());
            redefineEdition(instrumentation, "org.sonar.server.almsettings.MultipleAlmFeature", redefineIsAvailableFlag());
        } else if (component == Component.WEB) {
            redefineEdition(instrumentation, "org.sonar.server.almsettings.MultipleAlmFeature", redefineIsAvailableFlag());
            redefineEdition(instrumentation, "org.sonar.server.newcodeperiod.ws.SetAction", redefineConstructorEditionProviderField(EditionProvider.Edition.DEVELOPER));
            redefineEdition(instrumentation, "org.sonar.server.newcodeperiod.ws.UnsetAction", redefineConstructorEditionProviderField(EditionProvider.Edition.DEVELOPER));
        }
    }

    private static void redefineEdition(Instrumentation instrumentation, String targetClassName, Redefiner redefiner) throws ClassNotFoundException, UnmodifiableClassException {
        instrumentation.addTransformer(new ClassFileTransformer() {

            @Override
            public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
                                    ProtectionDomain protectionDomain, byte[] byteCode) {

                String finalTargetClassName = targetClassName.replace(".", "/");

                if (!className.equals(finalTargetClassName)) {
                    return byteCode;
                }

                LOGGER.debug("Transforming class " + targetClassName);
                try {
                    ClassPool cp = ClassPool.getDefault();
                    CtClass cc = cp.get(targetClassName);

                    redefiner.redefine(cc);

                    byteCode = cc.toBytecode();
                    cc.detach();
                } catch (NotFoundException | CannotCompileException | IOException e) {
                    LOGGER.error(String.format("Could not transform class %s, will use default class definition", targetClassName), e);
                }

                return byteCode;
            }

        });

        instrumentation.retransformClasses(Class.forName(targetClassName));
    }


    private static Redefiner redefineOptionalEditionGetMethod() {
        return ctClass -> {
            CtMethod ctMethod = ctClass.getDeclaredMethod("get");
            ctMethod.setBody("return java.util.Optional.of(org.sonar.core.platform.EditionProvider.Edition.DEVELOPER);");
        };
    }

    private static Redefiner redefineIsAvailableFlag() {
        return ctClass -> {
            CtMethod ctMethod = ctClass.getDeclaredMethod("isAvailable");
            ctMethod.setBody("return true;");
        };
    }

    private static Redefiner redefineConstructorEditionProviderField(EditionProvider.Edition edition) {
        return ctClass -> {
            CtConstructor ctConstructor = ctClass.getDeclaredConstructors()[0];
            ctConstructor.insertAfter("this.editionProvider = new com.github.mc1arke.sonarqube.plugin.CommunityPlatformEditionProvider(org.sonar.core.platform.EditionProvider.Edition." + edition.name() + ");");
        };
    }

    private enum Component {
        CE, WEB;

        private static Optional<Component> fromString(String input) {
            return Arrays.stream(values()).filter(v -> v.name().toLowerCase(Locale.ENGLISH).equals(input)).findFirst();
        }
    }

    @FunctionalInterface
    private interface Redefiner {
        void redefine(CtClass ctClass) throws CannotCompileException, NotFoundException;
    }

}
