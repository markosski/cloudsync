package cloudsync.client
import cloudsync._
import java.io._

import com.amazonaws.ClientConfiguration
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain
import com.amazonaws.services.s3.{AmazonS3, AmazonS3ClientBuilder}
import com.amazonaws.services.s3.model.{DeleteObjectsRequest, GetObjectRequest, ListObjectsV2Request}

import collection.JavaConverters._

class S3Client(region: String, bucket: String) extends CloudClient with Loggable {
  private val client = createClient()

  private def createClient(): AmazonS3 = {
    val clientConf = new ClientConfiguration()

    AmazonS3ClientBuilder.standard()
      .withRegion(region)
      .withClientConfiguration(clientConf)
      .withCredentials(new DefaultAWSCredentialsProviderChain)
      .build()
  }

  private def cleanPath(path: String) = path.stripPrefix("/")

  def put(file: File, path: String): Maybe[Boolean] = {
    log.debug(s"S3, uploading file: $file -> $path")
    toMaybe(client.putObject(bucket, cleanPath(path), file), true)
  }

  def put(content: String, path: String): Maybe[Boolean] = {
    log.debug(s"S3, creating file with content: $path")
    toMaybe(client.putObject(bucket, cleanPath(path), content), true)
  }

  /**
    * Only suitable for small files.
    */
  def get(path: String) = {
    log.debug(s"S3, getting contenst of file $path")
    toMaybe {
      val is = new InputStreamReader(client.getObject(bucket, cleanPath(path)).getObjectContent)
      val reader = new BufferedReader(is)
      reader.lines().toArray().mkString(System.getProperty("line.separator"))
    }
  }

  def get(remotePath: String, localPath: String): Maybe[Boolean] = {
    log.debug(s"S3, download file $remotePath -> $localPath")
    toMaybe {
      val basePath = FileOps.getBasePath(localPath)
      if (!FileOps.pathExists(basePath))
        FileOps.createPath(basePath)

      log.debug(s"Downloading remote: $remotePath to local: $localPath")

      val file = new File(localPath)
      client.getObject(new GetObjectRequest(bucket, cleanPath(remotePath)), file)
      true
    }
  }

  def delete(path: String): Maybe[Boolean] = {
    log.debug(s"S3, deleting file: $path")
    for {
      deleteList <- list(cleanPath(path))
      res <- deleteList match {
        case h :: t => {
          val multiObjectDeleteRequest = new DeleteObjectsRequest(bucket)
            .withKeys(deleteList: _*)
            .withQuiet(false)
          toMaybe(client.deleteObjects(multiObjectDeleteRequest), true)
        }
        case Nil => Right(true)
      }
    } yield res
  }

  def exists(path: String): Maybe[Boolean] = {
    log.debug(s"S3, checking if file exist: $path")
    for {
      x <- list(cleanPath(path))
      y <- x match {
          case h :: Nil => Right(true)
          case Nil => Right(false)
          case h :: t => Left("More than one matching record found!")
      }
    } yield y
  }

  def list(path: String): Maybe[List[String]] = {
    log.debug(s"S3, listing path: $path")
    val request = new ListObjectsV2Request()
      .withBucketName(bucket)
      .withPrefix(cleanPath(path))

    toMaybe {
      val result = client.listObjectsV2(request).getObjectSummaries.asScala
      (for (obj <- result) yield obj.getKey).toList
    }
  }
}
