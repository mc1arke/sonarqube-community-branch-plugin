ARG SONARQUBE_VERSION="community"
ARG WORKDIR="/home/build/project"

FROM gradle:8.9-jdk17-jammy AS builder
ARG WORKDIR

COPY . ${WORKDIR}
WORKDIR ${WORKDIR}
RUN gradle build -x test


FROM node:22.16-alpine AS webapp-builder
ARG WORKDIR

COPY ./sonarqube-webapp ${WORKDIR}
COPY ./sonarqube-webapp-addons ${WORKDIR}/libs/sq-server-addons

WORKDIR ${WORKDIR}
RUN yarn install
RUN yarn nx run sq-server:build


FROM sonarqube:${SONARQUBE_VERSION}
ARG PLUGIN_VERSION
ARG WORKDIR

COPY --from=builder --chown=sonarqube:root ${WORKDIR}/build/libs/sonarqube-community-branch-plugin-*.jar /opt/sonarqube/extensions/plugins/

RUN chmod -R 770 /opt/sonarqube/web && rm -rf /opt/sonarqube/web/*
COPY --from=webapp-builder --chown=sonarqube:root ${WORKDIR}/apps/sq-server/build/webapp /opt/sonarqube/web
RUN chmod -R 550 /opt/sonarqube/web

ENV PLUGIN_VERSION=${PLUGIN_VERSION}
ENV SONAR_WEB_JAVAADDITIONALOPTS="-javaagent:./extensions/plugins/sonarqube-community-branch-plugin-${PLUGIN_VERSION}.jar=web"
ENV SONAR_CE_JAVAADDITIONALOPTS="-javaagent:./extensions/plugins/sonarqube-community-branch-plugin-${PLUGIN_VERSION}.jar=ce"
