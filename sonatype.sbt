useGpg := true

useGpgAgent := true

usePgpKeyHex("9DD8C3E9")

sonatypeProfileName := "org.tsers"

publishMavenStyle := true

credentials += Credentials("Sonatype Nexus Repository Manager",
  "oss.sonatype.org",
  sys.env.getOrElse("SONATYPE_USERNAME", ""),
  sys.env.getOrElse("SONATYPE_PASSWD", ""))

licenses := Seq("MIT" -> url("https://opensource.org/licenses/MIT"))

homepage := Some(url("https://github.com/tsers/zeison"))

scmInfo := Some(
  ScmInfo(
    url("https://github.com/tsers/zeison"),
    "scm:git@github.com:tsers/zeison.git"
  )
)

developers := List(
  Developer(id="milankinen", name="Matti Lankinen", email="m.lankinen@iki.fi", url=url("https://github.com/milankinen"))
)