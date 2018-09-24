package cloudsync.utils

object Implicits {
  implicit def stringFileSupport(s: String) = new StringFileSupport(s)
}
