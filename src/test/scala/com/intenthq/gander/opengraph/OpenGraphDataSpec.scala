package com.intenthq.gander.opengraph

import java.net.URL

import org.joda.time.DateTime
import org.jsoup.Jsoup
import org.specs2.mutable.Specification

class OpenGraphDataSpec extends Specification {

  def html(property: String, value: String) =
      s"""<html><head>
         |<meta property="$property" content="$value" />
         |</head><body></body></html>""".stripMargin

  "apply" >> {
    "should extract no tags correctly" >> {
      val elem = Jsoup.parse("<html><head></head><body></body></html>")
      OpenGraphData(elem) must_== OpenGraphData()
    }
    "should extract an empty tag correctly" >> {
      val elem = Jsoup.parse(html("og:description", ""))
      OpenGraphData(elem).description must beSome("")
    }
    "should extract the og:title correctly" >> {
      val elem = Jsoup.parse(html("og:title", "the title"))
      OpenGraphData(elem).title must beSome("the title")
    }
    "should extract the og:site_name correctly" >> {
      val elem = Jsoup.parse(html("og:site_name", "the site name"))
      OpenGraphData(elem).siteName must beSome("the site name")
    }
    "should extract the og:url correctly" >> {
      val elem = Jsoup.parse(html("og:url", "http://example.com"))
      OpenGraphData(elem).url must beSome(new URL("http://example.com"))
    }
    "should return none if the og:url is not a valid url" >> {
      val elem = Jsoup.parse(html("og:url", "not a valid url"))
      OpenGraphData(elem).url must beNone
    }
    "should extract the og:description correctly" >> {
      val elem = Jsoup.parse(html("og:description", "the desc"))
      OpenGraphData(elem).description must beSome("the desc")
    }
    "should extract the og:image correctly" >> {
      val elem = Jsoup.parse(html("og:image", "http://example.com/image.png"))
      OpenGraphData(elem).image must beSome(new URL("http://example.com/image.png"))
    }
    "should return none if the og:image is not a valid url" >> {
      val elem = Jsoup.parse(html("og:image", "not a valid url"))
      OpenGraphData(elem).image must beNone
    }
    "should extract the og:type correctly" >> {
      val elem = Jsoup.parse(html("og:type", "the type"))
      OpenGraphData(elem).`type` must beSome("the type")
    }
    "should extract the og:locale correctly" >> {
      val elem = Jsoup.parse(html("og:locale", "the locale"))
      OpenGraphData(elem).locale must beSome("the locale")
    }
    "should extract the article:published_time correctly" >> {
      val elem = Jsoup.parse(html("article:published_time", "2015-07-31"))
      OpenGraphData(elem).publishedTime must beSome(new DateTime(2015, 7, 31, 0, 0))
    }
    "should return none the article:published_time is not a valid date" >> {
      val elem = Jsoup.parse(html("article:published_time", "not a valid date"))
      OpenGraphData(elem).publishedTime must beNone
    }
  }
}
