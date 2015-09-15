package com.intenthq.gander

import org.jsoup.Jsoup
import org.specs2.mutable.Specification

class DocumentCleanerSpec extends Specification {

  def html(body: String) = s"<html><head></head><body>$body</body></html>"

  def check(actual: String, expected: String) = {
    val doc = Jsoup.parse(html(actual))
    DocumentCleaner.clean(doc).toString.replaceAll(" +", " ") must_== Jsoup.parse(html(expected)).toString.replaceAll(" +", " ")
  }

  "clean" >> {
    "should clean em, strong, b, i, strike, del and ins tags" >> {
      val body = """<p>This is a paragraph with <em>emphasis</em>, <strong>strong</strong>,
                 | <b>bold</b>, <i><img src="image">italics</i>, <strike>strike</strike>, <del>deleted</del>
                 | and <ins>insterted</ins> elements.</p>""".stripMargin
      val expected = """<p>This is a paragraph with emphasis, strong,
                   | bold, italics, strike, deleted
                   | and insterted elements.</p>""".stripMargin
      check(body, expected)
    }

    "should remove naughty tags" >> {
      val body = """<p>This is a paragraph.</p>
                   |<p class="retweet">to remove</p>
                   |<p>hey</p>
                   |<p id="vcard">to remove</p>
                   |<p>hey</p>
                   |<p name="menucontainer">to remove</p>
                   |<p>hey</p>""".stripMargin
      val expected = """<p>This is a paragraph.</p> <p>hey</p> <p>hey</p> <p>hey</p>""".stripMargin

      check(body, expected)
    }

    "should remove style and script tags" >> {
      val body = """<p>This is a paragraph with a<script>javascript</script>
                   |<strong>strong<style>css</style></strong> element.</p>""".stripMargin
      val expected = """<p>This is a paragraph with a strong element.</p>""".stripMargin

      check(body, expected)
    }

    "should remove style and script tags" >> {
      val body = """<p>This is a paragraph with a<script>javascript</script>
                   |<strong>strong<style>css</style></strong> element.</p>""".stripMargin
      val expected = """<p>This is a paragraph with a strong element.</p>""".stripMargin

      check(body, expected)
    }

    "should remove tags with caption as class or id" >> {
      val body = """<p>This is a paragraph</p><p id="caption">to remove</p>
                   |<p class="caption">to remove</p><p>hey</p>""".stripMargin
      val expected = """<p>This is a paragraph</p> <p>hey</p>""".stripMargin

      check(body, expected)
    }

    "should remove tags with google as class or id" >> {
      val body = """<p>This is a paragraph</p><p id=" google ">to remove</p>
                   |<p class=" google ">to remove</p><p>hey</p>""".stripMargin
      val expected = """<p>This is a paragraph</p> <p>hey</p>""".stripMargin

      check(body, expected)
    }

    "should remove tags with facebook as class or id" >> {
      val body = """<p>This is a paragraph</p><p id="facebook">to remove</p>
                   |<p class="facebook">to remove</p><p>hey</p>""".stripMargin
      val expected = """<p>This is a paragraph</p> <p>hey</p>""".stripMargin

      check(body, expected)
    }

    "should remove tags with twitter as class or id" >> {
      val body = """<p>This is a paragraph</p><p id="twitter">to remove</p>
                   |<p class="twitter">to remove</p><p>hey</p>""".stripMargin
      val expected = """<p>This is a paragraph</p> <p>hey</p>""".stripMargin

      check(body, expected)
    }

    "should clean span tags inside paragraphs" >> {
      val body = """<p>This is a paragraph <span>in a span</span>
                   |and more <span>s1<span>s2</span></span></p>""".stripMargin
      val expected = """<p>This is a paragraph in a span and more s1s2</p>""".stripMargin

      check(body, expected)
    }
  }

}
