FROM centos:7

MAINTAINER Ariel Carrera

# GraalVM
ARG GRAALVM_VERSION=20.0.0
ARG JAVA_VERSION=8
ARG GRAALVM_FILE=graalvm-ce-java$JAVA_VERSION-linux-amd64-$GRAALVM_VERSION.tar.gz
ARG GRAALVM_PKG=https://github.com/graalvm/graalvm-ce-builds/releases/download/vm-$GRAALVM_VERSION/${GRAALVM_FILE}

ENV LANG=en_US.UTF-8 \
    JAVA_HOME=/opt/graalvm-ce-java$JAVA_VERSION-$GRAALVM_VERSION

RUN yum update -y \
	&& yum install -y bzip2-devel ed gcc gcc-c++ gcc-gfortran gzip file fontconfig less libcurl-devel make openssl openssl-devel readline-devel tar vi which xz-devel zlib-devel \
	&& yum install -y glibc-static libcxx libcxx-devel libstdc++-static zlib-static \
    && rm -rf /var/cache/yum

RUN fc-cache -f -v

# Download and untar GraalVM 
RUN set -eux \
    && curl --location ${GRAALVM_PKG} | gunzip | tar x -C /opt/ \
    && rm -f ${GRAALVM_FILE}
# Set alternative links
RUN mkdir -p "/usr/java" \
    && ln -sfT "$JAVA_HOME" /usr/java/default \
    && ln -sfT "$JAVA_HOME" /usr/java/latest \
    && for bin in "$JAVA_HOME/bin/"*; do \
        base="$(basename "$bin")"; \
        [ ! -e "/usr/bin/$base" ]; \
        alternatives --install "/usr/bin/$base" "$base" "$bin" 20000; \
    done

# Install native-image
RUN $JAVA_HOME/bin/gu install native-image

VOLUME /project
WORKDIR /project/docker-graalvm/centos

# Run native-image
ENTRYPOINT ["/project/docker-graalvm/centos/build.sh"]