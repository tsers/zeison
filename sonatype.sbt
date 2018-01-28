sonatypeProfileName := "org.tsers"

publishMavenStyle := true

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