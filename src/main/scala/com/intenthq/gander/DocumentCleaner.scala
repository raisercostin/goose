package com.intenthq.gander

import java.util.regex.Pattern

import com.intenthq.gander.utils.JSoup._
import com.intenthq.gander.utils.Logging
import org.jsoup.nodes.{Document, TextNode}

object DocumentCleaner extends Logging {

  private val captionPattern = Pattern.compile("^caption$")
  private val googlePattern = Pattern.compile("google")
  private val facebookPattern = Pattern.compile("facebook")
  private val twitterPattern = Pattern.compile("twitter")
  /**
   * this regex is used to remove undesirable nodes from our doc
   * indicate that something maybe isn't content but more of a comment, footer or some other undesirable node
   */
  private val regExRemoveNodes = "^side$|combx|retweet|mediaarticlerelated|menucontainer|navbar|comment(?!ed)|PopularQuestions|contact|foot|footer|Footer|footnote|cnn_strycaptiontxt|links|meta$|scroll(?!able)|shoutbox|sponsor" +
    "|tags|socialnetworking|socialNetworking|cnnStryHghLght|cnn_stryspcvbx|^inset$|pagetools|post-attributes|welcome_form|contentTools2|the_answers|remember-tool-tip" +
    "|communitypromo|promo_holder|runaroundLeft|subscribe|vcard|articleheadings|date|^print$|popup|author-dropdown|tools|socialtools|byline|konafilter|KonaFilter|breadcrumbs|^fn$|wp-caption-text"
  private val queryNaughtyIDs = "[id~=(" + regExRemoveNodes + ")]"
  private val queryNaughtyClasses = "[class~=(" + regExRemoveNodes + ")]"
  private val queryNaughtyNames = "[name~=(" + regExRemoveNodes + ")]"

  def clean(doc: Document): Document = {
    trace("Starting cleaning phase")
    //TODO right now this solution mutates this document
    // it would be very nice to implement this with an immutable solution
    implicit val docToClean: Document = doc.clone

    cleanTextTags
    removeScriptsAndStyles
    cleanBadTags
    removeNodesViaRegEx(captionPattern)
    removeNodesViaRegEx(googlePattern)
    removeNodesViaRegEx(facebookPattern)
    removeNodesViaRegEx(twitterPattern)
    cleanUpSpanTagsInParagraphs
    docToClean
  }

  /**
  * replaces various tags with textnodes
  */
  private def cleanTextTags(implicit doc: Document): Unit =
    (byTag("em") ++ byTag("strong") ++ byTag("b") ++ byTag("i") ++
      byTag("strike") ++ byTag("del") ++ byTag("ins")).foreach { node =>
      val tn = new TextNode(node.text, doc.baseUri)
      node.replaceWith(tn)
    }

  private def removeScriptsAndStyles(implicit doc: Document): Unit =
    (byTag("script") ++ byTag("style")).foreach(remove)

  private def cleanBadTags(implicit doc: Document): Unit =
    (select(queryNaughtyIDs) ++ select(queryNaughtyClasses) ++ select(queryNaughtyNames)).foreach(remove)

  /**
   * removes nodes that may have a certain pattern that matches against a class or id tag
   */
  private def removeNodesViaRegEx(pattern: Pattern)(implicit doc: Document): Unit =
    (byAttrRe("id", pattern) ++ byAttrRe("class", pattern)).foreach(remove)

  /**
  * takes care of the situation where you have a span tag nested in a paragraph tag
  * e.g. businessweek2.txt
  */
  private def cleanUpSpanTagsInParagraphs(implicit doc: Document): Unit =
    byTag("span").filter(_.parent.nodeName == "p").foreach { node =>
      val tn = new TextNode(node.text, doc.baseUri)
      node.replaceWith(tn)
    }

}