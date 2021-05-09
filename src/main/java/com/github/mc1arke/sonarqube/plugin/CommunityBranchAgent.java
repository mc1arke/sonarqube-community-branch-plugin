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

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.NotFoundException;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;

import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.security.ProtectionDomain;

public final class CommunityBranchAgent {

    private static final Logger LOGGER = Loggers.get(CommunityBranchAgent.class);

    private CommunityBranchAgent() {
        super();
    }

    public static void premain(String args, Instrumentation instrumentation) throws UnmodifiableClassException, ClassNotFoundException {
        LOGGER.info("Loading agent");

        if (!"ce".equals(args) && !"web".equals(args)) {
            throw new IllegalArgumentException("Invalid/missing agent argument");
        }

        if ("ce".equals(args)) {
            redefineEdition(instrumentation);
        }
    }

    private static void redefineEdition(Instrumentation instrumentation) throws ClassNotFoundException, UnmodifiableClassException {
        String targetClassName = "org.sonar.core.platform.PlatformEditionProvider";

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
                    CtMethod m = cc.getDeclaredMethod("get");
                    m.setBody("return java.util.Optional.of(org.sonar.core.platform.EditionProvider.Edition.DEVELOPER);");

                    byteCode = cc.toBytecode();
                    cc.detach();
                } catch (NotFoundException | CannotCompileException | IOException e) {
                    LOGGER.error("Could not transform class, will use default class definition", e);
                }

                return byteCode;
            }

        });

        instrumentation.retransformClasses(Class.forName(targetClassName));
    }

}
