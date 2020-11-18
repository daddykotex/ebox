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
    libraryDependencies ++= Seq(
      "org.typelevel" %% "cats-core" % "2.2.0",
      "org.http4s" %% "http4s-blaze-client" % "0.21.8",
      "com.monovore" %% "decline" % declineVersion,
      "com.monovore" %% "decline-effect" % declineVersion,
      "org.scalameta" %% "munit" % "0.7.17" % Test,
      "org.typelevel" %% "munit-cats-effect-2" % "0.7.0" % Test
    ),
    testFrameworks += new TestFramework("munit.Framework")
  )
