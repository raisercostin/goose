package com.gravity.goose

import java.net.URL

import com.google.common.base.Charsets
import com.google.common.io.Resources
import com.intenthq.gander.{Gander, PageInfo}
import junit.framework.Assert._

/**
 * Created by Jim Plush
 * User: jim
 * Date: 8/19/11
 */

object TestUtils {

  val staticHtmlDir = "/com/gravity/goose/statichtml/"

  /**
  * returns an article object from a crawl
  */
  def getPageInfo(url: String): PageInfo = {
    val rawHTML = Resources.toString(new URL(url), Charsets.UTF_8)
    Gander.extract(rawHTML).get
  }

  def runArticleAssertions(pageInfo: PageInfo,
                           expectedTitle: Option[String] = None,
                           expectedStart: Option[String] = None,
                           expectedDescription: Option[String] = None,
                           expectedKeywords: Option[String] = None): Unit = {

    expectedTitle.foreach(assertEquals("Expected title was not returned!", _, pageInfo.title))

/*
    if (expectedStart != null) {
      val articleText: String = article.cleanedArticleText
      assertNotNull("Resulting article text was NULL!", articleText)
      assertTrue("Article text was not as long as expected beginning!", expectedStart.length <= articleText.length)
      val actual: String = articleText.substring(0, expectedStart.length)
      assertEquals("The beginning of the article text was not as expected!", expectedStart, actual)
    }
    if (expectedHtmlStart != null) {
      val articleHtml: String = article.htmlArticle
      assertNotNull("Resulting article html was NULL!", articleHtml)
      assertTrue("Article html was not as long as expected beginning!", expectedHtmlStart.length <= articleHtml.length)
      val actual: String = articleHtml.substring(0, expectedHtmlStart.length)
      assertEquals("The beginning of the article html was not as expected!", expectedHtmlStart, actual)
    }
    if (expectedDescription != null) {
      val description: String = article.metaDescription
      assertNotNull("Meta Description was NULL!", description)
      assertEquals("Meta Description was not as expected!", expectedDescription, description)
    }
    if (expectedKeywords != null) {
      val keywords: String = article.metaDescription
      assertNotNull("Meta Keywords was NULL!", keywords)
      assertEquals("Meta Keywords was not as expected!", expectedKeywords, keywords)
    }
    */
  }

}
