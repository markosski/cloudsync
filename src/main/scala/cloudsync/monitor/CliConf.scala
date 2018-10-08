package cloudsync.monitor

import org.rogach.scallop._

class CliConf(arguments: Seq[String]) extends ScallopConf(arguments) {
  val syncOnStart = opt[Boolean](short='s', descr="sync all files with remote on start")
  val monitorPaths = trailArg[String](descr = "local path to monitor in format: \"localDir1:remoteDir1,localDir2:remoteDir2,...\"")
  verify()
}
