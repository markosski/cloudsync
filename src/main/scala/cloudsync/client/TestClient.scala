package cloudsync.client
import java.io.File

import cloudsync.{Maybe, WithLogger}

class TestClient extends CloudClient with WithLogger {
  def put(file: File, path: String): Maybe[Boolean] = {
    log.info(s"put, $file -> $path")
    Right(true)
  }

  def put(content: String, path: String): Maybe[Boolean] = {
    log.info(s"put, ${content.slice(0, 50)} -> $path")
    Right(true)
  }

  def get(path: String): Maybe[String] = {
    log.info(s"get, $path")
    Right("test contents")
  }

  def get(remotePath: String, localPath: String): Maybe[Boolean] = {
    log.info(s"get, $remotePath -> $localPath")
    Right(true)
  }

  def delete(path: String): Maybe[Boolean] = {
    log.info(s"delete, $path")
    Right(true)
  }

  def exists(path: String): Maybe[Boolean] = {
    log.info(s"exists, $path")
    Right(true)
  }

  def list(path: String): Maybe[Seq[String]] = {
    log.info(s"list, $path")
    Right(List("1", "2", "3"))
  }
}
