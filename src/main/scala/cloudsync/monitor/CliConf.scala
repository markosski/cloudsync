package cloudsync.monitor

import org.rogach.scallop._

class CliConf(arguments: Seq[String]) extends ScallopConf(arguments) {
//  val remoteDir = opt[String](descr = "local path to monitor or copy,remote path (required)")
  val syncOnStart = opt[Boolean](short='s', descr="sync all files with remote on start")
  val monitorPaths = trailArg[String](descr = "local path to monitor or copy,remote path (required)")

  verify()
}
