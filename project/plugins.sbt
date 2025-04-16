addSbtPlugin("org.scala-js" % "sbt-scalajs" % "1.16.0")

addSbtPlugin("org.scalablytyped.converter" % "sbt-converter" % "1.0.0-beta44")

addSbtPlugin("org.portable-scala"          % "sbt-scalajs-crossproject" % "1.0.0")
addSbtPlugin("org.scalablytyped.converter" % "sbt-converter" % "1.0.0-beta44")
// sbt plugin to load environment variables from .env into the JVM System Environment for local development.
addSbtPlugin("nl.gn0s1s" % "sbt-dotenv" % "3.0.1")