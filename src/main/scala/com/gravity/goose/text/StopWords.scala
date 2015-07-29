/**
 * Licensed to Gravity.com under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  Gravity.com licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.gravity.goose.text

/**
 * Created by Jim Plush
 * User: jim
 * Date: 8/16/11
 */

import java.io.StringReader
import java.util.{HashMap, Map}

import com.chenlb.mmseg4j.{ComplexSeg, Dictionary, MMSeg}
import com.gravity.goose.Language
import com.gravity.goose.Language._
import com.gravity.goose.utils.FileHelper
import com.intenthq.gander.text.WordStats

import scala.collection.JavaConversions._
import scala.collection.Set

object StopWords {

  // the confusing pattern below is basically just match any non-word character excluding white-space.
  private val PUNCTUATION: StringReplacement = StringReplacement.compile("[^\\p{Ll}\\p{Lu}\\p{Lt}\\p{Lo}\\p{Nd}\\p{Pc}\\s]", string.empty)

  //val STOP_WORDS = FileHelper.loadResourceFile("stopwords-en.txt", StopWords.getClass).split(sys.props("line.separator")).toSet
  private val stopWordsMap: Map[String, Set[String]] = new HashMap[String, Set[String]]()

  def removePunctuation(str: String): String = {
    PUNCTUATION.replaceAll(str)
  }
  
  def getStopWords(language: Language): Set[String] = getStopWords(language.toString)

  def getStopWords(lname: String): Set[String] = {

    var stopWords = stopWordsMap.get(lname)
    if (stopWords == null) {
      var stopWordsFile = "stopwords-%s.txt" format lname
      stopWords = FileHelper.loadResourceFile(stopWordsFile, StopWords.getClass).split(sys.props("line.separator")).toSet
      stopWords = stopWords.map(s=>s.trim)
      stopWordsMap.put(lname, stopWords)
    }
    stopWords    
  }
  def getCandidateWords(strippedInput: String, language: String): Array[String] =
    getCandidateWords(strippedInput, Language(language))

  def getCandidateWords(strippedInput: String, language: Language): Array[String] = {
    language match {
      case Chinese => tokenize(strippedInput).toArray
      case _ => string.SPACE_SPLITTER.split(strippedInput)
    }
  } 

  def getStopWordCount(content: String, lang: String = "en"): WordStats = {
    if (string.isNullOrEmpty(content))
      WordStats(List.empty, 0)
    else {
      val strippedInput = removePunctuation(content)
      val candidateWords = getCandidateWords(strippedInput, lang)
      val stopWords = getStopWords(lang)
      val overlappingStopWords = candidateWords.map(_.toLowerCase).filter(stopWords.contains)
      WordStats(overlappingStopWords.toList, candidateWords.length)
    }
  }
  
  def  tokenize(line: String): List[String] = {

    val seg = new ComplexSeg(Dictionary.getInstance());
    val mmSeg = new MMSeg(new StringReader(line), seg);
    var tokens = List[String]();
    var word = mmSeg.next()
    while (word != null) {
      tokens = word.getString() :: tokens ;
      word = mmSeg.next();
    }
    return tokens;
  }  
}