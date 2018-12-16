package cloudsync

import com.typesafe.scalalogging.Logger
import cats.effect._

trait Loggable {
  val log = Logger(getClass)

  def logInfo[F[_]: Effect](msg: String) = implicitly[Effect[F]].delay(log.info(msg))
  def logDebug[F[_]: Effect](msg: String) = implicitly[Effect[F]].delay(log.debug(msg))
  def logWarn[F[_]: Effect](msg: String) = implicitly[Effect[F]].delay(log.warn(msg))
  def logError[F[_]: Effect](msg: String) = implicitly[Effect[F]].delay(log.error(msg))
}

