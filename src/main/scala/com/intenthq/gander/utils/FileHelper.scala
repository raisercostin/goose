package com.intenthq.gander.utils

import com.google.common.base.Charsets
import com.google.common.io.Resources
import org.slf4j.LoggerFactory

import scala.util.{Failure, Success, Try}

object FileHelper {

  private val logger = LoggerFactory.getLogger(getClass)

  def loadResourceFile[A](filename: String, cls: Class[A]): String = {
    val url = cls.getResource(filename)
    Try(Resources.toString(url, Charsets.UTF_8)) match {
      case Success(v) => v
      case Failure(tr) => logger.warn(s"Error while reading $filename: $tr", tr.toString); ""
    }
  }

}