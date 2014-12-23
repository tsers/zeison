package org.tsers.zeison

import org.tsers.zeison.Zeison.JUndefined

class ModificationSpec extends BaseSpec {
  import org.tsers.zeison.{Zeison => json}

  describe("JSON object modification") {
    it("supports adding new fields with given order") {
      val original = json.obj("msg" -> "tsers")
      val modified = original.copy(
        "num" -> 123,
        "foo" -> "bar"
      )
      modified should equal(json.obj(
        "msg" -> "tsers",
        "num" -> 123,
        "foo" -> "bar"
      ))
    }

    it("supports existing field modification") {
      val original = json.obj("msg" -> "foobar", "num" -> 123)
      val modified = original.copy("msg" -> "tsers")
      modified should equal(json.obj("msg" -> "tsers", "num" -> 123))
    }

    it("supports field removal") {
      val original = json.obj("msg" -> "tsers", "num" -> 123)
      val modified = original.copy("num" -> JUndefined)
      modified should equal(json.obj("msg" -> "tsers"))
    }
  }

}
