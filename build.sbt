name := "gitbucket-push-rules-plugin"
organization := "io.github.takahino"
version := "1.0.1"
scalaVersion := "2.13.18"
gitbucketVersion := "4.46.1"

libraryDependencies ++= Seq(
  "org.scalatest" %% "scalatest" % "3.2.19" % Test
)

scalacOptions ++= Seq("-deprecation", "-feature")
