import Dependencies._

ThisBuild / scalaVersion     := "2.13.8"
ThisBuild / version          := "0.1.0-SNAPSHOT"
ThisBuild / organization     := "com.example"
ThisBuild / organizationName := "example"

lazy val root = (project in file("."))
  .settings(
    name := "Scala Seed Project",
    libraryDependencies += scalaTest % Test,
    libraryDependencies += "com.typesafe.akka" %% "akka-actor" % "2.6.9",
    libraryDependencies += "com.typesafe.akka" %% "akka-stream" % "2.6.9",
    libraryDependencies += "com.typesafe.akka" %% "akka-http" % "10.2.9",
  )

// See https://www.scala-sbt.org/1.x/docs/Using-Sonatype.html for instructions on how to publish to Sonatype.
