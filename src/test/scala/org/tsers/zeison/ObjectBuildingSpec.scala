package org.tsers.zeison

import java.lang.{Float, Double}

class ObjectBuildingSpec extends BaseSpec {
  import Zeison._

  describe("JSON object building") {
    it("supports JSON basic types") {
      val json = toJson(Map(
        "str"    -> "tsers",
        "bool"   -> false,
        "char"   -> 'a',
        "byte"   -> Byte.box(2),
        "short"  -> Short.box(3),
        "int"    -> 123,
        "double" -> 123.2d,
        "float"  -> 1.0f,
        "jDbl"   -> new Double(12.2),
        "jFlt"   -> new Float(1.0),
        "big"    -> BigDecimal(123.123),
        "null"   -> null
      ))

      json should equal(parse(
        """
          | {
          |   "str": "tsers",
          |   "bool": false,
          |   "char": "a",
          |   "byte": 2,
          |   "short": 3,
          |   "int": 123,
          |   "double": 123.2,
          |   "float": 1.0,
          |   "jDbl": 12.2,
          |   "jFlt": 1.0,
          |   "big": 123.123,
          |   "null": null
          | }
        """.stripMargin))
    }

    it("supports options") {
      toJson(None) should not be 'defined
      val int = toJson(Some(1))
      render(int) should equal("1")
      val obj = toJObject("exists" -> Some("tsers"), "not" -> None)
      render(obj) should equal("""{"exists":"tsers"}""")
      val arr = toJArray(1, Some("tsers"), None)
      render(arr) should equal("""[1,"tsers"]""")
    }

    it("supports nested objects and arrays") {
      val json = toJson(Map(
        "nested" -> toJson(Map("foo" -> "bar")),
        "array"  -> toJson(Seq(1, 2, Map("bar" -> "foo")))
      ))

      json should equal(parse(
        """
          | {
          |   "nested": { "foo": "bar" },
          |   "array": [ 1, 2, {"bar": "foo"} ]
          | }
        """.stripMargin))
    }

    it("supports building objects from Scala maps") {
      val json = toJson(Map(
        "foo" -> "bar",
        "int" -> 1
      ))

      json should equal(parse(
        """
          | {
          |   "foo": "bar",
          |   "int": 1
          | }
        """.stripMargin))
    }

    it("supports building arrays from Scala iterables") {
      val json = toJson(Seq(1, "foobar"))
      json should equal(parse(
        """
          | [ 1, "foobar" ]
        """.stripMargin))
    }

    it("supports building arrays from JVM arrays") {
      val json = toJson(Array(1, "foobar"))
      json should equal(parse(
        """
          | [ 1, "foobar" ]
        """.stripMargin))
    }

    it("throws an exception if trying to build from unsupported values") {
      intercept[ZeisonException] {
        toJson(Seq("(regex)?".r))
      }
    }
  }

}
