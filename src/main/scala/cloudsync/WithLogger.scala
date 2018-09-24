package cloudsync

import com.typesafe.scalalogging.Logger

trait WithLogger {
  val log = Logger(getClass)
}

