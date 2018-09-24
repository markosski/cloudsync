import cats.data.{Kleisli, Reader}

import scala.util.{Failure, Success, Try}
import cloudsync.Env

package object cloudsync {
  type Maybe[A] = Either[String, A]
  type ReaderEnv[A] = Reader[Env, A]

  def toMaybe[A](expr: => A): Maybe[A] = {
    Try(expr) match {
      case Success(x) => Right(x)
      case Failure(err) => Left(err.getMessage)
    }
  }

  def toMaybeWithMessage[A](expr: => A)(msg: String): Maybe[A] = {
    Try(expr) match {
      case Success(x) => Right(x)
      case Failure(err) => Left(s"$msg: ${err.getMessage}")
    }
  }

  def toMaybe[A, B](expr: => A, right: B): Maybe[B] = {
    Try(expr) match {
      case Success(x) => Right(right)
      case Failure(err) => Left(err.getMessage)
    }
  }
}
