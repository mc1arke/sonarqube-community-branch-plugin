[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=mc1arke_sonarqube-community-branch-plugin&metric=alert_status)](https://sonarcloud.io/dashboard?id=mc1arke_sonarqube-community-branch-plugin)
[![Build Status](https://img.shields.io/github/actions/workflow/status/mc1arke/sonarqube-community-branch-plugin/.github/workflows/build.yml?branch=master&logo=github)](https://github.com/mc1arke/sonarqube-community-branch-plugin?workflow=build)

# Sonarqube Community Branch Plugin

A plugin for SonarQube to allow branch analysis in the Community version.

# Support

This plugin is not maintained or supported by SonarSource and has no official upgrade path for migrating from the
SonarQube Community Edition to any of the Commercial Editions (Developer, Enterprise, or Data Center Edition). Support
for any problems is only available through issues on the Github repository or through alternative channels (e.g.
StackOverflow) and any attempt to request support for this plugin directly from SonarSource or an affiliated channel (
e.g. Sonar Community forum) is likely to result in your request being closed or ignored.

If you plan on migrating your SonarQube data to a commercial edition after using this plugin then please be aware that
this may result in some or all of your data being lost due to this compatibility of this plugin and the official
SonarQube branch features being untested.

# Compatibility

Use the following table to find the correct plugin version for each SonarQube version

SonarQube Version | Plugin Version
------------------|---------------
9.8+              | 1.14.0
9.7               | 1.13.0
9.1 - 9.6         | 1.12.0
9.0               | 1.9.0
8.9               | 1.8.3
8.7 - 8.8         | 1.7.0
8.5 - 8.6         | 1.6.0
8.2 - 8.4         | 1.5.0
8.1               | 1.4.0
7.8 - 8.0         | 1.3.2
7.4 - 7.7         | 1.0.2

# Features

The plugin is intended to support the
[features and parameters from the SonarQube documentation](https://docs.sonarqube.org/latest/branches/overview/).

# Installation

## Manual Install

__Please ensure you follow the installation instructions for the version of the plugin you're installing by looking at
the README on the relevant release tag.__

Either build the project
or [download a compatible release version of the plugin JAR](https://github.com/mc1arke/sonarqube-community-branch-plugin/releases)
.

1. Copy the plugin JAR file to the `extensions/plugins/` directory of your SonarQube instance
2. Add `-javaagent:./extensions/plugins/sonarqube-community-branch-plugin-${version}.jar=web` to
   the `sonar.web.javaAdditionalOpts` property in your Sonarqube installation's `conf/sonar.properties` file,
   e.g. `sonar.web.javaAdditionalOpts=-javaagent:./extensions/plugins/sonarqube-community-branch-plugin-${version}.jar=web`
   where ${version} is the version of the plugin being worked with. e.g `1.8.0`
3. Add `-javaagent:./extensions/plugins/sonarqube-community-branch-plugin-${version}.jar=ce` to
   the `sonar.ce.javaAdditionalOpts` property in your Sonarqube installation's `conf/sonar.properties` file,
   e.g. `sonar.ce.javaAdditionalOpts=-javaagent:./extensions/plugins/sonarqube-community-branch-plugin-${version}.jar=ce`
4. Start Sonarqube, and accept the warning about using third-party plugins

## Docker

The plugin is distributed in
the [mc1arke/sonarqube-with-community-branch-plugin](https://hub.docker.com/r/mc1arke/sonarqube-with-community-branch-plugin)
Docker image, with the image versions matching the up-stream Sonarqube image version.

__Note:__ If you're setting the `SONAR_WEB_JAVAADDITIONALOPTS` or `SONAR_CE_JAVAADDITIONALOPTS` environment variables in
your container launch then you'll need to add the `javaagent` configuration to your overrides to match what's in the
provided Dockerfile.

## Kubernetes with official Helm Chart

When using
[Sonarqube official Helm Chart](https://github.com/SonarSource/helm-chart-sonarqube/tree/master/charts/sonarqube),
you need to add the following settings to your helm values, where `${version}` should be replaced with the plugin
version (e.g. `1.11.0`). Beware of the changes made in helm chart version [6.1.0](https://github.com/SonarSource/helm-chart-sonarqube/blob/master/charts/sonarqube/CHANGELOG.md#610):

### helm chart version < 6.1.0

```yaml
plugins:
  install:
    - https://github.com/mc1arke/sonarqube-community-branch-plugin/releases/download/${version}/sonarqube-community-branch-plugin-${version}.jar
  lib:
    - sonarqube-community-branch-plugin-${version}.jar
jvmOpts: "-javaagent:/opt/sonarqube/lib/common/sonarqube-community-branch-plugin-${version}.jar=web"
jvmCeOpts: "-javaagent:/opt/sonarqube/lib/common/sonarqube-community-branch-plugin-${version}.jar=ce"
```

### helm chart version >= 6.1.0

```yaml
plugins:
  install:
    - https://github.com/mc1arke/sonarqube-community-branch-plugin/releases/download/${version}/sonarqube-community-branch-plugin-${version}.jar
jvmOpts: "-javaagent:/opt/sonarqube/extensions/plugins/sonarqube-community-branch-plugin-${version}.jar=web"
jvmCeOpts: "-javaagent:/opt/sonarqube/extensions/plugins/sonarqube-community-branch-plugin-${version}.jar=ce"
```

### Issues with file path with persistency

If you set `persistence.enabled=true` on SonarQube chart, the plugin might be copied to this path, based on the helm chart version, mentioned above (`${plugin-path}` equals `lib/common` or `extensions/plugins`):

```
/opt/sonarqube/${plugin-path}/sonarqube-community-branch-plugin-${version}.jar/sonarqube-community-branch-plugin-${version}.jar
```

instead of this:

```
/opt/sonarqube/${plugin-path}/sonarqube-community-branch-plugin-${version}.jar
```

As a workaround either change the paths in the config above, or exec into the container and move file up the directory
to match the config.

# Configuration

## Global configuration

Make sure `sonar.core.serverBaseURL` in SonarQube [/admin/settings](http://localhost:9000/admin/settings) is properly
set in order to for the links in the comment to work.

Set all other properties that you can define globally for all of your projects.

## How to decorate a Pull Request

In order to decorate your Pull Request's source branch, you need to analyze your target branch first.

### Run analysis of branches

If the scan is being run from a CI supporting auto-configuration then the scanner can be launched without any branch
parameters. Otherwise, the analysis needs the following setting:
`sonar.branch.name = branch_name (e.g master)`

### Run analysis of the PR branch

Carefully read the official SonarQube guide
for [pull request decoration](https://docs.sonarqube.org/latest/analysis/pull-request/)

In there you'll find the following properties that need to be set, unless your CI support auto-configuration.

```
sonar.pullrequest.key = pull_request_id (e.g. 100)
sonar.pullrequest.branch = source_branch_name (e.g feature/TICKET-123)
sonar.pullrequest.base = target_branch_name (e.g master)
```

:warning: There must not be any `sonar.branch` properties like `sonar.branch.name` arguments set when you analyze a
pull-request. These properties indicate to sonar that a branch is being analyzed rather than a pull-request so no
pull-request decoration will be executed.

## Serving images for PR decoration

By default, images for PR decoration are served as static resources on the SonarQube server as a part of Community
Branch Plugin.

If you use a SonarQube server behind a firewall and/or PR service (Github, Gitlab etc.) doesn't have access to SonarQube
server, you should change `Images base URL` property in `General > Pull Request` settings.

Anyone needing to set this value can use the
URL `https://raw.githubusercontent.com/mc1arke/sonarqube-community-branch-plugin/master/src/main/resources/static`, or
download the files from this location and host them themself.

# Building the plugin from source

If you want to try and test the current branch or build it for your development execute `./gradlew clean build`
inside of the project directory. This will put the built jar under `libs/sonarqube-community-branch-plugin*.jar`
