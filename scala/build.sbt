lazy val akkaHttpVersion = "10.1.11"
lazy val akkaVersion     = "2.6.5"
lazy val osLibVersion    = "0.3.3"

lazy val pennsieveCoreVersion = SettingKey[String]("pennsieveCoreVersion")
pennsieveCoreVersion := (CrossVersion.partialVersion(scalaVersion.value) match {
  case Some((2, 12)) => "166-27f7fae"
  case _ => "230-d06f311"
})

lazy val circeVersion = SettingKey[String]("circeVersion")
circeVersion := (CrossVersion.partialVersion(scalaVersion.value) match {
  case Some((2, 12)) => "0.11.1"
  case _ => "0.14.1"
})

lazy val enumeratumVersion = SettingKey[String]("enumeratumVersion")
enumeratumVersion := (CrossVersion.partialVersion(scalaVersion.value) match {
  case Some((2, 12)) => "1.5.15"
  case _ => "1.7.0"
})

lazy val scallopVersion = SettingKey[String]("scallopVersion")
scallopVersion := (CrossVersion.partialVersion(scalaVersion.value) match {
  case Some((2, 12)) => "3.2.0"
  case _ => "4.1.0"
})

lazy val jwtCirceModuleID = SettingKey[ModuleID]("jwtCirceModuleID")
jwtCirceModuleID := (CrossVersion.partialVersion(scalaVersion.value) match {
  case Some((2, 12)) => "com.pauldijou" %% "jwt-circe" % "2.1.0"
  // The groupId changed in version 6.0
  case _ => "com.github.jwt-scala" %% "jwt-circe" % "9.0.5"
})

lazy val scala212 = "2.12.6"
lazy val scala213 = "2.13.8"
lazy val supportedScalaVersions = List(scala212, scala213)

val assemblyJarPath = taskKey[Unit]("Call assembly and get the JAR file path.")

lazy val root = (project in file("."))
  .enablePlugins(AutomateHeaderPlugin)
  .settings(
    inThisBuild(List(
      organization := "com.pennsieve",
      scalaVersion := scala212,
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
      "com.beachape" %% "enumeratum" % enumeratumVersion.value,
      "com.beachape" %% "enumeratum-circe" % enumeratumVersion.value,

      "com.pennsieve" %% "core-models" % pennsieveCoreVersion.value,
      "com.pennsieve" %% "utilities" % "4-55953e4",

      jwtCirceModuleID.value,
      "io.circe" %% "circe-core" % circeVersion.value,
      "io.circe" %% "circe-generic-extras" % circeVersion.value,
      "io.circe" %% "circe-parser" % circeVersion.value,
      "com.chuusai" %% "shapeless" % "2.3.3",

      "com.lihaoyi" %% "os-lib" % "0.3.0",

      "com.typesafe.akka" %% "akka-http" % akkaHttpVersion,

      "com.typesafe.akka" %% "akka-http-testkit" % akkaHttpVersion % Test,
      "com.typesafe.akka" %% "akka-stream" % akkaVersion % Test,
      "com.typesafe.akka" %% "akka-testkit" % akkaVersion % Test,

      "org.rogach" %% "scallop" % scallopVersion.value,

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
