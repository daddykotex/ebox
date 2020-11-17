lazy val root = (project in file(".")).settings(
  inThisBuild(
    List(
      organization := "com.daddykotex",
      scalaVersion := "2.13.3"
    )
  ),
  name := "ebox"
)

lazy val declineVersion = "1.3.0"

lazy val eboxCli = (project in file("ebox-cli")).settings(
  name := "ebox-cli",
  libraryDependencies ++= Seq(
    "org.typelevel" %% "cats-core" % "2.2.0",
    "org.http4s" %% "http4s-blaze-client" % "0.21.8",
    "com.monovore" %% "decline" % declineVersion,
    "com.monovore" %% "decline-effect" % declineVersion
  )
)
