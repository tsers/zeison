package org.tsers.zeison

class ObjectBuildingSpec extends BaseSpec {

  describe("JSON object building") {
    it("supports JSON basic types") {
      val json = obj(
        "foo"   -> "bar",
        "bool"  -> false,
        "value" -> 123
      )

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
      val json = obj(
        "nested" -> obj("foo" -> "bar"),
        "array"  -> arr(1, 2, obj("bar" -> "foo"))
      )

      json should equal(parse(
        """
          | {
          |   "nested": { "foo": "bar" },
          |   "array": [ 1, 2, {"bar": "foo"} ]
          | }
        """.stripMargin))
    }

    it("supports building objects from Scala maps") {
      val json = obj.from(Map(
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
      val json = arr.from(Seq(1, "foobar"))
      json should equal(parse(
        """
          | [ 1, "foobar" ]
        """.stripMargin))
    }
  }

}
