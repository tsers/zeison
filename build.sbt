name                := "Zeison"
version             := "0.8.0-SNAPSHOT"
organization        := "org.tsers.zeison"
scalaVersion        := "2.12.4"
crossScalaVersions  := Seq("2.10.7", "2.11.12", "2.12.4")

scalacOptions += "-target:jvm-1.7"

libraryDependencies += "org.spire-math" %% "jawn-parser" % "0.11.1"

libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.3" % "test"

publishArtifact in Test := false

publishTo := sonatypePublishTo.value
