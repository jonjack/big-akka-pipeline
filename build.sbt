name := "cargo"

version := "0.1-SNAPSHOT"

scalaVersion := "2.12.3"

lazy val akkaVersion = "2.5.4"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % akkaVersion,
  "com.typesafe.akka" %% "akka-stream" % akkaVersion,
  "com.typesafe.akka" %% "akka-testkit" % akkaVersion,
  "org.scalatest" %% "scalatest" % "3.0.1" % "test",
  "net.databinder.dispatch" %% "dispatch-core" % "0.13.2",
  "net.databinder.dispatch" %% "dispatch-json4s-native" % "0.13.2",
  "com.typesafe.akka" %% "akka-http-spray-json" % "10.0.10"
)
