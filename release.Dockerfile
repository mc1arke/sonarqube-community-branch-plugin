ARG SONARQUBE_VERSION

FROM sonarqube:${SONARQUBE_VERSION}

ARG PLUGIN_VERSION
ENV PLUGIN_VERSION=${PLUGIN_VERSION}

ADD --chown=sonarqube:sonarqube https://github.com/mc1arke/sonarqube-community-branch-plugin/releases/download/${PLUGIN_VERSION}/sonarqube-community-branch-plugin-${PLUGIN_VERSION}.jar /opt/sonarqube/lib/common/
ADD --chown=sonarqube:sonarqube https://github.com/mc1arke/sonarqube-community-branch-plugin/releases/download/${PLUGIN_VERSION}/sonarqube-community-branch-plugin-${PLUGIN_VERSION}.jar /opt/sonarqube/extensions/plugins/
