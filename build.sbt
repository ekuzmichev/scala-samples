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
      lenses,
      `zio-samples`,
      `zio-kafka`
    )

lazy val `akka-samples` =
  project
    .settings(
      libraryDependencies ++= Seq(
        libs.akkaActorTyped,
        libs.akkaStream,
        libs.akkaHttp,
        libs.akkaHttpSprayJson,
        libs.akkaHttpCirce,
        libs.akkaHttpJackson,
        libs.circeCore,
        libs.circeGeneric,
        libs.circeParser,
        libs.slf4jSimple
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

lazy val lenses =
  project
    .settings(
      libraryDependencies ++= Seq(
        libs.monocleCore,
        libs.monocleMacro
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
  val akkaHttpJsonSerializersV = "1.34.0"
  val akkaHttpV                = "10.2.0"
  val akkaVersion              = "2.6.19"
  val byteUnitsV               = "0.9.1"
  val catsEffectV              = "3.3.0"
  val catsRetryV               = "1.1.0"
  val catsV                    = "2.7.0"
  val circeV                   = "0.12.3"
  val commonsIoV               = "2.6"
  val kafkaStreamsCirceV       = "0.6.3"
  val monocleVersion           = "2.0.3"
  val refinedV                 = "0.9.15"
  val slf4jV                   = "1.7.10"
  val zioKafkaV                = "0.14.0"
  val zioV                     = "1.0.2"

  val akkaActorTyped      = "com.typesafe.akka"          %% "akka-actor-typed"              % akkaVersion
  val akkaHttp            = "com.typesafe.akka"          %% "akka-http"                     % akkaHttpV
  val akkaHttpCirce       = "de.heikoseeberger"          %% "akka-http-circe"               % akkaHttpJsonSerializersV
  val akkaHttpJackson     = "de.heikoseeberger"          %% "akka-http-jackson"             % akkaHttpJsonSerializersV
  val akkaHttpSprayJson   = "com.typesafe.akka"          %% "akka-http-spray-json"          % akkaHttpV
  val akkaStream          = "com.typesafe.akka"          %% "akka-stream"                   % akkaVersion
  val byteUnits           = "com.jakewharton.byteunits"  % "byteunits"                      % byteUnitsV
  val catsCore            = "org.typelevel"              %% "cats-core"                     % catsV
  val catsEffect          = "org.typelevel"              %% "cats-effect"                   % catsEffectV
  val catsEffectScalatest = "org.typelevel"              %% "cats-effect-testing-scalatest" % "1.4.0" % Test
  val catsEffectTestkit   = "org.typelevel"              %% "cats-effect-testkit"           % catsEffectV % Test
  val catsFree            = "org.typelevel"              %% "cats-free"                     % catsV
  val catsRetry           = "com.github.cb372"           %% "cats-retry"                    % catsRetryV
  val circeCore           = "io.circe"                   %% "circe-core"                    % circeV
  val circeGeneric        = "io.circe"                   %% "circe-generic"                 % circeV
  val circeParser         = "io.circe"                   %% "circe-parser"                  % circeV
  val commonsIo           = "commons-io"                 % "commons-io"                     % commonsIoV
  val kafkaStreamsCirce   = "com.goyeau"                 %% "kafka-streams-circe"           % kafkaStreamsCirceV
  val monocleCore         = "com.github.julien-truffaut" %% "monocle-core"                  % monocleVersion
  val monocleMacro        = "com.github.julien-truffaut" %% "monocle-macro"                 % monocleVersion
  val munitCatsEffectTest = "org.typelevel"              %% "munit-cats-effect-3"           % "1.0.6" % Test
  val refined             = "eu.timepit"                 %% "refined"                       % refinedV
  val scalaReflect        = "org.scala-lang"             % "scala-reflect"                  % scalaV
  val slf4jSimple         = "org.slf4j"                  % "slf4j-simple"                   % slf4jV
  val zio                 = "dev.zio"                    %% "zio"                           % zioV
  val zioKafka            = "dev.zio"                    %% "zio-kafka"                     % zioKafkaV
  val zioStreams          = "dev.zio"                    %% "zio-streams"                   % zioV
}
