package com.intenthq.gander

import com.intenthq.gander.extractors.ContentExtractor._
import org.jsoup.Jsoup
import org.specs2.mutable.Specification

class ContentExtractorSpec extends Specification {

  "extractTitle" >> {
    def docFromTitle(title: String) = Jsoup.parse(s"<html><head><title>$title</title></head><body></body></html>")
    "should extract a title" >> {
      val title = "the title"

      extractTitle(docFromTitle(title)) must_== title
    }
    "should extract an empty title" >> {
      val title = ""

      extractTitle(docFromTitle(title)) must_== title
    }
    "should keep the first segment if the title contains a separator" >> {
      "| (2 segments)" >> {
        val title = "The first segment | Wikipedia, the free encyclopaedia"

        extractTitle(docFromTitle(title)) must_== "The first segment"
      }
      "| (3 segments)" >> {
        val title = "The first segment | other 1 | other 2"

        extractTitle(docFromTitle(title)) must_== "The first segment"
      }
      "- (2 segments)" >> {
        val title = "The first segment - Wikipedia, the free encyclopaedia"

        extractTitle(docFromTitle(title)) must_== "The first segment"
      }
      "- (3 segments)" >> {
        val title = "The first segment - other 1 - other 2"

        extractTitle(docFromTitle(title)) must_== "The first segment"
      }
      "- not used as a sparator" >> {
        val title = "this-is-a-title"

        extractTitle(docFromTitle(title)) must_== title
      }
      "» (2 segments)" >> {
        val title = "The first segment » Wikipedia, the free encyclopaedia"

        extractTitle(docFromTitle(title)) must_== "The first segment"
      }
      "» (3 segments)" >> {
        val title = "The first segment » other 1 » other 2"

        extractTitle(docFromTitle(title)) must_== "The first segment"
      }
      "» not used as a separator" >> {
        val title = "«this is a title»"

        extractTitle(docFromTitle(title)) must_== title
      }
      "· (2 segments)" >> {
        val title = "The first segment · Wikipedia, the free encyclopaedia"

        extractTitle(docFromTitle(title)) must_== "The first segment"
      }
      "· (3 segments)" >> {
        val title = "The first segment · other 1 · other 2"

        extractTitle(docFromTitle(title)) must_== "The first segment"
      }
    }

    "extractLang" >> {
      "should extract lang from html tag and give priority to it" >> {
        val html =
          """<html lang="ca">
            |  <head>
            |    <meta http-equiv="Content-Language" content="en">
            |    <meta property="og:locale" content="en_GB" />
            |  </head>
            |<body></body></html>""".stripMargin

        extractLang(Jsoup.parse(html)) must beSome("ca")
      }
      "should extract language from meta tag with more priority than og:locale" >> {
        val html =
          """<html>
            |  <head>
            |    <meta http-equiv="Content-Language" content="ca">
            |    <meta property="og:locale" content="en_GB" />
            |  </head>
            |<body></body></html>""".stripMargin

        extractLang(Jsoup.parse(html)) must beSome("ca")
      }
      "should extract language from og:locale" >> {
        val html =
          """<html>
            |  <head>
            |    <meta property="og:locale" content="ca" />
            |  </head>
            |<body></body></html>""".stripMargin

        extractLang(Jsoup.parse(html)) must beSome("ca")
      }
    }
  }
}
