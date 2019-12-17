package com.github.mc1arke.sonarqube.plugin;

/**
 * Provides a marker to indicate a class is implementing a feature to remain compatible with a specific version of Sonarqube.
 * Classes should not normally implement this interface directly, but should implement an interface that extends from one
 * of the sub-interfaces included in this interface.
 *
 * When creating the compatibility interfaces for individual features, mark methods as {@link Deprecated} if they're only
 * there to provide compatibility with historic builds of SonarQube, otherwise put a comment on the method to make it clear
 * it's a temporary marker to allow {@link Override} annotations and allow suporession of build warnings when building
 * against older SonarQube versions.
 *
 * @author Michael Clarke
 */
public interface SonarqubeCompatibility {

    /**
     * A marker for all features needed from SonarQube v7 that are no longer present in later v7 releases, or in v8, or
     * features that have been specifically written to allow support for running a plugin against newer version when
     * built against v7.
     */
    interface Major7 extends SonarqubeCompatibility {

        /**
         * A marker for all features needed from SonarQube v7.8 that are no longer present in v7.9.
         */
        interface Minor8 extends Major7 {

        }

        /**
         * A marker for all features needed from SonarQube v7.9 that are no longer present in v8.0
         */
        interface Minor9 extends Major7 {

        }

    }

    /**
     * A marker for all features needed from SonarQube v8 that are no longer present in later v8 releases.
     */
    interface Major8 extends SonarqubeCompatibility {

        /**
         * A marker for all features needed from SonarQube v8.0 that are no longer present in v8.1.
         */
        interface Minor0 extends Major8 {

        }
    }
}
