name                := "Zeison"
version             := "0.8.0"
organization        := "org.tsers.zeison"
scalaVersion        := "2.12.4"
crossScalaVersions  := Seq("2.10.7", "2.11.12", "2.12.4")

libraryDependencies += "org.typelevel" %% "jawn-parser" % "1.0.0"

libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.8" % "test"

publishArtifact in Test := false

publishTo := sonatypePublishTo.value
