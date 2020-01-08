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
* __Pull Requests:__ Analysis of Pull Requests is fully supported, but the decoration of pull requests is only currently available for Github, Gitlab and Bitbucket Server, and only as an experimental feature

# Properties
Property key | Description 
--- | ---
com.github.mc1arke.sonarqube.plugin.branch.image-url-base | Can be set in `sonar.properties` file on the SonarQube server and is used to load the images for the PR comments. [Default base image location link.](https://raw.githubusercontent.com/mc1arke/sonarqube-community-branch-plugin/master/src/main/resources/pr-decoration-images)

## Bitbucket Server
To enable setting of several properties in SonarQube on project level is required.

The property "projectKey" or "userSlug" are mandatory in order to decide which API endpoint should be used.

Tasks:
- [x] overall comment
- [x] enable and disable file comment and overall comment 
- [x] file comment
- [x] reset comments (all comments are reset by property userSlug. It's therefore highly recommended to create a user in your company that's only purpose it is to comment sonar issues)

# Contribution
To generate the jar file to copy to your Sonar Server execute ```./gradlew clean build``` inside of the project dir. This will put the jar under ```libs/sonarqube-community-branch-plugin*.jar```

## SonarQube / Docker
Add the plugin to the `extensions/plugins/` and also into the `lib/common/` directory of your SonarQube instance and restart the server.

Quick start to your SonarQube docker container:
```
version: 2

services:
  sonarqube:
    image: sonarqube
    container_name: sonarqube
    ports:
      - 9000:9000
    networks:
      - sonarnet
    environment:
      - SONARQUBE_JDBC_URL=jdbc:postgresql://db:5432/sonar
      - SONARQUBE_JDBC_USERNAME=sonar
      - SONARQUBE_JDBC_PASSWORD=sonar
    volumes:
      - sonarqube_conf:/opt/sonarqube/conf
      - sonarqube_data:/opt/sonarqube/data
      - sonarqube_extensions:/opt/sonarqube/extensions
      - sonarqube_bundled-plugins:/opt/sonarqube/lib/bundled-plugins
      - sonarqube_common:/opt/sonarqube/lib/common

  db:
    image: postgres
    container_name: postgres
    networks:
      - sonarnet
    environment:
      - POSTGRES_USER=sonar
      - POSTGRES_PASSWORD=sonar
    volumes:
      - postgresql:/var/lib/postgresql
      - postgresql_data:/var/lib/postgresql/data
``` 
