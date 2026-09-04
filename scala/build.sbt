lazy val akkaHttpVersion = "10.1.11"
lazy val akkaVersion     = "2.6.5"
lazy val osLibVersion    = "0.3.3"

// Scala 2.12 cross-build dropped (2.13 only); per-version settings flattened
// to their 2.13 values. core-models bumped to a version that includes
// Role.Guest (added 2022-10), which the previously-pinned core-models predated.
lazy val pennsieveCoreVersion = "436-bb30af9"
lazy val circeVersion         = "0.14.1"
lazy val enumeratumVersion    = "1.7.0"
lazy val scallopVersion       = "4.1.0"
lazy val jwtCirceModuleID: ModuleID = "com.github.jwt-scala" %% "jwt-circe" % "9.0.5"

lazy val scala213 = "2.13.8"
lazy val supportedScalaVersions = List(scala213)

val assemblyJarPath = taskKey[Unit]("Call assembly and get the JAR file path.")

lazy val root = (project in file("."))
  .enablePlugins(AutomateHeaderPlugin)
  .settings(
    inThisBuild(List(
      organization := "com.pennsieve",
      scalaVersion := scala213,
      version := sys.props.get("version").getOrElse("bootstrap-SNAPSHOT"),
      crossScalaVersions := supportedScalaVersions,
      scalacOptions ++= Seq(
        "-deprecation",
      )
    )),
    name := "auth-middleware",
    headerLicense := Some(HeaderLicense.Custom(
      "Copyright (c) 2021 University of Pennsylvania. All Rights Reserved."
    )),
    headerMappings := headerMappings.value + (HeaderFileType.scala -> HeaderCommentStyle.cppStyleLineComment),
    resolvers ++= Seq(
      "Pennsieve Releases" at "https://nexus.pennsieve.cc/repository/maven-releases",
      "Pennsieve Snapshots" at "https://nexus.pennsieve.cc/repository/maven-snapshots",
      Resolver.sonatypeRepo("releases"),
      Resolver.sonatypeRepo("snapshots")
    ),
    libraryDependencies ++= Seq(
      "com.beachape" %% "enumeratum" % enumeratumVersion,
      "com.beachape" %% "enumeratum-circe" % enumeratumVersion,

      "com.pennsieve" %% "core-models" % pennsieveCoreVersion,
      "com.pennsieve" %% "utilities" % "4-55953e4",

      jwtCirceModuleID,
      "io.circe" %% "circe-core" % circeVersion,
      "io.circe" %% "circe-generic-extras" % circeVersion,
      "io.circe" %% "circe-parser" % circeVersion,
      "com.chuusai" %% "shapeless" % "2.3.3",

      "com.lihaoyi" %% "os-lib" % "0.3.0",

      "com.typesafe.akka" %% "akka-http" % akkaHttpVersion,

      "com.typesafe.akka" %% "akka-http-testkit" % akkaHttpVersion % Test,
      "com.typesafe.akka" %% "akka-stream" % akkaVersion % Test,
      "com.typesafe.akka" %% "akka-testkit" % akkaVersion % Test,

      "org.rogach" %% "scallop" % scallopVersion,

      "org.scalatest" %% "scalatest" % "3.2.11" % Test,
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
    assembly / test := {},  // Skip running tests during JAR assembly
    assemblyJarPath := {
      println(assembly.value.getAbsolutePath)
    }
  )
