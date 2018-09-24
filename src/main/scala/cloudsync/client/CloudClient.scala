package cloudsync.client

import java.io.File

import cloudsync.Maybe

trait CloudClient {
  def put(file: File, path: String): Maybe[Boolean]

  def put(content: String, path: String): Maybe[Boolean]

  def get(path: String): Maybe[String]

  def get(remotePath: String, localPath: String): Maybe[Boolean]

  def delete(path: String): Maybe[Boolean]

  def exists(path: String): Maybe[Boolean]

  def list(path: String): Maybe[Seq[String]]
}
