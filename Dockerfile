FROM gradle:jdk8 as builder

COPY --chown=gradle:gradle . /home/gradle/src
WORKDIR /home/gradle/src
RUN ./gradlew build -x test

FROM sonarqube:7.9-community
COPY --from=builder /home/gradle/src/build/libs/sonarqube-community-branch-plugin-*.jar /opt/sonarqube/lib/common/
COPY --from=builder /home/gradle/src/build/libs/sonarqube-community-branch-plugin-*.jar /opt/sonarqube/extensions/plugins/
