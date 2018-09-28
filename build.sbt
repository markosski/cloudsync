import Dependencies._

lazy val root = (project in file(".")).
  settings(
    inThisBuild(List(
      organization := "com.marcinkossakowski",
      scalaVersion := "2.12.5",
      version      := "0.1.0-SNAPSHOT"
    )),
    name := "cloudsync",
    libraryDependencies ++= Seq(scalaTest % Test, conf, s3)
      ++ cats
      ++ log
      ++ json
      ++ commonsIO
  )
