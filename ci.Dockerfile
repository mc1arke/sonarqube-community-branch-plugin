# CI Dockerfile - Uses local build artifacts (no download)
# Used by GitHub Actions for building release images

ARG SQ_VERSION=26.1.0.118079-community
ARG SQ_IMAGE_NAME=sonarqube
ARG REGISTRY_PREFIX=

FROM ${REGISTRY_PREFIX}${SQ_IMAGE_NAME}:${SQ_VERSION}

# hadolint ignore=DL3002
USER root

# Copy local build artifacts
COPY build/libs/*.jar /opt/sonarqube/extensions/plugins/sonarqube-community-branch-plugin.jar
COPY build/ui/sonarqube-webapp.zip /tmp/sonarqube-webapp.zip

# hadolint ignore=DL3018,DL3008,DL3009
RUN apt-get update && apt-get upgrade -y && \
    apt-get install --no-install-recommends -y unzip && \
    rm -rf /opt/sonarqube/web && mkdir -p /opt/sonarqube/web && \
    unzip /tmp/sonarqube-webapp.zip -d /opt/sonarqube/web && rm /tmp/sonarqube-webapp.zip && \
    sed -i "s|#sonar.web.javaAdditionalOpts=|sonar.web.javaAdditionalOpts=-javaagent:./extensions/plugins/sonarqube-community-branch-plugin.jar=web|g" /opt/sonarqube/conf/sonar.properties && \
    sed -i "s|#sonar.ce.javaAdditionalOpts=|sonar.ce.javaAdditionalOpts=-javaagent:./extensions/plugins/sonarqube-community-branch-plugin.jar=ce|g" /opt/sonarqube/conf/sonar.properties && \
    apt-get purge -y unzip && apt-get autoremove -y && \
    apt-get clean && rm -rf /var/lib/apt/lists/*

# hadolint ignore=DL3002
USER sonarqube
