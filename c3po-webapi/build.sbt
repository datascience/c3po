name := "c3po-webapi"

version := "0.6"


lazy val c3po-core = (project in file("."))
  .enablePlugins(PlayJava)
  .dependsOn(c3po-core)

lazy val c3po-core = project.in(file("../c3po-core"))



libraryDependencies ++= Seq(
      "com.typesafe.play" %% "play-mailer" % "3.0.1",

      "org.assertj" % "assertj-core" % "3.12.2" % Test,
      "org.awaitility" % "awaitility" % "3.1.6" % Test,
      "com.novocode" % "junit-interface" % "0.10" % "test"
    )


