package cloudsync

import java.io.File
import java.nio.file.{FileVisitOption, Files, Path, Paths}
import java.security.MessageDigest
import java.util.Base64

import org.apache.commons.io.FileUtils
import org.apache.commons.io.filefilter.{FalseFileFilter, TrueFileFilter}

import scala.collection.JavaConverters._
import scala.util.Try

case class LocalFile(path: String, hash: String)
case class RemoteFile(path: String, hash: String)
case class TriggerFile(localFile: LocalFile, localBasePath: String)

object FileOps extends Loggable {
  import cloudsync.utils.Implicits._

  val pathSeparator = ("" / "").sep

  def getBasePath(path: String): String = new File(path).getParent

  def createPath(path: String): Boolean = {
    log.info(s"Create path: $path")
    new File(path).mkdirs()
  }

  def absoluteToRelative(absolutePath: String, basePath: String): Maybe[String] = {
    log.info(s"Converting absolute to relative path: $absolutePath, $basePath")
    if (absolutePath.startsWith(basePath)) Right(absolutePath.stripPrefix(basePath).stripPrefix("/"))
    else Left(s"basePath: $basePath is not a suffix of $absolutePath")
  }

  def pathExists(path: String): Boolean = {
    log.info(s"Check if file exists: $path")
    new File(path).exists()
  }

  def listFiles(path: String): Option[Seq[String]] = {
    log.info(s"List files for path: $path")
    val file = new File(path)
    Try {
      file.listFiles().map(_.getAbsolutePath).toList
    }.toOption
  }

  def listAllFiles(path: String): Seq[String] = {
    log.info(s"List files for path: $path")
    FileUtils.listFiles(new File(path), TrueFileFilter.INSTANCE, FalseFileFilter.INSTANCE).iterator().asScala.toSeq
      .map(_.getAbsolutePath)
  }

  def splitPathToParts(path: String): Seq[String] = {
    path.split(pathSeparator)
  }

  def joinPathParts(parts: Seq[String]): String = {
    parts.mkString(pathSeparator)
  }

  def buildRemotePath(triggerFile: TriggerFile, remoteBasePath: String): String = {
    log.info(s"Building remote path: $triggerFile, $remoteBasePath")
    remoteBasePath / triggerFile.localFile.path.replaceFirst(
      triggerFile.localBasePath, ""
    )
  }

  def buildRemotePath(localPath: String, localBasePath: String, remoteBasePath: String): String = {
    log.info(s"Building remote path: $localBasePath, $localPath, $remoteBasePath")
    remoteBasePath / localPath.replaceFirst(localBasePath, "")
  }

  def buildLocalPath(remotePath: String, remoteBasePath: String, localBasePath: String): String = {
    log.info(s"Building local path: $remotePath, $remoteBasePath, $localBasePath")
    localBasePath / remotePath.replaceFirst(remoteBasePath, "")
  }

  def toTriggerFile(localBasePath: String, relativePath: String): Maybe[TriggerFile] = {
    for {
      localFile <- toLocalFile(localBasePath / relativePath)
    } yield TriggerFile(localFile, localBasePath)
  }

  def toLocalFile(path: String): Maybe[LocalFile] = {
    log.info(s"Converting $path to LocalFile")

    val file = new File(path)
    computeMD5(file) match {
      case Some(hash) =>
        Right(LocalFile(path, hash))
      case None =>
        Left(s"File ${file.getAbsolutePath} does not exist.")
    }
  }

  /**
    * https://stackoverflow.com/questions/304268/getting-a-files-md5-checksum-in-java
    */
  def computeMD5(file: File): Option[String] = {
    log.info(s"Calculating md5 hash for file: ${file.getAbsolutePath}")
    Try {
      val b = Files.readAllBytes(Paths.get(file.getAbsolutePath))
      val bytes: Array[Byte] = MessageDigest.getInstance("MD5").digest(b)
      Base64.getEncoder().encodeToString(bytes)
    }.toOption
  }
}
