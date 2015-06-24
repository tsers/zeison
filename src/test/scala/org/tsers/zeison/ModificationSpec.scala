package org.tsers.zeison

class ModificationSpec extends BaseSpec {
  import org.tsers.zeison.Zeison._

  describe("JSON object modification") {
    it("supports adding new fields with given order") {
      val original = toJson(Map("msg" -> "tsers"))
      val modified = original.copy(
        "num" -> 123,
        "foo" -> "bar"
      )
      modified should equal(toJson(Map(
        "msg" -> "tsers",
        "num" -> 123,
        "foo" -> "bar"
      )))
    }

    it("supports existing field modification") {
      val original = toJson(Map("msg" -> "foobar", "num" -> 123))
      val modified = original.copy("msg" -> "tsers")
      modified should equal(toJson(Map("msg" -> "tsers", "num" -> 123)))
    }

    it("supports field removal") {
      val original = toJson(Map("msg" -> "tsers", "num" -> 123))
      val modified = original.copy("num" -> JUndefined)
      modified should equal(toJson(Map("msg" -> "tsers")))
    }

    it("supports only JSON objects") {
      val arr = toJArray("1", "2", "3")
      intercept[ZeisonException] {
        arr.copy("1" -> "10")
      }
    }
  }

}
