package org.tsers.zeison

import org.tsers.zeison.Zeison.JValue

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
    toJson("tsers?\ntsers!")
  ) ++ (if (lvl > 0) buildTestSet(lvl - 1).flatMap(v => Seq(toJson(Seq(v)), toJson(Seq(v, v)))) else Nil) ++
    (if (lvl > 0) buildTestSet(lvl - 1).flatMap(v => Seq(toJson(Map("foo" -> v)), toJson(Map("foo" -> v, "bar" -> v)))) else Nil)

  val testSet = buildTestSet(3)

  describe("JSON rendering") {
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
  }

}
