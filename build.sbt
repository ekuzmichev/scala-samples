name := "scala-samples"

val scalaV = "2.12.8"

ThisBuild / organization := "ru.ekuzmichev"
ThisBuild / version := "0.1-SNAPSHOT"
ThisBuild / scalaVersion := scalaV
ThisBuild / autoCompilerPlugins := true

lazy val root =
  project
    .in(file("."))
    .aggregate(
      `akka-samples`,
      cats,
      `cats-retry-samples`,
      `cats-effect`,
      lang,
      `zio-samples`,
      `zio-kafka`
    )

lazy val `akka-samples` =
  project
    .settings(
      libraryDependencies ++= Seq(
        libs.akkaActorTyped,
        libs.akkaStream
      ),
      Seq(scalacOptions ++= commonScalaOptions)
    )

lazy val `cats-retry-samples` =
  project
    .settings(
      libraryDependencies ++= Seq(libs.catsRetry),
      Seq(scalacOptions ++= commonScalaOptions)
    )

lazy val `cats-effect` =
  project
    .settings(
      libraryDependencies ++= Seq(
        libs.catsEffect,
        libs.catsFree,
        libs.munitCatsEffectTest,
        libs.catsEffectScalatest,
        libs.catsEffectTestkit,
        "org.typelevel" %% "log4cats-core"  % "2.2.0",
        "org.typelevel" %% "log4cats-slf4j" % "2.2.0",
        "org.slf4j"     % "slf4j-simple"    % "1.7.32"
      ),
      Seq(scalacOptions ++= commonScalaOptions)
    )

lazy val `cats` =
  project
    .settings(
      libraryDependencies ++= Seq(
        libs.catsCore,
        libs.catsFree
      ),
      Seq(scalacOptions ++= commonScalaOptions)
    )

lazy val lang =
  project
    .settings(
      libraryDependencies ++= Seq(
        libs.commonsIo,
        libs.byteUnits,
        libs.refined,
        libs.scalaReflect
      ),
      Seq(scalacOptions ++= commonScalaOptions)
    )

lazy val `zio-samples` =
  project
    .settings(
      libraryDependencies ++= Seq(libs.zio),
      Seq(scalacOptions ++= commonScalaOptions)
    )

lazy val `zio-kafka` =
  project
    .settings(
      libraryDependencies ++= Seq(
        libs.zioStreams,
        libs.zioKafka,
        libs.kafkaStreamsCirce
      ),
      Seq(scalacOptions ++= commonScalaOptions)
    )

lazy val commonScalaOptions =
  Seq(
    "-Xfatal-warnings",
    "-Ypartial-unification",
    "-feature",
    "-deprecation",
    "-unchecked",
    "-language:postfixOps",
    "-language:higherKinds"
  )

lazy val libs = new {
  val akkaVersion        = "2.6.19"
  val byteUnitsV         = "0.9.1"
  val catsEffectV        = "3.3.0"
  val catsRetryV         = "1.1.0"
  val catsV              = "2.7.0"
  val commonsIoV         = "2.6"
  val kafkaStreamsCirceV = "0.6.3"
  val refinedV           = "0.9.15"
  val zioKafkaV          = "0.14.0"
  val zioV               = "1.0.2"

  val akkaActorTyped      = "com.typesafe.akka"         %% "akka-actor-typed"              % akkaVersion withSources () withJavadoc ()
  val akkaStream          = "com.typesafe.akka"         %% "akka-stream"                   % akkaVersion withSources () withJavadoc ()
  val byteUnits           = "com.jakewharton.byteunits" % "byteunits"                      % byteUnitsV
  val catsCore            = "org.typelevel"             %% "cats-core"                     % catsV withSources () withJavadoc ()
  val catsEffect          = "org.typelevel"             %% "cats-effect"                   % catsEffectV withSources () withJavadoc ()
  val catsEffectScalatest = "org.typelevel"             %% "cats-effect-testing-scalatest" % "1.4.0" % Test
  val catsEffectTestkit   = "org.typelevel"             %% "cats-effect-testkit"           % catsEffectV % Test
  val catsFree            = "org.typelevel"             %% "cats-free"                     % catsV withSources () withJavadoc ()
  val catsRetry           = "com.github.cb372"          %% "cats-retry"                    % catsRetryV
  val commonsIo           = "commons-io"                % "commons-io"                     % commonsIoV
  val kafkaStreamsCirce   = "com.goyeau"                %% "kafka-streams-circe"           % kafkaStreamsCirceV
  val munitCatsEffectTest = "org.typelevel"             %% "munit-cats-effect-3"           % "1.0.6" % Test
  val refined             = "eu.timepit"                %% "refined"                       % refinedV
  val scalaReflect        = "org.scala-lang"            % "scala-reflect"                  % scalaV
  val zio                 = "dev.zio"                   %% "zio"                           % zioV
  val zioKafka            = "dev.zio"                   %% "zio-kafka"                     % zioKafkaV
  val zioStreams          = "dev.zio"                   %% "zio-streams"                   % zioV
}
