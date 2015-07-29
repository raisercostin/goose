package com.intenthq.gander.text

import java.io.StringReader
import com.chenlb.mmseg4j.{ComplexSeg, Dictionary, MMSeg}
import com.gravity.goose.Language
import com.gravity.goose.Language.Chinese
import com.gravity.goose.text.StringReplacement
import com.gravity.goose.text.string
import com.gravity.goose.utils.FileHelper
import scala.collection.mutable

object StopWords {
  // the confusing pattern below is basically just match any non-word character excluding white-space.
  private val PUNCTUATION: StringReplacement = StringReplacement.compile("[^\\p{Ll}\\p{Lu}\\p{Lt}\\p{Lo}\\p{Nd}\\p{Pc}\\s]", string.empty)

  private val stopWordsMap = mutable.Map.empty[String, Set[String]]

  private def removePunctuation(str: String): String = PUNCTUATION.replaceAll(str)
  
  def stopWords(lname: String): Set[String] =
    stopWordsMap.getOrElse(lname, {
      val stopWordsFile = "stopwords-%s.txt" format lname
      val stopWords = FileHelper.loadResourceFile(stopWordsFile, StopWords.getClass)
        .split(sys.props("line.separator"))
        .map(s => s.trim)
        .toSet
      stopWordsMap += lname -> stopWords
      stopWords
    })

  def candidateWords(strippedInput: String, language: String): Array[String] =
    Language(language) match {
      case Chinese => tokenize(strippedInput).toArray
      case _ => string.SPACE_SPLITTER.split(strippedInput)
    }

  def stopWordCount(content: String, lang: String = "en"): WordStats =
    if (string.isNullOrEmpty(content))
      WordStats(List.empty, 0)
    else {
      val strippedInput = removePunctuation(content)
      val candidates = candidateWords(strippedInput, lang)
      val stop = stopWords(lang)
      val overlappingStopWords = candidates.map(_.toLowerCase).filter(stop.contains)
      WordStats(overlappingStopWords.toList, candidates.length)
    }

  def tokenize(line: String): List[String] = {
    val seg = new ComplexSeg(Dictionary.getInstance())
    val mmSeg = new MMSeg(new StringReader(line), seg)
    var tokens = List.empty[String]
    var word = mmSeg.next()
    while (word != null) {
      tokens = word.getString :: tokens
      word = mmSeg.next()
    }
    tokens
  }
}