package com.intenthq.gander

import com.intenthq.gander.extractors.ContentExtractor._
import org.jsoup.Jsoup
import org.specs2.mutable.Specification

class ContentExtractorSpec extends Specification {
  def docFromTitle(title: String) = Jsoup.parse(s"<html><head><title>$title</title></head><body></body></html>")

  "extractTitle" >> {
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
  }
}
