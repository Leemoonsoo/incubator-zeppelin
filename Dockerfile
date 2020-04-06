FROM ubuntu:16.04 AS builder

ENV JAVA_HOME=/usr/lib/jvm/java-8-openjdk-amd64

# git, bzip2, sudo for zeppelin-web build
RUN apt-get -y update && \
    apt-get install -y wget curl git bzip2 sudo openjdk-8-jdk && \
    adduser root sudo && \
    rm -rf /var/lib/apt/lists/*

RUN wget https://www-us.apache.org/dist/maven/maven-3/3.6.3/binaries/apache-maven-3.6.3-bin.tar.gz -P /tmp/ && \
    tar xf /tmp/apache-maven-*.tar.gz -C /opt
ENV PATH="/opt/apache-maven-3.6.3/bin:${PATH}"

WORKDIR /zeppelin
COPY ./ /zeppelin/

RUN rm -rf .git

RUN curl -sL https://deb.nodesource.com/setup_10.x | sh - && \
    apt-get install -y nodejs && \
    cd zeppelin-web && npm install -g bower && bower install --allow-root && \
    cd .. && \
    cd zeppelin-web-angular && npm install && npm install -g @angular/cli && npm run build:projects && \
    cd ..

RUN cd zeppelin-web && mvn package -DskipTests
RUN mvn package -DskipTests -Pweb-angular -Pscala-2.11 -Pbuild-distr -pl '!groovy,!submarine,!livy,!hbase,!pig,!file,!flink,!ignite,!kylin,!lens,!cassandra,!elasticsearch,!bigquery,!alluxio,!scio,!neo4j,!sap,!scalding,!java,!beam,!hazelcastjet,!geode,!ksql,!sparql,!:launcher-docker,!:launcher-cluster,!:launcher-flink' && \
    cd zeppelin-distribution/target/zeppelin-0.9.0-SNAPSHOT/zeppelin-0.9.0-SNAPSHOT && \
    mkdir -p zeppelin-web/dist && mkdir -p zeppelin-web-angular/dist && \
    cd zeppelin-web/dist && jar xf ../../zeppelin-web-0.9.0-SNAPSHOT.war && rm -f ../../zeppelin-web-0.9.0-SNAPSHOT.war && \
    cd ../../zeppelin-web-angular/dist && jar xf ../../zeppelin-web-angular-0.9.0-SNAPSHOT.war && rm -f ../../zeppelin-web-angular-0.9.0-SNAPSHOT.war && \
    cd ../..

# container image
FROM openjdk:8-jre-alpine

ADD https://storage.googleapis.com/kubernetes-release/release/v1.16.3/bin/linux/amd64/kubectl /usr/local/bin/kubectl
RUN chmod +x /usr/local/bin/kubectl && \
    apk add --no-cache bash curl ca-certificates

ARG ZEPPELIN_USER_ID=2100
ARG ZEPPELIN_GROUP_ID=2100

ENV Z_VERSION="0.9.0-SNAPSHOT"

ENV LOG_TAG="[ZEPPELIN_${Z_VERSION}]:" \
    Z_HOME="/zeppelin" \
    LANG=en_US.UTF-8 \
    LC_ALL=en_US.UTF-8 \
    ZEPPELIN_ADDR="0.0.0.0"

RUN addgroup -S --gid $ZEPPELIN_GROUP_ID zeppelin && adduser -S --uid $ZEPPELIN_USER_ID zeppelin -G zeppelin
USER zeppelin

COPY --chown=zeppelin:zeppelin --from=builder /zeppelin/zeppelin-distribution/target/zeppelin-0.9.0-SNAPSHOT/zeppelin-0.9.0-SNAPSHOT /zeppelin

EXPOSE 8080

WORKDIR ${Z_HOME}
CMD ["sh", "/zeppelin/bin/zeppelin.sh"]
