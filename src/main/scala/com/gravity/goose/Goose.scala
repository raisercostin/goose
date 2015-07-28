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

package com.gravity.goose

/**
 * Created by Jim Plush - Gravity.com
 * Date: 8/14/11
 */
object Goose {

  /**
  * Main method to extract an article object from a URL, pass in a url and get
  * back an Article.
  * 
 * @param url the URL of the page.
 * @param rawHTML the raw HTML page source -- optional. If not specified, and
 *                fetching is configured in {@code config}, the page will be
 *                downloaded.
 * @param lang the surmised language of the page -- optional. Used as a fallback
 *             when the page does not report its language.
  */
  def extractContent(url: String, rawHTML: String, lang: String = "all")(implicit config: Configuration): Article = {
    Crawler.crawl(new CrawlCandidate(config, url, rawHTML, lang))
  }

}