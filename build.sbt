import Dependencies._
import Settings._

lazy val alam = project.in(file("."))
  .enablePlugins(NativeImagePlugin)
  .settings(
    name := "awslambda",
    organization := "de.rmgk",
    scalaVersion_213,
    strictCompile,
    resolvers += "jitpack" at "https://jitpack.io",
    resolvers += ("STG old bintray repo" at "http://www.st.informatik.tu-darmstadt.de/maven/").withAllowInsecureProtocol(
      true
    ),
    libraryDependencies ++= Seq(
      replication.value,
      awsLambdaCore.value,
      awsLambdaEvents.value,
      awsS3.value,
      scribe.value
    ),
    nativeImageVersion := "20.3.0",
    nativeImageOptions ++= Seq(
      // "--initialize-at-build-time",
      "--initialize-at-build-time=org.slf4j.LoggerFactory",
      "--no-fallback",
      "--no-server",
      "-H:EnableURLProtocols=http,https",
      "--features=org.graalvm.home.HomeFinderFeature"
      // "--static",
    ),
    // javaOptions += "-agentlib:native-image-agent=config-output-dir=src/main/resources/META-INF/native-image",
    // fork := true
  )

// fix some linting nonsene
Global / excludeLintKeys += nativeImageVersion
