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
  .aggregate(eboxCli)

lazy val declineVersion = "1.3.0"

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
    testFrameworks += new TestFramework("munit.Framework"),
    nativeImageOptions ++= List(
      "--no-server", // avoids starting a local build server
      "--no-fallback",
      "--enable-https",
      "-H:+TraceClassInitialization",
      //https://github.com/oracle/graal/blob/146a5548cf95c4803c10103f1e67a0e59dd93ce7/substratevm/ClassInitialization.md
      "--initialize-at-build-time=scala,org.slf4j.LoggerFactory",
      "--initialize-at-run-time=io.netty.handler.ssl.ConscryptAlpnSslEngine",
      "--allow-incomplete-classpath"
    )
  )
