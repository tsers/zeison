import SonatypeKeys._

sonatypeSettings

name                := "Zeison"
version             := "0.5.0"
profileName         := "org.tsers"
organization        := "org.tsers.zeison"
scalaVersion        := "2.10.4"
crossScalaVersions  := Seq("2.10.4", "2.11.4")

scalacOptions += "-target:jvm-1.6"

libraryDependencies += "org.spire-math" %% "jawn-parser" % "0.7.1"

libraryDependencies += "org.scalatest" %% "scalatest" % "2.2.2" % "test"


credentials += Credentials("Sonatype Nexus Repository Manager",
                           "oss.sonatype.org",
                           sys.env.getOrElse("SONATYPE_USERNAME", ""),
                           sys.env.getOrElse("SONATYPE_PASSWD", ""))

publishMavenStyle       := true
pomIncludeRepository    := { _ => false }
publishArtifact in Test := false

publishTo := {
  val nexus = "https://oss.sonatype.org/"
  isSnapshot.value match {
    case true  => Some("snapshots" at nexus + "content/repositories/snapshots")
    case false => Some("releases" at nexus + "service/local/staging/deploy/maven2")
  }
}

pomExtra := (
  <url>https://github.com/milankinen/zeison</url>
    <licenses>
      <license>
        <name>MIT</name>
        <url>http://opensource.org/licenses/MIT</url>
        <distribution>repo</distribution>
      </license>
    </licenses>
    <scm>
      <url>git@github.com:milankinen/zeison.git</url>
      <connection>scm:git:git@github.com:milankinen/zeison.git</connection>
    </scm>
    <developers>
      <developer>
        <id>milankinen</id>
        <name>Matti Lankinen</name>
        <url>https://github.com/milankinen</url>
      </developer>
    </developers>
  )
