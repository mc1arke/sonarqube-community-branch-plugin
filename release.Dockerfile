ARG SONARQUBE_VERSION="community"

FROM alpine:3.23 AS downloader
ARG PLUGIN_VERSION
ENV PLUGIN_VERSION=${PLUGIN_VERSION}

ARG DOWNLOAD_BASE_URL=https://github.com/mc1arke/sonarqube-community-branch-plugin/releases/download/${PLUGIN_VERSION}

ADD ${DOWNLOAD_BASE_URL}/sonarqube-webapp.zip /tmp/sonarqube-webapp.zip
ADD ${DOWNLOAD_BASE_URL}/sonarqube-community-branch-plugin-${PLUGIN_VERSION}.jar /tmp/sonarqube-community-branch-plugin.jar

RUN apk update && \
    apk add unzip && \
    mkdir -p /opt/sonarqube/web && \
    unzip /tmp/sonarqube-webapp.zip -d /opt/sonarqube/web

FROM sonarqube:${SONARQUBE_VERSION}

USER root
RUN rm -rf /opt/sonarqube/web/*

COPY --from=downloader --chown=sonarqube:root /tmp/sonarqube-community-branch-plugin.jar /opt/sonarqube/extensions/plugins/sonarqube-community-branch-plugin.jar
COPY --from=downloader --chmod=550 --chown=sonarqube:root /opt/sonarqube/web /opt/sonarqube/web


USER sonarqube
ENV SONAR_WEB_JAVAADDITIONALOPTS="-javaagent:/opt/sonarqube/extensions/plugins/sonarqube-community-branch-plugin.jar=web"
ENV SONAR_CE_JAVAADDITIONALOPTS="-javaagent:/opt/sonarqube/extensions/plugins/sonarqube-community-branch-plugin.jar=ce"
