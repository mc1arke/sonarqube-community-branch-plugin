ARG SONARQUBE_VERSION="community"
ARG WORKDIR="/home/build/project"

FROM gradle:8.9-jdk21-jammy AS builder
ARG WORKDIR

COPY . ${WORKDIR}
WORKDIR ${WORKDIR}
RUN gradle build -x test


FROM node:22.16-alpine AS webapp-builder

COPY ./sonarqube-webapp /home/build/sonarqube-webapp
COPY ./sonarqube-webapp-addons /home/build/sonarqube-webapp-addons

WORKDIR /home/build
RUN apk add --no-cache bash
RUN ./sonarqube-webapp-addons/setup.sh

WORKDIR /home/build/sonarqube-webapp
RUN yarn install
RUN yarn nx run sq-server:build


FROM sonarqube:${SONARQUBE_VERSION}
ARG PLUGIN_VERSION
ARG WORKDIR

COPY --from=builder --chown=sonarqube:root ${WORKDIR}/build/libs/sonarqube-community-branch-plugin-*.jar /opt/sonarqube/lib/community-branch-plugin/
COPY --chown=sonarqube:root docker/community-branch-entrypoint.sh /opt/sonarqube/docker/community-branch-entrypoint.sh
RUN chmod 755 /opt/sonarqube/docker/community-branch-entrypoint.sh

RUN chmod -R 770 /opt/sonarqube/web && rm -rf /opt/sonarqube/web/*
COPY --from=webapp-builder --chown=sonarqube:root /home/build/sonarqube-webapp/apps/sq-server/build/webapp /opt/sonarqube/web
RUN chmod -R 550 /opt/sonarqube/web

ENV PLUGIN_VERSION=${PLUGIN_VERSION}
ENV SONAR_WEB_JAVAADDITIONALOPTS="-javaagent:/opt/sonarqube/extensions/plugins/sonarqube-community-branch-plugin-${PLUGIN_VERSION}.jar=web"
ENV SONAR_CE_JAVAADDITIONALOPTS="-javaagent:/opt/sonarqube/extensions/plugins/sonarqube-community-branch-plugin-${PLUGIN_VERSION}.jar=ce"
ENTRYPOINT ["/opt/sonarqube/docker/community-branch-entrypoint.sh"]
