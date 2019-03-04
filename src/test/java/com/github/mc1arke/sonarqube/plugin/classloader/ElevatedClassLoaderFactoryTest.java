package com.github.mc1arke.sonarqube.plugin.classloader;

import org.hamcrest.core.IsEqual;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.Plugin;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;

import static org.junit.Assert.assertEquals;

public class ElevatedClassLoaderFactoryTest {

    private final ExpectedException expectedException = ExpectedException.none();

    @Rule
    public ExpectedException expectedException() {
        return expectedException;
    }

    @Test
    public void testExceptionOnInvalidLoader() {
        ElevatedClassLoaderFactory testCase = new ElevatedClassLoaderFactoryImpl();

        ClassLoader classLoaderImpl = new ClassLoader() {
        };

        expectedException.expect(IllegalStateException.class);
        expectedException.expectMessage(IsEqual.equalTo(
                "Incorrect ClassLoader type. Expected 'java.net.URLClassLoader' but got '" +
                classLoaderImpl.getClass().getName() + "'"));

        testCase.createClassLoader(classLoaderImpl, classLoaderImpl);
    }

    @Test
    public void testFallthroughClassLoading() throws IOException, ClassNotFoundException {
        ElevatedClassLoaderFactory testCase = new ElevatedClassLoaderFactoryImpl();

        try (URLClassLoader classLoader1 = new URLClassLoader(new URL[]{}) {
            public Class<?> loadClass(String name) throws ClassNotFoundException {
                if ("1".equals(name)) {
                    throw new ClassNotFoundException("Not here");
                }
                return this.getClass();
            }
        }; URLClassLoader classLoader2 = new URLClassLoader(new URL[]{}) {
            public Class<?> loadClass(String name) throws ClassNotFoundException {
                if ("2".equals(name)) {
                    throw new ClassNotFoundException("Whoops");
                }
                return this.getClass();
            }
        }) {
            ClassLoader createdClassLoader = testCase.createClassLoader(classLoader2, classLoader1);
            assertEquals(classLoader2.getClass(), createdClassLoader.loadClass("1"));

            expectedException.expect(ClassNotFoundException.class);
            expectedException.expectMessage(IsEqual.equalTo("Whoops"));

            createdClassLoader.loadClass("2");
        }
    }


    private static class ElevatedClassLoaderFactoryImpl implements ElevatedClassLoaderFactory {

        @Override
        public ClassLoader createClassLoader(Class<? extends Plugin> pluginClass) {
            return null;
        }
    }
}
