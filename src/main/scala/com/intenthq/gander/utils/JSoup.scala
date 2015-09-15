package com.intenthq.gander.utils

import java.util.regex.Pattern

import org.jsoup.nodes.{Element, Document}

import scala.collection.convert.Wrappers.JListWrapper

object JSoup {
  def byTag(tag: String)(implicit elem: Element): Seq[Element] = JListWrapper(elem.getElementsByTag(tag))

  def byAttrRe(attr: String, pattern: Pattern)(implicit doc: Document): Seq[Element] =
    JListWrapper(doc.getElementsByAttributeValueMatching(attr, pattern))

  def byAttr(value: String)(implicit elem: Element): Seq[Element] =
    JListWrapper(elem.getElementsByAttribute(value))

  def select(query: String)(implicit elem: Element): Seq[Element] = JListWrapper(elem.select(query))

  def remove(elem: Element) = Option(elem.parent()).foreach(_ => elem.remove())

}
