name                := "Zeison"
version             := "0.7.0"
organization        := "org.tsers.zeison"
scalaVersion        := "2.12.4"
crossScalaVersions  := Seq("2.10.7", "2.11.12", "2.12.4")

scalacOptions += "-target:jvm-1.6"

libraryDependencies += "org.spire-math" %% "jawn-parser" % "0.11.1"

libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.3" % "test"


credentials += Credentials("Sonatype Nexus Repository Manager",
                           "oss.sonatype.org",
                           sys.env.getOrElse("SONATYPE_USERNAME", ""),
                           sys.env.getOrElse("SONATYPE_PASSWD", ""))

publishArtifact in Test := false

publishTo := sonatypePublishTo.value
