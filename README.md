[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=mc1arke_sonarqube-community-branch-plugin&metric=alert_status)](https://sonarcloud.io/dashboard?id=mc1arke_sonarqube-community-branch-plugin) [![Build Status](https://travis-ci.org/mc1arke/sonarqube-community-branch-plugin.svg?branch=master)](https://travis-ci.org/mc1arke/sonarqube-community-branch-plugin)

# Sonarqube Community Branch Plugin
A plugin for SonarQube to allow branch analysis in the Community version.

# Support
This plugin is not maintained or supported by SonarSource and has no official upgrade path for migrating from the SonarQube Community Edition to any of the Commercial Editions (Developer, Enterprise, or Data Center Edition). Support for any problems is only available through issues on the Github repository or through alternative channels (e.g. StackOverflow) and any attempt to request support for this plugin directly from SonarSource or an affiliated channel (e.g. Sonar Community forum) is likely to result in your request being closed or ignored.

If you plan on migrating your SonarQube data to a commercial edition after using this plugin then please be aware that this may result in some or all of your data being lost due to this compatibility of this plugin and the official SonarQube branch features being untested.

# Compatibility
Use the following table to find the correct plugin version for each SonarQube version

SonarQube Version | Plugin Version
------------------|---------------
7.8+              | 1.2.0
7.4 - 7.7         | 1.0.2

# Installation
Either build the project or [download a compatible release version of the plugin JAR](https://github.com/mc1arke/sonarqube-community-branch-plugin/releases). Copy the plugin JAR file to the `extensions/plugins/` **and** the `lib/common/` directories of your SonarQube instance and restart SonarQube.

# Features
The plugin is intended to support the [features and parameters specified in the SonarQube documentation](https://docs.sonarqube.org/latest/branches/overview/), with the following caveats
* __Pull Requests:__ Analysis of Pull Requests is fully supported, but the decoration of pull requests with any issues is not currently supported
