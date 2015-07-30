package com.intenthq.gander.utils

import com.google.common.base.Charsets
import com.google.common.io.Resources
import scala.util.{Failure, Success, Try}

object FileHelper extends Logging {
  def loadResourceFile[A](filename: String, cls: Class[A]): String = {
    val url = cls.getResource(filename)
    Try(Resources.toString(url, Charsets.UTF_8)) match {
      case Success(v) => v
      case Failure(tr) => warn(s"Error while reading $filename: $tr", tr.toString); ""
    }
  }
}