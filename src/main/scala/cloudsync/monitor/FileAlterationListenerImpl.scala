package cloudsync.monitor

import java.io.File
import java.util.concurrent.LinkedBlockingQueue

import cloudsync.Loggable
import org.apache.commons.io.monitor.{FileAlterationListener, FileAlterationObserver}

class FileAlterationListenerImpl(queue: LinkedBlockingQueue[Event], createEvent: (String, String) => Event) extends FileAlterationListener with Loggable {

  override def onStart(observer: FileAlterationObserver): Unit = {
    log.debug("Checking events.")
  }

  override def onDirectoryCreate(directory: File): Unit = log.debug(s"dir create event: ${directory.getAbsolutePath}")

  override def onDirectoryChange(directory: File): Unit = log.debug(s"dir change event: ${directory.getAbsolutePath}")

  override def onDirectoryDelete(directory: File): Unit = log.debug(s"dir delete event: ${directory.getAbsolutePath}")

  override def onFileCreate(file: File): Unit = {
    log.debug(s"file create event: ${file.getAbsolutePath}")
    queue.put(createEvent("create", file.getAbsolutePath))
  }

  override def onFileChange(file: File): Unit = {
    log.debug(s"file update event: ${file.getAbsolutePath}")
    queue.put(createEvent("update", file.getAbsolutePath))
  }

  override def onFileDelete(file: File): Unit = {
    log.debug(s"file delete event: ${file.getAbsolutePath}")
    queue.put(createEvent("delete", file.getAbsolutePath))
  }

  override def onStop(observer: FileAlterationObserver): Unit = ()
}
