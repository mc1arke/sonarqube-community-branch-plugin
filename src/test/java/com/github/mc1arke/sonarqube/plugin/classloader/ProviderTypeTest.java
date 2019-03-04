package com.github.mc1arke.sonarqube.plugin.classloader;

import org.hamcrest.core.IsEqual;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.Plugin;
import org.sonar.api.config.Configuration;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ProviderTypeTest {

    private final ExpectedException expectedException = ExpectedException.none();

    @Rule
    public ExpectedException expectedException() {
        return expectedException;
    }

    @Test
    public void testReflectiveTypeMatched() {
        Plugin.Context context = mock(Plugin.Context.class);
        Configuration configuration = mock(Configuration.class);
        when(context.getBootConfiguration()).thenReturn(configuration);

        assertTrue(ProviderType.fromName("REFLECTIVE")
                           .createFactory(context) instanceof ReflectiveElevatedClassLoaderFactory);
    }

    @Test
    public void testClassReferenceTypeMatched() {
        Plugin.Context context = mock(Plugin.Context.class);
        Configuration configuration = mock(Configuration.class);
        when(context.getBootConfiguration()).thenReturn(configuration);

        assertTrue(ProviderType.fromName("CLASS_REFERENCE")
                           .createFactory(context) instanceof ClassReferenceElevatedClassLoaderFactory);
    }

    @Test
    public void errorOnInvalidConfig() {
        expectedException.expect(IllegalStateException.class);
        expectedException.expectMessage(IsEqual.equalTo("No provider with type 'xxx' could be found"));

        ProviderType.fromName("xxx");
    }
}
