package cloudsync

import java.io.File
import java.nio.file.{FileVisitOption, Files, Path, Paths}
import java.security.MessageDigest
import java.util.Base64

import cats.effect._
import cats.implicits._
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

  def pathHasPrefix(path: String, prefix: String): Boolean = path.startsWith(prefix)

  def createPath[F[_]](path: String)(implicit F: Effect[F]): F[Unit] = for {
    _ <- logDebug(s"Create path: $path")
    _ <- F.delay(new File(path).mkdirs())
  } yield ()

  def absoluteToRelative(absolutePath: String, basePath: String): String = {
    log.debug(s"Converting absolute to relative path: $absolutePath, $basePath")

    if (absolutePath.startsWith(basePath))
      absolutePath.stripPrefix(basePath).stripPrefix("/")
    else
      s"basePath: $basePath is not a suffix of $absolutePath"
  }

  def pathExists(path: String): Boolean = {
    log.debug(s"Check if file exists: $path")
    new File(path).exists()
  }

  def listFiles(path: String): Option[List[String]] = {
    log.debug(s"List files for path: $path")
    val file = new File(path)
    Try {
      file.listFiles().map(_.getAbsolutePath).toList
    }.toOption
  }

  def listAllFiles(path: String): List[String] = {
    log.debug(s"List files for path: $path")
    FileUtils.listFiles(new File(path), TrueFileFilter.INSTANCE, FalseFileFilter.INSTANCE).iterator().asScala.toList
      .map(_.getAbsolutePath)
  }

  def splitPathToParts(path: String): List[String] = {
    path.split(pathSeparator).toList
  }

  def joinPathParts(parts: Seq[String]): String = {
    parts.mkString(pathSeparator)
  }

  def buildRemotePath(localPath: String, localBasePath: String, remoteBasePath: String): String = {
    log.debug(s"Building remote path: $localPath, $localBasePath, $remoteBasePath")
    remoteBasePath / localPath.replaceFirst(localBasePath, "")
  }

  def buildLocalPath(remotePath: String, remoteBasePath: String, localBasePath: String): String = {
    log.debug(s"Building local path: $remotePath, $remoteBasePath, $localBasePath")
    localBasePath / remotePath.replaceFirst(remoteBasePath, "")
  }

  def toTriggerFile[F[_]: Effect](localBasePath: String, relativePath: String): F[TriggerFile] = {
    for {
      localFile <- toLocalFile(localBasePath / relativePath)
    } yield TriggerFile(localFile, localBasePath)
  }

  def toLocalFile[F[_]](path: String)(implicit E: Effect[F]): F[LocalFile] = {
    for {
      _     <- logInfo(s"Converting $path to LocalFile")
      hash  <- computeMD5(new File(path))
    } yield LocalFile(path, hash)
  }

  /**
    * https://stackoverflow.com/questions/304268/getting-a-files-md5-checksum-in-java
    */
  def computeMD5[F[_]](file: File)(implicit E: Effect[F]): F[String] = for {

    _     <- logDebug(s"Calculating md5 hash for file: ${file.getAbsolutePath}")
    hash <- E.delay {
      val b = Files.readAllBytes(Paths.get(file.getAbsolutePath))
      val bytes: Array[Byte] = MessageDigest.getInstance("MD5").digest(b)
      Base64.getEncoder().encodeToString(bytes)
    }
  } yield hash
}
