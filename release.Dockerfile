ARG SONARQUBE_VERSION="community"

FROM sonarqube:${SONARQUBE_VERSION}

ARG PLUGIN_VERSION
ARG DOWNLOAD_BASE_URL=https://github.com/mc1arke/sonarqube-community-branch-plugin/releases/download/${PLUGIN_VERSION}
ENV PLUGIN_VERSION=${PLUGIN_VERSION}

ADD --chown=sonarqube:root ${DOWNLOAD_BASE_URL}/sonarqube-community-branch-plugin-${PLUGIN_VERSION}.jar /opt/sonarqube/extensions/plugins/

USER root
RUN apt-get --no-install-recommends -y install unzip && \
    wget ${DOWNLOAD_BASE_URL}/sonarqube-webapp.zip && \
    rm -rf /opt/sonarqube/web/* && \
    unzip sonarqube-webapp.zip -d /opt/sonarqube/web && \
    chown -R sonarqube:root /opt/sonarqube/web && \
    chmod -R 550 /opt/sonarqube/web && \
    apt-get remove -y unzip && \
    rm sonarqube-webapp.zip && \
    apt-get clean

USER sonarqube
ENV SONAR_WEB_JAVAADDITIONALOPTS="-javaagent:./extensions/plugins/sonarqube-community-branch-plugin-${PLUGIN_VERSION}.jar=web"
ENV SONAR_CE_JAVAADDITIONALOPTS="-javaagent:./extensions/plugins/sonarqube-community-branch-plugin-${PLUGIN_VERSION}.jar=ce"
