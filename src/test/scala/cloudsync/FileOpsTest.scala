package cloudsync

import org.scalatest._
import FileOps._

class FileOpsTest extends FlatSpec with Matchers {
  val testPath = "/tmp/dir1/dir2/file.txt"

  "getBasePath" should "return base path" in {
    assert(getBasePath(testPath) == "/tmp/dir1/dir2")
  }

  "pathHasPrefix" should "test for prefix" in {
    assert(pathHasPrefix(testPath, "/tmp/dir1/dir2/"))
    assert(pathHasPrefix(testPath, "/tmp/dir1/dir2"))
    assert(pathHasPrefix(testPath, "/tmp/dir1"))
    assert(pathHasPrefix(testPath, "/tmp"))
  }

  "absoluteToRelative" should "convert absolute path to relative path" in {
    assert(absoluteToRelative(testPath, "/tmp/") == "dir1/dir2/file.txt")
    assert(absoluteToRelative(testPath, "/tmp") == "dir1/dir2/file.txt")
  }

  "splitPathToParts" should "split files into parts" in {
    assert(splitPathToParts(testPath) == List("", "tmp", "dir1", "dir2", "file.txt"))
  }

  "buildRemotePath" should "build remote path" in {
    assert(buildRemotePath(testPath, "/tmp/dir1", "/tmp/remoteDir1") == "/tmp/remoteDir1/dir2/file.txt")
    assert(buildRemotePath(testPath, "/tmp/dir1/", "/tmp/remoteDir1/") == "/tmp/remoteDir1/dir2/file.txt")
  }

  "buildLocalPath" should "build local path" in {
    assert(buildLocalPath("/tmp/remoteDir1/dir2/file.txt", "/tmp/remoteDir1", "/tmp/dir1") == testPath)
    assert(buildLocalPath("/tmp/remoteDir1/dir2/file.txt", "/tmp/remoteDir1/", "/tmp/dir1/") == testPath)
  }
}