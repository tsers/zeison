package org.tsers.zeison

import java.text.SimpleDateFormat
import java.util.Date

class CustomTypesSpec extends BaseSpec {
  import Zeison._

  case class JDate(value: Date) extends JCustom {
    override def valueAsJson: String = {
      val sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss")
      "\"" + sdf.format(value) + "\""
    }
  }

  def parseDate(str: String) = {
    val sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm")
    sdf.parse(str)
  }

  describe("Custom types") {

    it("can be be added to built JSON object and rendered correctly") {
      val json = toJson(Map("date" -> JDate(parseDate("2011-11-11 11:11"))))
      render(json) should equal("""{"date":"2011-11-11T11:11:00"}""")
    }

    it("can be tested from the built JSON") {
      val date = JDate(parseDate("2011-11-11 11:11"))
      val json = toJson(Map("date" -> date, "num" -> 2))

      json.date.isInt should equal(false)
      json.date.is[Date] should equal(true)
      json.num.isInt should equal(true)
      json.num.is[Date] should equal(false)
    }

    it("can be extracted from the built JSON") {
      val date = JDate(parseDate("2011-11-11 11:11"))
      val json = toJson(Map("date" -> date))

      json.date.to[Date] should equal(date.value)
    }

    it("throw an exception if wrong type is extracted from the built JSON") {
      val json = toJson(Map("date" -> JDate(parseDate("2011-11-11 11:11"))))

      intercept[ZeisonException] {
        json.date.toInt
      }
      intercept[ZeisonException] {
        json.date.to[java.math.BigDecimal]
      }
    }
  }

}
