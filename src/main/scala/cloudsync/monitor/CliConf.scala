package cloudsync.monitor

import org.rogach.scallop._

class CliConf(arguments: Seq[String]) extends ScallopConf(arguments) {
  val syncOnStart = opt[Boolean](short='s')
  val monitor = opt[Boolean](short='m')
  val localDir = trailArg[String](required = true)
  val remoteDir = trailArg[String](required = true)
  verify()
}
