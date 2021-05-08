name := "c3po-core"

version := "0.6"


lazy val c3po-core = (project in file("."))
  .dependsOn(c3po-api)

lazy val c3po-api = project.in(file("../c3po-api"))



libraryDependencies ++= Seq(
      "com.opencsv" % "opencsv" % "5.2",
      "com.fasterxml.jackson.core" % "jackson-databind" % "2.12.3",
      "com.google.code.gson" % "gson" % "2.3.1",
      "org.mongodb" % "mongo-java-driver" % "3.4.0",
      "org.apache.commons" % "commons-compress" % "1.20",
      "dom4j" % "dom4j" % "1.6.1",
      "org.simpleframework" % "simple-xml" % "2.7.1",
      "log4j" % "log4j" % "1.2.17",
      "junit" % "junit" % "4.13.2",
      "org.mockito" % "mockito-all" % "2.0.2-beta",
      "org.assertj" % "assertj-core" % "3.12.2" % Test,
      "org.awaitility" % "awaitility" % "3.1.6" % Test,
      "com.novocode" % "junit-interface" % "0.10" % "test"
    )


