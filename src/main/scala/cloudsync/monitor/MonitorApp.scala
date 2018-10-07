package cloudsync.monitor

import cloudsync._
import java.io.File
import java.util.concurrent.{LinkedBlockingQueue, TimeUnit}

import cloudsync.client.{CloudClient, S3Client}
import org.apache.commons.io.monitor.{FileAlterationMonitor, FileAlterationObserver}

import scala.concurrent.{Await, Future}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.io.Source
import scala.util.{Failure, Success, Try}

case class Event(eventType: String, filePath: String, localDir: String, remoteDir: String)
case class PathMap(localDir: String, remoteDir: String)

/**
  * https://stackify.com/java-thread-pools/
  */
object MonitorApp extends Loggable {
  val queue = new LinkedBlockingQueue[Event](1000)

  private def processQueueThread(env: Env): Future[Unit] = {
    import cloudsync.FileSyncOps
    import cloudsync.FileOps

    var continue = true

    Future {
      log.info(s"Started queue processing thread, current queue size ${queue.size}.")
      while (continue) {
        val resp: Maybe[Boolean] = queue.poll(1000, TimeUnit.MILLISECONDS) match {
          case Event("create", path, localDir, remoteDir) => {
            log.info(s"processing create event with path: $path")
            for {
              relativePath  <- FileOps.absoluteToRelative(path, localDir)
              ret           <- FileSyncOps.uploadFileIfChanged(localDir, relativePath, remoteDir).run(env)
            } yield ret
          }
          case Event("update", path, localDir, remoteDir) => {
            log.info(s"processing update event with path: $path")
            for {
              relativePath  <- FileOps.absoluteToRelative(path, localDir)
              ret           <- FileSyncOps.uploadFileIfChanged(localDir, relativePath, remoteDir).run(env)
            } yield ret
          }
          case Event("delete", path, localDir, remoteDir) => {
            log.info(s"processing delete event with path: $path")
            for {
              relativePath  <- FileOps.absoluteToRelative(path, localDir)
              ret           <- FileSyncOps.deleteFile(localDir, relativePath, remoteDir).run(env)
            } yield ret
          }
          case Event("terminate", _, _, _) => continue = false; Right(true)
          case null => Right(true)
          case _ => throw new Exception("Unknown event")
        }

        resp match {
          case Left(msg) => throw new Exception(s"!!! There was an ERROR: $msg")
          case _ => ()
        }
      }
    }
  }

  private def pathMonitorThread(pathMaps: Seq[PathMap]): Unit = {
    val monitor = new FileAlterationMonitor()
    val listener = new FileAlterationListenerImpl(queue, createEvent(pathMaps)_)

    for (path <- pathMaps.map(_.localDir)) {
      val fao = new FileAlterationObserver(new File(path))
      fao.addListener(listener)
      monitor.addObserver(fao)
      log.info(s"Registering listener for path $path")
    }

    println("Starting monitor. CTRL+C to stop.")
    monitor.start()

    Runtime.getRuntime.addShutdownHook(new Thread(
      () => {
        println("Stopping file monitor... It may take several seconds.")
        monitor.stop()
        queue.put(Event("terminate", "", "", ""))
      }
    ))
  }

  def createEvent(pathMaps: Seq[PathMap])(eventType: String, triggerFile: String): Event = {
      val pathMap: PathMap = pathMaps.filter(x => FileOps.pathHasPrefix(triggerFile, x.localDir)).head
      Event(eventType, triggerFile, pathMap.localDir, pathMap.remoteDir)
  }

  def main(args: Array[String]): Unit = {
    print(Source.fromFile("banner.txt").mkString)

    val cli = new CliConf(args)
    val sync    = cli.syncOnStart()
    val paths   = cli.monitorPaths().replaceAll(" ", "").split(",")

    val pathMaps: List[PathMap] = Try {
      paths
        .map(_.split(":"))
        .map(x => PathMap(x(0), x(1)))
    } match {
      case Failure(err) => throw new Exception(s"ERROR: failed at parsing path maps: $err")
      case Success(v) => v.toList
    }

    val env: Env = new Env {
      val config: Config = Config.create
      val client: CloudClient = new S3Client(config.serviceOpts("region"), config.serviceOpts("bucketName"))
    }

    if (sync) {
      val filesToSync = FileOps.listAllFiles("")
      val createEventScoped = createEvent(pathMaps)_

      filesToSync.map(createEventScoped("update", _))
        .foreach(queue.put)
      log.info(s"Queued ${filesToSync.size} files")
    }

    processQueueThread(env)
    pathMonitorThread(pathMaps)
  }
}