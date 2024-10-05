import sbt.Keys.{libraryDependencies, scalaVersion, version}
import sbtassembly.MergeStrategy

assemblyMergeStrategy in assembly := {
  case PathList("META-INF", xs @ _*) => MergeStrategy.discard
  case "reference.conf"              => MergeStrategy.concat
  case "application.conf"            => MergeStrategy.concat
  case "git.properties"              => MergeStrategy.first
  case "module-info.class"           => MergeStrategy.discard
  case x                             => MergeStrategy.first
}
lazy val root = (project in file("."))
  .settings(
    name := "CSE512-Hotspot-Analysis-Template",
    version := "0.1.0",
    scalaVersion := "2.12.11", // Use a compatible Scala version for Spark 3.x
    organization := "org.datasyslab",
    publishMavenStyle := true,
    mainClass in Compile := Some(
      "cse512.Entrance"
    ) // Ensure the main class is correctly specified
  )

// Library dependencies for Spark, ScalaTest, and Specs2
libraryDependencies ++= Seq(
  "org.apache.spark" %% "spark-core" % "3.0.1", // Use Spark 3.x for better compatibility
  "org.apache.spark" %% "spark-sql" % "3.0.1",
  "org.scalatest" %% "scalatest" % "3.0.4" % Test, // ScalaTest for unit tests
  "org.specs2" %% "specs2-core" % "4.10.6" % Test, // Updated Specs2 version
  "org.specs2" %% "specs2-junit" % "4.10.6" % Test // Updated Specs2 version
)

// Add the scala library explicitly, though it should be included by default with sbt
libraryDependencies += "org.scala-lang" % "scala-library" % "2.12.11"
