ARG SONARQUBE_VERSION

FROM gradle:8.9-jdk17-jammy AS builder

COPY . /home/build/project
WORKDIR /home/build/project
RUN gradle build -x test

FROM sonarqube:${SONARQUBE_VERSION}
COPY --from=builder --chown=sonarqube:0 /home/build/project/build/libs/sonarqube-community-branch-plugin-*.jar /opt/sonarqube/extensions/plugins/

ARG PLUGIN_VERSION
ENV PLUGIN_VERSION=${PLUGIN_VERSION}
ENV SONAR_WEB_JAVAADDITIONALOPTS="-javaagent:./extensions/plugins/sonarqube-community-branch-plugin-${PLUGIN_VERSION}.jar=web"
ENV SONAR_CE_JAVAADDITIONALOPTS="-javaagent:./extensions/plugins/sonarqube-community-branch-plugin-${PLUGIN_VERSION}.jar=ce"
