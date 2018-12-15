package cloudsync.monitor

import org.rogach.scallop._

class CliConf(arguments: Seq[String]) extends ScallopConf(arguments) {
  val syncOnStart = opt[Boolean](short='s', descr="Sync all files with remote on start")
  val monitorPaths = trailArg[String](descr = """Path map of directories to monitor in format: `localDir1:remoteDir1,localDir2:remoteDir2,...`. 
  Alternatively accepts local file `file:///absolute/path/to/file.txt`. Format of path mapping is as above but using line break as delimiter character.""")
  verify()
}
