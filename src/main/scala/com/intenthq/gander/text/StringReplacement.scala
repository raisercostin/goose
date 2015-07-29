package com.intenthq.gander.text

import java.util.regex.Pattern

object StringReplacement {
  def apply(pattern: String, replaceWith: String): StringReplacement = {
    if (pattern.isEmpty) throw new IllegalArgumentException("Patterns must not be null or empty!")
    new StringReplacement(Pattern.compile(pattern), replaceWith)
  }
}

class StringReplacement private (pattern: Pattern, replaceWith: String) {
  def replaceAll(input: String): String = pattern.matcher(input).replaceAll(replaceWith)
}
