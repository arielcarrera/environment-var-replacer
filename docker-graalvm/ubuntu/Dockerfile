FROM ubuntu

MAINTAINER Ariel Carrera

# GraalVM
ARG GRAALVM_VERSION=20.0.0
ARG JAVA_VERSION=8
ARG GRAALVM_FILE=graalvm-ce-java$JAVA_VERSION-linux-amd64-$GRAALVM_VERSION.tar.gz
ARG GRAALVM_PKG=https://github.com/graalvm/graalvm-ce-builds/releases/download/vm-$GRAALVM_VERSION/${GRAALVM_FILE}

ENV LANG=en_US.UTF-8 \
    JAVA_HOME=/opt/graalvm-ce-java$JAVA_VERSION-$GRAALVM_VERSION

#RUN dpkg --configure -a && apt-get install -f && apt-get dist-upgrade
#gcc-5 
RUN apt-get update -y \
	&& apt-get install -y build-essential bzip2 ed gzip file fontconfig less make openssl tar zlib1g zlib1g-dev curl \
    && rm -rf /var/cache/apt/archives

RUN fc-cache -f -v

# Download and untar GraalVM 
RUN set -eux \
    && curl --location ${GRAALVM_PKG} | gunzip | tar x -C /opt/ \
    && rm -f ${GRAALVM_FILE}

# Install native-image
RUN $JAVA_HOME/bin/gu install native-image

VOLUME /project
WORKDIR /project/docker-graalvm/ubuntu

# Run native-image
ENTRYPOINT ["/project/docker-graalvm/ubuntu/build.sh"]