addSbtPlugin("org.scala-js" % "sbt-scalajs" % "1.20.1")

addSbtPlugin("org.scalablytyped.converter" % "sbt-converter" % "1.0.0-beta44")

addSbtPlugin("org.portable-scala"          % "sbt-scalajs-crossproject" % "1.0.0")
addSbtPlugin("org.scalablytyped.converter" % "sbt-converter" % "1.0.0-beta44")
// sbt plugin to load environment variables from .env into the JVM System Environment for local development.
addSbtPlugin("nl.gn0s1s" % "sbt-dotenv" % "3.0.1")
// sbt plugin for packaging JVM applications
addSbtPlugin("org.xerial.sbt" % "sbt-pack" % "0.17")

addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "2.1.1") // so we can build a sharable JAR later