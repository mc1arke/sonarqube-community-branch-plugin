package com.github.mc1arke.sonarqube.plugin.classloader;

import org.junit.Test;
import org.sonar.api.Plugin;
import org.sonar.api.config.Configuration;

import java.util.Optional;

import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DefaultElevatedClassLoaderFactoryProviderTest {

    @Test
    public void validFactoryReturnedOnNoPropertiesSet() {
        Plugin.Context context = mock(Plugin.Context.class);
        Configuration configuration = mock(Configuration.class);
        when(context.getBootConfiguration()).thenReturn(configuration);
        when(configuration.get(any())).thenReturn(Optional.empty());

        assertTrue(DefaultElevatedClassLoaderFactoryProvider.getInstance()
                           .createFactory(context) instanceof ClassReferenceElevatedClassLoaderFactory);
    }

}
