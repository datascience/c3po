import sbt._
import Keys._

object ApplicationBuild extends Build {
    val appName         = "c3po"
    val appVersion      = "0.5.0"
    val appDependencies = Seq(
      javaCore,
      javaJdbc,
      javaEbean,
      "dom4j" % "dom4j" % "1.6.1",
      "org.apache.commons" % "commons-digester3" % "3.2",
      "org.apache.commons" % "commons-math" % "2.2",
      "org.mongodb" % "mongo-java-driver" % "3.4.0",
      "com.typesafe.play.plugins" %% "play-plugins-mailer" % "2.3.1"
    )

    val main = Project(appName, file(".")).enablePlugins(play.PlayJava).settings(
      version := Pom.version(baseDirectory.value),
      libraryDependencies ++=appDependencies,
      libraryDependencies ++= Pom.dependencies(baseDirectory.value).filterNot(d => d.name == core.id))
    ).settings(
      scalaVersion := "2.11"
    )
}
