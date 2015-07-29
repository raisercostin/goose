import sbt._

organization := "com.intenthq"

name := "gander"

version := "0.1-SNAPSHOT"

description := "Extracts text, metadata from web pages."

licenses += "Apache2" -> url("http://www.apache.org/licenses/")

scalaVersion := "2.11.6"

resolvers += "scalaz-bintray" at "http://dl.bintray.com/scalaz/releases"

Defaults.itSettings

testOptions in Test += Tests.Argument("-oF")

libraryDependencies ++= Seq(
  "org.slf4j"	% "slf4j-api"	% "1.7.7",
  "org.slf4j"	% "slf4j-log4j12"	% "1.7.7" % Test,
  "log4j" % "log4j" % "1.2.14",
  "commons-io" % "commons-io" % "2.4",
  "commons-lang" % "commons-lang" % "2.6",
  "org.apache.httpcomponents" % "httpclient" % "4.3.3",
  "com.ibm.icu" % "icu4j" % "53.1",
  "me.champeau.jlangdetect" % "jlangdetect-extra" % "0.4",
  "org.jsoup" % "jsoup" % "1.8.2",
  "net.liftweb" %% "lift-json" % "2.6-RC1",
  "com.github.nscala-time" %% "nscala-time" % "1.4.0",
  "com.typesafe" % "config" % "1.0.2",
  "com.jsuereth" %% "scala-arm" % "1.4",
  "com.chenlb.mmseg4j" % "mmseg4j-core" % "1.9.1",
  "com.googlecode.juniversalchardet" % "juniversalchardet" % "1.0.3",
  //add json service ,
  "com.fasterxml.jackson.core" % "jackson-databind" % "2.4.3",
  "com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.4.3",
  "org.simpleframework" % "simple" % "4.1.21",
  //tests
  "junit" % "junit" % "4.11" % Test,
  "org.specs2" %% "specs2-core" % "3.6" % "it,test"
)

scalacOptions ++= Seq("-unchecked", "-deprecation")

lazy val root = project.in(file(".")).configs(IntegrationTest)