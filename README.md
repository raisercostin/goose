#Gander [![Circle CI](https://circleci.com/gh/intenthq/gander.svg?style=svg)](https://circleci.com/gh/intenthq/gander) [![Coverage Status](https://coveralls.io/repos/intenthq/gander/badge.svg?branch=master&service=github)](https://coveralls.io/github/intenthq/gander?branch=master)

**Gander is a scala library that extracts metadata and content from web pages.**
 
It is based on [Goose](https://github.com/GravityLabs/goose) with the idea to:
- Simplify its codebase by removing some of its functionality (like crawling, there are plenty of project that do it well)
- Keep it alive (goose has been inactive for several years now)
- Make its codebase more functional and take advantage of some of newer scala features

## What data does it extract?

Gander will try to extract three different kinds of data from a web page:
- Metadata: (title, meta description, meta keywords, language, canonical link, open graph data,
publish date)
- Main text for the page
- Links present in the main text of the page

## Using Gander

### Adding the dependency

The artefact is published in maven central. If you are using sbt you just need to add
the following line (remember to replace 1.0 with the latest version):
```
"com.intenthq" % "gander" % "1.0"
```
### In your code

Gander provides a single object and a single method to access its functionality
and it's pretty straight forward and intuitive to use.

This three lines of code, for example, would download the url specified (using
Guava) and extract the page information from the raw html:
```scala
val url = "http://engineering.intenthq.com/2015/03/what-is-good-code-a-scientific-definition/"
val rawHTML = Resources.toString(new URL(url), charset)
println(Gander.extract(rawHTML))

```

You can find more examples in our tests.

## Collaborate & Philosophy
Keep it simple and make 1 thing
Remove the code that was doing other stuff (downloading)
Removed images for simplicity, we may want to add it in the future.
The interface is so simple that can be easily used from Java as well.

Please, feel free to fork the repo and raise a PR.