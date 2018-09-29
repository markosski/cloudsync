package cloudsync.monitor

import java.io.File
import java.util.concurrent.LinkedBlockingQueue

import cloudsync.Loggable
import org.apache.commons.io.monitor.{FileAlterationListener, FileAlterationObserver}

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
