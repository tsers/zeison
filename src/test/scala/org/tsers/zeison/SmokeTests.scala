package org.tsers.zeison

import java.text.SimpleDateFormat
import java.util.{TimeZone, Date}

import org.scalatest.FunSuite

import scala.util.Try

class SmokeTests extends FunSuite {
  import org.tsers.zeison.{Zeison => json}

  test("smoke tests") {

    val data = json.parse( """
                        | {
                        |   "messages": ["tsers", {"msg": "tsers!"}],
                        |   "meta": {
                        |     "numKeys": 2,
                        |     "active": true,
                        |     "score": 0.6
                        |   },
                        |   "response": null
                        | }""".stripMargin)

    // conversions
    assert(data.meta.numKeys.toInt == 2)
    assert(data.meta.active.toBool == true)
    assert(data.meta.score.toDouble == 0.6)
    assert(data.meta.toMap.get("numKeys").map(_.toInt) == Some(2))
    assert(data.messages.toSeq.head.toStr == "tsers")

    // type checking
    assert(data.meta.numKeys.isInt == true) // also .isStr .isBool .isDouble .isArray .isObject .isNull .isDefined

    // traversing
    assert(data("meta")("numKeys").toInt == 2)
    assert(data.messages(0).toStr == "tsers")
    assert(data.messages(1).msg.toStr == "tsers!")
    assert(data.messages.filter(_.isObject).map(_.msg.toStr).toSeq == Seq("tsers!"))

    // undefined values
    assert(data.meta.numKeys.isDefined == true)
    assert(data.non_existing.isDefined == false)
    assert(data.messages(-1).isDefined == false)
    assert(data.messages(10).isDefined == false)
    assert(data.meta.numKeys.toOption.map(_.toInt) == Some(2))
    assert(data.non_existing.toOption == None)
    assert(data.response.toOption == None)

    // exceptions
    assert(Try(data.messages.toInt).isSuccess == false) // bad type cast
    assert(Try(data.non_existing.toInt).isSuccess == false) // undefined has no value
    assert(Try(data.non_existing.sub_field).isSuccess == false)
    // undefined has no member x


    val src = json.parse( """
                       | {
                       |   "meta": {
                       |     "numKeys": 2
                       |   },
                       |   "response": null
                       | }""".stripMargin)

    // building objects with obj/arr
    assert(json.render(json.obj("msg" -> "tsers!", "meta" -> src.meta)) == """{"msg":"tsers!","meta":{"numKeys":2}}""")
    assert(json.render(json.arr(1, json.obj("msg" -> "tsers!"))) == """[1,{"msg":"tsers!"}]""")

    // building objects from Scala collections
    val primes = Seq(1, 2, 3, 5)
    assert(json.render(json.from(primes)) == "[1,2,3,5]")
    val config = Map("version" -> 2)
    assert(json.render(json.from(config)) == """{"version":2}""")

    // custom types building and rendering
    val now = new Date()
    val customJson = json.obj("createdAt" -> JDate(now))
    assert(json.render(customJson) == s"""{"createdAt":"${toISO8601(now)}"}""")

    // custom types type checking and extraction
    assert(customJson.createdAt.is[Date])
    assert(customJson.createdAt.to[Date] == now)
    assert(!customJson.createdAt.isInt)
    assert(Try(customJson.createdAt.toInt).isFailure)
  }

  def toISO8601(date: Date) = {
    val sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssz") { setTimeZone(TimeZone.getTimeZone("UTC")) }
    sdf.format(date).replaceAll("UTC$", "+00:00")
  }
  case class JDate(value: Date) extends Zeison.JCustom {
    override def valueAsJson: String = "\"" + toISO8601(value) + "\""
  }

}
