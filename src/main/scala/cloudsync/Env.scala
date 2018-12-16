package cloudsync

import cats.effect.{Effect, IO}
import client.{CloudClient, TestClient}


trait Env[F[_]] {
  val client: CloudClient[F]
  val config: Config
}

object TestEnv extends Env[IO] {
  val client = new TestClient[IO]
  val config = Config.create
}
