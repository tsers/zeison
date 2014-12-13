package org.tsers.zeison

class RenderingSpec extends BaseSpec {
  import Zeison._

  describe("JSON rendering") {
    it("renders strings in a compact form") {
      val jsonStr = render(obj(
        "arr" -> arr(1, 2, 5),
        "foo" -> "bar"
      ))

      jsonStr should equal("""{"arr":[1,2,5],"foo":"bar"}""")
    }
  }

}
