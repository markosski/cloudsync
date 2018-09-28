package cloudsync.monitor

import cats.data.Reader
import cloudsync._
import java.io.File
import java.util.concurrent.{LinkedBlockingQueue, TimeUnit}

import cloudsync.client.S3Client
import org.apache.commons.io.monitor.{FileAlterationListener, FileAlterationMonitor, FileAlterationObserver}

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

case class Event(eventType: String, filePath: String)

class FileAlterationListenerImpl extends FileAlterationListener with Loggable {
  val queue = new LinkedBlockingQueue[Event](100)

  override def onStart(observer: FileAlterationObserver): Unit = {
    log.info("Checking events.")
  }

  override def onDirectoryCreate(directory: File): Unit = log.info(s"dir create: ${directory.getAbsolutePath}")

  override def onDirectoryChange(directory: File): Unit = log.info(s"dir change: ${directory.getAbsolutePath}")

  override def onDirectoryDelete(directory: File): Unit = log.info(s"dir delete: ${directory.getAbsolutePath}")

  override def onFileCreate(file: File): Unit = {
    log.info(s"file create: ${file.getAbsolutePath}")
    queue.put(Event("create", file.getAbsolutePath))
  }

  override def onFileChange(file: File): Unit = {
    log.info(s"file update: ${file.getAbsolutePath}")
    queue.put(Event("update", file.getAbsolutePath))
  }

  override def onFileDelete(file: File): Unit = {
    log.info(s"file delete: ${file.getAbsolutePath}")
    queue.put(Event("delete", file.getAbsolutePath))
  }

  override def onStop(observer: FileAlterationObserver): Unit = ()
}

object FileSyncMonitor extends Loggable {
  val listener = new FileAlterationListenerImpl()

  def processQueue(localBasePath: String, remoteBasePath: String) = Reader[Env, Future[Unit]] {
    import cloudsync.FileSyncOps
    import cloudsync.FileOps

    env => {
      Future {
        while (true) {
          val resp = listener.queue.poll(1, TimeUnit.SECONDS) match {
            case Event("create", path) => {
              for {
                relativePath  <- FileOps.absoluteToRelative(path, localBasePath)
                ret           <- FileSyncOps.uploadFileIfChanged(localBasePath, relativePath, remoteBasePath).run(env)
              } yield ret
            }
            case Event("update", path) => {
              for {
                relativePath  <- FileOps.absoluteToRelative(path, localBasePath)
                ret           <- FileSyncOps.uploadFileIfChanged(localBasePath, relativePath, remoteBasePath).run(env)
              } yield ret
            }
            case Event("delete", path) => {
              for {
                relativePath  <- FileOps.absoluteToRelative(path, localBasePath)
                ret           <- FileSyncOps.deleteFile(localBasePath, relativePath, remoteBasePath).run(env)
              } yield ret
            }
            case Event("terminate", _) => throw new Exception("terminating thread")
            case _ => Right(true)
          }

          resp match {
            case Left(msg) => log.info(s"Error: $msg")
            case Right(x) => ()
          }
        }
      }
    }
  }

  private def runMonitor(path: String): Unit = {
    val dir = new File(path)
    val fao = new FileAlterationObserver(dir)
    fao.addListener(listener)

    val monitor = new FileAlterationMonitor()
    monitor.addObserver(fao)

    println("Starting monitor. CTRL+C to stop.")
    monitor.start()

    Runtime.getRuntime.addShutdownHook(new Thread(
      () => {
        println("Stopping file monitor.")
        monitor.stop()
        listener.queue.put(Event("terminate", ""))
      }
    ))
  }

  def main(args: Array[String]): Unit = {
    val env: Env = new Env {
      val client = new S3Client("us-east-1", "markos-files")
    }

    val localBasePath = args(0)
    val remoteBasePath = args(1)

    processQueue(localBasePath, remoteBasePath).run(env)
    runMonitor(localBasePath)
  }
}
