package cloudsync

import client.{CloudClient, TestClient}

trait Env {
  val client: CloudClient
}

object TestEnv extends Env {
  lazy val client = new TestClient
}
