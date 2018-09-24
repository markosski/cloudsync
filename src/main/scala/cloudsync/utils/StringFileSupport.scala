package cloudsync.utils

import scala.language.implicitConversions
import java.io.File

class StringFileSupport(s: String) {
  val sep: String = java.io.File.separatorChar.toString

  def /(z: String): String = {
    z match {
      case z if s == "~" => System.getProperty("user.home") + sep + z
      case _ => join(s, z)
    }
  }

  def join(a: String, b: String): String = a.stripSuffix(sep) + sep + b.stripPrefix(sep)

  def /(z: File): File = {
    new File(join(s, z.toString))
  }
  def toFile = new File(s)
}
