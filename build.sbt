import sbt._

organization := "com.intenthq"

name := "gander"

version := "0.1-SNAPSHOT"

description := "Extracts text, metadata from web pages."

licenses += "Apache2" -> url("http://www.apache.org/licenses/")

scalaVersion := "2.11.7"

resolvers += "scalaz-bintray" at "http://dl.bintray.com/scalaz/releases"

Defaults.itSettings

testOptions in Test += Tests.Argument("-oF")

libraryDependencies ++= Seq(
  "org.slf4j"	% "slf4j-api"	% "1.7.12",
  "commons-lang" % "commons-lang" % "2.6",
  "com.google.guava" % "guava" % "18.0",
  "org.jsoup" % "jsoup" % "1.8.2",
  "joda-time" % "joda-time" % "2.8.1",
  "com.chenlb.mmseg4j" % "mmseg4j-core" % "1.10.0",
  "junit" % "junit" % "4.12" % Test,
  "org.specs2" %% "specs2-core" % "3.6.3" % "it,test"
)

scalacOptions ++= Seq("-unchecked", "-deprecation")

lazy val root = project.in(file(".")).configs(IntegrationTest)