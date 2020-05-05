FROM ubuntu:18.04 AS builder

ENV JAVA_HOME=/usr/lib/jvm/java-8-openjdk-amd64

# git, bzip2, sudo for zeppelin-web build
RUN apt-get -y update && \
    apt-get install -y wget openjdk-8-jdk && \
    rm -rf /var/lib/apt/lists/*

RUN wget -q https://www-us.apache.org/dist/maven/maven-3/3.6.3/binaries/apache-maven-3.6.3-bin.tar.gz -P /tmp/ && \
    tar xf /tmp/apache-maven-*.tar.gz -C /opt
ENV PATH="/opt/apache-maven-3.6.3/bin:${PATH}"
ENV MAVEN_CLI_OPTS="--no-transfer-progress"

WORKDIR /zeppelin
COPY ./ /zeppelin/

RUN mvn -B -T 2C package -DskipTests -Pscala-2.11 -pl ':spark-interpreter,:zeppelin-spark-dependencies,:spark1-shims,:spark2-shims,:spark-scala-2.12,:spark-scala-2.11,:spark-scala-2.10,python,markdown,shell,jdbc' -am

# container image
FROM ubuntu:18.04

# curl, python3-distutils to install pip manually

RUN apt-get -y update && \
    apt-get install -y openjdk-8-jre-headless python3-minimal curl python3-distutils && \
    ln -s /usr/bin/python3 /usr/bin/python && \
    curl https://bootstrap.pypa.io/get-pip.py | python && \
    apt-get clean

ARG ZEPPELIN_USER_ID=2100
ARG ZEPPELIN_GROUP_ID=2100

ENV Z_VERSION="0.9.0-SNAPSHOT"

ENV LOG_TAG="[ZEPPELIN_${Z_VERSION}]:" \
    Z_HOME="/zeppelin" \
    LANG=en_US.UTF-8 \
    LC_ALL=en_US.UTF-8 \
    ZEPPELIN_ADDR="0.0.0.0"

RUN groupadd --gid $ZEPPELIN_GROUP_ID zeppelin \
    && useradd -ms /bin/bash -d ${Z_HOME} zeppelin --uid $ZEPPELIN_USER_ID --gid $ZEPPELIN_GROUP_ID
USER 2100

# essential python packages to make python interpreter work
RUN pip install --no-cache-dir ipython==5.4.1 jupyter_client==5.1.0 ipykernel==4.7.0 grpcio==1.8.2

# additional packages to include in the image
# RUN pip install --no-cache-dir numpy==1.12.1 pandas==0.21.1 matplotlib==2.1.1 pandasql==0.7.3 bokeh==0.12.10 ggplot==0.11.5 bkzep==0.4.0

COPY --chown=zeppelin:zeppelin --from=builder /zeppelin/bin /zeppelin/bin
COPY --chown=zeppelin:zeppelin --from=builder /zeppelin/conf /zeppelin/conf
COPY --chown=zeppelin:zeppelin --from=builder /zeppelin/interpreter /zeppelin/interpreter
COPY --chown=zeppelin:zeppelin --from=builder /zeppelin/zeppelin-interpreter/target/lib /zeppelin/lib
COPY --chown=zeppelin:zeppelin --from=builder /zeppelin/zeppelin-interpreter/target/*.jar /zeppelin/lib/
ADD ./.staroid/conf/log4j.properties /zeppelin/conf/log4j.properties

WORKDIR ${Z_HOME}
CMD ["sh", "/zeppelin/bin/zeppelin.sh"]
