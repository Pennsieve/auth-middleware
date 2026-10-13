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

lazy val scala213 = "2.13.16"

// Pennsieve packages live in AWS CodeArtifact (pennsieve-cc), `maven` repository.
// Access is IAM: the build exchanges the active AWS credentials -- the Jenkins
// agent's instance role, or `assume-role non-prod admin` locally -- for a
// short-lived token. CODEARTIFACT_AUTH_TOKEN, if set, is used instead.
lazy val codeArtifactHost = "pennsieve-932986170183.d.codeartifact.us-east-1.amazonaws.com"
lazy val codeArtifactRepo = s"https://$codeArtifactHost/maven/maven/"
lazy val codeArtifactToken: String = sys.env.get("CODEARTIFACT_AUTH_TOKEN").getOrElse {
  import scala.sys.process._
  val cmd = Seq("aws", "codeartifact", "get-authorization-token", "--domain", "pennsieve",
    "--domain-owner", "932986170183", "--region", "us-east-1",
    "--query", "authorizationToken", "--output", "text")
  scala.util.Try(cmd.!!(ProcessLogger(_ => ())).trim).getOrElse {
    println("[warn] No CodeArtifact token: AWS credentials missing or not allowed. Run assume-role non-prod admin.")
    ""
  }
}
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
    resolvers += "pennsieve" at codeArtifactRepo,
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
    publishTo := Some("pennsieve" at codeArtifactRepo),
    publishMavenStyle := true,
    // sbt's own publisher writes no maven-metadata.xml, so its releases stay
    // Unfinished in CodeArtifact; sbt-aether-deploy publishes like Maven.
    overridePublishSettings,
    scalafmtOnCompile := true,
    credentials += Credentials("pennsieve/maven", codeArtifactHost, "aws", codeArtifactToken),
    assembly / test := {},  // Skip running tests during JAR assembly
    assemblyJarPath := {
      println(assembly.value.getAbsolutePath)
    }
  )
