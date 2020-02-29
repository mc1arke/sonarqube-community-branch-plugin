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
7.8 - 8.0         | 1.3.0
7.4 - 7.7         | 1.0.2

# Installation
Either build the project or [download a compatible release version of the plugin JAR](https://github.com/mc1arke/sonarqube-community-branch-plugin/releases). Copy the plugin JAR file to the `extensions/plugins/` **and** the `lib/common/` directories of your SonarQube instance and restart SonarQube.

# Features
The plugin is intended to support the [features and parameters specified in the SonarQube documentation](https://docs.sonarqube.org/latest/branches/overview/), with the following caveats
* __Pull Requests:__ Analysis of Pull Requests is fully supported, but the decoration of pull requests is only currently available for Github, Gitlab, Bitbucket Server and Bitbucket Cloud

# Pull request decoration

## Bitbucket Server

To enable setting of several properties in SonarQube on project level is required.

The property "projectKey" or "userSlug" are mandatory in order to decide which API endpoint should be used.

## Bitbucket Cloud

Please be aware that when using the Bitbucket cloud PR decoration feature the resulting HTML that gets rendered by
Bitbucket is misbehaving. We have already opened a ticket for further investigation and will update this note once it
is resolved.

| Property         | Description                                                     | Example             |
|------------------|-----------------------------------------------------------------|---------------------|
| `appPassword`    | The App password used for accessing the Bitbucket API           | ySHHJDFZIUDFJGHJGDF |
| `appUsername`    | The App username used for accessing the Bitbucket API           | username            |
| `userUuid`       | The user uuid that is used when deleting old comments           | {UUID}              |
| `workspace`      | The workspace used for the pull request decoration              | organization        |
| `repositorySlug` | The repository slug used. This is your project id in bitbucket. | sonartest           |

# Contribution
To generate the jar file to copy to your Sonar Server execute ```./gradlew clean build``` inside of the project dir. This will put the jar under ```libs/sonarqube-community-branch-plugin*.jar```

## Development with a local sonarqube

Add the plugin to the `extensions/plugins/` and also into the `lib/common/` directory of your SonarQube instance and restart the server.

## Development with docker

You can use the `docker-compose.yaml` file from inside the `./development` folder for local testing against the LTS version of sonarqube.

Now it would be very easy for you to set up a simple bash script that does the following:

* Rebuild the plugin
* Copy the jar into the correct place
* Restart/start your sonarqube server with the plugin JAR mounted in the correct places

Please note that as of now it is *not* possible to debug the plugin. You'll still need a local instance if you
want to use debugging tools.
