package com.intenthq.gander.extractors

import java.util.Date

import org.specs2.mutable.Specification

class ContentExtractorSpec extends Specification {
  "extractDateFromURLUnsafe" >> {
    " should extract the date from the path, if present" >> {
      ContentExtractor.extractDateFromURL("http://a.com/no/date/in/this/path") must_== None
      ContentExtractor.extractDateFromURL("http://a.com/not/every/number/1900/is/a/date") must_== None
      ContentExtractor.extractDateFromURL("http://a.com/number/2000a/plus/letters") must_== None

      ContentExtractor.extractDateFromURL("http://a.com/a/year/2000/and/nothing/else") must_== Some(new Date(100, 0, 1))
      ContentExtractor.extractDateFromURL("http://a.com/a/year/2000/and/10/not/a/month") must_== Some(new Date(100, 0, 1))
      ContentExtractor.extractDateFromURL("http://a.com/a/year/2000/13/not/a/month") must_== Some(new Date(100, 0, 1))

      ContentExtractor.extractDateFromURL("http://a.com/a/year/2000/10/and/a/month") must_== Some(new Date(100, 9, 1))
      ContentExtractor.extractDateFromURL("http://a.com/not/2000/10/a/20/day") must_== Some(new Date(100, 9, 1))
      ContentExtractor.extractDateFromURL("http://a.com/not/2000/10/32/a/day") must_== Some(new Date(100, 9, 1))

      ContentExtractor.extractDateFromURL("http://a.com/not/2000/10/31/a/day") must_== Some(new Date(100, 9, 31))
    }
  }
}
