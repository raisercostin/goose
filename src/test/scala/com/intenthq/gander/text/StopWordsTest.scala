package com.intenthq.gander.text

import org.specs2.mutable.Specification

class StopWordsTest extends Specification {
  "StopWords" >> {
    " should find how many stopwords are there" >> {
      StopWords.stopWordCount("blah blah blah").stopWordCount must_== 0
      StopWords.stopWordCount("although blah de blah").stopWordCount must_== 1
    }

    " should determine which words are stopwords" >> {
      StopWords.stopWordCount("although blah de blah").stopWords must_== List("although")
      StopWords.stopWordCount("blah de blah").stopWords must_== List.empty
    }
  }
}
