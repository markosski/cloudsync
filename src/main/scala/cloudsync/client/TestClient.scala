package cloudsync.client
import java.io.File

import cats.{Applicative, Monad}
import cats.effect.{Effect, Sync}
import cloudsync.{Loggable}

class TestClient[F[_]: Effect](implicit E: Effect[F]) extends CloudClient[F] with Loggable {

  def put(file: File, path: String): F[Unit] = E.delay {
    log.info(s"put, $file -> $path")
  }

  def put(content: String, path: String): F[Unit] = E.delay {
    log.info(s"put, ${content.slice(0, 50)} -> $path")
  }

  def getContents(path: String): F[String] = E.delay {
    log.info(s"get, $path")
    "test contents"
  }

  def get(remotePath: String, localPath: String): F[Unit] = E.delay {
    log.info(s"get, $remotePath -> $localPath")
  }

  def delete(path: String): F[Unit] = E.delay {
    log.info(s"delete, $path")
  }

  def exists(path: String): F[Boolean] = E.delay {
    log.info(s"exists, $path")
    true
  }

  def list(path: String): F[Seq[String]] = E.delay {
    log.info(s"list, $path")
    List("1", "2", "3")
  }
}
