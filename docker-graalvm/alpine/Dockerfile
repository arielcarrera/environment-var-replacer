FROM alpine:3

MAINTAINER Ariel Carrera

# GraalVM
ARG GRAALVM_VERSION=20.0.0
ARG JAVA_VERSION=8
ARG GRAALVM_FILE=graalvm-ce-java$JAVA_VERSION-linux-amd64-$GRAALVM_VERSION.tar.gz
ARG GRAALVM_PKG=https://github.com/graalvm/graalvm-ce-builds/releases/download/vm-$GRAALVM_VERSION/${GRAALVM_FILE}

ENV GLIBC_VERSION=2.27-r0 \
	LANG=en_US.UTF-8 \
    JAVA_HOME=/opt/graalvm-ce-java$JAVA_VERSION-$GRAALVM_VERSION

RUN apk --no-cache add ca-certificates wget gcc zlib zlib-dev libc-dev curl 

#RUN apk update \
#	&& apk add bzip2-devel ed gcc gcc-c++ gcc-gfortran gzip file fontconfig less libcurl-devel make openssl openssl-devel readline-devel tar vi which xz-devel zlib-devel terminus-font\
#	&& apk add glibc-static libcxx libcxx-devel libstdc++-static zlib-static \
#    && rm -rf /var/cache/apk/*

RUN wget -q -O /etc/apk/keys/sgerrand.rsa.pub https://alpine-pkgs.sgerrand.com/sgerrand.rsa.pub \
    &&  wget -nv "https://github.com/sgerrand/alpine-pkg-glibc/releases/download/$GLIBC_VERSION/glibc-$GLIBC_VERSION.apk" \
    &&  apk --no-cache add "glibc-$GLIBC_VERSION.apk" \
    &&  rm "glibc-$GLIBC_VERSION.apk" \
    &&  wget -nv "https://github.com/sgerrand/alpine-pkg-glibc/releases/download/$GLIBC_VERSION/glibc-bin-$GLIBC_VERSION.apk" \
    &&  apk --no-cache add "glibc-bin-$GLIBC_VERSION.apk" \
    &&  rm "glibc-bin-$GLIBC_VERSION.apk" \
    &&  wget -nv "https://github.com/sgerrand/alpine-pkg-glibc/releases/download/$GLIBC_VERSION/glibc-i18n-$GLIBC_VERSION.apk" \
    &&  apk --no-cache add "glibc-i18n-$GLIBC_VERSION.apk" \
    &&  rm "glibc-i18n-$GLIBC_VERSION.apk"
    
# Download and untar GraalVM 
RUN mkdir /usr/lib/jvm \
    && curl --location "${GRAALVM_PKG}" | tar -zxC /opt/ \
    && rm -f ${GRAALVM_FILE}
	
# Install native-image
RUN $JAVA_HOME/bin/gu install native-image

VOLUME /project
WORKDIR /project/docker-graalvm/alpine

# Run native-image
ENTRYPOINT ["/project/docker-graalvm/alpine/build.sh"]