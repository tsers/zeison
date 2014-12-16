package org.tsers.zeison

import org.tsers.zeison.Zeison.JValue

class RenderingSpec extends BaseSpec {
  import org.tsers.zeison.{Zeison => json}

  def buildTestSet(lvl: Int): Seq[JValue] = Seq(
    json.from(null),
    json.from(true),
    json.from(false),
    json.from(1),
    json.from(-1),
    json.from(1337.1337),
    json.from("\"tsers\""),
    json.from("tsers?\ntsers!")
  ) ++ (if (lvl > 0) buildTestSet(lvl - 1).flatMap(v => Seq(json.arr(v), json.arr(v, v))) else Nil) ++
    (if (lvl > 0) buildTestSet(lvl - 1).flatMap(v => Seq(json.obj("foo" -> v), json.obj("foo" -> v, "bar" -> v))) else Nil)

  val testSet = buildTestSet(3)

  describe("JSON rendering") {
    it("renders compact json correctly") {
      val rendered = testSet.map(json.render)
      val parsed   = rendered.map(json.parse)
      parsed should equal(testSet)
    }
    it("renders pretty json correctly") {
      val rendered = testSet.map(json.renderPretty)
      val parsed   = rendered.map(json.parse)
      parsed should equal(testSet)
    }
  }

}
