name                := "Zeison"
version             := "0.8.0"
organization        := "org.tsers.zeison"
scalaVersion        := "2.12.12"
crossScalaVersions  := Seq("2.12.12")

libraryDependencies += "org.typelevel" %% "jawn-parser" % "1.0.0"

libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.8" % "test"

publishArtifact in Test := false

publishTo := sonatypePublishTo.value
