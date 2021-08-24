FROM amazonlinux:2

RUN curl -fL -o /bin/cs https://git.io/coursier-cli-linux; chmod +x /bin/cs
RUN cs java-home --jvm graalvm-java11:20.3.0
RUN yum install -y gcc
RUN curl -fL -o /bin/sbt https://git.io/sbt; chmod +x /bin/sbt
RUN yum install -y gcc glibc-devel zlib-devel libstdc++-static
RUN cd $(cs java-home --jvm graalvm-ce-java11:20.3.0); ./bin/gu install native-image
RUN cd /tmp; sbt -java-home $(cs java-home --jvm graalvm-ce-java11:20.3.0) -sbt-create exit
ADD . /tmpproj
RUN cd tmpproj; sbt -java-home $(cs java-home --jvm graalvm-ce-java11:20.3.0) nativeImage

CMD (cd proj; sbt -java-home $(cs java-home --jvm graalvm-ce-java11:20.3.0) nativeImage -H:ReflectionConfigurationFiles=reflection-config.json; chown 1000:1000 -R target)
