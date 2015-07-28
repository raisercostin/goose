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

import com.gravity.goose.cleaners.{DocumentCleaner, StandardDocumentCleaner}
import com.gravity.goose.extractors.{PublishDateExtractor, ContentExtractor}
import com.gravity.goose.outputformatters.{OutputFormatter, StandardOutputFormatter}
import com.gravity.goose.utils.{Logging, URLHelper}
import org.jsoup.Jsoup
import org.jsoup.nodes.{Document, Element}

import scala.util.Try

/**
 * Created by Jim Plush
 * User: jim
 * Date: 8/18/11
 */

/**
 * Represents the information we may know of a page we crawl.
 *
 * @param config the configuration.
 * @param url the URL of the page.
 * @param rawHTML the raw HTML page source -- optional. If not specified, and
 *                fetching is configured in {@code config}, the page will be
 *                downloaded.
 * @param lang the surmised language of the page -- optional. Used as a fallback
 *             when the page does not report its language.
 */
case class CrawlCandidate(config: Configuration, url: String,
  rawHTML: String = null, lang: String = null)

object Crawler extends Logging {
  val logPrefix = "crawler: "

  def crawl(crawlCandidate: CrawlCandidate)(implicit config: Configuration): Article = {
    val outputFormatter: OutputFormatter = StandardOutputFormatter
    val docCleaner: DocumentCleaner = new StandardDocumentCleaner
    val article = new Article()
    for {
      parseCandidate <- URLHelper.getCleanedUrl(crawlCandidate.url)
      rawHtml = crawlCandidate.rawHTML
      doc <- parseDoc(parseCandidate.url.toString, rawHtml)
      lang = crawlCandidate.lang
    } {
      trace("Crawling url: " + parseCandidate.url)

      val extractor = config.contentExtractor

      article.finalUrl = parseCandidate.url.toString
      article.domain = parseCandidate.url.getHost
      article.linkhash = parseCandidate.linkhash
      article.rawHtml = rawHtml
      article.doc = doc
      article.rawDoc = doc.clone

      article.title = extractor.getTitle(article)
      article.publishDate = Option(PublishDateExtractor.extract(doc)).map(_.toDate).getOrElse(null)
      article.additionalData = config.additionalDataExtractor.extract(doc)
      article.metaDescription = extractor.getMetaDescription(article)
      article.metaKeywords = extractor.getMetaKeywords(article)
      article.canonicalLink = extractor.getCanonicalLink(article)
      article.tags = extractor.extractTags(article)
      article.openGraphData = config.openGraphDataExtractor.extract(doc)
      // before we do any calcs on the body itself let's clean up the document
      article.doc = docCleaner.clean(article)

      if (article.publishDate == null) {
        article.publishDate = extractor.getDateFromURL(article.canonicalLink)
      }

      //      extractor.calculateBestNodeBasedOnClustering(article, config.language) match {
      extractor.calculateBestNodeBasedOnClustering(article, lang) match {
        case Some(node: Element) => {
          article.movies = extractor.extractVideos(node)
          article.links = extractor.extractLinks(node)
          article.topNode = extractor.postExtractionCleanup(node, lang)
          article.cleanedArticleText = outputFormatter.getFormattedText(node, lang)
          article.htmlArticle = outputFormatter.cleanupHtml(node, lang)

        }
        case _ => trace("NO ARTICLE FOUND")
      }
    }
    article
  }


  def parseDoc(url: String, rawlHtml: String): Option[Document] = Try(Jsoup.parse(rawlHtml, url)).toOption

}
