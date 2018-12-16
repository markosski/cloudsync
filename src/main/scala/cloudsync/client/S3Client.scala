package cloudsync.client
import cloudsync._
import java.io._

import cats.Monad
import cats.implicits._
import cats.effect._
import com.amazonaws.ClientConfiguration
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain
import com.amazonaws.services.s3.{AmazonS3, AmazonS3ClientBuilder}
import com.amazonaws.services.s3.model.{DeleteObjectsRequest, GetObjectRequest, ListObjectsV2Request}

import collection.JavaConverters._

class S3Client[F[_]](region: String, bucket: String)(implicit E: Effect[F]) extends CloudClient[F] with Loggable {
  private val client = createClient()

  private def createClient(): AmazonS3 = {
    val clientConf = new ClientConfiguration()

    AmazonS3ClientBuilder.standard()
      .withRegion(region)
      .withClientConfiguration(clientConf)
      .withCredentials(new DefaultAWSCredentialsProviderChain)
      .build()
  }

  private def cleanPath(path: String): String = path.stripPrefix("/")

  def put(file: File, path: String) = for {
      _   <- E.delay(log.debug(s"S3, uploading file: $file -> $path"))
      res <- E.delay(client.putObject(bucket, cleanPath(path), file))
    } yield res

  def put(content: String, path: String) = for {
      _   <- E.delay(log.debug(s"S3, creating file with content: $path"))
      res <- E.delay(client.putObject(bucket, cleanPath(path), content))
    } yield res

  def getContents(path: String): F[String] = for {
      _   <- E.delay(log.debug(s"S3, getting contenst of file $path"))
      res <- E.delay {
        val is = new InputStreamReader(client.getObject(bucket, cleanPath(path)).getObjectContent)
        val reader = new BufferedReader(is)
        reader.lines().toArray().mkString(System.getProperty("line.separator"))
      }
    } yield res

  def get(remotePath: String, localPath: String) = for {
      _   <- E.delay(log.debug(s"S3, download file $remotePath -> $localPath"))
      res <- E.delay {
        val basePath = FileOps.getBasePath(localPath)
        if (!FileOps.pathExists(basePath))
          FileOps.createPath(basePath)

        log.debug(s"Downloading remote: $remotePath to local: $localPath")

        val file = new File(localPath)
        client.getObject(new GetObjectRequest(bucket, cleanPath(remotePath)), file)
      }
    } yield res

  def delete(path: String) = for {
      _          <- E.delay(log.debug(s"S3, deleting file: $path"))
      deleteList <- list(cleanPath(path))
      res <- E.delay(deleteList match {
        case h :: t => {
          val multiObjectDeleteRequest = new DeleteObjectsRequest(bucket)
            .withKeys(deleteList: _*)
            .withQuiet(false)
          client.deleteObjects(multiObjectDeleteRequest)
          true
        }
        case Nil => true
      })
    } yield res

  def exists(path: String) = for {
      _   <- E.delay(log.debug(s"S3, checking if file exist: $path"))
      res <- for {
        clean <- list(cleanPath(path))
        exists <- E.pure(clean match {
          case h :: Nil => true
          case Nil => false
          case h :: t => throw new Exception("More than one matching record found!")
        })
      } yield exists
    } yield res

  def list(path: String): F[Seq[String]] = for {
      _   <- E.delay (log.debug(s"S3, listing path: $path"))
      res <- E.delay {
        val request = new ListObjectsV2Request()
          .withBucketName(bucket)
          .withPrefix(cleanPath(path))
        val result = client.listObjectsV2(request).getObjectSummaries.asScala
        (for (obj <- result) yield obj.getKey).toList
      }
    } yield res
}
