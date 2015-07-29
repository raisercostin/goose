package com.gravity.goose.utils

import com.intenthq.gander.text.StopWords
import org.junit.Test
import org.junit.Assert._

/**
* Created by Jim Plush
* User: jim
* Date: 8/16/11
*/

class FileHelperTest {

  @Test
  def loadFileContents() {
    val txt = FileHelper.loadResourceFile("stopwords-en.txt", StopWords.getClass)
    assertTrue(txt.startsWith("a's"))
  }

}
