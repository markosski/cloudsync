import sbt._

object Dependencies {
  lazy val scalaTest = "org.scalatest" %% "scalatest" % "3.0.5"
  lazy val cats = Seq(
    "org.typelevel" %% "cats-core" % "1.3.1",
    "org.typelevel" %% "cats-effect" % "1.0.0"
  )
  lazy val conf = "com.typesafe" % "config" % "1.3.2"
  lazy val log = Seq(
    "com.typesafe.scala-logging" %% "scala-logging" % "3.9.0",
    "ch.qos.logback" % "logback-classic" % "1.2.3"
  )
  lazy val s3 = "com.amazonaws" % "aws-java-sdk-s3" % "1.11.409"
  lazy val json = Seq(
    "com.fasterxml.jackson.core" % "jackson-core" % "2.9.6",
    "com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.9.6"
  )
  lazy val commonsIO = Seq(
    "commons-io" % "commons-io" % "2.6"
  )
  lazy val cli = "org.rogach" %% "scallop" % "3.1.3"
  lazy val akka = "com.typesafe.akka" %% "akka-actor" % "2.5.17"
}
