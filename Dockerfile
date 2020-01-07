FROM gradle:jdk8 as builder

COPY --chown=gradle:gradle . /home/gradle/src
WORKDIR /home/gradle/src
RUN ./gradlew build -Pversion=latest -x test
RUN ls /home/gradle/src/build/libs/

FROM alpine
COPY --from=builder /home/gradle/src/build/libs/sonarqube-community-branch-plugin-latest.jar /
RUN printf '#!/bin/bash \
\nDIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )" \
\ncp $DIR/sonarqube-community-branch-plugin-latest.jar lib/common/sonarqube-community-branch-plugin.jar \
\ncp $DIR/sonarqube-community-branch-plugin-latest.jar extensions/plugins/sonarqube-community-branch-plugin.jar \
\n./bin/run.sh $@ \
\n' > /run.sh

VOLUME /sonarqube-community-branch-plugin
CMD ["/bin/ash", "-c", "cp /run.sh /sonarqube-community-branch-plugin/run.sh ;\
      chmod a+x /sonarqube-community-branch-plugin/run.sh ;\
      cp sonarqube-community-branch-plugin-latest.jar /sonarqube-community-branch-plugin/sonarqube-community-branch-plugin-latest.jar ;\
      while sleep 3600; do :; done"]
