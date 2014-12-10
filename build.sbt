import play.PlayImport.PlayKeys._

name := """Termasaurus"""

version := "0.1.0"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

routesImport += "models._"

scalaVersion := "2.11.4"

libraryDependencies ++= Seq(
  jdbc,
  cache,
  ws,
  "com.typesafe.akka" %% "akka-actor" % "2.3.7",
  "mysql" % "mysql-connector-java" % "5.1.34",
  "com.typesafe.play" %% "play-slick" % "0.8.0",
  "joda-time" % "joda-time" % "2.4",
  "org.joda" % "joda-convert" % "1.6",
  "com.github.tototoshi" %% "slick-joda-mapper" % "1.2.0"
)

