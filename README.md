[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=mc1arke_sonarqube-community-branch-plugin&metric=alert_status)](https://sonarcloud.io/dashboard?id=mc1arke_sonarqube-community-branch-plugin)

# Sonarqube Community Branch Plugin
A plugin for SonarQube to allow branch analysis in the Community version.

# Compatibility
Use the following table to find the correct plugin version for each SonarQube version

SonarQube Version | Plugin Version
------------------|---------------
7.8+              | 1.1.0
7.4 - 7.7         | 1.0.2

# Installation
Either build the project or [download a compatible release version of the plugin JAR](https://github.com/mc1arke/sonarqube-community-branch-plugin/releases). Copy the plugin JAR file to the `extensions/plugins/` directory of your SonarQube instance and restart SonarQube.

# Features
The plugin is intended to support the features and parameters specified in the SonarQube documentation, with the following caveats
* __Pull Requests:__ Analysis of Pull Requests is fully supported, but the decoration of pull requests with any issues is not currently supported
