package com.intenthq.gander.utils

import com.intenthq.gander.text.StopWords
import org.specs2.mutable.Specification

class FileHelperTest extends Specification {
  "FileHelper" >> {
    " should load file contents" >> {
      val txt = FileHelper.loadResourceFile("stopwords-en.txt", StopWords.getClass)
      txt must startWith("a's")
    }
    " should return empty if the file doesn't exist" >> {
      val txt = FileHelper.loadResourceFile("stopwords-nonexistant.txt", StopWords.getClass)
      txt must beEmpty
    }
  }
}
