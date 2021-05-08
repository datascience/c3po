name := "c3po-cmd"

version := "0.6"



lazy val c3po-cmd = (project in file(".")).settings(commonSettings)
  .dependsOn(c3poc-ore)

lazy val c3po-core = project.in(file("../c3po-core"))

  val commonSettings = Seq(
    assemblyMergeStrategy in assembly := {
      case PathList("reference.conf") => MergeStrategy.concat
      case PathList("META-INF", xs @ _*) => MergeStrategy.discard
      case x => MergeStrategy.first
      },
    publishArtifact in (Compile, packageDoc) := false,
    publishArtifact in packageDoc := false,
    sources in(Compile, doc) := Seq.empty,
    scalaVersion := "2.11.8",
    testOptions in Test := Seq(Tests.Argument(TestFrameworks.JUnit, "-a", "-v"))
  )


libraryDependencies ++= Seq(
      "com.beust" % "jcommander" % "1.30",
      "org.assertj" % "assertj-core" % "3.12.2" % Test,
      "org.awaitility" % "awaitility" % "3.1.6" % Test,
      "com.novocode" % "junit-interface" % "0.10" % "test"
    )