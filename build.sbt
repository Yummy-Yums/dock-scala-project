ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "3.3.0"

val commonSettings = Seq(
  scalacOptions -= "-Xfatal-warnings",
  scalacOptions += "-Xmax-inlines:256",
  libraryDependencies ++= Seq(
    "org.typelevel" %% "cats-effect" % "3.5.0",
    //    "ch.epfl.scala" %% "bloop-config" % "1.5.5",
    "com.disneystreaming" %% "weaver-cats" % "0.8.3" % Test,
    "com.disneystreaming" %% "weaver-scalacheck" % "0.8.3" % Test,
    "com.disneystreaming" %% "weaver-discipline" % "0.8.3" % Test,
    "org.typelevel" %% "cats-laws" % "2.7.0" % Test,
     compilerPlugin("org.polyvariant" % "better-tostring_3.3.0" % "0.3.17")
  )
)

val tapirVersion = "1.0.1"

val shared = project.settings(
  commonSettings,
  libraryDependencies ++= Seq(
    "org.typelevel" %% "cats-effect" % "3.3.12",
    "co.fs2" %% "fs2-core" % "3.2.7",
    "com.softwaremill.sttp.client3" %% "fs2" % "3.5.1",
    "com.softwaremill.sttp.tapir" %% "tapir-core" % "1.8.2",
    "com.softwaremill.sttp.tapir" %% "tapir-json-circe" % "1.8.2",
    "io.circe" %% "circe-fs2" % "0.14.0",
    "org.typelevel" %% "log4cats-core" % "2.6.0",
    "org.typelevel" %% "log4cats-noop" % "2.6.0",
    compilerPlugin("org.polyvariant" %% "better-tostring" % "0.3.15" cross CrossVersion.full)
  ),
  testFrameworks += new TestFramework("weaver.framework.CatsEffect"),
)

val http4sVersion = "0.23.23"

def full(p: Project) = p % "test->test;compile->compile"
val sharedFull = shared % "test->test;compile->compile"

val server = project
  .settings(
    commonSettings,
    libraryDependencySchemes += "org.http4s" %% "http4s-server_3" % VersionScheme.Always,
    libraryDependencies ++= Seq(
      "com.softwaremill.sttp.tapir" %% "tapir-http4s-server" % "1.8.2",
      "org.http4s" %% "http4s-ember-client" % http4sVersion,
      "org.http4s" %% "http4s-ember-server" % http4sVersion,
      "org.http4s" %% "http4s-dsl" % http4sVersion,
      "ch.qos.logback" % "logback-classic" % "1.1.7",
      "org.http4s" %% "http4s-circe" % http4sVersion % Test,
      "org.http4s" %% "http4s-dsl" % http4sVersion % Test,
      "dev.optics" %% "monocle-core" % "3.1.0",


    )
  )
  .dependsOn(full(shared))

val client = project.settings(
    commonSettings,
    libraryDependencies ++= Seq(
      "org.http4s" %% "http4s-ember-client" % http4sVersion,
      "com.softwaremill.sttp.tapir" %% "tapir-http4s-client" % "1.8.2",
      "ch.qos.logback" % "logback-classic" % "1.1.7",
      "com.monovore" %% "decline-effect" % "2.4.1",
    ),
    Compile / mainClass := Some("dock.Main"),
    nativeImageVersion := "22.1.0",
    nativeImageOptions ++= Seq(
      "-H:+ReportExceptionStackTraces",
      "--no-fallback"
    )
  )
  .enablePlugins(NativeImagePlugin)
  .dependsOn(full(shared))

val e2e = project
  .settings(commonSettings)
  .dependsOn(server, client)

lazy val root = (project in file("."))
  .settings(
    name := "dock",
    idePackagePrefix := Some("dock"),
    publish := {},
    publish / skip := true
  )
  .aggregate(server, client, shared, e2e)
