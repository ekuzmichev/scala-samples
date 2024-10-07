name := "scala-samples"

val versions = new {
  val scala = "2.12.8"
}

ThisBuild / organization := "ru.ekuzmichev"
ThisBuild / version             := "0.1-SNAPSHOT"
ThisBuild / scalaVersion        := versions.scala
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
      `zio-amqp`,
      `zio-samples`,
      `zio-kafka`
    )

lazy val `akka-samples` =
  project
    .settings(
      libraryDependencies ++= Seq(
        "com.typesafe.akka" %% "akka-actor-typed"     % "2.6.19",
        "com.typesafe.akka" %% "akka-stream"          % "2.6.19",
        "com.typesafe.akka" %% "akka-http"            % "10.2.0",
        "de.heikoseeberger" %% "akka-http-circe"      % "1.34.0",
        "de.heikoseeberger" %% "akka-http-jackson"    % "1.34.0",
        "com.typesafe.akka" %% "akka-http-spray-json" % "10.2.0",
        "io.circe"          %% "circe-core"           % "0.12.3",
        "io.circe"          %% "circe-generic"        % "0.12.3",
        "io.circe"          %% "circe-parser"         % "0.12.3",
        "org.slf4j"          % "slf4j-simple"         % "1.7.10"
      ),
      Seq(scalacOptions ++= commonScalaOptions)
    )

lazy val `cats-retry-samples` =
  project
    .settings(
      libraryDependencies ++= Seq(
        "com.github.cb372" %% "cats-retry" % "1.1.0"
      ),
      Seq(scalacOptions ++= commonScalaOptions)
    )

lazy val `cats-effect` =
  project
    .settings(
      libraryDependencies ++= Seq(
        "org.typelevel" %% "cats-effect"                   % "3.3.0",
        "org.typelevel" %% "cats-free"                     % "2.7.0",
        "org.typelevel" %% "log4cats-core"                 % "2.2.0",
        "org.typelevel" %% "log4cats-slf4j"                % "2.2.0",
        "org.slf4j"      % "slf4j-simple"                  % "1.7.32",
        "org.typelevel" %% "cats-effect-testing-scalatest" % "1.4.0" % Test,
        "org.typelevel" %% "cats-effect-testkit"           % "3.3.0" % Test,
        "org.typelevel" %% "munit-cats-effect-3"           % "1.0.6" % Test
      ),
      Seq(scalacOptions ++= commonScalaOptions)
    )

lazy val `cats` =
  project
    .settings(
      libraryDependencies ++= Seq(
        "org.typelevel" %% "cats-core" % "2.7.0",
        "org.typelevel" %% "cats-free" % "2.7.0"
      ),
      Seq(scalacOptions ++= commonScalaOptions)
    )

lazy val lang =
  project
    .settings(
      libraryDependencies ++= Seq(
        "commons-io"                % "commons-io"    % "2.6",
        "com.jakewharton.byteunits" % "byteunits"     % "0.9.1",
        "eu.timepit"               %% "refined"       % "0.9.15",
        "org.scala-lang"            % "scala-reflect" % versions.scala
      ),
      Seq(scalacOptions ++= commonScalaOptions)
    )

lazy val lenses =
  project
    .settings(
      libraryDependencies ++= Seq(
        "com.github.julien-truffaut" %% "monocle-core"  % "2.0.3",
        "com.github.julien-truffaut" %% "monocle-macro" % "2.0.3"
      ),
      Seq(scalacOptions ++= commonScalaOptions)
    )

lazy val `streaming-json` =
  project
    .settings(
      libraryDependencies ++= Seq(
        "dev.zio"                   %% "zio"                  % "2.0.21",
        "dev.zio"                   %% "zio-streams"          % "2.0.21",
        "com.fasterxml.jackson.core" % "jackson-core"         % "2.17.0",
        "io.circe"                  %% "circe-core"           % "0.14.3",
        "io.circe"                  %% "circe-generic"        % "0.14.3",
        "io.circe"                  %% "circe-generic-extras" % "0.14.3"
      ),
      Seq(scalacOptions ++= commonScalaOptions)
    )

lazy val `zio-amqp` =
  project
    .settings(
      libraryDependencies ++= Seq(
        "dev.zio" %% "zio"      % "2.1.9",
        "dev.zio" %% "zio-amqp" % "1.0.0"
      ),
      Seq(scalacOptions ++= commonScalaOptions)
    )

lazy val `zio-samples` =
  project
    .settings(
      libraryDependencies ++= Seq("dev.zio" %% "zio" % "1.0.2"),
      Seq(scalacOptions ++= commonScalaOptions)
    )

lazy val `zio-kafka` =
  project
    .settings(
      libraryDependencies ++= Seq(
        "dev.zio"    %% "zio-streams"         % "1.0.2",
        "dev.zio"    %% "zio-kafka"           % "0.14.0",
        "com.goyeau" %% "kafka-streams-circe" % "0.6.3"
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
