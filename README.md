[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=mc1arke_sonarqube-community-branch-plugin&metric=alert_status)](https://sonarcloud.io/dashboard?id=mc1arke_sonarqube-community-branch-plugin)
[![Build Status](https://img.shields.io/github/workflow/status/mc1arke/sonarqube-community-branch-plugin/build?label=build&logo=github)](https://github.com/mc1arke/sonarqube-community-branch-plugin?workflow=build)

# Sonarqube Community Branch Plugin
A plugin for SonarQube to allow branch analysis in the Community version.

# Support
This plugin is not maintained or supported by SonarSource and has no official upgrade path for migrating from the SonarQube Community Edition to any of the Commercial Editions (Developer, Enterprise, or Data Center Edition). Support for any problems is only available through issues on the Github repository or through alternative channels (e.g. StackOverflow) and any attempt to request support for this plugin directly from SonarSource or an affiliated channel (e.g. Sonar Community forum) is likely to result in your request being closed or ignored.

If you plan on migrating your SonarQube data to a commercial edition after using this plugin then please be aware that this may result in some or all of your data being lost due to this compatibility of this plugin and the official SonarQube branch features being untested.

# Compatibility
Use the following table to find the correct plugin version for each SonarQube version

SonarQube Version | Plugin Version
------------------|---------------
8.5               | 1.6.0
8.2 - 8.4         | 1.5.0
8.1               | 1.4.0
7.8 - 8.0         | 1.3.2
7.4 - 7.7         | 1.0.2

# Features
The plugin is intended to support the [features and parameters specified in the SonarQube documentation](https://docs.sonarqube.org/latest/branches/overview/).

# Installation
Either build the project or [download a compatible release version of the plugin JAR](https://github.com/mc1arke/sonarqube-community-branch-plugin/releases). Copy the plugin JAR file to the `extensions/plugins/` **and** the `lib/common/` directories of your SonarQube instance and restart SonarQube.

# Configuration
## Global configuration
Make sure `sonar.core.serverBaseURL` in SonarQube [/admin/settings](http://localhost:9000/admin/settings) is properly
 set in order to for the links in the comment to work.

Set all other properties that you can define globally for all of your projects.

## How to decorate the PR
In order to decorate your Pull Request's source branch, you need to analyze your target branch first.

### Run analysis of branches
  
The analysis needs the following setting:
`sonar.branch.name = branch_name (e.g master)`

### Run analysis of the PR branch
Carefully read the official SonarQube guide for [pull request decoration](https://docs.sonarqube.org/latest/analysis/pull-request/) 

In there you'll find the following properties that need to be set.
```
sonar.pullrequest.key = pull_request_id (e.g. 100)
sonar.pullrequest.branch = source_branch_name (e.g feature/TICKET-123)
sonar.pullrequest.base = target_branch_name (e.g master)
```

:warning: There must not be any `sonar.branch` properties like `sonar.branch.name` arguments set when you analyze a
  pull-request. These properties indicate to sonar that a branch is being analyzed rather than a pull-request so no
    pull-request decoration will be executed.
    
## Serving images for PR decoration
By default, images for PR decoration are served as static resources on the SonarQube server as a part of Community Branch Plugin. 

If you use a SonarQube server behind a firewall and/or PR service (Github, Gitlab etc.) hasn't access to SonarQube server, you should change `Images base URL` property in `General > Pull Request` settings.

Anyone needing to set this value can use the URL `https://raw.githubusercontent.com/mc1arke/sonarqube-community-branch-plugin/master/src/main/resources/static`, or download the files from this location and host them themself.
 
# Building the plugin from source
In case you want to try and test the current branch or build it for your development execute `./gradlew clean build
` inside of the project directory. This will put the built jar under `libs/sonarqube-community-branch-plugin*.jar`
