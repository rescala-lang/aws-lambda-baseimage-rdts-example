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
      "--initialize-at-build-time",
      "--no-fallback",
      "--no-server",
      "-H:EnableURLProtocols=http,https",
      // "--static",
    ),
  )

// fix some linting nonsene
Global / excludeLintKeys += nativeImageVersion
