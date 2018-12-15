package cloudsync.monitor

import cloudsync._
import java.io.File
import java.util.concurrent.{LinkedBlockingQueue, TimeUnit}

import cats.Parallel
import cats.effect._
import cats.implicits._
import cloudsync.client.{CloudClient, S3Client}
import org.apache.commons.io.monitor.{FileAlterationMonitor, FileAlterationObserver}

import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
import scala.io.Source
import scala.util.{Failure, Success, Try}
import scala.annotation.tailrec

case class Event(eventType: String, filePath: String, localDir: String, remoteDir: String)
case class PathMap(localDir: String, remoteDir: String)

/**
  * https://stackify.com/java-thread-pools/
  */
object MonitorApp extends App with Loggable {
  val queue = new LinkedBlockingQueue[Event](1000)
  implicit val cs = IO.contextShift(scala.concurrent.ExecutionContext.global)

  @tailrec
  final def poll[A](queue: LinkedBlockingQueue[A], xs: List[A]): List[A] = {
    if (queue.size == 0 || xs.size == 100) xs.reverse
    else poll(queue, queue.take :: xs)
  }

  def processEvent[F[_]](event: Event)(implicit env: Env[F], E: Effect[F]): F[Unit] = event match {
    case Event("create", path, localDir, remoteDir) => for {
        _             <- logInfo(s"processing create event with path: $path")
        relativePath  <- E.pure(FileOps.absoluteToRelative(path, localDir))
        ret           <- FileSyncService.uploadFileIfChanged(localDir, relativePath, remoteDir)
      } yield ret
    case Event("update", path, localDir, remoteDir) => for {
        _             <- logInfo(s"processing update event with path: $path")
        relativePath  <- E.pure(FileOps.absoluteToRelative(path, localDir))
        ret           <- FileSyncService.uploadFileIfChanged(localDir, relativePath, remoteDir)
      } yield ret
    case Event("delete", path, localDir, remoteDir) => for {
        _             <- logInfo(s"processing delete event with path: $path")
        relativePath  <- E.pure(FileOps.absoluteToRelative(path, localDir))
        ret           <- FileSyncService.deleteFile(localDir, relativePath, remoteDir)
      } yield ret
    case Event("terminate", _, _, _) => E.pure(())
    case null => E.pure(())
    case _ => E.raiseError(new Exception("Unknown event"))
  } 

  /**
   * https://stackoverflow.com/questions/50232860/effect-abstraction-in-cats-and-parallel-execution
   * https://typelevel.org/cats/typeclasses/parallel.html
   */
  def processQueue[F[_]: ConcurrentEffect, G[_]](queue: LinkedBlockingQueue[Event])
  (implicit env: Env[F], E: Effect[F], cs: ContextShift[F], ev: Parallel[F, G]) = {
    for {
      _       <- logDebug(s"Draining event queue of current size: ${queue.size}")
      events  <- E.delay(poll(queue, List[Event]()))
      proc    <- events.map(processEvent(_)).parSequence
    } yield proc
  }

  def createEvent(pathMaps: Seq[PathMap])(eventType: String, triggerFile: String): Event = {
    val pathMap: PathMap = pathMaps.filter(x => FileOps.pathHasPrefix(triggerFile, x.localDir)).head
    Event(eventType, triggerFile, pathMap.localDir, pathMap.remoteDir)
  }

  def startMonitor[F[_]](queue: LinkedBlockingQueue[Event], pathMaps: Seq[PathMap])
  (implicit E: Effect[F]): F[Unit] = E.delay {
    val monitor = new FileAlterationMonitor()
    val listener = new FileAlterationListenerImpl(queue, createEvent(pathMaps) _)

    for (path <- pathMaps.map(_.localDir)) {
      val fao = new FileAlterationObserver(new File(path))
      fao.addListener(listener)
      monitor.addObserver(fao)
      log.info(s"Registering listener for path $path")
    }

    monitor.start
  }

  print(Source.fromFile("src/main/resources/banner.txt").mkString)

  val cli = new CliConf(args)
  val sync  = cli.syncOnStart()
  val paths = cli.monitorPaths().split(",").map(_.trim)

  val pathMaps: List[PathMap] = Try {
    paths
      .map(_.split(":"))
      .map(x => PathMap(x(0), x(1)))
  } match {
    case Failure(err) => throw new Exception(s"ERROR: failed at parsing path maps: $err")
    case Success(v) => v.toList
  }

  if (sync) {
    println("test test test")
    val createEventScoped = createEvent(pathMaps)_

    for (pathMap <- pathMaps) {
      val filesToSync = FileOps.listAllFiles(pathMap.localDir)

      filesToSync.map(createEventScoped("update", _))
        .foreach(queue.put)
      log.info(s"Queued ${filesToSync.size} files for local dir ${pathMap.localDir}")
    }
  }

  implicit val env = new Env[IO] {
    val config: Config = Config.create
    val client: CloudClient[IO] = new S3Client(config.serviceOpts("region"), config.serviceOpts("bucketName"))
  }

  startMonitor[IO](queue, pathMaps).unsafeRunSync

  while (true) {
    processQueue[IO, IO.Par](queue).attempt.unsafeRunSync
    Thread.sleep(1000)
  }
}
