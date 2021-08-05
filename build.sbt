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
  .aggregate(eboxCli, blazeRepro)

lazy val blazeRepro = (project in file("blaze-repro"))
  .enablePlugins(NativeImagePlugin)
  .settings(
    name := "blaze-repro",
    Compile / mainClass := Some("blaze.Main"),
    addCompilerPlugin("com.olegpy" %% "better-monadic-for" % "0.3.1"),
    libraryDependencies ++= Seq(
      "org.typelevel" %% "cats-core" % "2.2.0",
      "org.http4s" %% "http4s-blaze-client" % "0.21.8",
      "ch.qos.logback" % "logback-classic" % "1.2.3"
    ),
    fork in run := true
  )

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
    // javaOptions in run := {
    //   val path = baseDirectory.value / "src" / "main" / "resources" / "META-INF" / "native-image"
    //   Seq(s"-agentlib:native-image-agent=config-output-dir=$path")
    // },
    fork in run := true,
    testFrameworks += new TestFramework("munit.Framework"),
    nativeImageOptions ++= List(
      "--verbose",
      "--no-server",
      "--no-fallback",
      "--enable-http",
      "--enable-https",
      "--enable-all-security-services",
      "--report-unsupported-elements-at-runtime",
      "--allow-incomplete-classpath",
      "--initialize-at-build-time=scala,org.slf4j.LoggerFactory",
      "--initialize-at-run-time=io.netty.handler.ssl.ConscryptAlpnSslEngine,org.asynchttpclient"
    )
  )
