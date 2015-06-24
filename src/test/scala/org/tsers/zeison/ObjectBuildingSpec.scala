package org.tsers.zeison

class ObjectBuildingSpec extends BaseSpec {
  import Zeison._

  describe("JSON object building") {
    it("supports JSON basic types") {
      val json = toJson(Map(
        "foo"   -> "bar",
        "bool"  -> false,
        "value" -> 123
      ))

      json should equal(parse(
        """
          | {
          |   "foo": "bar",
          |   "bool": false,
          |   "value": 123
          | }
        """.stripMargin))
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
  }

}
