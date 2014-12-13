package org.tsers.zeison


class ComparisonSpec extends BaseSpec {
  import Zeison._

  describe("JValue comparison") {
    it("is possible by using equality operators") {
      val json1 = parse("""
          | {
          |   "arr": [1, 2, 3.2],
          |   "status": false,
          |   "title": "foo",
          |   "nullable": null,
          |   "nested": { "foo": "bar" }
          | }
        """.stripMargin)

      val json2 = parse(
        """
          | {
          |   "arr": [1, 2, 3.2],
          |   "title": "foo",
          |   "nullable": null,
          |   "status": false,
          |   "nested": { "foo": "bar" }
          | }
        """.stripMargin)

      val json3 = parse(
        """
          | {
          |   "arr": [1, 2, 3.2],
          |   "title": "foo",
          |   "nullable": null,
          |   "status": false
          | }
        """.stripMargin)

      (json1 == json2) should equal(true)
      (json1 == json3) should equal(false)
      (json2 != json3) should equal(true)
    }
  }

}
