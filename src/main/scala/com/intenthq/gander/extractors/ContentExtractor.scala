package com.intenthq.gander.extractors

import java.util.Date
import com.intenthq.gander.utils.Logging
import com.intenthq.gander.Link
import com.intenthq.gander.text.{ReplaceSequence, StopWords, StringReplacement, WordStats}
import org.jsoup.nodes.{Document, Element}
import org.jsoup.select.{Collector, Elements, TagsEvaluator}
import scala.collection.JavaConversions._
import scala.collection.mutable
import scala.math._
import scala.util.Try

object ContentExtractor extends Logging {
  val logPrefix = "ContentExtractor: "

  class StringSplitter(pattern: String) {
    def split(input: String): Array[String] = input.split(pattern)
  }

  private val motleyReplacement = StringReplacement("&#65533;", "")
  private val titleReplacements = ReplaceSequence("&raquo;").append("»")
  private val pipeSplitter = new StringSplitter("\\|")
  private val dashSplitter = new StringSplitter(" - ")
  private val arrowsSplitter = new StringSplitter("»")
  private val spaceSplitter = new StringSplitter(" ")
  private val topNodeTags = new TagsEvaluator(Set("p", "td", "pre", "strong", "li", "code"))

  def extractTitle(doc: Document): String = {
    val titleElem = doc.getElementsByTag("title")
    val title =
      if (titleElem.isEmpty) ""
      else {
        val titleText = titleElem.first.text
        if (titleText.contains("|")) doTitleSplits(titleText, pipeSplitter)
        else if (titleText.contains("-")) doTitleSplits(titleText, dashSplitter)
        else if (titleText.contains("»")) doTitleSplits(titleText, arrowsSplitter)
        else ""
      }
    motleyReplacement.replaceAll(title).trim
  }

  /**
  * based on a delimeter in the title take the longest piece or do some custom logic based on the site
  */
  private def doTitleSplits(title: String, splitter: StringSplitter): String = {
    val titlePieces: Array[String] = splitter.split(title)
    if (titlePieces.isEmpty) ""
    else titleReplacements.replaceAll(titlePieces.maxBy(_.length)).trim
  }

  private def getMetaContent(doc: Document, metaName: String): String = {
    val meta: Elements = doc.select(metaName)
    if (meta.isEmpty) ""
    else meta.first.attr("content").trim
  }

  private def firstNonEmpty(elements: (() => String)*): String = {
    if (elements.isEmpty) ""
    else {
      val first: String = elements.head()
      if (first.isEmpty) firstNonEmpty(elements.tail :_ *)
      else first
    }
  }

  /**
  * if the article has meta description set in the source, use that
  */
  def extractMetaDescription(doc: Document): String =
    firstNonEmpty(
      () => doc.select("meta[name=description]").attr("content"),
      () => doc.select("meta[property=og:description]").attr("content"),
      () => doc.select("meta[name=twitter:description]").attr("content")
    ).trim

  /**
  * if the article has meta keywords set in the source, use that
  */
  def extractMetaKeywords(doc: Document): String = getMetaContent(doc, "meta[name=keywords]")

  /**
   * if the article has meta canonical link set in the url
   */
  def extractCanonicalLink(doc: Document): Option[String] = {
    val href = firstNonEmpty(
      () => doc.select("link[rel=canonical]").attr("abs:href"),
      () => doc.select("meta[property=og:url]").attr("abs:content"),
      () => doc.select("meta[name=twitter:url]").attr("abs:content")
    ).trim
    if (href.nonEmpty) Some(href) else None
  }

  def extractDateFromURL(url: String): Option[Date] = {
    def findYearMonthAndDay(segments: Array[String]): (Option[Int], Option[Int], Option[Int]) = {
      def findMonthAndDay(segments: Array[String]): (Option[Int], Option[Int]) = {
        def findDay(segment: String): Option[Int] = Try(segment.toInt).filter(d => d >= 1 && d <= 31).toOption
        Try(segments.head.toInt).filter(m => m >= 1 && m <= 12).map { month =>
          (Some(month), findDay(segments.tail.head))
        }.getOrElse((None, None))
      }

      if (segments.isEmpty)
        (None, None, None)
      else {
        Try(segments.head.toInt).filter(y => y > 1970 && y < 3000).map { year =>
          val (month, day) = findMonthAndDay(segments.tail)
          (Some(year), month, day)
        }.getOrElse(findYearMonthAndDay(segments.tail))
      }
    }

    val (year, month, day) = findYearMonthAndDay(url.split("/"))
    year.map { y =>
      val m = month.getOrElse(1) - 1
      val d = day.getOrElse(1)
      new Date(y - 1900, m, d)
    }
  }

  /**
  * we're going to start looking for where the clusters of paragraphs are. We'll score a cluster based on the number of stopwords
  * and the number of consecutive paragraphs together, which should form the cluster of text that this node is around
  * also store on how high up the paragraphs are, comments are usually at the bottom and should get a lower score
  */
  def calculateBestNodeBasedOnClustering(document: Document, lang:String): Option[Element] = {
    val doc = document.clone
    val nodesToCheck = Collector.collect(topNodeTags, doc)

    val nodesWithText = nodesToCheck.filter { node =>
      val nodeText = node.text
      val wordStats = StopWords.stopWordCount(nodeText, lang)
      val highLinkDensity = isHighLinkDensity(node)
      trace("Candidate: " + node.tagName() + " score: " + wordStats + " d:" + highLinkDensity + " text:" + nodeText)
      wordStats.stopWordCount > 2 && !highLinkDensity
    }

    val numberOfNodes = nodesWithText.size
    val bottomNodesForNegativeScore = numberOfNodes * 0.25

    trace(logPrefix + "About to inspect num of nodes with text: " + numberOfNodes)

    def boostScoreForNode(node: Element, startingBoost: Double, count: Int): (Double, Double) = {
      var newStartingBoost = startingBoost
      var result = 0.0
      if (isOkToBoost(node, lang)) {
        result = (1.0 / startingBoost) * 50
        newStartingBoost += 1
      }
      if (numberOfNodes > 15) {
        if ((numberOfNodes - count) <= bottomNodesForNegativeScore) {
          val booster = bottomNodesForNegativeScore - (numberOfNodes - count)
          result = -pow(booster, 2)
          if (abs(result) > 40)
            result = 5
        }
      }
      (newStartingBoost, result)
    }

    var count = 0
    var startingBoost: Double = 1.0
    val parentNodes = mutable.Set.empty[Element]

    for (node <- nodesWithText) {
      val (newStartingBoost, boostScore) = boostScoreForNode(node, startingBoost, count)
      startingBoost = newStartingBoost

      trace(logPrefix + "Location Boost Score: " + boostScore + " on interation: " + count + " tag='"+ node.tagName +"' id='" + node.parent.id + "' class='" + node.parent.attr("class"))

      val wordStats: WordStats = StopWords.stopWordCount(node.text, lang)
      val upscore: Int = (wordStats.stopWordCount + boostScore).toInt
      updateScore(node.parent, upscore)
      updateScore(node.parent.parent, upscore / 2)
      updateNodeCount(node.parent, 1)
      updateNodeCount(node.parent.parent, 1)
      parentNodes.add(node.parent)
      parentNodes.add(node.parent.parent)
      count += 1
    }

    if (parentNodes.isEmpty)
      None
    else {
      Some(parentNodes.maxBy(getScore)).filter(getScore(_) >= 20)
    }
  }

  /**
  * alot of times the first paragraph might be the caption under an image so we'll want to make sure if we're going to
  * boost a parent node that it should be connected to other paragraphs, at least for the first n paragraphs
  * so we'll want to make sure that the next sibling is a paragraph and has at least some substatial weight to it
  */
  private def isOkToBoost(node: Element, lang: String): Boolean = {
    var stepsAway: Int = 0
    val minimumStopWordCount = 5
    val maxStepsAwayFromNode = 3

    walkSiblings(node) { currentNode =>
      if (currentNode.tagName == "p" || currentNode.tagName == "strong") {
        if (stepsAway >= maxStepsAwayFromNode) {
          return false
        }
        val wordStats = StopWords.stopWordCount(currentNode.text, lang)
        if (wordStats.stopWordCount > minimumStopWordCount)
          return true
        stepsAway += 1
      }
    }
    false
  }

  private def getShortText(e: String, max: Int): String = if (e.length > max) e.take(max) + "..." else e

  /**
   * Checks the density of links within a node. If there's not much text and what's there is mostly links,
   * we're not interested
   */
  private def isHighLinkDensity(e: Element, limit: Double = 1.0): Boolean = {
    val links = e.getElementsByTag("a")
    links.addAll(e.getElementsByAttribute("onclick"))

    if (links.size == 0)
      false
    else {
      val text = e.text.trim
      val words = spaceSplitter.split(text)
      val numberOfWords = words.length.toDouble
      val linkText: String = links.mkString(" ").toString
      val linkWords: Array[String] = spaceSplitter.split(linkText)
      val numberOfLinkWords = linkWords.length.toDouble
      val numberOfLinks = links.size
      val score = numberOfLinks * numberOfLinkWords / numberOfWords

      trace(logPrefix + "Calculated link density score as: " + score + " for node: " + getShortText(e.text, 50))

      score >= limit
    }
  }

  private def getScore(node: Element): Int = getGravityScoreFromNode(node).getOrElse(0)

  private def getGravityScoreFromNode(node: Element): Option[Int] = Try(node.attr("gravityScore").toInt).toOption

  /**
  * adds a score to the gravityScore Attribute we put on divs
  * we'll get the current score then add the score we're passing in to the current
  *
  * @param addToScore - the score to add to the node
  */
  private def updateScore(node: Element, addToScore: Int) {
    val currentScore = Try(node.attr("gravityScore").toInt).getOrElse(0)
    val newScore = currentScore + addToScore
    node.attr("gravityScore", newScore.toString)
  }

  /**
  * stores how many decent nodes are under a parent node
  */
  private def updateNodeCount(node: Element, addToCount: Int) {
    val currentScore = Try(node.attr("gravityNodes").toInt).getOrElse(0)
    val newScore: Int = currentScore + addToCount
    node.attr("gravityNodes", newScore.toString)
  }

  /**
   * pulls out links we like
   */
  def extractLinks(node: Element): Seq[Link] =
    node.parent.select("a[href]")
      .filter(el => el.attr("href") != "#" && !el.attr("abs:href").trim.isEmpty)
      .map(el => Link(el.text, el.attr("abs:href")))

  private def isTableTagAndNoParagraphsExist(e: Element): Boolean = {
    val subParagraphs: Elements = getChildParagraphs(e)
    for (p <- subParagraphs) {
      if (p.text.length < 25) {
        p.remove()
      }
    }

    val subParagraphs2: Elements = e.getElementsByTag("p")
    if (subParagraphs2.size == 0 && e.tagName != "td") {
      if (e.tagName == "ul" || e.tagName == "ol") {
        val linkTextLength = e.getElementsByTag("a").map(_.text.length).sum
        val elementTextLength = e.text.length
        elementTextLength <= 2 * linkTextLength
      }
      else true
    } else false
  }

  /**
  * remove any divs that looks like non-content, clusters of links, or paras with no gusto
  */
  def postExtractionCleanup(targetNode: Element, lang: String): Element = {
    val node = addSiblings(targetNode, lang)
    node.children
      .filter(e => e.tagName != "p" || isHighLinkDensity(e))
      .filter(e => isHighLinkDensity(e) || isTableTagAndNoParagraphsExist(e) || !isNodeScoreThresholdMet(node, e))
      .foreach(e => Try(e.remove()))
    node
  }
  
  private def isNodeScoreThresholdMet(node: Element, e: Element): Boolean = {
    val topNodeScore = getScore(node)
    val currentNodeScore = getScore(e)
    val thresholdScore = topNodeScore * .08
    !(currentNodeScore < thresholdScore && e.tagName != "td")
  }

  private def getChildParagraphs(e: Element): Elements = {
    val potentialParagraphs = e.getElementsByTag("p")
    potentialParagraphs.addAll(e.getElementsByTag("strong"))
    potentialParagraphs
  }

  /**
  * adds any siblings that may have a decent score to this node
  */
  private def getSiblingContent(currentSibling: Element,
                        baselineScoreForSiblingParagraphs: Int,
                        lang: String): Option[String] = {
    if ((currentSibling.tagName == "p" || currentSibling.tagName == "strong") && currentSibling.text.nonEmpty)
      Some(currentSibling.outerHtml)
    else {
      val siblingBaseLineScore = baselineScoreForSiblingParagraphs * 0.3
      val text = getChildParagraphs(currentSibling)
        .filter(p => StopWords.stopWordCount(p.text, lang).stopWordCount >= siblingBaseLineScore)
        .map(p => "<p>" + p.text + "<p>")
        .mkString(" ")
      if (text.isEmpty) None else Some(text)
    }
  }

  private def walkSiblings[T](node: Element)(work: (Element) => T): Seq[T] = {
    var currentSibling = node.previousElementSibling
    val b = mutable.Buffer[T]()

    while (currentSibling != null) {
      b += work(currentSibling)
      currentSibling = currentSibling.previousElementSibling
    }
    b
  }

  private def addSiblings(topNode: Element, lang: String): Element = {
    val baselineScoreForSiblingParagraphs = getBaselineScoreForSiblings(topNode, lang)
    val results = walkSiblings(topNode) { currentNode =>
      getSiblingContent(currentNode, baselineScoreForSiblingParagraphs, lang)
    }.reverse.flatten
    topNode.child(0).before(results.mkString)
    topNode
  }

  /**
  * we could have long articles that have tons of paragraphs so if we tried to calculate the base score against
  * the total text score of those paragraphs it would be unfair. So we need to normalize the score based on the average scoring
  * of the paragraphs within the top node. For example if our total score of 10 paragraphs was 1000 but each had an average value of
  * 100 then 100 should be our base.
  */
  private def getBaselineScoreForSiblings(topNode: Element, lang: String): Int = {
    val nodesToCheck = getChildParagraphs(topNode)

    val scores = nodesToCheck.flatMap { node =>
      val wordStats = StopWords.stopWordCount(node.text, lang)
      if (wordStats.stopWordCount > 2 && !isHighLinkDensity(node)) Some(wordStats.stopWordCount)
      else None
    }

    if (scores.nonEmpty) scores.sum / scores.length
    else Int.MaxValue
  }
}
