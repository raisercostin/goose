package com.intenthq.gander.extractors

import java.util.Date
import java.util.regex.Pattern

import com.intenthq.gander.Link
import com.intenthq.gander.text.{StopWords, WordStats}
import com.intenthq.gander.utils.JSoup._
import org.joda.time.DateTime
import org.jsoup.nodes.{Document, Element}
import org.slf4j.{Logger, LoggerFactory}

import scala.collection.convert.Wrappers.JListWrapper
import scala.collection.mutable
import scala.math._
import scala.util.Try

object ContentExtractor {

  val logger: Logger = LoggerFactory.getLogger(getClass)

  def extractTitle(doc: Document): String = {
    val titleElem = byTag("title")(doc)
    titleElem.headOption.map { x =>
      val titleText = x.text
      List(" | ", " - ", " » ", " · ").collectFirst {
        case separator if titleText.contains(separator) => titleText.split(Pattern.quote(separator)).head
      }.getOrElse(titleText)
    }.getOrElse("")
     .replace("&#65533;", "").trim
  }

  def extractLang(doc: Document): Option[String] =
    byTag("html")(doc).headOption.map(_.attr("lang")).filter(_.nonEmpty).orElse(
      metaContent("http-equiv=Content-Language")(doc).orElse(
        metaContent("property=og:locale")(doc)
      )
    )

  private def metaContent(metaName: String)(implicit doc: Document): Option[String] =
    select(s"meta[$metaName]").headOption.map(_.attr("content").trim)

  /**
  * if the article has meta description set in the source, use that
  */
  def extractMetaDescription(implicit doc: Document): String =
    metaContent("name=description").orElse(
      metaContent("og:description").orElse(
        metaContent("name=twitter:description")
      )
    ).getOrElse("").trim

  /**
  * if the article has meta keywords set in the source, use that
  */
  def extractMetaKeywords(implicit doc: Document): String = metaContent("name=keywords").getOrElse("")

  /**
   * if the article has meta canonical link set in the url
   */
  def extractCanonicalLink(implicit doc: Document): Option[String] =
    select("link[rel=canonical]").headOption.map(_.attr("abs:href")).orElse(
      select("meta[property=og:url]").headOption.map(_.attr("abs:content"))
    ).orElse(
      select("meta[name=twitter:url]").headOption.map(_.attr("abs:content"))
    ).map(_.trim)

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
      val m = month.getOrElse(1)
      val d = day.getOrElse(1)
      new DateTime(y, m, d, 0, 0).toDate
    }
  }

  /**
  * we're going to start looking for where the clusters of paragraphs are. We'll score a cluster based on the number of stopwords
  * and the number of consecutive paragraphs together, which should form the cluster of text that this node is around
  * also store on how high up the paragraphs are, comments are usually at the bottom and should get a lower score
  */
  def calculateBestNodeBasedOnClustering(document: Document, lang:String): Option[Element] = {
    implicit val doc = document.clone

    val nodesToCheck = byTag("p") ++ byTag("td") ++ byTag("pre") ++ byTag("strong") ++ byTag("li") ++ byTag("code")

    val nodesWithText = nodesToCheck.filter { node =>
      val nodeText = node.text
      val wordStats = StopWords.stopWordCount(nodeText, lang)
      val highLinkDensity = isHighLinkDensity(node)
      logger.trace("Candidate: " + node.tagName() + " score: " + wordStats + " d:" + highLinkDensity + " text:" + nodeText)
      wordStats.stopWordCount > 2 && !highLinkDensity
    }

    val numberOfNodes = nodesWithText.size
    val bottomNodesForNegativeScore = numberOfNodes * 0.25

    logger.trace("About to inspect num of nodes with text: " + numberOfNodes)

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

      logger.trace("Location Boost Score: " + boostScore + " on interation: " + count + " tag='"+ node.tagName +"' id='" + node.parent.id + "' class='" + node.parent.attr("class"))

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
  private def isHighLinkDensity(implicit e: Element): Boolean = {
    val limit = 1.0
    val links = byTag("a") ++ byAttr("onclick")

    if (links.isEmpty)
      false
    else {
      val words = e.text.trim.split("\\s+")
      val linkWords = links.mkString(" ").split("\\s+")
      val numberOfLinks = links.size
      val numberOfWords = words.length.toDouble
      val numberOfLinkWords = linkWords.length.toDouble
      val score = numberOfLinks * numberOfLinkWords / numberOfWords

      logger.trace("Calculated link density score as: {} for node: {}", score, getShortText(e.text, 50))

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
  def extractLinks(implicit node: Element): Seq[Link] =
    select("a[href]")
      .filter(el => el.attr("href") != "#" && !el.attr("abs:href").trim.isEmpty)
      .map(el => Link(el.text, el.attr("abs:href")))

  private def isTableTagAndNoParagraphsExist(implicit e: Element): Boolean = {
    getChildParagraphs(e).filter(_.text.length < 25).foreach(remove)

    val subParagraphs2 = byTag("p")
    if (subParagraphs2.isEmpty && e.tagName != "td") {
      if (e.tagName == "ul" || e.tagName == "ol") {
        val linkTextLength = byTag("a").map(_.text.length).sum
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
    JListWrapper(node.children)
      .filter(e => e.tagName != "p" || isHighLinkDensity(e))
      .filter(e => isHighLinkDensity(e) || isTableTagAndNoParagraphsExist(e) || !isNodeScoreThresholdMet(node, e))
      .foreach(remove)
    node
  }
  
  private def isNodeScoreThresholdMet(node: Element, e: Element): Boolean = {
    val topNodeScore = getScore(node)
    val currentNodeScore = getScore(e)
    val thresholdScore = topNodeScore * .08
    !(currentNodeScore < thresholdScore && e.tagName != "td")
  }

  private def getChildParagraphs(implicit e: Element): Seq[Element] = byTag("p") ++ byTag("strong")

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
