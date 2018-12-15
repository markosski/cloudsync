package cloudsync

import com.typesafe.config.ConfigFactory

case class Config(
                 cloudService: String,
                 serviceOpts: Map[String, String]
                 )

object Config {
  val conf = ConfigFactory.load()
  def create: Config = {

    Config(
      conf.getString("cloudService"),
      Map(
        "bucketName"  -> conf.getString("s3.bucketName"),
        "region"      -> conf.getString("s3.region")
      )
    )
  }
}
