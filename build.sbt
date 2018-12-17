import Dependencies._

lazy val root = (project in file(".")).
  settings(
    mainClass in assembly := Some("cloudsync.monitor.MonitorApp"),
    inThisBuild(List(
      organization := "com.marcinkossakowski",
      scalaVersion := "2.12.5",
      version      := "0.1.0-SNAPSHOT"
    )),
    name := "cloudsync",
    libraryDependencies ++= Seq(scalaTest % Test, conf, s3, cli, akka)
      ++ cats
      ++ log
      ++ json
      ++ commonsIO
  )

scalacOptions ++= Seq(
  "-deprecation",
  "-encoding", "UTF-8",
  "-unchecked",
  "-language:higherKinds",
  "-language:implicitConversions",
  "-feature",
  "-Xfatal-warnings",
  "-Ypartial-unification"
)
