package cloudsync

import com.typesafe.scalalogging.Logger

trait Loggable {
  val log = Logger(getClass)
}

