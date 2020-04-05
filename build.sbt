name := "scala_samples"

ThisBuild / organization := "ru.ekuzmichev"
ThisBuild / version := "0.1-SNAPSHOT"
ThisBuild / scalaVersion := "2.12.8"

lazy val root =
  project
  .in(file("."))
  .aggregate(
    `cats-retry-samples`
  )

lazy val `cats-retry-samples` =
  project
    .settings(
      libraryDependencies ++= Seq(libs.catsRetry),
      Seq(scalacOptions ++= fpScalaOptions)
    )

lazy val fpScalaOptions = Seq("-Xfatal-warnings", "-Ypartial-unification")

lazy val libs = new {
  private val catsRetryV = "1.1.0"

  val catsRetry = "com.github.cb372" %% "cats-retry" % catsRetryV
}
