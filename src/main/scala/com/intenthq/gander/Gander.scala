package com.intenthq.gander

import java.util.Date

import com.gravity.goose.Configuration
import com.gravity.goose.opengraph.OpenGraphData
import com.gravity.goose.outputformatters.StandardOutputFormatter
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

  def extract(html: String, lang: String)(implicit config: Configuration): Option[PageInfo] =
    Try(Jsoup.parse(html)).toOption.flatMap { doc =>
      val extractor = config.contentExtractor
      val canonicalLink = extractor.extractCanonicalLink(doc)
      val publishDate = extractDate(doc).map(_.toDate).orElse(canonicalLink.flatMap(extractor.extractDateFromURL))

      val info = PageInfo(title = extractor.extractTitle(doc),
                          metaDescription = extractor.extractMetaDescription(doc),
                          metaKeywords = extractor.extractMetaKeywords(doc),
                          canonicalLink = canonicalLink,
                          publishDate = publishDate,
                          openGraphData = config.openGraphDataExtractor.extract(doc)
      )
      extractor.calculateBestNodeBasedOnClustering(doc, lang).map { node =>
        info.copy(cleanedText = Some(StandardOutputFormatter.getFormattedText(node, lang)),
                  links = extractor.extractLinks1(node))
      }
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
