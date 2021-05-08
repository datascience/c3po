name := "c3po-api"

version := "0.6"

libraryDependencies ++= Seq(
        "org.slf4j" % "slf4j-api" % "2.0.0-alpha1",
        "commons-io" % "commons-io" % "2.8.0",
        "org.assertj" % "assertj-core" % "3.12.2" % Test,
        "org.awaitility" % "awaitility" % "3.1.6" % Test,
        "com.novocode" % "junit-interface" % "0.10" % "test"
      )