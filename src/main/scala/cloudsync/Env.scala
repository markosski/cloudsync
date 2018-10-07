package cloudsync

import client.{CloudClient, TestClient}


trait Env {
  val client: CloudClient
  val config: Config
}

object TestEnv extends Env {
  lazy val client = new TestClient
  lazy val config = Config.create
}
