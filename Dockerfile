ARG SONARQUBE_VERSION

FROM openjdk:11-jdk-slim as builder

COPY . /home/build/project
WORKDIR /home/build/project
RUN ./gradlew build -x test

FROM sonarqube:${SONARQUBE_VERSION}
COPY --from=builder --chown=sonarqube:sonarqube /home/build/project/build/libs/sonarqube-community-branch-plugin-*.jar /opt/sonarqube/lib/common/
COPY --from=builder --chown=sonarqube:sonarqube /home/build/project/build/libs/sonarqube-community-branch-plugin-*.jar /opt/sonarqube/extensions/plugins/
