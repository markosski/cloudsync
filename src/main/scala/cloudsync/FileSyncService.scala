package cloudsync

import java.io.File

import cats.data.{Kleisli, Reader}
import cloudsync.client.CloudClient

object FileSyncService extends WithLogger {

  /**
    * @return
    */
  def uploadFile(triggerFile: TriggerFile, remoteBasePath: String) = Reader[Env, Maybe[Boolean]] {
    env: Env => {
      log.info(s"Uploading file: ${triggerFile} to remote prefix $remoteBasePath")

      for {
        file <- env.client.put(
          new File(triggerFile.localFile.path),
          FileOps.buildRemotePath(triggerFile, remoteBasePath)
        )
        metaFile <- env.client.put(
          s"""{"deviceName":"test","hash":"${triggerFile.localFile.hash}"}""",
          MetaFile.getMetaFilePath(
            FileOps.buildRemotePath(triggerFile, remoteBasePath)
          )
        )
      } yield metaFile
    }
  }

  def isFileChanged(localFile: LocalFile, remotePath: String) = Reader[Env, Maybe[Boolean]] {
    env => {
      log.info(s"Check if file has changed: $localFile, $remotePath")
      for {
        metaFilePath  <- toMaybe(MetaFile.getMetaFilePath(remotePath))
        exists        <- env.client.exists(metaFilePath)
        ret           <- {
          if (!exists) Right(true)
          else
            for {
              metaRecord <- MetaFile.getMetaFileContents(metaFilePath, env.client)
              ret <- {
                if (metaRecord.hash == localFile.hash)
                  Right(false)
                else
                  Right(true)
              }
            } yield ret
        }
      } yield ret
    }
  }

  def uploadFileIfChanged(localBasePath: String, localRelativePath: String, remoteBasePath: String) = Reader[Env, Maybe[Boolean]] {
    env => {
      log.info(s"Uploading $localBasePath, $localRelativePath -> $remoteBasePath")
      for {
        triggerFile <- FileOps.toTriggerFile(localBasePath, localRelativePath)
        remotePath <- toMaybe(FileOps.buildRemotePath(triggerFile, remoteBasePath))
        isChanged <- isFileChanged(triggerFile.localFile, remotePath).run(env)
        ret       <- {
          if (isChanged)
            uploadFile(triggerFile, remoteBasePath).run(env)
          else
            Right(false)
        }
      } yield ret
    }
  }

  def downloadFileIfChanged(remotePath: String, remoteBasePath: String, localBasePath: String) = Reader[Env, Maybe[Boolean]] {
    env => {
      log.info(s"Downloading $remotePath -> $localBasePath")

      for {
        localPath <- toMaybe(FileOps.buildLocalPath(remotePath, remoteBasePath, localBasePath))
        exists    <- toMaybe(FileOps.pathExists(localPath))
        ret       <-
          if (!exists) env.client.get(remotePath, localPath)
          else for {
            localFile <- FileOps.toLocalFile(localPath)
            isChanged <- isFileChanged(localFile, remotePath).run(env)
            ret       <-
              if (isChanged) env.client.get(remotePath, localPath)
              else Right(false)
          } yield ret
      } yield ret
    }
  }

  def deleteFile(triggerFile: TriggerFile, remoteBasePath: String) = Reader[Env, Maybe[Boolean]] {
    env => {
      for {
        file <- env.client.delete(
          FileOps.buildRemotePath(triggerFile, remoteBasePath)
        )
        metaFile <- env.client.delete(
          MetaFile.getMetaFilePath(
            FileOps.buildRemotePath(triggerFile, remoteBasePath)
          )
        )
      } yield metaFile
    }
  }
}
