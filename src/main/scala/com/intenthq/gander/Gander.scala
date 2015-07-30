package com.intenthq.gander

import java.util.Date

import com.intenthq.gander.extractors.ContentExtractor
import com.intenthq.gander.opengraph.OpenGraphData
import org.joda.time.DateTime
import org.jsoup.Jsoup
import org.jsoup.nodes.{Document, Element}

import scala.collection.convert.Wrappers.JListWrapper
import scala.util.Try


case class Link(text: String, target: String)

case class PageInfo(title: String,
                    metaDescription: String,
                    metaKeywords: String,
                    canonicalLink: Option[String],
                    openGraphData: OpenGraphData,
                    cleanedText: Option[String] = None,
                    links: Seq[Link] = Seq.empty,
                    publishDate: Option[Date] = None)

object Gander {

  def extract(html: String, lang: String = "all"): Option[PageInfo] =
    Try(Jsoup.parse(html)).toOption.map { doc =>
      val canonicalLink = ContentExtractor.extractCanonicalLink(doc)
      val publishDate = extractDate(doc).map(_.toDate).orElse(canonicalLink.flatMap(ContentExtractor.extractDateFromURL))

      val info = PageInfo(title = ContentExtractor.extractTitle(doc),
                          metaDescription = ContentExtractor.extractMetaDescription(doc),
                          metaKeywords = ContentExtractor.extractMetaKeywords(doc),
                          canonicalLink = canonicalLink,
                          publishDate = publishDate,
                          openGraphData = OpenGraphData(doc)
      )

      val cleanedDoc = DocumentCleaner.clean(doc)
      ContentExtractor.calculateBestNodeBasedOnClustering(cleanedDoc, lang).map { node =>
        //some mutability beauty
        ContentExtractor.postExtractionCleanup(node, lang)
        info.copy(cleanedText = Some(node.text()),
                  links = ContentExtractor.extractLinks(node))
      }.getOrElse(info)
    }


  def parseDoc(url: String, rawlHtml: String): Option[Document] = Try(Jsoup.parse(rawlHtml, url)).toOption

  def extractDate(rootElement: Element): Option[DateTime] = {
    // Try to retrieve publish time from open graph data
    import org.joda.time.format.ISODateTimeFormat._
    JListWrapper(rootElement.select("meta[property=article:published_time]")).headOption.map(x =>
      dateTimeParser.parseDateTime(x.attr("content"))
    )
  }

}
