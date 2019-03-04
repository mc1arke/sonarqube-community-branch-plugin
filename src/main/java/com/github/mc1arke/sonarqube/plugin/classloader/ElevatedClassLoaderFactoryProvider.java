package com.github.mc1arke.sonarqube.plugin.classloader;

import org.sonar.api.Plugin;

public interface ElevatedClassLoaderFactoryProvider {
    ElevatedClassLoaderFactory createFactory(Plugin.Context context);
}
