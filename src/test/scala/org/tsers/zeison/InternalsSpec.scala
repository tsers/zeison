package org.tsers.zeison


class InternalsSpec extends BaseSpec {
  import Zeison._

  describe("FieldMap") {
    val tsers = JString("tsers")
    val bar   = JString("bar")
    it("supports immutable operations") {
      val javaMap = new java.util.LinkedHashMap[String, JValue]()
      javaMap.put("msg", tsers)
      val fields = new internal.FieldMap(javaMap)
      fields should equal(Map("msg" -> tsers))

      val withFoo = fields ++ Map("foo" -> bar)
      withFoo should equal(Map("msg" -> tsers, "foo" -> bar))
      fields should equal(Map("msg" -> tsers))

      val withoutMsg = fields -- Set("msg")
      withoutMsg should equal(Map.empty)
      fields should equal(Map("msg" -> tsers))
    }
  }

  describe("JParsedNum") {
    it("is comparable with JDouble and JInt") {
      JParsedNum("2") should equal(JInt(2))
      JParsedNum("2.2") should equal(JDouble(2.2))
      JParsedNum("1") should not equal JString("1")
    }
  }
}
