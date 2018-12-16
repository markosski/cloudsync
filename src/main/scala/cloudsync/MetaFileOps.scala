package cloudsync

import cats.Monad
import cloudsync.client.CloudClient
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import cats.implicits._
import cats.effect._

case class MetaRecord(deviceName: String, hash: String)
case class MetaFileOps(path: String, meta: MetaRecord)

object MetaFileOps extends Loggable {
  import cloudsync.utils.Implicits._

  def getMetaFilePath(path: String): String = {
    val parts = FileOps.splitPathToParts(path)
    FileOps.joinPathParts(parts.dropRight(1)) / "_syncmeta" / parts.last
  }

  def getMetaFileContents[F[_]](path: String, client: CloudClient[F])(implicit E: Effect[F]): F[MetaRecord] = {
    val mapper = new ObjectMapper()
    mapper.registerModule(DefaultScalaModule)

    for {
      _       <- logInfo(s"Getting contents of meta file: $path")
      content <- client.getContents(path)
      res     <- E.pure(mapper.readValue(content, classOf[MetaRecord]))
    } yield res
  }
}
