package org.tsers.zeison

class RenderingSpec extends BaseSpec {
  import org.tsers.zeison.Zeison._

  def buildTestSet(lvl: Int): Seq[JValue] = Seq(
    toJson(null),
    toJson(true),
    toJson(false),
    toJson(1),
    toJson(-1),
    toJson(1337.1337),
    toJson("\"tsers\""),
    toJson("tsers?\ntsers!"),
    toJson(specialChars)
  ) ++ (if (lvl > 0) buildTestSet(lvl - 1).flatMap(v => Seq(toJson(Seq(v)), toJson(Seq(v, v)))) else Nil) ++
    (if (lvl > 0) buildTestSet(lvl - 1).flatMap(v => Seq(toJson(Map("foo" -> v)), toJson(Map("foo" -> v, "bar" -> v)))) else Nil)

  val testSet = buildTestSet(3)

  describe("JSON rendering") {
    it("renders special characters correctly") {
      // using + because \u0081 is not escaped with """ by scala compiler (bug in Scala??)
      render(toJArray("aA\"\n\r\t\b\f\\/\u0081")) should equal("""["aA\"\n\r\t\b\f\\\/\""" + """u0081"]""")
    }
    it("renders compact json correctly") {
      val rendered = testSet.map(render)
      val parsed   = rendered.map(parse)
      parsed should equal(testSet)
    }
    it("renders pretty json correctly") {
      val rendered = testSet.map(renderPretty)
      val parsed   = rendered.map(parse)
      parsed should equal(testSet)
    }
    it("does not support rendering JUndefined") {
      intercept[ZeisonException] {
        println("asd:" + render(JUndefined))
      }
    }
  }

  describe("toString") {
    it("supports all JSON types") {
      JUndefined.toString() should equal("JUndefined")
      JNull.toString() should equal("JNull")
      JBoolean(true).toString() should equal("JBoolean(true)")
      JInt(1).toString() should equal("JInt(1)")
      JDouble(.2).toString() should equal("JDouble(0.2)")
      JString("tsers").toString() should equal("JString(tsers)")
      JArray(Vector.empty).toString should equal("JArray(Vector())")
      JObject(Map.empty).toString() should equal("JObject(Map())")
    }
  }

  def specialChars: String = {
    val ulist = 0x8232.toChar :: 0xFFF9.toChar :: 0x200E.toChar :: (1.toChar to 3000.toChar).toList
    "\n\r\t\b\f\\\"" + scala.util.Random.shuffle( ulist ).mkString
  }
}
