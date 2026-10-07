val scala3Version = "3.9.0"

ThisBuild / scalaVersion := scala3Version
ThisBuild / version      := "0.1.0-SNAPSHOT"

lazy val commonSettings = Seq(
  scalacOptions ++= Seq(
    "-Wvalue-discard",
    "-Wnonunit-statement",
    "-Wconf:msg=(unused.*value|discarded.*value|pure.*statement):error",
    "-language:strictEquality"
  ),
  // Kyo's scheduler is a JVM-wide singleton; running tests in sbt's layered classloaders makes
  // it log LinkageErrors. A forked JVM keeps the output clean.
  Test / fork := true,
  run / fork  := true,
  libraryDependencies ++= Dependencies.core,
  libraryDependencies += "org.scalameta" %% "munit" % "1.0.0" % Test
)

// Specs shared by `exercises` (red until solved) and `solutions` (always green).
lazy val sharedTests = Seq(
  Test / unmanagedSourceDirectories += baseDirectory.value / ".." / "tests" / "src" / "test" / "scala"
)

lazy val root = project
  .in(file("."))
  .aggregate(lessons, exercises, solutions)
  .settings(name := "kyo-courses")

lazy val lessons = project
  .in(file("modules/lessons"))
  .settings(commonSettings)

lazy val exercises = project
  .in(file("modules/exercises"))
  .dependsOn(lessons)
  .settings(commonSettings, sharedTests)

lazy val solutions = project
  .in(file("modules/solutions"))
  .dependsOn(lessons)
  .settings(commonSettings, sharedTests)
