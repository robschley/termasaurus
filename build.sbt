import play.PlayImport.PlayKeys._

name := """Termasaurus"""

version := "0.1.0"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

routesImport += "models._"

scalaVersion := "2.11.4"

resolvers ++= Seq(
  "RoundEights" at "http://maven.spikemark.net/roundeights",
  "jBCrypt Repository" at "http://repo1.maven.org/maven2/org/"
)

pipelineStages := Seq(digest)

libraryDependencies ++= Seq(
  jdbc,
  cache,
  ws,
  "com.typesafe.akka" %% "akka-actor" % "2.3.7",
  "mysql" % "mysql-connector-java" % "5.1.34",
  "com.typesafe.play" %% "play-slick" % "0.8.0",
  "com.roundeights" %% "hasher" % "1.0.0",
  "org.mindrot" % "jbcrypt" % "0.3m",
  "commons-codec" % "commons-codec" % "1.10",
  "joda-time" % "joda-time" % "2.4",
  "org.joda" % "joda-convert" % "1.6",
  "com.github.tototoshi" %% "slick-joda-mapper" % "1.2.0",
  "org.webjars" %% "webjars-play" % "2.3.0-2",
  "org.webjars" % "html5shiv" % "3.7.2",
  "org.webjars" % "es5-shim" % "4.0.3",
  "org.webjars" % "bootstrap" % "3.3.1",
  "org.webjars" % "angularjs" % "1.3.6",
  "org.webjars" % "angular-ui-router" % "0.2.13",
  "org.webjars" % "restangular" % "1.4.0-2",
  "org.webjars" % "lodash" % "2.4.1-6"
)
