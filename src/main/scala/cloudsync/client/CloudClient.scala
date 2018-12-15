package cloudsync.client

import java.io.File

import cats.Monad
import cats.implicits._

trait CloudClient[F[_]] {
  def put(file: File, path: String): F[Unit]

  def put(content: String, path: String): F[Unit]

  /**
    * Get contents of file.
    */
  def getContents(path: String): F[String]

  /**
    * Download remote file.
    */
  def get(remotePath: String, localPath: String): F[Unit]

  def delete(path: String): F[Unit]

  def exists(path: String): F[Boolean]

  def list(path: String): F[Seq[String]]
}
