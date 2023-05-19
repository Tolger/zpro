ThisBuild / version := "0.1.0"

ThisBuild / scalaVersion := "2.13.10"

val AkkaVersion = "2.7.0"
val AkkaHttpVersion = "10.4.0"
val ScalaTestVersion = "3.2.15"
val ScalaMockVersion = "5.1.0"
val Neo4jDriverVersion = "5.5.0"
val SangriaVersion = "3.5.3"
val SangriaAkkaHttpVersion = "0.0.3"
val SangriaSprayVersion = "1.0.3"
val ZioVersion = "2.0.8"

lazy val root = (project in file("."))
  .settings(
    name := "backend",
    crossPaths := false,
    crossScalaVersions := Nil,
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-actor-typed" % AkkaVersion,
      "com.typesafe.akka" %% "akka-stream" % AkkaVersion,
      "com.typesafe.akka" %% "akka-http" % AkkaHttpVersion,
      "com.typesafe.akka" %% "akka-http-spray-json" % AkkaHttpVersion,
      "org.sangria-graphql" %% "sangria" % SangriaVersion,
      "org.sangria-graphql" %% "sangria-spray-json" % SangriaSprayVersion,
      "org.neo4j.driver" % "neo4j-java-driver" % Neo4jDriverVersion,
      "dev.zio" %% "zio" % ZioVersion,
      "dev.zio" %% "zio-streams" % ZioVersion,
      "org.scalatest" %% "scalatest" % ScalaTestVersion % Test,
      "org.scalamock" %% "scalamock" % ScalaMockVersion % Test
    ),
    assembly / mainClass := Some("de.zpro.backend.Starter"),
    scalacOptions ++= Seq("-deprecation", "-feature"),
    coverageExcludedPackages := "<empty>;de.zpro.backend*;de.zpro.backend.load*;de.zpro.backend.graphql*",
    coverageFailOnMinimum := true,
    coverageMinimumStmtTotal := 95
  )
