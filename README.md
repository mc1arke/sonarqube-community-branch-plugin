[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=mc1arke_sonarqube-community-branch-plugin&metric=alert_status)](https://sonarcloud.io/dashboard?id=mc1arke_sonarqube-community-branch-plugin)
[![Build Status](https://img.shields.io/github/actions/workflow/status/mc1arke/sonarqube-community-branch-plugin/.github/workflows/build.yml?branch=master&logo=github)](https://github.com/mc1arke/sonarqube-community-branch-plugin?workflow=build)

# SonarQube Community Branch Plugin

A plugin for SonarQube to allow branch analysis and pull request decoration in the
Community version.

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

The plugin major and minor versions match the SonarQube version it is compatible with,
e.g. `25.4.0` of the plugin is compatible with SonarQube 25.4.x. Any older plugin
version is not guaranteed to work, nor are newer SonarQube versions guaranteed to work
with previous plugin versions.

# Features

The plugin is intended to support the
features and parameters from the SonarQube [branch](https://docs.sonarsource.com/sonarqube-server/latest/analyzing-source-code/branch-analysis/introduction/)
and [pull request](https://docs.sonarsource.com/sonarqube-server/latest/analyzing-source-code/pull-request-analysis/introduction/) documentation.

# Installation

## Manual Install

**Please ensure you follow the installation instructions for the version of the plugin you're installing by looking at
the README on the relevant release tag.**

Either build the project
or [download a compatible release version of the plugin JAR and associated sonarqube-webapp.zip](https://github.com/mc1arke/sonarqube-community-branch-plugin/releases)
.

1. Copy the plugin JAR file to the `extensions/plugins/` directory of your SonarQube instance
2. Add `-javaagent:./extensions/plugins/sonarqube-community-branch-plugin-${version}.jar=web` to
   the `sonar.web.javaAdditionalOpts` property in your SonarQube installation's `conf/sonar.properties` file,
   e.g. `sonar.web.javaAdditionalOpts=-javaagent:./extensions/plugins/sonarqube-community-branch-plugin-${version}.jar=web`
   where ${version} is the version of the plugin being worked with. e.g `1.8.0`
3. Add `-javaagent:./extensions/plugins/sonarqube-community-branch-plugin-${version}.jar=ce` to
   the `sonar.ce.javaAdditionalOpts` property in your SonarQube installation's `conf/sonar.properties` file,
   e.g. `sonar.ce.javaAdditionalOpts=-javaagent:./extensions/plugins/sonarqube-community-branch-plugin-${version}.jar=ce`
4. Replace the contents of the `web` directory in your SonarQube installation with the contents of the sonarqube-webapp zip archive
5. Start SonarQube, and accept the warning about using third-party plugins

## Docker

The plugin is distributed in
the [mc1arke/sonarqube-with-community-branch-plugin](https://hub.docker.com/r/mc1arke/sonarqube-with-community-branch-plugin)
Docker image, with the image versions matching the up-stream SonarQube image version.

**Note:** If you're setting the `SONAR_WEB_JAVAADDITIONALOPTS` or `SONAR_CE_JAVAADDITIONALOPTS` environment variables in
your container launch then you'll need to add the `javaagent` configuration to your overrides to match what's in the
provided Dockerfile.

## Docker Compose

A `docker-compose.yml` file is provided.
It uses the env variables available in `.env`.

To use it, clone the repository, create a `.env` with `SONARQUBE_VERSION` defined, and execute `docker compose up`. Note that you need to have `docker compose` installed in your system and added to your PATH.

## Kubernetes with the Official Helm Chart

When using the
[SonarQube official Helm Chart](https://github.com/SonarSource/helm-chart-sonarqube/tree/master/charts/sonarqube),
add the following settings to your helm values, where `${version}` should be replaced with the plugin
version (e.g. `25.4.0`).

```yaml
community:
  enabled: true

plugins:
  install:
    - https://github.com/mc1arke/sonarqube-community-branch-plugin/releases/download/${version}/sonarqube-community-branch-plugin-${version}.jar
sonarProperties:
  sonar.web.javaAdditionalOpts: "-javaagent:/opt/sonarqube/extensions/plugins/sonarqube-community-branch-plugin-${version}.jar=web"
  sonar.ce.javaAdditionalOpts: "-javaagent:/opt/sonarqube/extensions/plugins/sonarqube-community-branch-plugin-${version}.jar=ce"

extraVolumes:
  - name: webapp
    emptyDir:
      sizeLimit: 50Mi
extraVolumeMounts:
  - name: webapp
    mountPath: /opt/sonarqube/web
extraInitContainers:
  - name: download-webapp
    image: busybox:1.37
    volumeMounts:
      - name: webapp
        mountPath: /web
    command:
      - sh
      - -c
      - wget -O /tmp/sonarqube-webapp.zip https://github.com/mc1arke/sonarqube-community-branch-plugin/releases/download/${version}/sonarqube-webapp.zip &&
        unzip -o /tmp/sonarqube-webapp.zip -d /web &&
        chmod -R 755 /web &&
        chown -R 1000:0 /web &&
        rm -f /tmp/sonarqube-webapp.zip
```

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

If you are scanning a GitHub pull request, you will also need to set the `sonar.scm.revision` argument.

For example, using the official [SonarQube Scan](https://github.com/marketplace/actions/official-sonarqube-scan)
on GitHub Actions:

```yaml
- name: SonarQube Scan
  uses: sonarsource/sonarqube-scan-action@<action version>
  with:
    args: >
      -Dsonar.scm.revision=${{ github.event.pull_request.head.sha }}
  env:
    SONAR_TOKEN: ${{ secrets.SONAR_TOKEN }}
    SONAR_HOST_URL: ${{ vars.SONAR_HOST_URL }}
```

## Serving images for PR decoration

By default, images for PR decoration are served as static resources on the SonarQube server as a part of Community
Branch Plugin.

If you use a SonarQube server behind a firewall and/or PR service (Github, Gitlab etc.) doesn't have access to SonarQube
server, you should change `Images base URL` property in `General > Pull Request` settings.

Anyone needing to set this value can use the
URL `https://raw.githubusercontent.com/mc1arke/sonarqube-community-branch-plugin/master/src/main/resources/static`, or
download the files from this location and host them themself.

# Building the plugin from source

Run the following command to build and run a container with the plugin and modified frontend code:

```bash
docker compose up --build
```
