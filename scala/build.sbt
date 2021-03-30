lazy val akkaHttpVersion = "10.1.11"
lazy val akkaVersion     = "2.6.5"
lazy val pennsieveCoreVersion = "11-125f8fb"
lazy val circeVersion    = "0.11.1"
lazy val osLibVersion    = "0.3.3"


val assemblyJarPath = taskKey[Unit]("Call assembly and get the JAR file path.")

lazy val root = (project in file("."))
  .enablePlugins(AutomateHeaderPlugin)
  .settings(
    inThisBuild(List(
      organization := "com.pennsieve",
      scalaVersion := "2.12.6",
      version := sys.props.get("version").getOrElse("SNAPSHOT")
    )),
    name := "auth-middleware",
    headerLicense := Some(HeaderLicense.Custom(
      "Copyright (c) 2021 University of Pennsylvania. All Rights Reserved."
    )),
    headerMappings := headerMappings.value + (HeaderFileType.scala -> HeaderCommentStyle.cppStyleLineComment),
    resolvers ++= Seq(
      "Pennsieve Releases" at "https://nexus.pennsieve.cc/repository/maven-releases",
      "Pennsieve Snapshots" at "https://nexus.pennsieve.cc/repository/maven-snapshots",
      Resolver.bintrayRepo("commercetools", "maven"),
      Resolver.sonatypeRepo("releases"),
      Resolver.sonatypeRepo("snapshots")
    ),
    libraryDependencies ++= Seq(
      "com.beachape" %% "enumeratum" % "1.5.13",
      "com.beachape" %% "enumeratum-circe" % "1.5.17",

      "com.pennsieve" %% "core-models" % pennsieveCoreVersion,
      "com.pennsieve" %% "utilities" % "3-cd7539b",

      "com.pauldijou" %% "jwt-circe" % "2.1.0",
      "io.circe" %% "circe-core" % circeVersion,
      "io.circe" %% "circe-generic-extras" % circeVersion,
      "io.circe" %% "circe-parser" % circeVersion,
      "com.chuusai" %% "shapeless" % "2.3.3",

      "com.lihaoyi" %% "os-lib" % "0.3.0",

      "com.typesafe.akka" %% "akka-http" % akkaHttpVersion,

      "com.typesafe.akka" %% "akka-http-testkit" % akkaHttpVersion % Test,
      "com.typesafe.akka" %% "akka-stream" % akkaVersion % Test,
      "com.typesafe.akka" %% "akka-testkit" % akkaVersion % Test,

      "org.rogach" %% "scallop" % "3.2.0",

      "org.scalatest" %% "scalatest" % "3.0.5" % Test,
    ),
    publishTo := {
      val nexus = "https://nexus.pennsieve.cc/repository"

      if (isSnapshot.value) {
        Some("Nexus Realm" at s"$nexus/maven-snapshots")
      } else {
        Some("Nexus Realm" at s"$nexus/maven-releases")
      }
    },
    publishMavenStyle := true,
    scalafmtOnCompile := true,
    credentials += Credentials("Sonatype Nexus Repository Manager",
      "nexus.pennsieve.cc",
      sys.env("PENNSIEVE_NEXUS_USER"),
      sys.env("PENNSIEVE_NEXUS_PW")
    ),
    test in assembly := {},  // Skip running tests during JAR assembly
    assemblyJarPath := {
      println(assembly.value.getAbsolutePath)
    }
  )
