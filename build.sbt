name := "c3po"

organization in ThisBuild := "com.petpet"

scalaVersion in ThisBuild := "2.11.8"

lazy val root = project.in(file("."))
  .dependsOn(c3pocore, c3poapi, c3pocmd, c3powebapi)
  .aggregate(c3pocore, c3poapi, c3pocmd, c3powebapi)

// This is common, non-Play code
lazy val c3poapi = project

// This is common, non-Play code
lazy val c3pocore = project

// This is common, non-Play code
lazy val c3pocmd = project

lazy val c3powebapi = project