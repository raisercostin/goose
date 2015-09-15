package com.intenthq.gander.text

case class WordStats(stopWords: List[String], wordCount:Int) {
  /**
   * total number of stopwords or good words that we can calculate
   */
  val stopWordCount : Int = stopWords.size

  override def toString: String =
    "Word statistics: words = " + wordCount + ", stop words = " +
      stopWordCount + " (" + stopWords.mkString(", ") + ")"
}
