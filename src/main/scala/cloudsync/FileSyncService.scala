package cloudsync

import java.io.File
import cats.effect._
import cats.implicits._

object FileSyncService extends Loggable {
  def uploadFile[F[_]](triggerFile: TriggerFile, remoteBasePath: String)(implicit env: Env[F], E: Effect[F]) =
    for {
      _    <- logInfo(s"Uploading file: ${triggerFile} to remote prefix $remoteBasePath")
      file <- env.client.put(
        new File(triggerFile.localFile.path),
        FileOps.buildRemotePath(triggerFile.localFile.path, triggerFile.localBasePath, remoteBasePath)
      )
      metaFile <- env.client.put(
        s"""{"deviceName":"test","hash":"${triggerFile.localFile.hash}"}""",
        MetaFileOps.getMetaFilePath(
          FileOps.buildRemotePath(triggerFile.localFile.path, triggerFile.localBasePath, remoteBasePath)
        )
      )
    } yield metaFile

  def isFileChanged[F[_]](localFile: LocalFile, remotePath: String)(implicit env: Env[F], E: Effect[F]) =
    for {
      _             <- logInfo(s"Check if file has changed: $localFile, $remotePath")
      metaFilePath  <- MetaFileOps.getMetaFilePath(remotePath).pure[F]
      exists        <- env.client.exists(metaFilePath)
      ret           <- {
        if (!exists) E.pure(true)
        else
          for {
            metaRecord <- MetaFileOps.getMetaFileContents(metaFilePath, env.client)
            ret <- E.pure{
              if (metaRecord.hash == localFile.hash) false
              else true
            }
          } yield ret
      }
    } yield ret

  def uploadFileIfChanged[F[_]](localBasePath: String, localRelativePath: String, remoteBasePath: String)(implicit env: Env[F], E: Effect[F]) =
    for {
      _             <- logInfo(s"Uploading file if changed $localBasePath, $localRelativePath -> $remoteBasePath")
      triggerFile   <- FileOps.toTriggerFile(localBasePath, localRelativePath)
      remotePath = FileOps.buildRemotePath(localRelativePath, localBasePath, remoteBasePath)
      isChanged     <- isFileChanged(triggerFile.localFile, remotePath)
      ret           <- {
        if (isChanged)
          uploadFile(triggerFile, remoteBasePath) *> E.pure(true)
        else
          E.pure(false)
      }
    } yield ret

  def downloadFileIfChanged[F[_]](remotePath: String, remoteBasePath: String, localBasePath: String)(implicit env: Env[F], E: Effect[F]) =
    for {
      _         <- logInfo(s"Downloading $remotePath -> $localBasePath")
      localPath = FileOps.buildLocalPath(remotePath, remoteBasePath, localBasePath)
      exists    = FileOps.pathExists(localPath)
      ret       <-
        if (!exists)
          env.client.get(remotePath, localPath)
        else for {
          localFile <- FileOps.toLocalFile(localPath)
          isChanged <- isFileChanged(localFile, remotePath)
          ret       <-
            if (isChanged) {
              env.client.get(remotePath, localPath);
            }
            else E.pure(())
        } yield ret
    } yield ret

  def deleteFile[F[_]: Effect](localBasePath: String, localRelativePath: String, remoteBasePath: String)(implicit env: Env[F]) =
    for {
      metaFile <- env.client.delete(
        MetaFileOps.getMetaFilePath(
          FileOps.buildRemotePath(localRelativePath, localBasePath, remoteBasePath)
        )
      )
      file <- env.client.delete(
        FileOps.buildRemotePath(localRelativePath, localBasePath, remoteBasePath)
      )
    } yield file
}
