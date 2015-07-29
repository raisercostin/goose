package com.intenthq.gander.text

import org.specs2.mutable.Specification

class StopWordsTest extends Specification {
  "StopWords" >> {
    " should find how many stopwords are there" >> {
      StopWords.getStopWordCount("blah blah blah").stopWordCount must_== 0
      StopWords.getStopWordCount("although blah de blah").stopWordCount must_== 1
    }

    " should determine which words are stopwords" >> {
      StopWords.getStopWordCount("although blah de blah").stopWords must_== List("although")
      StopWords.getStopWordCount("blah de blah").stopWords must_== List.empty
    }
  }
}
