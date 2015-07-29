/**
 * Licensed to Gravity.com under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  Gravity.com licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.gravity.goose.utils

import com.google.common.base.Charsets
import com.google.common.io.Resources

import scala.util.{Failure, Success, Try}


/**
 * Created by Jim Plush
 * User: jim
 * Date: 8/16/11
 */

object FileHelper extends Logging {

  def loadResourceFile[A](filename: String, cls: Class[A]): String = {
    val url = cls.getResource(filename)
    Try(Resources.toString(url, Charsets.UTF_8)) match {
      case Success(v) => v
      case Failure(tr) => warn(s"Error while reading $filename: $tr", tr.toString); ""
    }
  }
}