package org.tsers.zeison


class InternalsSpec extends BaseSpec {
  import Zeison._
  describe("JParsedNum") {
    it("is comparable with JDouble and JInt") {
      JParsedNum("2") should equal(JInt(2))
      JParsedNum("2.2") should equal(JDouble(2.2))
      JParsedNum("1") should not equal JString("1")
    }
  }
}
