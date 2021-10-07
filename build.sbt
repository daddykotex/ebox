Global / excludeLintKeys ++= Set(nativeImageVersion, nativeImageJvmIndex, nativeImageJvm)
lazy val root = (project in file("."))
  .settings(
    inThisBuild(
      List(
        organization := "com.daddykotex",
        scalaVersion := "2.13.3"
      )
    ),
    name := "ebox"
  )
  .aggregate(eboxCli, coopCli)

lazy val declineVersion = "1.3.0"

lazy val debugOptionsNativeImage = Seq(
  "-H:+ReportExceptionStackTraces",
  "-H:+ReportUnsupportedElementsAtRuntime",
  "-H:+TraceClassInitialization",
  "-H:+PrintClassInitialization",
  "-H:+StackTrace",
  "-H:+JNI",
  "-H:-SpawnIsolates",
  "-H:-UseServiceLoaderFeature",
  "-H:+RemoveSaturatedTypeFlows"
)
lazy val allNativeImageOptions = Def.settings(
  nativeImageVersion := "21.1.0",
  nativeImageJvmIndex := "jabba",
  nativeImageJvm := "graalvm",
  nativeImageOptions ++= List(
    "--verbose",
    "--no-server",
    "--no-fallback",
    "--enable-http",
    "--enable-https",
    "-H:IncludeLocales=fr,en",
    "--enable-all-security-services",
    "--report-unsupported-elements-at-runtime",
    "--allow-incomplete-classpath",
    "--initialize-at-build-time=scala,org.slf4j.LoggerFactory",
    "--initialize-at-run-time=io.netty.handler.ssl.ConscryptAlpnSslEngine,org.asynchttpclient"
  )
  // run / javaOptions := {
  //   val path = baseDirectory.value / "src" / "main" / "resources" / "META-INF" / "native-image"
  //   Seq(s"-agentlib:native-image-agent=config-output-dir=$path")
  // }
)

addCommandAlias("buildCli", "eboxCli/nativeImage")
lazy val eboxCli = (project in file("ebox-cli"))
  .enablePlugins(NativeImagePlugin)
  .settings(
    name := "ebox-cli",
    Compile / mainClass := Some("Main"),
    addCompilerPlugin("com.olegpy" %% "better-monadic-for" % "0.3.1"),
    libraryDependencies ++= Seq(
      "org.typelevel" %% "cats-core" % "2.2.0",
      "org.http4s" %% "http4s-async-http-client" % "0.21.8",
      "com.monovore" %% "decline" % declineVersion,
      "com.monovore" %% "decline-effect" % declineVersion,
      "org.scalameta" %% "munit" % "0.7.17" % Test,
      "org.typelevel" %% "munit-cats-effect-2" % "0.7.0" % Test,
      "org.http4s" %% "http4s-dsl" % "0.21.8" % Test
    ),
    run / fork := true,
    testFrameworks += new TestFramework("munit.Framework")
  )
  .settings(allNativeImageOptions)

lazy val coopCli = (project in file("coop-cli"))
  .enablePlugins(NativeImagePlugin)
  .settings(
    name := "coop-cli",
    Compile / mainClass := Some("Main"),
    addCompilerPlugin("com.olegpy" %% "better-monadic-for" % "0.3.1"),
    addCompilerPlugin("org.typelevel" % "kind-projector" % "0.13.0" cross CrossVersion.full),
    libraryDependencies ++= Seq(
      "org.typelevel" %% "cats-core" % "2.2.0",
      "org.http4s" %% "http4s-async-http-client" % "0.21.8",
      "com.monovore" %% "decline" % declineVersion,
      "com.monovore" %% "decline-effect" % declineVersion,
      "org.scalameta" %% "munit" % "0.7.17" % Test,
      "org.typelevel" %% "munit-cats-effect-2" % "0.7.0" % Test,
      "org.http4s" %% "http4s-dsl" % "0.21.8" % Test
    ),
    run / fork := true,
    testFrameworks += new TestFramework("munit.Framework")
  )
  .settings(allNativeImageOptions)
