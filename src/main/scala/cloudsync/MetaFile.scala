package cloudsync

import cloudsync.client.CloudClient
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule

case class MetaRecord(deviceName: String, hash: String)
case class MetaFile(path: String, meta: MetaRecord)

object MetaFile extends WithLogger {
  import cloudsync.utils.Implicits._

  def getMetaFilePath(path: String): String = {
    val parts = FileOps.splitPathToParts(path)
    FileOps.joinPathParts(parts.dropRight(1)) / "_syncmeta" / parts.last
  }

  def getMetaFileContents(path: String, client: CloudClient): Maybe[MetaRecord] = {
    log.info(s"Getting contents of meta file: $path")
    val mapper = new ObjectMapper()
    mapper.registerModule(DefaultScalaModule)

    for {
      content <- client.get(path)
      res     <- toMaybe(mapper.readValue(content, classOf[MetaRecord]))
    } yield res
  }
}
